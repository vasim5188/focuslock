package com.focuslock.app.domain

import com.focuslock.app.data.db.AppScheduleWindow
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Local weekly schedule math for each selected app. End times are exclusive. */
object ScheduleEvaluator {
    private val timeFmt = DateTimeFormatter.ofPattern("h:mm a")

    fun isActive(windows: List<AppScheduleWindow>, packageName: String, now: LocalDateTime): Boolean =
        isActive(windows.firstOrNull { it.packageName == packageName }, now)

    fun currentWindowEnd(windows: List<AppScheduleWindow>, packageName: String, now: LocalDateTime): LocalDateTime? =
        currentWindowEnd(windows.firstOrNull { it.packageName == packageName }, now)

    fun isActive(window: AppScheduleWindow?, now: LocalDateTime): Boolean {
        if (window == null || !window.isEnabled) return false
        val start = window.startMinuteOfDay
        val end = window.endMinuteOfDay
        val minute = now.hour * 60 + now.minute
        val today = now.dayOfWeek

        return when {
            start == end -> false
            start < end -> Days.isActive(window.activeDays, today) && minute >= start && minute < end
            else -> (minute >= start && Days.isActive(window.activeDays, today)) ||
                (minute < end && Days.isActive(window.activeDays, today.minus(1)))
        }
    }

    fun currentWindowEnd(window: AppScheduleWindow?, now: LocalDateTime): LocalDateTime? {
        if (!isActive(window, now)) return null
        val selected = window!!
        val end = selected.endMinuteOfDay
        val endDate = when {
            selected.startMinuteOfDay < end -> now.toLocalDate()
            now.hour * 60 + now.minute < end -> now.toLocalDate()
            else -> now.toLocalDate().plusDays(1)
        }
        return endDate.atStartOfDay().plusMinutes(end.toLong())
    }

    fun formatMinuteOfDay(minuteOfDay: Int): String {
        val h = (minuteOfDay / 60) % 24
        val m = minuteOfDay % 60
        return LocalDateTime.of(2000, 1, 1, h, m).format(timeFmt)
    }
}
