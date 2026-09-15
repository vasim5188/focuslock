package com.focuslock.app.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.ui.semantics.Role
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
import com.focuslock.app.util.PermissionChecker

@Composable
fun SettingsScreen(
    factory: ViewModelProvider.Factory,
    onBack: () -> Unit,
    onPermissions: () -> Unit,
    onBattery: () -> Unit,
    onPrivacy: () -> Unit,
    onAbout: () -> Unit,
    appLockEnabled: Boolean,
    appLockError: String?,
    onChangeAppLock: (Boolean) -> Unit
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
            SectionLabel("Access")
            FocusCard {
                NavRow(Icons.Rounded.Accessibility, "Permissions", "Accessibility, overlay and notifications", "settings-permissions", onPermissions)

                NavRow(Icons.Rounded.BatteryAlert, "Background protection", "Allow background activity", "settings-battery", onBattery)
            }

            Spacer(Modifier.size(20.dp))
            SectionLabel("Security")
            FocusCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Lock Focus Lock", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Ask for your phone's fingerprint, face unlock or screen lock when opening this app.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = appLockEnabled, onCheckedChange = onChangeAppLock)
                }
                appLockError?.let {
                    Spacer(Modifier.size(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(Modifier.size(20.dp))
            SectionLabel("Appearance")
            FocusCard {
                Text("Theme", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.size(12.dp))
                Text(
                    "Applies to the app and the blocking screen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(Modifier.selectableGroup()) {
                    ThemeOption("Use device setting", settings.themeMode == ThemeMode.SYSTEM) { vm.setTheme(ThemeMode.SYSTEM) }
                    ThemeOption("Light", settings.themeMode == ThemeMode.LIGHT) { vm.setTheme(ThemeMode.LIGHT) }
                    ThemeOption("Dark", settings.themeMode == ThemeMode.DARK) { vm.setTheme(ThemeMode.DARK) }
                }
            }

            if (vm.supportVisible) {
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
            }

            Spacer(Modifier.size(20.dp))
            SectionLabel("Information")
            FocusCard {
                NavRow(Icons.Rounded.Info, "About", "Version ${vm.versionName}", "settings-about", onAbout)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = onPrivacy)
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
private fun ThemeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.size(12.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}
