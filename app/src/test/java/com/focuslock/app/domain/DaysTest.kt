package com.focuslock.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

class DaysTest {

    @Test fun `monday is bit zero`() {
        assertEquals(1, Days.bitFor(DayOfWeek.MONDAY))
        assertEquals(1 shl 6, Days.bitFor(DayOfWeek.SUNDAY))
    }

    @Test fun `weekdays mask excludes weekend`() {
        assertTrue(Days.isActive(Days.WEEKDAYS, DayOfWeek.WEDNESDAY))
        assertFalse(Days.isActive(Days.WEEKDAYS, DayOfWeek.SATURDAY))
        assertFalse(Days.isActive(Days.WEEKDAYS, DayOfWeek.SUNDAY))
    }

    @Test fun `toggle adds and removes a day`() {
        var mask = Days.WEEKDAYS
        assertFalse(Days.isActive(mask, DayOfWeek.SUNDAY))
        mask = Days.toggle(mask, DayOfWeek.SUNDAY)
        assertTrue(Days.isActive(mask, DayOfWeek.SUNDAY))
        mask = Days.toggle(mask, DayOfWeek.SUNDAY)
        assertFalse(Days.isActive(mask, DayOfWeek.SUNDAY))
    }

    @Test fun `all mask includes every day`() {
        DayOfWeek.values().forEach { assertTrue(Days.isActive(Days.ALL, it)) }
    }
}
