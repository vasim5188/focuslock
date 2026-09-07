package com.focuslock.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.focuslock.app.data.db.BlockedApp
import com.focuslock.app.data.db.Schedule
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
    val schedule: Schedule? = null
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
        repository.schedule,
        repository.observeTodayUnlockCount(),
        ticker
    ) { apps, schedule, count, _ ->
        val ctx = getApplication<Application>()
        val permsOk = PermissionChecker.protectionOperational(ctx)
        val now = TimeProvider.nowLocalDateTime()
        val active = ScheduleEvaluator.isActive(schedule, now)
        val end = ScheduleEvaluator.currentWindowEnd(schedule, now)
        HomeUiState(
            protectionActive = active && permsOk,
            permissionsOk = permsOk,
            blockedApps = apps,
            unlockCount = count ?: 0,
            scheduleEndLabel = end?.let { ScheduleEvaluator.formatMinuteOfDay(it.hour * 60 + it.minute) },
            schedule = schedule
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())
}
