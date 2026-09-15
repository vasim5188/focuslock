package com.focuslock.app.service

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import com.focuslock.app.data.db.BlockedApp
import com.focuslock.app.data.db.PendingWait
import com.focuslock.app.data.db.AppScheduleWindow
import com.focuslock.app.data.repository.FocusRepository
import com.focuslock.app.domain.BlockDecision
import com.focuslock.app.domain.BlockDecisionEngine
import com.focuslock.app.domain.EscalationPolicy
import com.focuslock.app.domain.ProtectionSnapshot
import com.focuslock.app.domain.ScheduleEvaluator
import com.focuslock.app.domain.WaitCalculator
import com.focuslock.app.util.TimeProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The always-running brain of protection. Keeps an in-memory snapshot of the
 * data (so accessibility events can be decided instantly), drives the overlay,
 * runs the persistent wait timer and enforces grant expiry / re-blocking.
 */
class ProtectionEngine(
    private val appContext: Context,
    private val repository: FocusRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
    private val overlayHost: OverlayWindow = OverlayHost(appContext)
) {
    private var presentationJob: Job? = null
    private var foregroundRevision = 0L
    private var actionInProgress = false

    @Volatile private var protectedApps: List<BlockedApp> = emptyList()
    @Volatile private var scheduleWindows: List<AppScheduleWindow> = emptyList()
    @Volatile private var grantsMap: Map<String, Long> = emptyMap()
    @Volatile private var pendingWaitValue: PendingWait? = null
    @Volatile private var currentForeground: String? = null
    @Volatile private var blockedEncounterShownFor: String? = null

    private val _overlayState = MutableStateFlow<OverlayState>(OverlayState.Hidden)
    val overlayState: StateFlow<OverlayState> = _overlayState.asStateFlow()

    private val _waitRemaining = MutableStateFlow(0)
    val waitRemaining: StateFlow<Int> = _waitRemaining.asStateFlow()

    @Volatile var accessibilityConnected: Boolean = false
    private var started = false

    fun start() {
        if (started) return
        started = true

        scope.launch { repository.blockedApps.collect { protectedApps = it; reevaluate() } }
        scope.launch { repository.appScheduleWindows.collect { scheduleWindows = it; reevaluate() } }
        scope.launch {
            repository.activeGrants.collect { grants ->
                grantsMap = grants.associate { it.packageName to it.expiresAt }
                reevaluate()
            }
        }
        scope.launch { repository.pendingWait.collect { pendingWaitValue = it } }

        scope.launch { tickLoop() }
    }

    private fun snapshot(): ProtectionSnapshot = ProtectionSnapshot(
        protectedPackages = protectedApps.map { it.packageName }.toSet(),
        scheduleActive = currentForeground?.let { ScheduleEvaluator.isActive(scheduleWindows, it, TimeProvider.nowLocalDateTime()) } ?: false,
        grantedUntil = grantsMap,
        deepFocusActive = false
    )

    /**
     * True only for a real telephony call. MODE_IN_COMMUNICATION is deliberately
     * NOT treated as a call: it is set by any VoIP/voice app (Discord, WhatsApp,
     * assistants), and honouring it would let a protected app bypass blocking
     * simply by holding an audio session. A genuine call puts the dialer in the
     * foreground, not a protected app, so MODE_IN_CALL is sufficient for §29.
     */
    private fun isInCall(): Boolean {
        val am = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return when (am.mode) {
            AudioManager.MODE_IN_CALL -> true
            AudioManager.MODE_CALL_SCREENING ->
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
            else -> false
        }
    }

    /** Called from the AccessibilityService whenever the foreground app changes. */
    fun onForegroundPackage(pkg: String) {
        // Transient system windows (status bar / quick settings shade) must not
        // dismiss the overlay while the user is still on the protected app.
        if (pkg in TRANSIENT_SYSTEM_PACKAGES) return

        // Our own package: the block overlay is a focusable window in this very
        // process, so showing it fires a window-state-changed event naming us.
        // Dismissing here would tear the overlay down the instant it appeared
        // (or flicker in a show/hide loop). Only our real activities dismiss it.
        if (pkg == appContext.packageName) {
            return
        }
        if (currentForeground != pkg) {
            foregroundRevision++
            presentationJob?.cancel()
            currentForeground = pkg
        }
        reevaluate()
    }

    /** Activity callbacks distinguish our real UI from our accessibility overlay. */
    fun onHostActivityResumed() {
        foregroundRevision++
        currentForeground = appContext.packageName
        blockedEncounterShownFor = null
        dismissOverlay()
    }

    private fun reevaluate() {
        val pkg = currentForeground ?: return
        if (pkg == appContext.packageName) return
        if (isInCall()) {
            dismissOverlay()
            return
        }

        when (BlockDecisionEngine.decide(pkg, snapshot(), TimeProvider.nowMillis())) {
            BlockDecision.Allow -> {
                blockedEncounterShownFor = null
                dismissOverlay()
            }
            BlockDecision.Block -> {
                val shown = when (val state = _overlayState.value) {
                    is OverlayState.Blocking -> state.packageName
                    is OverlayState.Waiting -> state.packageName
                    OverlayState.Hidden -> null
                }
                if (shown != pkg && presentationJob?.isActive != true) presentBlock(pkg)
            }
        }
    }

    private fun labelFor(pkg: String): String =
        protectedApps.firstOrNull { it.packageName == pkg }?.appLabel ?: pkg

    private fun presentBlock(pkg: String) {
        val revision = foregroundRevision
        presentationJob = scope.launch {
            // The grant flow may still be delivering a just-committed completion.
            if (repository.hasValidGrant(pkg)) return@launch
            val pending = repository.getPendingWait()
            val state = if (pending != null && pending.packageName == pkg) {
                // Wait may already be complete (e.g. after returning to the app).
                if (repository.tryCompleteWait(pkg)) {
                    dismissOverlayFor(pkg)
                    return@launch
                }
                _waitRemaining.value = WaitCalculator.remainingSeconds(
                    pending, TimeProvider.nowMillis(), TimeProvider.elapsedRealtime()
                )
                OverlayState.Waiting(pkg, labelFor(pkg))
            } else {
                if (blockedEncounterShownFor != pkg) {
                    repository.recordBlockedEncounter(pkg)
                    blockedEncounterShownFor = pkg
                }
                val count = repository.todayUnlockCount()
                val endLabel = ScheduleEvaluator
                    .currentWindowEnd(scheduleWindows, pkg, TimeProvider.nowLocalDateTime())
                    ?.let { ScheduleEvaluator.formatMinuteOfDay(it.hour * 60 + it.minute) }
                OverlayState.Blocking(
                    packageName = pkg,
                    appLabel = labelFor(pkg),
                    scheduleEndLabel = endLabel,
                    requiredWaitSeconds = EscalationPolicy.requiredWaitSeconds(count)
                )
            }
            if (revision != foregroundRevision || currentForeground != pkg ||
                BlockDecisionEngine.decide(pkg, snapshot(), TimeProvider.nowMillis()) != BlockDecision.Block ||
                isInCall()) return@launch
            _overlayState.value = state
            ensureOverlayShown()
        }
    }

    private fun ensureOverlayShown() {
        if (!overlayHost.isShowing) {
            overlayHost.show(
                onBackPressed = { onGoBack() },
                content = { OverlayRoot(engine = this) }
            )
        }
    }

    private fun dismissOverlay() {
        presentationJob?.cancel()
        presentationJob = null
        if (overlayHost.isShowing || _overlayState.value != OverlayState.Hidden) {
            _overlayState.value = OverlayState.Hidden
            overlayHost.hide()
        }
    }

    private fun dismissOverlayFor(pkg: String) {
        val shown = when (val state = _overlayState.value) {
            is OverlayState.Blocking -> state.packageName
            is OverlayState.Waiting -> state.packageName
            OverlayState.Hidden -> null
        }
        if (shown == pkg) dismissOverlay()
    }

    // ---- Overlay actions -------------------------------------------------

    /**
     * "Go back" / system back. RESISTED is only recorded from the block screen —
     * leaving the waiting screen is not resistance, and the pending wait keeps
     * running (§10: the wait survives the user leaving the screen).
     */
    fun onGoBack() {
        (_overlayState.value as? OverlayState.Blocking)?.let { state ->
            scope.launch { repository.recordResisted(state.packageName) }
        }
        blockedEncounterShownFor = null
        foregroundRevision++
        currentForeground = null
        dismissOverlay()
        goHome()
    }

    fun onStartWait() {
        if (actionInProgress) return
        val state = _overlayState.value as? OverlayState.Blocking ?: return
        val pkg = state.packageName
        val revision = foregroundRevision
        actionInProgress = true
        scope.launch {
            try {
                val required = repository.startWait(pkg)
                if (revision == foregroundRevision && _overlayState.value == state) {
                    _waitRemaining.value = required
                    _overlayState.value = OverlayState.Waiting(pkg, labelFor(pkg))
                }
            } finally { actionInProgress = false }
        }
    }

    fun onCancelWait() {
        if (actionInProgress) return
        val state = _overlayState.value as? OverlayState.Waiting ?: return
        val pkg = state.packageName
        val revision = foregroundRevision
        actionInProgress = true
        scope.launch {
            try {
                repository.cancelWait(pkg)
                val count = repository.todayUnlockCount()
                val endLabel = ScheduleEvaluator
                    .currentWindowEnd(scheduleWindows, pkg, TimeProvider.nowLocalDateTime())
                    ?.let { ScheduleEvaluator.formatMinuteOfDay(it.hour * 60 + it.minute) }
                if (revision == foregroundRevision && _overlayState.value == state) {
                    _overlayState.value = OverlayState.Blocking(
                        packageName = pkg,
                        appLabel = labelFor(pkg),
                        scheduleEndLabel = endLabel,
                        requiredWaitSeconds = EscalationPolicy.requiredWaitSeconds(count)
                    )
                }
            } finally { actionInProgress = false }
        }
    }

    private fun goHome() {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { appContext.startActivity(intent) }
    }

    // ---- Periodic work: wait completion, grant expiry, cleanup ----------

    private suspend fun tickLoop() {
        var lastCleanup = 0L
        var lastCheckpoint = 0L
        while (true) {
            val pending = pendingWaitValue
            if (pending != null) {
                val remaining = WaitCalculator.remainingSeconds(
                    pending, TimeProvider.nowMillis(), TimeProvider.elapsedRealtime()
                )
                _waitRemaining.value = remaining
                if (remaining <= 0) {
                    if (repository.tryCompleteWait(pending.packageName)) {
                        // Access granted; drop the overlay if it's over this app.
                        dismissOverlayFor(pending.packageName)
                    }
                }
            } else {
                _waitRemaining.value = 0
            }

            // Grant expiry -> re-block if the app is still in the foreground.
            val fg = currentForeground
            if (fg != null) {
                val expiry = grantsMap[fg]
                if (expiry != null && expiry <= TimeProvider.nowMillis()) {
                    repository.purgeExpiredGrants()
                    if (_overlayState.value == OverlayState.Hidden) onForegroundPackage(fg)
                }
            }

            val nowElapsed = TimeProvider.elapsedRealtime()

            // Bank the wait's monotonic progress regularly so a reboot (or the
            // process being killed) resumes it instead of restarting it.
            if (pending != null && nowElapsed - lastCheckpoint >= CHECKPOINT_INTERVAL_MS) {
                lastCheckpoint = nowElapsed
                repository.checkpointWait()
            }

            if (nowElapsed - lastCleanup >= CLEANUP_INTERVAL_MS) {
                lastCleanup = nowElapsed
                repository.purgeExpiredGrants()
                repository.purgeAbandonedWaits()
            }

            // A foreground app must also be checked at schedule boundaries.
            reevaluate()
            delay(ACTIVE_TICK_MS)
        }
    }

    /** Called on boot / service start to recover cleanly. */
    fun recover() {
        scope.launch {
            // Re-anchor first: after a reboot the wait's anchor belongs to the
            // previous boot session, and checkpointing banks what it had earned.
            repository.checkpointWait()
            repository.purgeAbandonedWaits()
            repository.purgeExpiredGrants()
            reevaluate()
        }
    }

    companion object {
        private const val ACTIVE_TICK_MS = 1_000L
        private const val CHECKPOINT_INTERVAL_MS = 5_000L
        private const val CLEANUP_INTERVAL_MS = 30_000L

        private val TRANSIENT_SYSTEM_PACKAGES = setOf(
            "com.android.systemui"
        )
    }
}
