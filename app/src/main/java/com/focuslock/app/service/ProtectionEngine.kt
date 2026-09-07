package com.focuslock.app.service

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import com.focuslock.app.data.db.BlockedApp
import com.focuslock.app.data.db.PendingWait
import com.focuslock.app.data.db.Schedule
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
    private val repository: FocusRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val overlayHost = OverlayHost(appContext)

    @Volatile private var protectedApps: List<BlockedApp> = emptyList()
    @Volatile private var scheduleValue: Schedule? = null
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

        scope.launch { repository.blockedApps.collect { protectedApps = it } }
        scope.launch { repository.schedule.collect { scheduleValue = it } }
        scope.launch {
            repository.activeGrants.collect { grants ->
                grantsMap = grants.associate { it.packageName to it.expiresAt }
            }
        }
        scope.launch { repository.pendingWait.collect { pendingWaitValue = it } }

        scope.launch { tickLoop() }
    }

    private fun snapshot(): ProtectionSnapshot = ProtectionSnapshot(
        protectedPackages = protectedApps.map { it.packageName }.toSet(),
        scheduleActive = ScheduleEvaluator.isActive(scheduleValue, TimeProvider.nowLocalDateTime()),
        grantedUntil = grantsMap,
        deepFocusActive = false
    )

    private fun isInCall(): Boolean {
        val am = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return am.mode == AudioManager.MODE_IN_CALL || am.mode == AudioManager.MODE_IN_COMMUNICATION
    }

    /** Called from the AccessibilityService whenever the foreground app changes. */
    fun onForegroundPackage(pkg: String) {
        // Transient system windows (status bar / quick settings shade) must not
        // dismiss the overlay while the user is still on the protected app.
        if (pkg in TRANSIENT_SYSTEM_PACKAGES) return

        currentForeground = pkg

        // Our own UI (settings / block overlay host) never gets blocked.
        if (pkg == appContext.packageName) {
            dismissOverlay()
            return
        }
        if (isInCall()) {
            dismissOverlay()
            return
        }

        when (BlockDecisionEngine.decide(pkg, snapshot(), TimeProvider.nowMillis())) {
            BlockDecision.Allow -> {
                blockedEncounterShownFor = null
                dismissOverlay()
            }
            BlockDecision.Block -> presentBlock(pkg)
        }
    }

    private fun labelFor(pkg: String): String =
        protectedApps.firstOrNull { it.packageName == pkg }?.appLabel ?: pkg

    private fun presentBlock(pkg: String) {
        scope.launch {
            val pending = pendingWaitValue
            if (pending != null && pending.packageName == pkg) {
                // Wait may already be complete (e.g. after returning to the app).
                if (repository.tryCompleteWait(pkg)) {
                    dismissOverlay()
                    return@launch
                }
                _overlayState.value = OverlayState.Waiting(pkg, labelFor(pkg))
            } else {
                if (blockedEncounterShownFor != pkg) {
                    repository.recordBlockedEncounter(pkg)
                    blockedEncounterShownFor = pkg
                }
                val count = repository.todayUnlockCount()
                val endLabel = ScheduleEvaluator
                    .currentWindowEnd(scheduleValue, TimeProvider.nowLocalDateTime())
                    ?.let { ScheduleEvaluator.formatMinuteOfDay(it.hour * 60 + it.minute) }
                _overlayState.value = OverlayState.Blocking(
                    packageName = pkg,
                    appLabel = labelFor(pkg),
                    scheduleEndLabel = endLabel,
                    requiredWaitSeconds = EscalationPolicy.requiredWaitSeconds(count)
                )
            }
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
        if (overlayHost.isShowing || _overlayState.value != OverlayState.Hidden) {
            _overlayState.value = OverlayState.Hidden
            overlayHost.hide()
        }
    }

    // ---- Overlay actions -------------------------------------------------

    fun onGoBack() {
        val pkg = (_overlayState.value as? OverlayState.Blocking)?.packageName
            ?: (_overlayState.value as? OverlayState.Waiting)?.packageName
        if (pkg != null) scope.launch { repository.recordResisted(pkg) }
        blockedEncounterShownFor = null
        dismissOverlay()
        goHome()
    }

    fun onStartWait() {
        val state = _overlayState.value as? OverlayState.Blocking ?: return
        val pkg = state.packageName
        scope.launch {
            repository.startWait(pkg)
            _overlayState.value = OverlayState.Waiting(pkg, labelFor(pkg))
        }
    }

    fun onCancelWait() {
        val state = _overlayState.value as? OverlayState.Waiting ?: return
        val pkg = state.packageName
        scope.launch {
            repository.cancelWait(pkg)
            val count = repository.todayUnlockCount()
            val endLabel = ScheduleEvaluator
                .currentWindowEnd(scheduleValue, TimeProvider.nowLocalDateTime())
                ?.let { ScheduleEvaluator.formatMinuteOfDay(it.hour * 60 + it.minute) }
            _overlayState.value = OverlayState.Blocking(
                packageName = pkg,
                appLabel = labelFor(pkg),
                scheduleEndLabel = endLabel,
                requiredWaitSeconds = EscalationPolicy.requiredWaitSeconds(count)
            )
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
        var sinceCleanup = 0
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
                        if (currentForeground == pending.packageName) dismissOverlay()
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

            if (++sinceCleanup >= 30) {
                sinceCleanup = 0
                repository.purgeExpiredGrants()
                repository.purgeAbandonedWaits()
            }
            delay(1000)
        }
    }

    /** Called on boot / service start to recover cleanly. */
    fun recover() {
        scope.launch {
            repository.purgeAbandonedWaits()
            repository.purgeExpiredGrants()
        }
    }

    companion object {
        private val TRANSIENT_SYSTEM_PACKAGES = setOf(
            "com.android.systemui"
        )
    }
}
