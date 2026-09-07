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

    fun observeTodayUnlockCount(): Flow<Int?> =
        db.unlockCounterDao().observeCount(TimeProvider.todayKey())

    // ---- Blocked apps ---------------------------------------------------

    suspend fun getBlockedApps(): List<BlockedApp> = db.blockedAppDao().getAll()

    suspend fun blockedCount(): Int = db.blockedAppDao().count()

    /** Returns false if the free limit is already reached. */
    suspend fun addBlockedApp(packageName: String, label: String): Boolean {
        if (db.blockedAppDao().count() >= MAX_FREE_APPS) return false
        db.blockedAppDao().upsert(BlockedApp(packageName, label, TimeProvider.nowMillis()))
        return true
    }

    suspend fun removeBlockedApp(packageName: String) {
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

    private suspend fun setTodayCount(count: Int) {
        db.unlockCounterDao().upsert(UnlockCounter(TimeProvider.todayKey(), count))
    }

    // ---- Grants ---------------------------------------------------------

    suspend fun hasValidGrant(packageName: String): Boolean {
        val g = db.activeGrantDao().get(packageName) ?: return false
        return g.expiresAt > TimeProvider.nowMillis()
    }

    /** Deletes grants that have expired, logging one GRANT_EXPIRED per app. */
    suspend fun purgeExpiredGrants() {
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
     * User deliberately pressed "Wait". This is the ONLY path that increments
     * the daily counter. Creates the persistent PendingWait and returns the
     * required wait in seconds (governed by the escalation level BEFORE this
     * unlock). Idempotent for an already-pending wait of the same app.
     */
    suspend fun startWait(packageName: String): Int {
        val existing = db.pendingWaitDao().get(packageName)
        if (existing != null) return existing.requiredSeconds

        val countBefore = todayUnlockCount()
        val required = EscalationPolicy.requiredWaitSeconds(countBefore)

        db.pendingWaitDao().deleteAll() // only one wait at a time
        db.pendingWaitDao().upsert(
            PendingWait(
                packageName = packageName,
                startedAt = TimeProvider.nowMillis(),
                startedElapsed = TimeProvider.elapsedRealtime(),
                requiredSeconds = required
            )
        )
        setTodayCount(countBefore + 1) // UNLOCK_STARTED increments exactly once
        logEvent(FocusEventType.UNLOCK_STARTED, packageName)
        return required
    }

    /** User cancelled the wait. Do NOT touch the counter. */
    suspend fun cancelWait(packageName: String) {
        db.pendingWaitDao().delete(packageName)
        logEvent(FocusEventType.WAIT_CANCELLED, packageName)
    }

    /**
     * Completes the wait if enough time has elapsed, creating a 10-minute grant.
     * Returns true if a grant was created. Safe to call repeatedly.
     */
    suspend fun tryCompleteWait(packageName: String): Boolean {
        val wait = db.pendingWaitDao().get(packageName) ?: return false
        val complete = WaitCalculator.isComplete(
            wait, TimeProvider.nowMillis(), TimeProvider.elapsedRealtime()
        )
        if (!complete) return false
        db.pendingWaitDao().delete(packageName)
        db.activeGrantDao().upsert(
            ActiveGrant(
                packageName = packageName,
                expiresAt = TimeProvider.nowMillis() + EscalationPolicy.GRANT_DURATION_SECONDS * 1000L
            )
        )
        logEvent(FocusEventType.UNLOCK_COMPLETED, packageName)
        return true
    }

    /** Discards waits that were started but abandoned past their max lifetime. */
    suspend fun purgeAbandonedWaits() {
        val now = TimeProvider.nowMillis()
        db.pendingWaitDao().getAny()?.let { wait ->
            if (WaitCalculator.isAbandoned(wait, now)) {
                db.pendingWaitDao().delete(wait.packageName)
            }
        }
    }

    suspend fun getPendingWait(): PendingWait? = db.pendingWaitDao().getAny()
}
