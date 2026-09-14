package com.focuslock.app.data.repository

import com.focuslock.app.data.db.ActiveGrant
import com.focuslock.app.data.db.BlockedApp
import com.focuslock.app.data.db.EventLog
import com.focuslock.app.data.db.FocusLockDatabase
import com.focuslock.app.data.db.PendingWait
import com.focuslock.app.data.db.Schedule
import com.focuslock.app.data.db.UnlockCounter
import com.focuslock.app.domain.EscalationPolicy
import com.focuslock.app.domain.FocusEventType
import com.focuslock.app.domain.WaitCalculator
import com.focuslock.app.util.TimeProvider
import androidx.room.withTransaction
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.Flow

const val MAX_FREE_APPS = 2

/**
 * Central data access + core unlock business rules. This is where the counter
 * rules live so they can never be violated by UI code.
 */
class FocusRepository(private val db: FocusLockDatabase) {

    val blockedApps: Flow<List<BlockedApp>> = db.blockedAppDao().observeAll()
    val schedule: Flow<Schedule?> = db.scheduleDao().observe()
    val activeGrants: Flow<List<ActiveGrant>> = db.activeGrantDao().observeAll()
    val pendingWait: Flow<PendingWait?> = db.pendingWaitDao().observe()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun observeTodayUnlockCount(): Flow<Int?> = flow {
        while (true) {
            emit(TimeProvider.todayKey())
            delay(1_000)
        }
    }.distinctUntilChanged().flatMapLatest { db.unlockCounterDao().observeCount(it) }

    // ---- Blocked apps ---------------------------------------------------

    suspend fun getBlockedApps(): List<BlockedApp> = db.blockedAppDao().getAll()

    suspend fun blockedCount(): Int = db.blockedAppDao().count()

    /** Returns false if the free limit is already reached. */
    suspend fun addBlockedApp(packageName: String, label: String): Boolean = db.withTransaction {
        if (db.blockedAppDao().count() >= MAX_FREE_APPS) return@withTransaction false
        db.blockedAppDao().upsert(BlockedApp(packageName, label, TimeProvider.nowMillis()))
        return@withTransaction true
    }

    suspend fun removeBlockedApp(packageName: String) = db.withTransaction {
        db.blockedAppDao().delete(packageName)
        db.activeGrantDao().delete(packageName)
        db.pendingWaitDao().delete(packageName)
    }

    // ---- Schedule -------------------------------------------------------

    suspend fun getSchedule(): Schedule? = db.scheduleDao().get()

    suspend fun saveSchedule(schedule: Schedule) = db.scheduleDao().upsert(schedule.copy(id = 1))

    // ---- Counter --------------------------------------------------------

    suspend fun todayUnlockCount(): Int =
        db.unlockCounterDao().getCount(TimeProvider.todayKey()) ?: 0

    // ---- Grants ---------------------------------------------------------

    suspend fun hasValidGrant(packageName: String): Boolean {
        val g = db.activeGrantDao().get(packageName) ?: return false
        return g.expiresAt > TimeProvider.nowMillis()
    }

    /** Deletes grants that have expired, logging one GRANT_EXPIRED per app. */
    suspend fun purgeExpiredGrants() = db.withTransaction {
        val now = TimeProvider.nowMillis()
        val expired = db.activeGrantDao().getAll().filter { it.expiresAt <= now }
        expired.forEach { logEvent(FocusEventType.GRANT_EXPIRED, it.packageName) }
        db.activeGrantDao().deleteExpired(now)
    }

    // ---- Event logging --------------------------------------------------

    suspend fun logEvent(type: FocusEventType, packageName: String?) {
        db.eventLogDao().insert(
            EventLog(
                type = type.name,
                packageName = packageName,
                timestamp = TimeProvider.nowMillis(),
                dateKey = TimeProvider.todayKey()
            )
        )
    }

    // ---- Core unlock rules ---------------------------------------------

    /** User opened a protected app while protection was active. Counter unchanged. */
    suspend fun recordBlockedEncounter(packageName: String) =
        logEvent(FocusEventType.BLOCKED_ENCOUNTER, packageName)

