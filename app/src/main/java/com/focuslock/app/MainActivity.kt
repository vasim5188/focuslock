package com.focuslock.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.focuslock.app.data.AppSettings
import com.focuslock.app.ui.FocusLockApp
import com.focuslock.app.ui.FocusViewModelFactory
import com.focuslock.app.ui.theme.FocusLockTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = FocusLockApplication.from(this).container
        val factory = FocusViewModelFactory(application, container)

        setContent {
            val settings by produceState<AppSettings?>(initialValue = null) {
                container.settings.settings.collect { value = it }
            }
            settings?.let { s ->
                FocusLockTheme(themeMode = s.themeMode) {
                    FocusLockApp(
                        factory = factory,
                        settingsStore = container.settings,
                        onboardingComplete = s.onboardingComplete
                    )
                }
            }
        }
    }
}
