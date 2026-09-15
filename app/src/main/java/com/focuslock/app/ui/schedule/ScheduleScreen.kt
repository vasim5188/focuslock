package com.focuslock.app.ui.schedule

import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.focuslock.app.data.db.AppScheduleWindow
import com.focuslock.app.domain.Days
import com.focuslock.app.domain.ScheduleEvaluator
import com.focuslock.app.ui.components.FocusCard
import com.focuslock.app.ui.components.ScreenHeader
import com.focuslock.app.ui.components.ProtectionSetupDialog
import com.focuslock.app.util.PermissionChecker
import java.time.DayOfWeek

@Composable
fun ScheduleScreen(factory: ViewModelProvider.Factory, onBack: () -> Unit, onOpenPermissions: () -> Unit) {
    val vm: ScheduleViewModel = viewModel(factory = factory)
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var editing by remember { mutableStateOf<AppScheduleWindow?>(null) }
    var addingPackage by remember { mutableStateOf<String?>(null) }
    var removing by remember { mutableStateOf<AppScheduleWindow?>(null) }
    var missingPermissions by remember { mutableStateOf<List<String>>(emptyList()) }

    if (missingPermissions.isNotEmpty()) ProtectionSetupDialog(
        savedMessage = "Your time window was saved.", missingPermissions = missingPermissions,
        onOpenPermissions = { missingPermissions = emptyList(); onOpenPermissions() },
        onDismiss = { missingPermissions = emptyList() }
    )

    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState())) {
        ScreenHeader(title = "App schedules", onBack = onBack)
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Set one focus time window for each app.",
                style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (state.apps.isEmpty()) Text("Choose an app to block first, then set its time window.")
            state.apps.forEach { app ->
                FocusCard {
                    Text(app.appLabel, style = MaterialTheme.typography.titleLarge)
                    val window = state.windows.firstOrNull { it.packageName == app.packageName }
                    if (window == null) {
                        Spacer(Modifier.height(8.dp))
                        Text("No time window. This app is not locked yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (window != null) {
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                val nextDay = if (window.startMinuteOfDay > window.endMinuteOfDay) " · ends next day" else ""
                                Text("${ScheduleEvaluator.formatMinuteOfDay(window.startMinuteOfDay)} – ${ScheduleEvaluator.formatMinuteOfDay(window.endMinuteOfDay)}$nextDay",
                                    style = MaterialTheme.typography.titleMedium)
                                Text(if (window.isEnabled) dayLabel(window.activeDays) else "Off · ${dayLabel(window.activeDays)}",
                                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = { editing = window }) { Text("Edit") }
                            IconButton(onClick = { removing = window }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove ${app.appLabel} time window",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    if (window == null) {
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { addingPackage = app.packageName }, modifier = Modifier.fillMaxWidth()) { Text("Set time window") }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    val packageName = editing?.packageName ?: addingPackage
    if (packageName != null) {
        key(editing?.id, packageName) {
            WindowEditor(
                window = editing,
                appLabel = state.apps.firstOrNull { it.packageName == packageName }?.appLabel ?: packageName,
                onDismiss = { editing = null; addingPackage = null },
                onSave = { start, end, days, enabled ->
                    vm.save(packageName, editing?.id ?: 0, start, end, days, enabled) {
                        editing = null; addingPackage = null
                        if (enabled) missingPermissions = PermissionChecker.missingProtectionPermissions(context)
                    }
                }
            )
        }
    }
    removing?.let { window ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text("Remove time window?") },
            text = { Text("${state.apps.firstOrNull { it.packageName == window.packageName }?.appLabel ?: "This app"} will no longer be blocked until you set another time window.") },
            confirmButton = { TextButton(onClick = { vm.delete(window.id); removing = null }) { Text("Remove", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { removing = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun WindowEditor(window: AppScheduleWindow?, appLabel: String, onDismiss: () -> Unit,
    onSave: (Int, Int, Int, Boolean) -> Unit) {
    val context = LocalContext.current
    var start by remember { mutableIntStateOf(window?.startMinuteOfDay ?: 9 * 60) }
    var end by remember { mutableIntStateOf(window?.endMinuteOfDay ?: 18 * 60) }
    var days by remember { mutableIntStateOf(window?.activeDays ?: Days.WEEKDAYS) }
    var enabled by remember { mutableStateOf(window?.isEnabled ?: true) }
    fun pick(initial: Int, update: (Int) -> Unit) {
        TimePickerDialog(context, { _, hour, minute -> update(hour * 60 + minute) }, initial / 60, initial % 60, false).show()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (window == null) "Add $appLabel window" else "Edit $appLabel window") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth().clickable { pick(start) { start = it } }, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Start"); Text(ScheduleEvaluator.formatMinuteOfDay(start), color = MaterialTheme.colorScheme.primary)
                }
                Row(Modifier.fillMaxWidth().clickable { pick(end) { end = it } }, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("End"); Text(ScheduleEvaluator.formatMinuteOfDay(end), color = MaterialTheme.colorScheme.primary)
                }
                if (start > end) Text("Ends the next day", color = MaterialTheme.colorScheme.tertiary)
                Text("Active days")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Days.orderedDays.forEach { day ->
                        FilterChip(selected = Days.isActive(days, day), onClick = { days = Days.toggle(days, day) }, label = { Text(day.name.take(3)) })
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Enabled", modifier = Modifier.weight(1f)); Switch(checked = enabled, onCheckedChange = { enabled = it })
                }
                if (start == end) Text("Start and end must be different.", color = MaterialTheme.colorScheme.error)
                if (days == 0) Text("Choose at least one day.", color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = { Button(onClick = { onSave(start, end, days, enabled) }, enabled = start != end && days != 0) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun dayLabel(mask: Int): String = Days.orderedDays.filter { Days.isActive(mask, it) }
    .joinToString(", ") { it.name.take(3).lowercase().replaceFirstChar { c -> c.uppercase() } }
