package com.focuslock.app.domain

import com.focuslock.app.data.db.PendingWait
import kotlin.math.abs
import kotlin.math.max

/**
 * Tamper-resistant remaining-time math for a persistent wait.
 *
 * A wait is measured entirely in monotonic time (SystemClock.elapsedRealtime),
 * which no clock change can move. Because that clock resets on reboot, progress
 * is *checkpointed*: [checkpoint] folds the monotonic delta into
 * [PendingWait.accumulatedSeconds] and re-anchors, so a reboot resumes the wait
 * from its banked progress instead of restarting it (§10).
 *
 * The boot session is identified by the boot epoch (wall clock minus uptime).
 * When it no longer matches, the delta since the last checkpoint is discarded
 * rather than trusted. That is deliberately one-directional: a reboot or a clock
 * change can only ever cost the user progress, never grant it, so moving the
 * clock forward cannot shorten a wait (§32).
 *
 * The trade-off: time with the device powered off does not count toward a wait.
 * That is the safe direction, and it makes "reboot to skip the wait" useless.
 */
object WaitCalculator {

    private const val ABANDON_GRACE_SECONDS = 30 * 60 // discard 30 min after required wait

    /**
     * How far the boot epoch may drift while still counting as the same boot
     * session. The wall and monotonic clocks tick independently, so a little
     * drift is normal; a reboot moves the epoch by the whole lost uptime.
     * Being generous here is safe, because the delta this gates is monotonic
     * and therefore trustworthy on its own.
     */
    private const val BOOT_EPOCH_TOLERANCE_MS = 60_000L

    /** Wall clock minus uptime: constant within a boot session, jumps on reboot. */
    fun bootEpoch(nowWall: Long, nowElapsed: Long): Long = nowWall - nowElapsed

    /** Is the wait's anchor still comparable to the current monotonic clock? */
    fun isSameBootSession(wait: PendingWait, nowWall: Long, nowElapsed: Long): Boolean =
        nowElapsed >= wait.anchorElapsed &&
            abs(bootEpoch(nowWall, nowElapsed) - wait.bootEpochMillis) <= BOOT_EPOCH_TOLERANCE_MS

    /** Banked progress plus the monotonic delta since the last checkpoint. */
    fun elapsedSeconds(wait: PendingWait, nowWall: Long, nowElapsed: Long): Int {
        val delta = if (isSameBootSession(wait, nowWall, nowElapsed)) {
            (nowElapsed - wait.anchorElapsed) / 1000L
        } else {
            0L // reboot or clock change: bank nothing rather than guess
        }
        return wait.accumulatedSeconds + delta.toInt()
    }

    fun remainingSeconds(wait: PendingWait, nowWall: Long, nowElapsed: Long): Int =
        max(0, wait.requiredSeconds - elapsedSeconds(wait, nowWall, nowElapsed))

    fun isComplete(wait: PendingWait, nowWall: Long, nowElapsed: Long): Boolean =
        remainingSeconds(wait, nowWall, nowElapsed) <= 0

    /**
     * Banks the progress made so far and re-anchors to the current clocks.
     * Called periodically while a wait runs, and once on boot recovery so the
     * stale anchor from the previous session is replaced.
     */
    fun checkpoint(wait: PendingWait, nowWall: Long, nowElapsed: Long): PendingWait =
        wait.copy(
            accumulatedSeconds = elapsedSeconds(wait, nowWall, nowElapsed),
            anchorElapsed = nowElapsed,
            bootEpochMillis = bootEpoch(nowWall, nowElapsed)
        )

    /** A wait that was started but never finished is discarded after this lifetime. */
    fun isAbandoned(wait: PendingWait, nowWall: Long): Boolean {
        val maxLifetimeMs = (wait.requiredSeconds + ABANDON_GRACE_SECONDS) * 1000L
        return nowWall - wait.startedAt > maxLifetimeMs
    }
}
