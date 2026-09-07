package com.focuslock.app.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.focuslock.app.data.ThemeMode
import com.focuslock.app.ui.components.FocusCard
import com.focuslock.app.ui.components.NavRow
import com.focuslock.app.ui.components.ScreenHeader

@Composable
fun SettingsScreen(
    factory: ViewModelProvider.Factory,
    onBack: () -> Unit,
    onEditApps: () -> Unit,
    onEditSchedule: () -> Unit,
    onPermissions: () -> Unit,
    onBattery: () -> Unit,
    onAbout: () -> Unit
) {
    val vm: SettingsViewModel = viewModel(factory = factory)
    val settings by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
    ) {
        ScreenHeader(title = "Settings", onBack = onBack)

        Column(Modifier.padding(horizontal = 20.dp)) {
            SectionLabel("Protection")
            FocusCard {
                NavRow(Icons.Rounded.Apps, "Blocked apps", null, "settings-apps", onEditApps)
                NavRow(Icons.Rounded.Schedule, "Schedule", null, "settings-schedule", onEditSchedule)
            }

            Spacer(Modifier.size(20.dp))
            SectionLabel("Access")
            FocusCard {
                NavRow(Icons.Rounded.Accessibility, "Permissions", "Usage, overlay, accessibility, notifications", "settings-permissions", onPermissions)
                NavRow(Icons.Rounded.BatteryAlert, "Battery optimization", null, "settings-battery", onBattery)
            }

            Spacer(Modifier.size(20.dp))
            SectionLabel("Appearance")
            FocusCard {
                Text("Theme", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.size(12.dp))
                Row {
                    ThemeChip("System", settings.themeMode == ThemeMode.SYSTEM) { vm.setTheme(ThemeMode.SYSTEM) }
                    Spacer(Modifier.size(8.dp))
                    ThemeChip("Light", settings.themeMode == ThemeMode.LIGHT) { vm.setTheme(ThemeMode.LIGHT) }
                    Spacer(Modifier.size(8.dp))
                    ThemeChip("Dark", settings.themeMode == ThemeMode.DARK) { vm.setTheme(ThemeMode.DARK) }
                }
            }

            Spacer(Modifier.size(20.dp))
            SectionLabel("Support")
            FocusCard {
                Text("Enjoying Focus Lock?", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.size(4.dp))
                Text(
                    "If the app has helped you spend less time on your phone, you can support development. Support does not unlock features or remove ads.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.size(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f))
                        .clickable {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(vm.donateUrl)))
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Coffee, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(Modifier.size(12.dp))
                    Text("Buy me a coffee", style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(Modifier.size(20.dp))
            SectionLabel("Information")
            FocusCard {
                NavRow(Icons.Rounded.Info, "About", "Version ${vm.versionName}", "settings-about", onAbout)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(vm.privacyUrl))) }
                        .padding(vertical = 14.dp, horizontal = 4.dp)
                ) {
                    Text("Privacy policy", style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(Modifier.size(32.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
    )
}

@Composable
private fun ThemeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
