package com.focuslock.app.domain

import com.focuslock.app.data.db.AppScheduleWindow
import java.time.LocalDateTime
import org.junit.Assert.*
import org.junit.Test

class AppScheduleEvaluatorTest {
    @Test fun `each app follows its own time window`() {
        val windows = listOf(
            AppScheduleWindow(1, "youtube", 9 * 60, 10 * 60, Days.ALL, true),
            AppScheduleWindow(2, "games", 15 * 60, 16 * 60, Days.ALL, true)
        )
        val morning = LocalDateTime.of(2026, 1, 5, 9, 30)
        val afternoon = LocalDateTime.of(2026, 1, 5, 15, 30)
        assertTrue(ScheduleEvaluator.isActive(windows, "youtube", morning))
        assertFalse(ScheduleEvaluator.isActive(windows, "games", morning))
        assertFalse(ScheduleEvaluator.isActive(windows, "youtube", afternoon))
        assertTrue(ScheduleEvaluator.isActive(windows, "games", afternoon))
        assertNull(ScheduleEvaluator.currentWindowEnd(windows, "youtube", afternoon))
    }
}
