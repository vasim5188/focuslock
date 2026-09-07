package com.focuslock.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.focuslock.app.BuildConfig
import com.focuslock.app.R
import com.focuslock.app.data.AppSettings
import com.focuslock.app.data.SettingsDataStore
import com.focuslock.app.data.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    app: Application,
    private val settings: SettingsDataStore
) : AndroidViewModel(app) {

    val state: StateFlow<AppSettings> =
        settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val versionName: String = BuildConfig.VERSION_NAME
    val donateUrl: String get() = getApplication<Application>().getString(R.string.donate_url)
    val privacyUrl: String get() = getApplication<Application>().getString(R.string.privacy_url)

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }
}
