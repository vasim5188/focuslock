package com.focuslock.app.domain

import com.focuslock.app.data.db.Schedule
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Pure schedule math. Handles same-day and midnight-crossing windows.
 * All calculations use local date/time supplied by the caller.
 */
object ScheduleEvaluator {

    private val timeFmt = DateTimeFormatter.ofPattern("h:mm a")

    /** Is [now] inside the enabled schedule window? */
    fun isActive(schedule: Schedule?, now: LocalDateTime): Boolean {
        if (schedule == null || !schedule.isEnabled) return false
        val start = schedule.startMinuteOfDay
        val end = schedule.endMinuteOfDay
        val minute = now.hour * 60 + now.minute
        val today = now.dayOfWeek
        val yesterday = today.minus(1)

        return when {
            // Zero-length / equal -> treat as inactive.
            start == end -> false

            // Same-day window, e.g. 09:00 -> 18:00
            start < end ->
                Days.isActive(schedule.activeDays, today) && minute >= start && minute < end

            // Midnight-crossing window, e.g. 22:00 -> 06:00
            else -> {
                val inTodayPortion = minute >= start && Days.isActive(schedule.activeDays, today)
                val inCarryPortion = minute < end && Days.isActive(schedule.activeDays, yesterday)
                inTodayPortion || inCarryPortion
            }
        }
    }

    /** Wall-clock LocalDateTime at which the currently-active window ends. Null if not active. */
    fun currentWindowEnd(schedule: Schedule?, now: LocalDateTime): LocalDateTime? {
        if (!isActive(schedule, now)) return null
        val end = schedule!!.endMinuteOfDay
        val minute = now.hour * 60 + now.minute
        val endDate = if (schedule.startMinuteOfDay < end) {
            // same day
            now.toLocalDate()
        } else {
            // midnight crossing: if we're still before end, it ends today; else tomorrow
            if (minute < end) now.toLocalDate() else now.toLocalDate().plusDays(1)
        }
        return endDate.atStartOfDay().plusMinutes(end.toLong())
    }

    fun formatMinuteOfDay(minuteOfDay: Int): String {
        val h = (minuteOfDay / 60) % 24
        val m = minuteOfDay % 60
        return LocalDateTime.of(2000, 1, 1, h, m).format(timeFmt)
    }
}
