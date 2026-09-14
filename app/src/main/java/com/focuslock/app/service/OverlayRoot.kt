package com.focuslock.app.service

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focuslock.app.data.AppSettings
import com.focuslock.app.data.SettingsDataStore
import com.focuslock.app.ui.overlay.BlockScreen
import com.focuslock.app.ui.overlay.WaitingScreen
import com.focuslock.app.ui.theme.FocusLockTheme

/** Root composable rendered inside the overlay window. */
@Composable
fun OverlayRoot(engine: ProtectionEngine) {
    val state by engine.overlayState.collectAsStateWithLifecycle()
    val context = LocalContext.current.applicationContext
    val settingsStore = remember(context) { SettingsDataStore(context) }
    val settings by settingsStore.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    FocusLockTheme(themeMode = settings.themeMode) {
        when (val s = state) {
            is OverlayState.Blocking -> BlockScreen(
                state = s,
                onWait = engine::onStartWait,
                onGoBack = engine::onGoBack
            )
            is OverlayState.Waiting -> {
                val remaining by engine.waitRemaining.collectAsStateWithLifecycle()
                WaitingScreen(
                    state = s,
                    remainingSeconds = remaining,
                    onCancel = engine::onCancelWait
                )
            }
            OverlayState.Hidden -> Unit
        }
    }
}
