package com.focuslock.app.ui.schedule

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.focuslock.app.data.db.AppScheduleWindow
import com.focuslock.app.data.db.BlockedApp
import com.focuslock.app.data.repository.FocusRepository
import com.focuslock.app.service.ProtectionServiceLauncher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ScheduleUiState(val apps: List<BlockedApp> = emptyList(), val windows: List<AppScheduleWindow> = emptyList())

class ScheduleViewModel(app: Application, private val repository: FocusRepository) : AndroidViewModel(app) {
    val state: StateFlow<ScheduleUiState> = combine(repository.blockedApps, repository.appScheduleWindows) { apps, windows ->
        ScheduleUiState(apps, windows)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScheduleUiState())

    fun save(packageName: String, id: Long, start: Int, end: Int, days: Int, enabled: Boolean, onSaved: () -> Unit) {
        if (start == end || days == 0) return
        viewModelScope.launch {
            repository.saveAppScheduleWindow(AppScheduleWindow(id, packageName, start, end, days, enabled))
            ProtectionServiceLauncher.start(getApplication())
            onSaved()
        }
    }

    fun delete(id: Long) { viewModelScope.launch { repository.deleteAppScheduleWindow(id) } }
}
