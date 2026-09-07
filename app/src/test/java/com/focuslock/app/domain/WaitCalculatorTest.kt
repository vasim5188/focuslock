package com.focuslock.app.domain

import com.focuslock.app.data.db.PendingWait
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WaitCalculatorTest {

    private fun wait(startedAt: Long, startedElapsed: Long, required: Int) =
        PendingWait("pkg", startedAt, startedElapsed, required)

    @Test fun `uses monotonic elapsed time when no reboot`() {
        val w = wait(startedAt = 0, startedElapsed = 1_000, required = 60)
        // 30s later on the monotonic clock
        val remaining = WaitCalculator.remainingSeconds(w, nowWall = 999_999, nowElapsed = 31_000)
        assertEquals(30, remaining)
    }

    @Test fun `falls back to wall clock after reboot`() {
        // nowElapsed < startedElapsed means the device rebooted.
        val w = wait(startedAt = 0, startedElapsed = 100_000, required = 60)
        val remaining = WaitCalculator.remainingSeconds(w, nowWall = 40_000, nowElapsed = 5_000)
        assertEquals(20, remaining)
    }

    @Test fun `moving clock backwards never shortens the wait`() {
        val w = wait(startedAt = 50_000, startedElapsed = 100_000, required = 60)
        // rebooted (elapsed smaller) AND wall clock moved backwards
        val remaining = WaitCalculator.remainingSeconds(w, nowWall = 10_000, nowElapsed = 1_000)
        assertEquals(60, remaining)
    }

    @Test fun `isComplete true when elapsed exceeds required`() {
        val w = wait(0, 1_000, 60)
        assertTrue(WaitCalculator.isComplete(w, nowWall = 0, nowElapsed = 61_000 + 1_000))
        assertFalse(WaitCalculator.isComplete(w, nowWall = 0, nowElapsed = 30_000))
    }

    @Test fun `abandoned after required plus 30 minutes grace`() {
        val w = wait(startedAt = 0, startedElapsed = 0, required = 60)
        val lifetimeMs = (60 + 30 * 60) * 1000L
        assertFalse(WaitCalculator.isAbandoned(w, nowWall = lifetimeMs - 1))
        assertTrue(WaitCalculator.isAbandoned(w, nowWall = lifetimeMs + 1))
    }
}
