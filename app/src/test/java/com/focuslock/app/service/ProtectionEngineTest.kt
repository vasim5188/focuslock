package com.focuslock.app.service

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.focuslock.app.data.db.FocusLockDatabase
import com.focuslock.app.data.db.AppScheduleWindow
import com.focuslock.app.data.repository.FocusRepository
import com.focuslock.app.domain.Days
import com.focuslock.app.util.TimeProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, sdk = [28])
class ProtectionEngineTest {
    private lateinit var db: FocusLockDatabase
    private lateinit var repo: FocusRepository
    private lateinit var context: Context
    private val pkg = "example.protected"
    private var now = LocalDateTime.of(2026, 1, 5, 8, 59, 59)
    private var elapsed = 10_000_000L

    private class FakeOverlay : OverlayWindow {
        override var isShowing = false
        var shows = 0
        override fun show(onBackPressed: () -> Unit, content: @Composable () -> Unit) {
            isShowing = true
            shows++
        }
        override fun hide() { isShowing = false }
    }

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, FocusLockDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = FocusRepository(db)
        TimeProvider.source = object : TimeProvider.Source {
            override fun nowMillis() = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            override fun elapsedRealtime() = elapsed
            override fun nowLocalDateTime() = now
            override fun todayKey() = now.toLocalDate().toString()
        }
    }

    @After
    fun cleanup() { db.close(); TimeProvider.resetToSystemClock() }

    private suspend fun TestScope.engine(overlay: FakeOverlay): ProtectionEngine {
        repo.addBlockedApp(pkg, "Protected")
        repo.saveAppScheduleWindow(AppScheduleWindow(1, pkg, 9 * 60, 18 * 60, Days.ALL, true))
        return ProtectionEngine(context, repo, backgroundScope, overlay).also {
            it.start()
            runCurrent()
        }
    }

    private fun TestScope.advance(seconds: Long) {
        now = now.plusSeconds(seconds)
        elapsed += seconds * 1_000
        advanceTimeBy(1_000)
        runCurrent()
    }

    // Room runs on real database executors; let those finish between virtual ticks.
    private suspend fun TestScope.awaitCondition(predicate: suspend () -> Boolean) {
        repeat(200) {
            runCurrent()
            if (predicate()) return
            withContext(Dispatchers.IO) { Thread.sleep(5) }
        }
        assertTrue("State did not settle after database work", predicate())
    }

    @Test
    fun `schedule starts and ends without a new foreground event`() = runTest {
        val overlay = FakeOverlay()
        val engine = engine(overlay)
        engine.onForegroundPackage(pkg)
        runCurrent()
        assertFalse(overlay.isShowing)
        advance(1)
        awaitCondition { overlay.isShowing }
        assertTrue(overlay.isShowing)
        advance(9 * 60 * 60)
        awaitCondition { !overlay.isShowing }
        assertFalse(overlay.isShowing)
        assertEquals(OverlayState.Hidden, engine.overlayState.value)
    }

    @Test
    fun `overlay focus does not prevent completion or grant expiry`() = runTest {
        val overlay = FakeOverlay()
        val engine = engine(overlay)
        advance(1)
        engine.onForegroundPackage(pkg)
        awaitCondition { overlay.isShowing }
        assertTrue(overlay.isShowing)
        engine.onForegroundPackage(context.packageName)
        engine.onStartWait()
        awaitCondition { engine.overlayState.value is OverlayState.Waiting }
        assertTrue(engine.overlayState.value is OverlayState.Waiting)
        advance(60)
        awaitCondition { repo.hasValidGrant(pkg) && !overlay.isShowing }
        assertTrue(repo.hasValidGrant(pkg))
        assertFalse(overlay.isShowing)
        advance(600)
        awaitCondition { overlay.isShowing }
        assertTrue(overlay.isShowing)
    }

    @Test
    fun `leaving before presentation runs cancels stale overlay`() = runTest {
        val overlay = FakeOverlay()
        val engine = engine(overlay)
        advance(1)
        engine.onForegroundPackage(pkg)
        engine.onForegroundPackage("example.launcher")
        runCurrent()
        assertEquals(0, overlay.shows)
        assertEquals(OverlayState.Hidden, engine.overlayState.value)
    }

    @Test
    fun `schedule edits and real host activity dismiss the overlay`() = runTest {
        val overlay = FakeOverlay()
        val engine = engine(overlay)
        advance(1)
        engine.onForegroundPackage(pkg)
        awaitCondition { overlay.isShowing }
        assertTrue(overlay.isShowing)
        repo.saveAppScheduleWindow(AppScheduleWindow(1, pkg, 9 * 60, 18 * 60, Days.ALL, false))
        awaitCondition { !overlay.isShowing }
        assertFalse(overlay.isShowing)
        repo.saveAppScheduleWindow(AppScheduleWindow(1, pkg, 9 * 60, 18 * 60, Days.ALL, true))
        awaitCondition { overlay.isShowing }
        assertTrue(overlay.isShowing)
        engine.onHostActivityResumed()
        advance(1)
        assertFalse(overlay.isShowing)
    }
}
