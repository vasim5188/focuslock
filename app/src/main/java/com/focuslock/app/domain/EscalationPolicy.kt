package com.focuslock.app.domain

/**
 * Escalating wait system. The counter is GLOBAL across all protected apps and
 * counts completed waits that earned access *today*.
 *
 * | Deliberate unlock | Required wait |
 * | 1st  | 60s  |
 * | 2nd  | 3m   |
 * | 3rd  | 7m   |
 * | 4th  | 15m  |
 * | 5th+ | 30m  |
 */
object EscalationPolicy {

    /** Fixed temporary access length for every successful unlock. */
    const val GRANT_DURATION_SECONDS = 10 * 60

    /**
     * Required wait for the NEXT deliberate unlock, given how many deliberate
     * unlocks have ALREADY happened today ([unlocksAlreadyToday], 0-based).
     */
    fun requiredWaitSeconds(unlocksAlreadyToday: Int): Int = when (unlocksAlreadyToday) {
        0 -> 60          // 1st unlock
        1 -> 3 * 60      // 2nd
        2 -> 7 * 60      // 3rd
        3 -> 15 * 60     // 4th
        else -> 30 * 60  // 5th and beyond
    }
}
