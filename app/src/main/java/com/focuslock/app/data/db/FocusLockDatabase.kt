package com.focuslock.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        BlockedApp::class,
        Schedule::class,
        UnlockCounter::class,
        ActiveGrant::class,
        PendingWait::class,
        EventLog::class
    ],
    version = 3, // 3: daily counters count completed unlocks, not wait attempts
    exportSchema = false
)
abstract class FocusLockDatabase : RoomDatabase() {
    abstract fun blockedAppDao(): BlockedAppDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun unlockCounterDao(): UnlockCounterDao
    abstract fun activeGrantDao(): ActiveGrantDao
    abstract fun pendingWaitDao(): PendingWaitDao
    abstract fun eventLogDao(): EventLogDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Correct old attempt counts without losing schedules, grants or history.
                db.execSQL("DELETE FROM unlock_counters")
                db.execSQL("""
                    INSERT INTO unlock_counters (dateKey, unlockCount)
                    SELECT dateKey, COUNT(*) FROM event_logs
                    WHERE type = 'UNLOCK_COMPLETED' GROUP BY dateKey
                """.trimIndent())
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE pending_waits_v2 (
                        packageName TEXT NOT NULL PRIMARY KEY,
                        startedAt INTEGER NOT NULL,
                        requiredSeconds INTEGER NOT NULL,
                        accumulatedSeconds INTEGER NOT NULL,
                        anchorElapsed INTEGER NOT NULL,
                        bootEpochMillis INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO pending_waits_v2
                    SELECT packageName, startedAt, requiredSeconds, 0,
                           startedElapsed, startedAt - startedElapsed
                    FROM pending_waits
                """.trimIndent())
                db.execSQL("DROP TABLE pending_waits")
                db.execSQL("ALTER TABLE pending_waits_v2 RENAME TO pending_waits")
            }
        }

        @Volatile
        private var INSTANCE: FocusLockDatabase? = null

        fun get(context: Context): FocusLockDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FocusLockDatabase::class.java,
                    "focuslock.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { INSTANCE = it }
            }
    }
}
