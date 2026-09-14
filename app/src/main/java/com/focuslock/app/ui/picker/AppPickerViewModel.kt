package com.focuslock.app.ui.picker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.focuslock.app.data.repository.FocusRepository
import com.focuslock.app.data.repository.MAX_FREE_APPS
import com.focuslock.app.service.ProtectionServiceLauncher
import com.focuslock.app.util.InstalledApp
import com.focuslock.app.util.InstalledAppsProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppPickerViewModel(
    app: Application,
    private val repository: FocusRepository
) : AndroidViewModel(app) {

    val maxApps = MAX_FREE_APPS

    private val allApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query
    private val _limitHit = MutableStateFlow(false)
    val limitHit: StateFlow<Boolean> = _limitHit

    val selectedPackages: StateFlow<Set<String>> =
        repository.blockedApps
            .map { list -> list.map { it.packageName }.toSet() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val visibleApps: StateFlow<List<InstalledApp>> =
        combine(allApps, _query) { apps, q ->
            if (q.isBlank()) apps
            else apps.filter { it.label.contains(q, ignoreCase = true) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            allApps.value = InstalledAppsProvider.loadApps(getApplication())
            _loading.value = false
        }
    }

    fun setQuery(q: String) { _query.value = q }

    fun dismissLimit() { _limitHit.value = false }

    fun toggle(app: InstalledApp, onAdded: () -> Unit = {}) {
        viewModelScope.launch {
            val current = selectedPackages.value
            if (app.packageName in current) {
                repository.removeBlockedApp(app.packageName)
            } else {
                val added = repository.addBlockedApp(app.packageName, app.label)
                if (!added) {
                    _limitHit.value = true
                } else {
                    ProtectionServiceLauncher.start(getApplication())
                    onAdded()
                }
            }
        }
    }
}
