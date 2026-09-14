package com.focuslock.app.domain

import com.focuslock.app.data.db.PendingWait
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WaitCalculatorTest {

    /** A wait as it looks the moment it is created. */
    private fun freshWait(
        startedAt: Long,
        nowElapsed: Long,
        required: Int
    ) = PendingWait(
        packageName = "pkg",
        startedAt = startedAt,
        requiredSeconds = required,
        accumulatedSeconds = 0,
        anchorElapsed = nowElapsed,
        bootEpochMillis = WaitCalculator.bootEpoch(startedAt, nowElapsed)
    )

    @Test fun `counts monotonic time within a boot session`() {
        val w = freshWait(startedAt = 1_000_000, nowElapsed = 1_000, required = 60)
        // 30s later on both clocks
        val remaining = WaitCalculator.remainingSeconds(
            w, nowWall = 1_030_000, nowElapsed = 31_000
        )
        assertEquals(30, remaining)
    }

    @Test fun `moving the clock forward does not shorten the wait`() {
        val w = freshWait(startedAt = 1_000_000, nowElapsed = 1_000, required = 60)
        // 10s of real time passed, but the user pushed the wall clock a day ahead.
        val remaining = WaitCalculator.remainingSeconds(
            w, nowWall = 1_000_000 + 86_400_000, nowElapsed = 11_000
        )
        // The clock jump breaks the boot-session match, so the delta is discarded
        // rather than trusted. It can never make the wait shorter.
        assertEquals(60, remaining)
    }

    @Test fun `moving the clock backwards does not shorten the wait`() {
        val w = freshWait(startedAt = 1_000_000, nowElapsed = 1_000, required = 60)
        val remaining = WaitCalculator.remainingSeconds(
            w, nowWall = 500_000, nowElapsed = 11_000
        )
        assertEquals(60, remaining)
    }

    // ---- Reboot behaviour (§10: the wait must not restart from zero) --------

    @Test fun `reboot resumes from checkpointed progress instead of restarting`() {
        var w = freshWait(startedAt = 1_000_000, nowElapsed = 3_600_000, required = 120)

        // 45s in, the periodic checkpoint banks the progress.
        w = WaitCalculator.checkpoint(w, nowWall = 1_045_000, nowElapsed = 3_645_000)
        assertEquals(45, w.accumulatedSeconds)

        // Device reboots: uptime restarts, wall clock carries on.
        val afterRebootWall = 1_100_000L
        val afterRebootElapsed = 20_000L
        assertFalse(WaitCalculator.isSameBootSession(w, afterRebootWall, afterRebootElapsed))
        assertEquals(75, WaitCalculator.remainingSeconds(w, afterRebootWall, afterRebootElapsed))

        // Boot recovery re-anchors, and the wait carries on from 45s.
        w = WaitCalculator.checkpoint(w, afterRebootWall, afterRebootElapsed)
        assertEquals(45, w.accumulatedSeconds)
        assertEquals(
            15,
            WaitCalculator.remainingSeconds(w, afterRebootWall + 60_000, afterRebootElapsed + 60_000)
        )
    }

    @Test fun `reboot shortly after a wait starts still does not restart it`() {
        // The old boot detection missed this case: the wait began when the device
        // had almost no uptime, so post-reboot uptime passes the anchor quickly.
        var w = freshWait(startedAt = 1_000_000, nowElapsed = 10_000, required = 1_800)
        w = WaitCalculator.checkpoint(w, nowWall = 1_300_000, nowElapsed = 310_000) // 5 min in
        assertEquals(300, w.accumulatedSeconds)

        // Reboot, then 30s of uptime - past the original 10s anchor.
        val remaining = WaitCalculator.remainingSeconds(
            w, nowWall = 1_400_000, nowElapsed = 30_000
        )
        assertEquals(1_800 - 300, remaining) // progress kept, not thrown away
    }

    @Test fun `checkpoint is idempotent when no time passes`() {
        val w = freshWait(startedAt = 1_000_000, nowElapsed = 5_000, required = 60)
        val checkpointed = WaitCalculator.checkpoint(w, nowWall = 1_000_000, nowElapsed = 5_000)
        assertEquals(w, checkpointed)
    }

    @Test fun `small clock drift still counts as the same boot session`() {
        val w = freshWait(startedAt = 1_000_000, nowElapsed = 1_000, required = 60)
        // 30s of monotonic time, wall clock nudged 2s by NTP.
        val remaining = WaitCalculator.remainingSeconds(
            w, nowWall = 1_032_000, nowElapsed = 31_000
        )
        assertEquals(30, remaining)
    }

    @Test fun `isComplete true when accumulated progress reaches the requirement`() {
        val w = freshWait(startedAt = 0, nowElapsed = 1_000, required = 60)
        assertTrue(WaitCalculator.isComplete(w, nowWall = 62_000, nowElapsed = 62_000))
        assertFalse(WaitCalculator.isComplete(w, nowWall = 30_000, nowElapsed = 30_000))
    }

    // ---- §11: abandonment is wall-clock based ------------------------------

    @Test fun `abandoned after required plus 30 minutes grace`() {
        val w = freshWait(startedAt = 0, nowElapsed = 0, required = 60)
        val lifetimeMs = (60 + 30 * 60) * 1000L
        assertFalse(WaitCalculator.isAbandoned(w, nowWall = lifetimeMs - 1))
        assertTrue(WaitCalculator.isAbandoned(w, nowWall = lifetimeMs + 1))
    }
}
