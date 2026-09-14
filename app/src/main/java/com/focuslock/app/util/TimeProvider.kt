package com.focuslock.app.util

import android.os.SystemClock
import androidx.annotation.VisibleForTesting
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Single source of time. Wall clock for schedules/recovery, monotonic for waits.
 *
 * Everything goes through [source] so tests can advance time deterministically
 * (grant expiry, midnight reset, abandoned waits) without sleeping. Production
 * code never touches [source].
 */
object TimeProvider {

    interface Source {
        fun nowMillis(): Long
        fun elapsedRealtime(): Long
        fun nowLocalDateTime(): LocalDateTime
        fun todayKey(): String
    }

    private val systemSource = object : Source {
        override fun nowMillis(): Long = System.currentTimeMillis()
        override fun elapsedRealtime(): Long = SystemClock.elapsedRealtime()
        override fun nowLocalDateTime(): LocalDateTime = LocalDateTime.now()
        override fun todayKey(): String = LocalDate.now().toString() // yyyy-MM-dd (local date)
    }

    @Volatile
    @VisibleForTesting
    var source: Source = systemSource

    @VisibleForTesting
    fun resetToSystemClock() {
        source = systemSource
    }

    fun nowMillis(): Long = source.nowMillis()
    fun elapsedRealtime(): Long = source.elapsedRealtime()
    fun nowLocalDateTime(): LocalDateTime = source.nowLocalDateTime()
    fun todayKey(): String = source.todayKey()
}
