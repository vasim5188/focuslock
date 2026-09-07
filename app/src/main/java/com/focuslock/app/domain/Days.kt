package com.focuslock.app.domain

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Utilities for the 7-bit active-days mask.
 * bit 0 = Monday, bit 1 = Tuesday ... bit 6 = Sunday (ISO order).
 */
object Days {
    const val MON = 1 shl 0
    const val TUE = 1 shl 1
    const val WED = 1 shl 2
    const val THU = 1 shl 3
    const val FRI = 1 shl 4
    const val SAT = 1 shl 5
    const val SUN = 1 shl 6

    const val WEEKDAYS = MON or TUE or WED or THU or FRI
    const val ALL = WEEKDAYS or SAT or SUN

    fun bitFor(day: DayOfWeek): Int = 1 shl (day.value - 1)

    fun isActive(mask: Int, day: DayOfWeek): Boolean = (mask and bitFor(day)) != 0

    fun isActive(mask: Int, date: LocalDate): Boolean = isActive(mask, date.dayOfWeek)

    fun toggle(mask: Int, day: DayOfWeek): Int = mask xor bitFor(day)

    val orderedDays: List<DayOfWeek> = listOf(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
    )
}
