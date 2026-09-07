package com.focuslock.app.service

/** What the single overlay window is currently displaying. */
sealed interface OverlayState {
    data object Hidden : OverlayState

    data class Blocking(
        val packageName: String,
        val appLabel: String,
        val scheduleEndLabel: String?,
        val requiredWaitSeconds: Int
    ) : OverlayState

    data class Waiting(
        val packageName: String,
        val appLabel: String
    ) : OverlayState
}
