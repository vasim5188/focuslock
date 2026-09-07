package com.focuslock.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class EscalationPolicyTest {

    @Test fun `escalation table matches spec`() {
        assertEquals(60, EscalationPolicy.requiredWaitSeconds(0))    // 1st
        assertEquals(180, EscalationPolicy.requiredWaitSeconds(1))   // 2nd
        assertEquals(420, EscalationPolicy.requiredWaitSeconds(2))   // 3rd
        assertEquals(900, EscalationPolicy.requiredWaitSeconds(3))   // 4th
        assertEquals(1800, EscalationPolicy.requiredWaitSeconds(4))  // 5th
        assertEquals(1800, EscalationPolicy.requiredWaitSeconds(9))  // 5th+
    }

    @Test fun `grant duration is ten minutes`() {
        assertEquals(600, EscalationPolicy.GRANT_DURATION_SECONDS)
    }
}
