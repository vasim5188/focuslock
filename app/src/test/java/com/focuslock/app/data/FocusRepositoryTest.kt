package com.focuslock.app.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.focuslock.app.data.db.FocusLockDatabase
import com.focuslock.app.data.repository.FocusRepository
import com.focuslock.app.data.repository.MAX_FREE_APPS
import com.focuslock.app.domain.EscalationPolicy
import com.focuslock.app.domain.FocusEventType
import com.focuslock.app.util.TimeProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * The counter, grant and wait rules from sections 8, 9, 11, 12, 13 and 14 of the
 * spec, plus acceptance tests 2, 3, 4, 5, 6 and 10 from section 44.
 *
 * These are the rules the whole product rests on, so they run against a real
 * Room database rather than a fake, on the JVM via Robolectric.
 */
@RunWith(RobolectricTestRunner::class)
// A plain Application: the real one starts a ProtectionEngine whose Room flow
// collectors would outlive each test and hit a closed database.
@Config(application = android.app.Application::class)
class FocusRepositoryTest {

    private lateinit var db: FocusLockDatabase
    private lateinit var repo: FocusRepository
    private lateinit var clock: FakeClock

    private val instagram = "com.instagram.android"
    private val youtube = "com.google.android.youtube"

    /** Wall clock and monotonic clock advance together unless a test says otherwise. */
    private class FakeClock(start: LocalDateTime) : TimeProvider.Source {
        var now: LocalDateTime = start

        /** Device has been up a few hours, as it usually has in real life. */
        var elapsed: Long = 3 * 60 * 60 * 1000L

        override fun nowMillis(): Long =
            now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        override fun elapsedRealtime(): Long = elapsed
        override fun nowLocalDateTime(): LocalDateTime = now
        override fun todayKey(): String = now.toLocalDate().toString()

        fun advanceSeconds(seconds: Long) {
            now = now.plusSeconds(seconds)
            elapsed += seconds * 1000L
        }

        /** Simulates a reboot: elapsedRealtime restarts, wall clock carries on. */
        fun reboot() {
            elapsed = 0L
        }
    }

