package com.focuslock.app.ui.schedule

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.focuslock.app.data.db.Schedule
import com.focuslock.app.data.repository.FocusRepository
import com.focuslock.app.domain.Days
import com.focuslock.app.service.ProtectionServiceLauncher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek

data class ScheduleUiState(
    val startMinute: Int = 9 * 60,
    val endMinute: Int = 18 * 60,
    val activeDays: Int = Days.WEEKDAYS,
    val enabled: Boolean = true,
    val loaded: Boolean = false
)

class ScheduleViewModel(
    app: Application,
    private val repository: FocusRepository
) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(ScheduleUiState())
    val state: StateFlow<ScheduleUiState> = _state

    init {
        viewModelScope.launch {
            val s = repository.getSchedule()
            _state.value = if (s == null) {
                ScheduleUiState(loaded = true)
            } else {
                ScheduleUiState(s.startMinuteOfDay, s.endMinuteOfDay, s.activeDays, s.isEnabled, true)
            }
        }
    }

    fun setStart(minute: Int) { _state.value = _state.value.copy(startMinute = minute) }
    fun setEnd(minute: Int) { _state.value = _state.value.copy(endMinute = minute) }
    fun setEnabled(enabled: Boolean) { _state.value = _state.value.copy(enabled = enabled) }
    fun toggleDay(day: DayOfWeek) {
        _state.value = _state.value.copy(activeDays = Days.toggle(_state.value.activeDays, day))
    }

    fun save(onSaved: () -> Unit) {
        val s = _state.value
        viewModelScope.launch {
            repository.saveSchedule(
                Schedule(
                    id = 1,
                    startMinuteOfDay = s.startMinute,
                    endMinuteOfDay = s.endMinute,
                    activeDays = s.activeDays,
                    isEnabled = s.enabled
                )
            )
            if (repository.blockedCount() > 0) ProtectionServiceLauncher.start(getApplication())
            onSaved()
        }
    }
}
