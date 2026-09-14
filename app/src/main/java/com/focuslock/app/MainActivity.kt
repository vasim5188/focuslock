package com.focuslock.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import com.focuslock.app.data.AppSettings
import com.focuslock.app.ui.FocusLockApp
import com.focuslock.app.ui.FocusViewModelFactory
import com.focuslock.app.ui.theme.FocusLockTheme

class MainActivity : ComponentActivity() {

    override fun onResume() {
        super.onResume()
        FocusLockApplication.from(this).container.protectionEngine.onHostActivityResumed()
    }

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
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                        contentColor = MaterialTheme.colorScheme.onBackground
                    ) {
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
}
