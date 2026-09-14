package com.focuslock.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** An app the user has chosen to protect. Max 2 in the free MVP. */
@Entity(tableName = "blocked_apps")
data class BlockedApp(
    @PrimaryKey val packageName: String,
    val appLabel: String,
    val addedAt: Long
)

/**
 * A single focus schedule. MVP supports exactly one row (id = 1).
 * [activeDays] is a 7-bit mask, bit 0 = Monday ... bit 6 = Sunday.
 * Supports midnight-crossing windows (start > end).
 */
@Entity(tableName = "schedules")
data class Schedule(
    @PrimaryKey val id: Int = 1,
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
    val activeDays: Int,
    val isEnabled: Boolean
)

/** Global deliberate-unlock counter for a given local date (yyyy-MM-dd). */
@Entity(tableName = "unlock_counters")
data class UnlockCounter(
    @PrimaryKey val dateKey: String,
    val unlockCount: Int
)

/** Temporary access granted after a completed wait. */
@Entity(tableName = "active_grants")
data class ActiveGrant(
    @PrimaryKey val packageName: String,
    val expiresAt: Long
)

/**
 * A wait deliberately started by the user. Only one is expected at a time.
 *
 * Progress is measured in *monotonic* time and checkpointed, so it survives a
 * reboot without restarting and cannot be shortened by moving the clock:
 *
 * - [accumulatedSeconds] is the progress already banked at the last checkpoint.
 * - [anchorElapsed] is SystemClock.elapsedRealtime() at that checkpoint; the
 *   delta since then is added on top while the device is in the same boot session.
 * - [bootEpochMillis] is (wall clock - elapsedRealtime) at that checkpoint. It
 *   identifies the boot session: after a reboot it moves by roughly the lost
 *   uptime, which tells us [anchorElapsed] is no longer comparable.
 * - [startedAt] is wall-clock ms, kept for the abandoned-wait lifetime (§11)
 *   and for recovery/debugging. It never shortens the wait.
 */
@Entity(tableName = "pending_waits")
data class PendingWait(
    @PrimaryKey val packageName: String,
    val startedAt: Long,
    val requiredSeconds: Int,
    val accumulatedSeconds: Int,
    val anchorElapsed: Long,
    val bootEpochMillis: Long
)

/** Event log powering future statistics. Never used to shame the user. */
@Entity(tableName = "event_logs")
data class EventLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val packageName: String?,
    val timestamp: Long,
    val dateKey: String
)
