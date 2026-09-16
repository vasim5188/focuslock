package com.focuslock.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import com.focuslock.app.BuildConfig

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
    exportSchema = true
)
abstract class FocusLockDatabase : RoomDatabase() {
    abstract fun blockedAppDao(): BlockedAppDao
    abstract fun appScheduleWindowDao(): AppScheduleWindowDao
    abstract fun unlockCounterDao(): UnlockCounterDao
    abstract fun activeGrantDao(): ActiveGrantDao
    abstract fun pendingWaitDao(): PendingWaitDao
    abstract fun eventLogDao(): EventLogDao

    companion object {
        /**
         * Every schema change from v6 onwards needs a migration here. v6 is the
         * first released schema, so there is nothing to migrate from yet.
         *
         * Adding one:
         *  1. Bump the version above and change the entities.
         *  2. Build once so KSP writes app/schemas/<version>.json, and commit it.
         *  3. Append the Migration below, and cover it with a MigrationTestHelper
         *     test in app/src/androidTest (room-testing is already a dependency).
         */
        val MIGRATIONS: Array<Migration> = emptyArray()

        @Volatile
        private var INSTANCE: FocusLockDatabase? = null

        fun get(context: Context): FocusLockDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FocusLockDatabase::class.java,
                    "focuslock.db"
                )
                    .addMigrations(*MIGRATIONS)
                    .apply {
                        // Released builds must never drop user data. Debug builds keep
                        // the throwaway behaviour so local schema churn stays cheap.
                        if (BuildConfig.DEBUG) {
                            fallbackToDestructiveMigration()
                        }
                    }
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
