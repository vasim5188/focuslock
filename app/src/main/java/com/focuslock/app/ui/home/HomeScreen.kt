package com.focuslock.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.focuslock.app.ui.components.FocusCard
import com.focuslock.app.ui.components.NavRow
import com.focuslock.app.ui.components.StatusDot
import com.focuslock.app.domain.ScheduleEvaluator
import com.focuslock.app.domain.Days
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HomeScreen(
    factory: ViewModelProvider.Factory,
    onEditApps: () -> Unit,
    onEditSchedule: () -> Unit,
    onSettings: () -> Unit,
    onFixProtection: () -> Unit
) {
    val vm: HomeViewModel = viewModel(factory = factory)
    val state by vm.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "FOCUS LOCK",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onSettings) {
                Icon(Icons.Rounded.Settings, contentDescription = "Settings")
            }
        }

        Spacer(Modifier.size(16.dp))

        FocusCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(active = state.protectionActive)
                Spacer(Modifier.size(10.dp))
                Text(
                    if (state.protectionActive) "Protection Active" else "Protection Stopped",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
            Spacer(Modifier.size(8.dp))
            val subtitle = when {
                state.protectionActive && state.scheduleEndLabel != null ->
                    "Locked until ${state.scheduleEndLabel}"
                !state.permissionsOk ->
                    "Your apps are currently not protected."
                state.blockedApps.isEmpty() ->
                    "Add an app to start protecting your focus."
                else ->
                    "You're outside your focus hours right now."
            }
            Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (!state.permissionsOk) {
                Spacer(Modifier.size(16.dp))
                Button(
                    onClick = onFixProtection,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) { Text("Fix protection", style = MaterialTheme.typography.labelLarge) }
            }
        }

        Spacer(Modifier.size(16.dp))

        FocusCard {
            Text("Protected apps", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.size(12.dp))
            if (state.blockedApps.isEmpty()) {
                Text("No apps protected yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                state.blockedApps.forEach { app ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(app.appLabel, style = MaterialTheme.typography.bodyLarge)
                            val schedule = state.schedule
                            val scheduleLabel = when {
                                schedule == null -> "No schedule set"
                                !schedule.isEnabled -> "Schedule off"
                                schedule.startMinuteOfDay == schedule.endMinuteOfDay || schedule.activeDays == 0 -> "No active hours"
                                else -> {
                                    val days = Days.orderedDays.filter { Days.isActive(schedule.activeDays, it) }
                                        .joinToString(", ") { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
                                    val overnight = if (schedule.startMinuteOfDay > schedule.endMinuteOfDay) " (ends next day)" else ""
                                    "${ScheduleEvaluator.formatMinuteOfDay(schedule.startMinuteOfDay)} – ${ScheduleEvaluator.formatMinuteOfDay(schedule.endMinuteOfDay)}$overnight\n$days"
                                }
                            }
                            Text(scheduleLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.size(16.dp))

        Text(
            "${state.unlockCount} ${if (state.unlockCount == 1) "unlock" else "unlocks"} today",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Spacer(Modifier.size(16.dp))

        FocusCard {
            NavRow(Icons.Rounded.Apps, "Choose apps to block", "${state.blockedApps.size} of 2 selected", "home-edit-apps", onEditApps)
            NavRow(Icons.Rounded.Schedule, "Edit schedule", null, "home-edit-schedule", onEditSchedule)
            NavRow(Icons.Rounded.Settings, "Settings", null, "home-settings", onSettings)
        }

        Spacer(Modifier.size(24.dp))
    }
}
