package com.focuslock.app.domain

/** Immutable snapshot of everything the block decision needs, held in memory. */
data class ProtectionSnapshot(
    val protectedPackages: Set<String> = emptySet(),
    val scheduleActive: Boolean = false,
    val grantedUntil: Map<String, Long> = emptyMap(),
    val deepFocusActive: Boolean = false // MVP: always false; architecture reserved
)

sealed interface BlockDecision {
    data object Allow : BlockDecision
    data object Block : BlockDecision
}

/**
 * Block decision algorithm (section 16):
 * 1. Not protected            -> allow
 * 2. Has valid ActiveGrant    -> allow
 * 3. Deep Focus active        -> block (reserved; MVP always false)
 * 4. Inside active schedule   -> block, else allow
 */
object BlockDecisionEngine {

    fun decide(
        packageName: String,
        snapshot: ProtectionSnapshot,
        nowMillis: Long
    ): BlockDecision {
        if (packageName !in snapshot.protectedPackages) return BlockDecision.Allow

        val grantExpiry = snapshot.grantedUntil[packageName]
        if (grantExpiry != null && grantExpiry > nowMillis) return BlockDecision.Allow

        if (snapshot.deepFocusActive) return BlockDecision.Block

        return if (snapshot.scheduleActive) BlockDecision.Block else BlockDecision.Allow
    }
}
