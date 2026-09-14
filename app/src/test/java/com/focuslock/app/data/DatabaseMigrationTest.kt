package com.focuslock.app.data

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.focuslock.app.data.db.FocusLockDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, sdk = [28])
class DatabaseMigrationTest {
    @Test
    fun `version one upgrades without losing configuration or wait anchor`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-test.db"
        context.deleteDatabase(name)
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE blocked_apps (packageName TEXT NOT NULL PRIMARY KEY, appLabel TEXT NOT NULL, addedAt INTEGER NOT NULL)")
                        db.execSQL("CREATE TABLE schedules (id INTEGER NOT NULL PRIMARY KEY, startMinuteOfDay INTEGER NOT NULL, endMinuteOfDay INTEGER NOT NULL, activeDays INTEGER NOT NULL, isEnabled INTEGER NOT NULL)")
                        db.execSQL("CREATE TABLE unlock_counters (dateKey TEXT NOT NULL PRIMARY KEY, unlockCount INTEGER NOT NULL)")
                        db.execSQL("CREATE TABLE active_grants (packageName TEXT NOT NULL PRIMARY KEY, expiresAt INTEGER NOT NULL)")
                        db.execSQL("CREATE TABLE pending_waits (packageName TEXT NOT NULL PRIMARY KEY, startedAt INTEGER NOT NULL, startedElapsed INTEGER NOT NULL, requiredSeconds INTEGER NOT NULL)")
                        db.execSQL("CREATE TABLE event_logs (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, type TEXT NOT NULL, packageName TEXT, timestamp INTEGER NOT NULL, dateKey TEXT NOT NULL)")
                        db.execSQL("INSERT INTO blocked_apps VALUES ('app', 'Protected', 100000)")
                        db.execSQL("INSERT INTO schedules VALUES (1, 540, 1080, 127, 1)")
                        db.execSQL("INSERT INTO unlock_counters VALUES ('2026-01-05', 3)")
                        db.execSQL("INSERT INTO active_grants VALUES ('other', 900000)")
                        db.execSQL("INSERT INTO pending_waits VALUES ('app', 100000, 20000, 420)")
                        db.execSQL("INSERT INTO event_logs VALUES (1, 'UNLOCK_STARTED', 'app', 100000, '2026-01-05')")
                        db.execSQL("INSERT INTO event_logs VALUES (2, 'UNLOCK_COMPLETED', 'other', 90000, '2026-01-05')")
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }).build()
        )
        helper.writableDatabase
        helper.close()
        val db = Room.databaseBuilder(context, FocusLockDatabase::class.java, name)
            .addMigrations(FocusLockDatabase.MIGRATION_1_2, FocusLockDatabase.MIGRATION_2_3).allowMainThreadQueries().build()
        try {
            assertEquals("Protected", db.blockedAppDao().getAll().single().appLabel)
            assertEquals(540, db.scheduleDao().get()!!.startMinuteOfDay)
            // The old count was 3 attempts; only one actually earned access.
            assertEquals(1, db.unlockCounterDao().getCount("2026-01-05"))
            assertEquals(900000L, db.activeGrantDao().get("other")!!.expiresAt)
            val wait = db.pendingWaitDao().getAny()!!
            assertEquals(420, wait.requiredSeconds)
            assertEquals(0, wait.accumulatedSeconds)
            assertEquals(20000L, wait.anchorElapsed)
            assertEquals(80000L, wait.bootEpochMillis)
            db.openHelper.writableDatabase.query("SELECT COUNT(*) FROM event_logs").use {
                it.moveToFirst()
                assertEquals(2, it.getInt(0))
            }
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }
}

