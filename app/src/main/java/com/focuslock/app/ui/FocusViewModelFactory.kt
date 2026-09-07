package com.focuslock.app.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.focuslock.app.AppContainer
import com.focuslock.app.ui.home.HomeViewModel
import com.focuslock.app.ui.permissions.PermissionsViewModel
import com.focuslock.app.ui.picker.AppPickerViewModel
import com.focuslock.app.ui.schedule.ScheduleViewModel
import com.focuslock.app.ui.settings.SettingsViewModel

class FocusViewModelFactory(
    private val app: Application,
    private val container: AppContainer
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(HomeViewModel::class.java) ->
            HomeViewModel(app, container.repository)
        modelClass.isAssignableFrom(AppPickerViewModel::class.java) ->
            AppPickerViewModel(app, container.repository)
        modelClass.isAssignableFrom(ScheduleViewModel::class.java) ->
            ScheduleViewModel(app, container.repository)
        modelClass.isAssignableFrom(PermissionsViewModel::class.java) ->
            PermissionsViewModel(app)
        modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
            SettingsViewModel(app, container.settings)
        else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    } as T
}
