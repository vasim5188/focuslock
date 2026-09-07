package com.focuslock.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        BlockedApp::class,
        Schedule::class,
        UnlockCounter::class,
        ActiveGrant::class,
        PendingWait::class,
        EventLog::class
    ],
    version = 1,
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
        @Volatile
        private var INSTANCE: FocusLockDatabase? = null

        fun get(context: Context): FocusLockDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FocusLockDatabase::class.java,
                    "focuslock.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}