    @Before
    fun setUp() {
        clock = FakeClock(LocalDateTime.of(2026, 1, 5, 10, 0, 0))
        TimeProvider.source = clock
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FocusLockDatabase::class.java
        ).allowMainThreadQueries().build()
        repo = FocusRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
        TimeProvider.resetToSystemClock()
    }

    private suspend fun eventsToday(): List<String> =
        db.eventLogDao().observeForDate(TimeProvider.todayKey()).first().map { it.type }

    // ---- Acceptance test 2: opening and going back never increment ---------

    @Test
    fun `blocked encounter and resisted never touch the counter`() = runTest {
        repo.recordBlockedEncounter(instagram)
        repo.recordResisted(instagram)
        repo.recordBlockedEncounter(instagram)
        repo.recordResisted(instagram)

        assertEquals(0, repo.todayUnlockCount())
        assertEquals(2, eventsToday().count { it == FocusEventType.BLOCKED_ENCOUNTER.name })
        assertEquals(2, eventsToday().count { it == FocusEventType.RESISTED.name })
        assertEquals(0, eventsToday().count { it == FocusEventType.UNLOCK_STARTED.name })
    }

    // ---- Acceptance test 3: Wait increments exactly once -------------------

    @Test
    fun `starting a wait records an attempt without counting an unlock`() = runTest {
        val required = repo.startWait(instagram)

        assertEquals(60, required)
        assertEquals(0, repo.todayUnlockCount())
        assertEquals(1, eventsToday().count { it == FocusEventType.UNLOCK_STARTED.name })

        val pending = repo.getPendingWait()
        assertNotNull(pending)
        assertEquals(instagram, pending!!.packageName)
        assertEquals(60, pending.requiredSeconds)
    }

    @Test
    fun `re-entering an app with a wait already pending does not increment again`() = runTest {
        repo.startWait(instagram)
        clock.advanceSeconds(20)

        val secondCall = repo.startWait(instagram)

        assertEquals(60, secondCall)
        assertEquals(0, repo.todayUnlockCount())
        assertEquals(1, eventsToday().count { it == FocusEventType.UNLOCK_STARTED.name })
    }

    // ---- Acceptance test 4: cancelling does not re-increment ---------------

    @Test
    fun `cancelling a wait keeps the counter and the escalation level`() = runTest {
        // Climb to unlock #3, which the spec prices at 7 minutes.
        repo.startWait(instagram)
        completeWait(instagram)
        repo.startWait(instagram)
        completeWait(instagram)
        assertEquals(2, repo.todayUnlockCount())

        val third = repo.startWait(instagram)
        assertEquals(7 * 60, third)
        assertEquals(2, repo.todayUnlockCount())

        // The grant earned by unlock #2 is still legitimately running; what
        // matters is that cancelling neither creates nor extends one.
        val grantBefore = db.activeGrantDao().get(instagram)?.expiresAt

        clock.advanceSeconds(120)
        repo.cancelWait(instagram)

        assertEquals(2, repo.todayUnlockCount())
        assertNull(repo.getPendingWait())
        assertEquals(grantBefore, db.activeGrantDao().get(instagram)?.expiresAt)
        assertEquals(1, eventsToday().count { it == FocusEventType.WAIT_CANCELLED.name })

        // Cancelling leaves the next wait at the same level.
        val restarted = repo.startWait(instagram)
        assertEquals(7 * 60, restarted)
        assertEquals(2, repo.todayUnlockCount())
    }

    // ---- Acceptance test 5: completed wait grants ten minutes --------------

    @Test
    fun `wait completes into a ten minute grant`() = runTest {
        repo.startWait(instagram)

        clock.advanceSeconds(59)
        assertFalse(repo.tryCompleteWait(instagram))
        assertFalse(repo.hasValidGrant(instagram))

        clock.advanceSeconds(1)
        assertTrue(repo.tryCompleteWait(instagram))
        assertTrue(repo.hasValidGrant(instagram))
        assertNull(repo.getPendingWait())

        val grant = db.activeGrantDao().get(instagram)!!
        assertEquals(
            TimeProvider.nowMillis() + EscalationPolicy.GRANT_DURATION_SECONDS * 1000L,
            grant.expiresAt
        )
        assertEquals(1, eventsToday().count { it == FocusEventType.UNLOCK_COMPLETED.name })
    }

    @Test
    fun `a wait survives reboot and does not restart from zero`() = runTest {
        repo.startWait(instagram) // 60s required

        clock.advanceSeconds(40)
        repo.checkpointWait() // the engine banks progress every few seconds

        clock.reboot() // elapsedRealtime resets; banked progress survives
        repo.checkpointWait() // boot recovery re-anchors to the new session

        assertEquals(40, repo.getPendingWait()!!.accumulatedSeconds)
        assertFalse(repo.tryCompleteWait(instagram)) // 20s still owed

        clock.advanceSeconds(20)
        assertTrue(repo.tryCompleteWait(instagram))
        assertTrue(repo.hasValidGrant(instagram))
    }

    @Test
    fun `moving the clock forward cannot skip a wait`() = runTest {
        repo.startWait(instagram) // 60s required

        // Only 5 real seconds pass, but the user pushes the wall clock a day on.
        clock.advanceSeconds(5)
        clock.now = clock.now.plusDays(1)

        assertFalse(repo.tryCompleteWait(instagram))
        assertFalse(repo.hasValidGrant(instagram))
        assertNotNull(repo.getPendingWait())
    }

    @Test
    fun `checkpointing does not change the remaining wait`() = runTest {
        repo.startWait(instagram)
        clock.advanceSeconds(25)

        repo.checkpointWait()
        val banked = repo.getPendingWait()!!
        assertEquals(25, banked.accumulatedSeconds)

        clock.advanceSeconds(34)
        assertFalse(repo.tryCompleteWait(instagram)) // 59s in
        clock.advanceSeconds(1)
        assertTrue(repo.tryCompleteWait(instagram))  // 60s in
    }

    // ---- Acceptance test 6: grant expiry re-blocks -------------------------

    @Test
    fun `grant expires and the app becomes blocked again`() = runTest {
        repo.startWait(instagram)
        clock.advanceSeconds(60)
        repo.tryCompleteWait(instagram)

        clock.advanceSeconds(EscalationPolicy.GRANT_DURATION_SECONDS - 1L)
        assertTrue(repo.hasValidGrant(instagram))

        clock.advanceSeconds(1)
        assertFalse(repo.hasValidGrant(instagram))

        repo.purgeExpiredGrants()
        assertNull(db.activeGrantDao().get(instagram))
        assertEquals(1, eventsToday().count { it == FocusEventType.GRANT_EXPIRED.name })

        // No free second period: the next unlock costs the escalated wait.
        assertEquals(3 * 60, repo.startWait(instagram))
    }

    // ---- Acceptance test 10: the counter is global across apps -------------

    @Test
    fun `counter is global across protected apps`() = runTest {
        assertEquals(60, repo.startWait(instagram))
        completeWait(instagram)

        // A different app is unlock #2, not unlock #1 all over again.
        assertEquals(3 * 60, repo.startWait(youtube))
        assertEquals(1, repo.todayUnlockCount())
        completeWait(youtube)

        // ...and the next one, on either app, is unlock #3.
        assertEquals(7 * 60, repo.startWait(instagram))
        assertEquals(2, repo.todayUnlockCount())
    }

    @Test
    fun `escalation keeps climbing to the thirty minute ceiling`() = runTest {
        val expected = listOf(60, 3 * 60, 7 * 60, 15 * 60, 30 * 60, 30 * 60)
        expected.forEach { required ->
            assertEquals(required, repo.startWait(instagram))
            completeWait(instagram)
        }
        assertEquals(expected.size, repo.todayUnlockCount())
    }

    // ---- Section 14: midnight reset ---------------------------------------

    @Test
    fun `counter resets at local midnight`() = runTest {
        repeat(4) {
            repo.startWait(instagram)
            completeWait(instagram)
        }
        assertEquals(4, repo.todayUnlockCount())

        clock.now = LocalDateTime.of(2026, 1, 6, 0, 30)
        clock.elapsed += 1_000L

        assertEquals(0, repo.todayUnlockCount())
        assertEquals(60, repo.startWait(instagram)) // first unlock of the new day
        assertEquals(0, repo.todayUnlockCount())

        // Yesterday's tally is left intact for future statistics.
        assertEquals(
            4,
            db.unlockCounterDao().getCount(LocalDate.of(2026, 1, 5).toString())
        )
    }

    // ---- Section 11: abandoned waits ---------------------------------------

    @Test
    fun `abandoned wait is discarded after required plus grace`() = runTest {
        repo.startWait(instagram) // 60s required -> 31 minute lifetime

        clock.advanceSeconds(30 * 60)
        repo.purgeAbandonedWaits()
        assertNotNull(repo.getPendingWait())

        clock.advanceSeconds(2 * 60)
        repo.purgeAbandonedWaits()
        assertNull(repo.getPendingWait())

        // An abandoned wait never counts as an unlock.
        assertEquals(0, repo.todayUnlockCount())
    }

    // ---- Section 21 / section 40: the protected app list --------------------

    @Test
    fun `free tier accepts two apps and refuses a third`() = runTest {
        assertTrue(repo.addBlockedApp(instagram, "Instagram"))
        assertTrue(repo.addBlockedApp(youtube, "YouTube"))
        assertFalse(repo.addBlockedApp("com.reddit.frontpage", "Reddit"))
        assertEquals(MAX_FREE_APPS, repo.blockedCount())
    }

    @Test
    fun `removing an app clears its grant and pending wait`() = runTest {
        repo.addBlockedApp(instagram, "Instagram")
        repo.startWait(instagram)
        clock.advanceSeconds(60)
        repo.tryCompleteWait(instagram)
        assertTrue(repo.hasValidGrant(instagram))

        repo.removeBlockedApp(instagram)

        assertEquals(0, repo.blockedCount())
        assertFalse(repo.hasValidGrant(instagram))
        assertNull(repo.getPendingWait())
    }

    private suspend fun completeWait(pkg: String) {
        val required = repo.getPendingWait()!!.requiredSeconds
        clock.advanceSeconds(required.toLong())
        assertTrue(repo.tryCompleteWait(pkg))
    }

    @Test
    fun `cancelling at 45 seconds keeps the next wait at one minute`() = runTest {
        assertEquals(60, repo.startWait(instagram))
        clock.advanceSeconds(45)
        repo.cancelWait(instagram)
        assertEquals(0, repo.todayUnlockCount())
        assertFalse(repo.hasValidGrant(instagram))
        assertEquals(60, repo.startWait(instagram))
        completeWait(instagram)
        assertEquals(1, repo.todayUnlockCount())
        assertEquals(180, repo.startWait(instagram))
    }

    @Test
    fun `wait crossing midnight counts on completion day`() = runTest {
        clock.now = LocalDateTime.of(2026, 1, 5, 23, 59, 30)
        repo.startWait(instagram)
        clock.advanceSeconds(60)
        assertTrue(repo.tryCompleteWait(instagram))
        assertEquals(1, repo.todayUnlockCount())
        assertNull(db.unlockCounterDao().getCount("2026-01-05"))
    }

    @Test
    fun `concurrent starts record one attempt without counting an unlock`() = runTest {
        val results = coroutineScope {
            List(20) { async { repo.startWait(instagram) } }.awaitAll()
        }
        assertTrue(results.all { it == 60 })
        assertEquals(0, repo.todayUnlockCount())
        assertEquals(1, eventsToday().count { it == FocusEventType.UNLOCK_STARTED.name })
    }

    @Test
    fun `concurrent completion creates one grant event`() = runTest {
        repo.startWait(instagram)
        clock.advanceSeconds(60)
        val results = coroutineScope {
            List(20) { async { repo.tryCompleteWait(instagram) } }.awaitAll()
        }
        assertEquals(1, results.count { it })
        assertEquals(1, eventsToday().count { it == FocusEventType.UNLOCK_COMPLETED.name })
        assertTrue(repo.hasValidGrant(instagram))
    }

    @Test
    fun `failed grant write rolls back wait deletion`() = runTest {
        repo.startWait(instagram)
        clock.advanceSeconds(60)
        db.openHelper.writableDatabase.execSQL("""
            CREATE TRIGGER reject_grant BEFORE INSERT ON active_grants
            BEGIN SELECT RAISE(ABORT, 'simulated grant write failure'); END
        """.trimIndent())
        val result = runCatching { repo.tryCompleteWait(instagram) }
        assertTrue(result.isFailure)
        assertNotNull(repo.getPendingWait())
        assertFalse(repo.hasValidGrant(instagram))
        assertEquals(0, eventsToday().count { it == FocusEventType.UNLOCK_COMPLETED.name })
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER reject_grant")
        assertTrue(repo.tryCompleteWait(instagram))
    }

    @Test
    fun `checkpoint cannot resurrect a cancelled wait`() = runTest {
        repeat(10) {
            repo.startWait(instagram)
            clock.advanceSeconds(5)
            coroutineScope {
                listOf(async { repo.checkpointWait() }, async { repo.cancelWait(instagram) }).awaitAll()
            }
            assertNull(repo.getPendingWait())
        }
    }

    @Test
    fun `simultaneous selections respect the free app limit`() = runTest {
        coroutineScope {
            List(10) { index -> async { repo.addBlockedApp("app.$index", "App $index") } }.awaitAll()
        }
        assertEquals(MAX_FREE_APPS, repo.blockedCount())
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `existing daily count subscription switches at midnight`() = runTest {
        repo.startWait(instagram)
        completeWait(instagram)
        val firstDay = CompletableDeferred<Unit>()
        val nextDay = CompletableDeferred<Unit>()
        val observer = backgroundScope.launch {
            repo.observeTodayUnlockCount().collect { count ->
                if (count == 1) firstDay.complete(Unit)
                if (firstDay.isCompleted && count == null) nextDay.complete(Unit)
            }
        }
        firstDay.await()
        clock.now = clock.now.plusDays(1)
        advanceTimeBy(1_000)
        runCurrent()
        nextDay.await()
        observer.cancel()
    }
}
