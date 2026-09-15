package com.focuslock.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        BlockedApp::class,
        AppScheduleWindow::class,
        UnlockCounter::class,
        ActiveGrant::class,
        PendingWait::class,
        EventLog::class
    ],
    version = 6,
    exportSchema = false
)
abstract class FocusLockDatabase : RoomDatabase() {
    abstract fun blockedAppDao(): BlockedAppDao
    abstract fun appScheduleWindowDao(): AppScheduleWindowDao
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
