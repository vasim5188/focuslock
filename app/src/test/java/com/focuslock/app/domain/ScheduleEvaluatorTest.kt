package com.focuslock.app.domain

import com.focuslock.app.data.db.AppScheduleWindow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class ScheduleEvaluatorTest {

    // 2024-01-01 is a Monday. 2024-01-06 is a Saturday. 2024-01-07 is a Sunday.
    private fun mon(h: Int, m: Int = 0) = LocalDateTime.of(2024, 1, 1, h, m)
    private fun sat(h: Int, m: Int = 0) = LocalDateTime.of(2024, 1, 6, h, m)

    private val weekday9to6 = AppScheduleWindow(1, "youtube", 9 * 60, 18 * 60, Days.WEEKDAYS, true)

    @Test fun `active inside same-day window`() {
        assertTrue(ScheduleEvaluator.isActive(weekday9to6, mon(10, 15)))
    }

    @Test fun `inactive before window`() {
        assertFalse(ScheduleEvaluator.isActive(weekday9to6, mon(8, 59)))
    }

    @Test fun `end is exclusive`() {
        assertFalse(ScheduleEvaluator.isActive(weekday9to6, mon(18, 0)))
        assertTrue(ScheduleEvaluator.isActive(weekday9to6, mon(17, 59)))
    }

    @Test fun `inactive on non-active day`() {
        assertFalse(ScheduleEvaluator.isActive(weekday9to6, sat(10, 0)))
    }

    @Test fun `disabled schedule never active`() {
        assertFalse(ScheduleEvaluator.isActive(weekday9to6.copy(isEnabled = false), mon(10, 0)))
    }

    @Test fun `midnight crossing - evening portion belongs to same day`() {
        val night = AppScheduleWindow(1, "youtube", 22 * 60, 6 * 60, Days.ALL, true)
        assertTrue(ScheduleEvaluator.isActive(night, mon(23, 0)))
    }

    @Test fun `midnight crossing - early morning belongs to previous day`() {
        // Only Sunday active: Monday 05:00 should be active because it carries from Sunday night.
        val sundayNight = AppScheduleWindow(1, "youtube", 22 * 60, 6 * 60, Days.SUN, true)
        assertTrue(ScheduleEvaluator.isActive(sundayNight, mon(5, 0)))
        // Monday 23:00 should NOT be active (Monday itself not in mask).
        assertFalse(ScheduleEvaluator.isActive(sundayNight, mon(23, 0)))
    }

    @Test fun `currentWindowEnd for same-day window`() {
        val end = ScheduleEvaluator.currentWindowEnd(weekday9to6, mon(10, 0))!!
        assertEquals(18, end.hour)
        assertEquals(0, end.minute)
        assertEquals(1, end.dayOfMonth)
    }

    @Test fun `currentWindowEnd for midnight crossing rolls to next day`() {
        val night = AppScheduleWindow(1, "youtube", 22 * 60, 6 * 60, Days.ALL, true)
        val end = ScheduleEvaluator.currentWindowEnd(night, mon(23, 0))!!
        assertEquals(6, end.hour)
        assertEquals(2, end.dayOfMonth) // Tuesday 06:00
    }
}