    /** User pressed "Go back". Counter unchanged. */
    suspend fun recordResisted(packageName: String) =
        logEvent(FocusEventType.RESISTED, packageName)

    /**
     * User deliberately pressed "Wait". Does not increment the counter until
     * access is earned. Creates the persistent PendingWait and returns the
     * required wait in seconds (governed by the escalation level BEFORE this
     * unlock). Idempotent for an already-pending wait of the same app.
     */
    suspend fun startWait(packageName: String): Int = db.withTransaction {
        val existing = db.pendingWaitDao().get(packageName)
        if (existing != null) return@withTransaction existing.requiredSeconds

        val dateKey = TimeProvider.todayKey()
        val countBefore = db.unlockCounterDao().getCount(dateKey) ?: 0
        val required = EscalationPolicy.requiredWaitSeconds(countBefore)

        val nowWall = TimeProvider.nowMillis()
        val nowElapsed = TimeProvider.elapsedRealtime()
        db.pendingWaitDao().deleteAll() // only one wait at a time
        db.pendingWaitDao().upsert(
            PendingWait(
                packageName = packageName,
                startedAt = nowWall,
                requiredSeconds = required,
                accumulatedSeconds = 0,
                anchorElapsed = nowElapsed,
                bootEpochMillis = WaitCalculator.bootEpoch(nowWall, nowElapsed)
            )
        )
        logEvent(FocusEventType.UNLOCK_STARTED, packageName)
        return@withTransaction required
    }

    /** User cancelled the wait. Do NOT touch the counter. */
    suspend fun cancelWait(packageName: String) = db.withTransaction {
        db.pendingWaitDao().delete(packageName)
        logEvent(FocusEventType.WAIT_CANCELLED, packageName)
    }

    /**
     * Completes the wait if enough time has elapsed, creating a 10-minute grant.
     * Returns true if a grant was created. Safe to call repeatedly.
     */
    suspend fun tryCompleteWait(packageName: String): Boolean = db.withTransaction {
        val wait = db.pendingWaitDao().get(packageName) ?: return@withTransaction false
        val complete = WaitCalculator.isComplete(
            wait, TimeProvider.nowMillis(), TimeProvider.elapsedRealtime()
        )
        if (!complete) return@withTransaction false
        val dateKey = TimeProvider.todayKey()
        val count = db.unlockCounterDao().getCount(dateKey) ?: 0
        db.pendingWaitDao().delete(packageName)
        db.activeGrantDao().upsert(
            ActiveGrant(
                packageName = packageName,
                expiresAt = TimeProvider.nowMillis() + EscalationPolicy.GRANT_DURATION_SECONDS * 1000L
            )
        )
        db.unlockCounterDao().upsert(UnlockCounter(dateKey, count + 1))
        logEvent(FocusEventType.UNLOCK_COMPLETED, packageName)
        return@withTransaction true
    }

    /**
     * Banks the wait's monotonic progress and re-anchors it to the current
     * clocks, so a reboot resumes from banked progress instead of restarting.
     * Cheap and idempotent; called periodically while a wait runs.
     */
    suspend fun checkpointWait() = db.withTransaction {
        val wait = db.pendingWaitDao().getAny() ?: return@withTransaction
        val updated = WaitCalculator.checkpoint(
            wait, TimeProvider.nowMillis(), TimeProvider.elapsedRealtime()
        )
        if (updated != wait) db.pendingWaitDao().upsert(updated)
    }

    /** Discards waits that were started but abandoned past their max lifetime. */
    suspend fun purgeAbandonedWaits() = db.withTransaction {
        val now = TimeProvider.nowMillis()
        db.pendingWaitDao().getAny()?.let { wait ->
            if (WaitCalculator.isAbandoned(wait, now)) {
                db.pendingWaitDao().delete(wait.packageName)
            }
        }
    }

    suspend fun getPendingWait(): PendingWait? = db.pendingWaitDao().getAny()
}
