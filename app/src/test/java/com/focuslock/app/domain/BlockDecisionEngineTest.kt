package com.focuslock.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class BlockDecisionEngineTest {

    private val base = ProtectionSnapshot(
        protectedPackages = setOf("com.instagram.android"),
        scheduleActive = true,
        grantedUntil = emptyMap()
    )

    @Test fun `unprotected app is always allowed`() {
        val d = BlockDecisionEngine.decide("com.whatsapp", base, now())
        assertEquals(BlockDecision.Allow, d)
    }

    @Test fun `protected app inside schedule is blocked`() {
        val d = BlockDecisionEngine.decide("com.instagram.android", base, now())
        assertEquals(BlockDecision.Block, d)
    }

    @Test fun `protected app outside schedule is allowed`() {
        val d = BlockDecisionEngine.decide(
            "com.instagram.android",
            base.copy(scheduleActive = false),
            now()
        )
        assertEquals(BlockDecision.Allow, d)
    }

    @Test fun `valid grant allows the app even inside schedule`() {
        val d = BlockDecisionEngine.decide(
            "com.instagram.android",
            base.copy(grantedUntil = mapOf("com.instagram.android" to now() + 60_000)),
            now()
        )
        assertEquals(BlockDecision.Allow, d)
    }

    @Test fun `expired grant is ignored`() {
        val d = BlockDecisionEngine.decide(
            "com.instagram.android",
            base.copy(grantedUntil = mapOf("com.instagram.android" to now() - 1)),
            now()
        )
        assertEquals(BlockDecision.Block, d)
    }

    private fun now() = 1_000_000L
}
