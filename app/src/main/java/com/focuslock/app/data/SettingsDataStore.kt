package com.focuslock.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val onboardingComplete: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val batteryGuidanceDone: Boolean = false
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "focus_lock_settings")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val ONBOARDING = booleanPreferencesKey("onboarding_complete")
        val THEME = stringPreferencesKey("theme_mode")
        val BATTERY = booleanPreferencesKey("battery_guidance_done")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            onboardingComplete = p[Keys.ONBOARDING] ?: false,
            themeMode = runCatching { ThemeMode.valueOf(p[Keys.THEME] ?: "SYSTEM") }
                .getOrDefault(ThemeMode.SYSTEM),
            batteryGuidanceDone = p[Keys.BATTERY] ?: false
        )
    }

    suspend fun setOnboardingComplete(value: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING] = value }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME] = mode.name }
    }

    suspend fun setBatteryGuidanceDone(value: Boolean) {
        context.dataStore.edit { it[Keys.BATTERY] = value }
    }
}
