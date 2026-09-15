package com.focuslock.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.focuslock.app.data.db.BlockedApp
import com.focuslock.app.data.db.AppScheduleWindow
import com.focuslock.app.data.repository.FocusRepository
import com.focuslock.app.domain.ScheduleEvaluator
import com.focuslock.app.util.PermissionChecker
import com.focuslock.app.util.TimeProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val protectionActive: Boolean = false,
    val permissionsOk: Boolean = false,
    val blockedApps: List<BlockedApp> = emptyList(),
    val unlockCount: Int = 0,
    val scheduleEndLabel: String? = null,
    val windows: List<AppScheduleWindow> = emptyList()
)

class HomeViewModel(
    app: Application,
    repository: FocusRepository
) : AndroidViewModel(app) {

    private val ticker = flow {
        while (true) { emit(Unit); delay(1000) }
    }

    val state: StateFlow<HomeUiState> = combine(
        repository.blockedApps,
        repository.appScheduleWindows,
        repository.observeTodayUnlockCount(),
        ticker
    ) { apps, windows, count, _ ->
        val ctx = getApplication<Application>()
        val permsOk = PermissionChecker.protectionOperational(ctx)
        val now = TimeProvider.nowLocalDateTime()
        val active = apps.any { ScheduleEvaluator.isActive(windows, it.packageName, now) }
        val end = apps.mapNotNull { ScheduleEvaluator.currentWindowEnd(windows, it.packageName, now) }.minOrNull()
        HomeUiState(
            protectionActive = active && permsOk,
            permissionsOk = permsOk,
            blockedApps = apps,
            unlockCount = count ?: 0,
            scheduleEndLabel = end?.let { ScheduleEvaluator.formatMinuteOfDay(it.hour * 60 + it.minute) },
            windows = windows
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())
}
