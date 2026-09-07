package com.focuslock.app.util

import android.os.SystemClock
import java.time.LocalDate
import java.time.LocalDateTime

/** Single source of time. Wall clock for schedules/recovery, monotonic for waits. */
object TimeProvider {
    fun nowMillis(): Long = System.currentTimeMillis()
    fun elapsedRealtime(): Long = SystemClock.elapsedRealtime()
    fun nowLocalDateTime(): LocalDateTime = LocalDateTime.now()
    fun todayKey(): String = LocalDate.now().toString() // yyyy-MM-dd (local date)
}
