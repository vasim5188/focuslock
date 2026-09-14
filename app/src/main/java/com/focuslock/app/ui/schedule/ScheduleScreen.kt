package com.focuslock.app.ui.schedule

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.focuslock.app.domain.Days
import com.focuslock.app.domain.ScheduleEvaluator
import com.focuslock.app.ui.components.FocusCard
import com.focuslock.app.ui.components.ScreenHeader
import com.focuslock.app.ui.components.ProtectionSetupDialog
import com.focuslock.app.util.PermissionChecker
import java.time.DayOfWeek

@Composable
fun ScheduleScreen(
    factory: ViewModelProvider.Factory,
    onBack: () -> Unit,
    onOpenPermissions: () -> Unit
) {
    val vm: ScheduleViewModel = viewModel(factory = factory)
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var missingPermissions by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }

    if (missingPermissions.isNotEmpty()) {
        ProtectionSetupDialog(
            savedMessage = "Your schedule was saved.",
            missingPermissions = missingPermissions,
            onOpenPermissions = {
                missingPermissions = emptyList()
                onOpenPermissions()
            },
            onDismiss = {
                missingPermissions = emptyList()
                onBack()
            }
        )
    }

    fun openTimePicker(initial: Int, onPicked: (Int) -> Unit) {
        TimePickerDialog(
            context,
            { _, h, m -> onPicked(h * 60 + m) },
            initial / 60, initial % 60, false
        ).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
    ) {
        ScreenHeader(title = "Schedule", onBack = onBack)

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {

            FocusCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Enable schedule", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Apps are only locked during this window.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = state.enabled, onCheckedChange = vm::setEnabled)
                }
            }

            Spacer(Modifier.size(16.dp))

            FocusCard {
                TimeRow("Start", state.startMinute) { openTimePicker(state.startMinute, vm::setStart) }
                Spacer(Modifier.size(8.dp))
                TimeRow("End", state.endMinute) { openTimePicker(state.endMinute, vm::setEnd) }
                if (state.endMinute <= state.startMinute) {
                    Spacer(Modifier.size(8.dp))
                    Text(
                        "This window crosses midnight (ends the next day).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            Spacer(Modifier.size(16.dp))

            FocusCard {
                Text("Active days", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.size(12.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Days.orderedDays.forEach { day ->
                        DayChip(day, Days.isActive(state.activeDays, day)) { vm.toggleDay(day) }
                    }
                }
            }

            Spacer(Modifier.size(24.dp))

            Button(
                onClick = {
                    vm.save {
                        val missing = if (state.enabled) {
                            PermissionChecker.missingProtectionPermissions(context)
                        } else emptyList()
                        if (missing.isEmpty()) onBack() else missingPermissions = missing
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
            ) { Text("Save schedule", style = MaterialTheme.typography.labelLarge) }

            Spacer(Modifier.size(24.dp))
        }
    }
}

@Composable
private fun TimeRow(label: String, minute: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(
            ScheduleEvaluator.formatMinuteOfDay(minute),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun DayChip(day: DayOfWeek, selected: Boolean, onClick: () -> Unit) {
    val label = day.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    Box(
        modifier = Modifier
            .size(width = 60.dp, height = 44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
