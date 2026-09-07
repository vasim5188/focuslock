package com.focuslock.app.domain

import com.focuslock.app.data.db.PendingWait
import kotlin.math.max

/**
 * Tamper-resistant remaining-time math for a persistent wait.
 *
 * Prefers SystemClock.elapsedRealtime() (monotonic, immune to clock changes)
 * while the device has not rebooted. Falls back to wall-clock after a reboot,
 * since elapsedRealtime resets on boot. Wall clock is always persisted so the
 * schedule/recovery logic keeps working.
 */
object WaitCalculator {

    private const val ABANDON_GRACE_SECONDS = 30 * 60 // discard 30 min after required wait

    fun remainingSeconds(wait: PendingWait, nowWall: Long, nowElapsed: Long): Int {
        val elapsedSec: Long = if (wait.startedElapsed > 0L && nowElapsed >= wait.startedElapsed) {
            (nowElapsed - wait.startedElapsed) / 1000L
        } else {
            // Rebooted (or elapsed unavailable): fall back to wall clock, guarding
            // against a backwards clock jump (never let a wait get shorter than started).
            max(0L, (nowWall - wait.startedAt) / 1000L)
        }
        return max(0, wait.requiredSeconds - elapsedSec.toInt())
    }

    fun isComplete(wait: PendingWait, nowWall: Long, nowElapsed: Long): Boolean =
        remainingSeconds(wait, nowWall, nowElapsed) <= 0

    /** A wait that was started but never finished is discarded after this lifetime. */
    fun isAbandoned(wait: PendingWait, nowWall: Long): Boolean {
        val maxLifetimeMs = (wait.requiredSeconds + ABANDON_GRACE_SECONDS) * 1000L
        return nowWall - wait.startedAt > maxLifetimeMs
    }
}
