package com.focuslock.app.domain.ads

/**
 * Future rewarded-unlock provider abstraction (section 20). NOT used in the MVP.
 *
 * A rewarded-ad path could offer an alternative to the wait, but it must only
 * be implemented after verifying compliance with Google/AdMob policy. The MVP
 * ships a no-op so the wait path is always fully functional and never depends
 * on an ad loading.
 */
interface RewardedUnlockProvider {
    val isAvailable: Boolean
    fun requestRewardedUnlock(packageName: String, callback: (granted: Boolean) -> Unit)
}

/** Default implementation: no rewarded ads. The wait path always works. */
object NoOpRewardedUnlockProvider : RewardedUnlockProvider {
    override val isAvailable: Boolean = false
    override fun requestRewardedUnlock(packageName: String, callback: (Boolean) -> Unit) {
        callback(false)
    }
}
