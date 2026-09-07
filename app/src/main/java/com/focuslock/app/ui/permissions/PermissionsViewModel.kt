package com.focuslock.app.ui.permissions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.focuslock.app.util.PermissionChecker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class PermissionStatuses(
    val usageAccess: Boolean = false,
    val overlay: Boolean = false,
    val accessibility: Boolean = false,
    val notifications: Boolean = false,
    val batteryUnrestricted: Boolean = false
) {
    val allCriticalGranted: Boolean get() = overlay && accessibility
}

class PermissionsViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(PermissionStatuses())
    val state: StateFlow<PermissionStatuses> = _state

    init { refresh() }

    fun refresh() {
        val ctx = getApplication<Application>()
        _state.value = PermissionStatuses(
            usageAccess = PermissionChecker.hasUsageAccess(ctx),
            overlay = PermissionChecker.hasOverlay(ctx),
            accessibility = PermissionChecker.isAccessibilityEnabled(ctx),
            notifications = PermissionChecker.hasNotifications(ctx),
            batteryUnrestricted = PermissionChecker.isIgnoringBatteryOptimizations(ctx)
        )
    }
}
