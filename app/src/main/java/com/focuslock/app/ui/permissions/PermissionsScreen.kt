package com.focuslock.app.ui.permissions

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.DisposableEffect
import com.focuslock.app.ui.components.FocusCard
import com.focuslock.app.ui.components.ScreenHeader
import com.focuslock.app.util.PermissionChecker

@Composable
fun PermissionsScreen(
    factory: ViewModelProvider.Factory,
    isOnboarding: Boolean,
    onBack: () -> Unit,
    onFinishOnboarding: () -> Unit,
    onOpenBattery: () -> Unit
) {
    val vm: PermissionsViewModel = viewModel(factory = factory)
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { vm.refresh() }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
    ) {
        ScreenHeader(title = "Permissions", onBack = if (isOnboarding) null else onBack)

        Column(Modifier.padding(horizontal = 20.dp)) {
            Text(
                "Focus Lock needs these permissions to detect and block distracting apps. Everything stays on your device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.size(16.dp))

            PermissionCard(
                Icons.Rounded.Accessibility,
                "Accessibility Service",
                "Focus Lock uses Accessibility Service to detect when a protected app is opened so it can show the lock screen.",
                state.accessibility
            ) { context.startActivity(PermissionChecker.accessibilitySettingsIntent()) }

            Spacer(Modifier.size(12.dp))
            PermissionCard(
                Icons.Rounded.Layers,
                "Display over other apps",
                "Lets Focus Lock draw the lock screen on top of the app you opened.",
                state.overlay
            ) { context.startActivity(PermissionChecker.overlaySettingsIntent(context)) }

            Spacer(Modifier.size(12.dp))
            PermissionCard(
                Icons.Rounded.QueryStats,
                "Usage Access",
                "Helps Focus Lock reliably confirm which app is in the foreground.",
                state.usageAccess
            ) { context.startActivity(PermissionChecker.usageAccessSettingsIntent()) }

            Spacer(Modifier.size(12.dp))
            PermissionCard(
                Icons.Rounded.Notifications,
                "Notifications",
                "Shows the ongoing notification that keeps protection running in the background.",
                state.notifications
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    context.startActivity(PermissionChecker.notificationSettingsIntent(context))
                }
            }

            Spacer(Modifier.size(20.dp))
            OutlinedButton(
                onClick = onOpenBattery,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) { Text("Battery optimization guidance") }

            Spacer(Modifier.size(12.dp))
            Button(
                onClick = { if (isOnboarding) onFinishOnboarding() else onBack() },
                enabled = state.allCriticalGranted || !isOnboarding,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
            ) {
                Text(if (isOnboarding) "Continue" else "Done", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.size(24.dp))
        }
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    why: String,
    granted: Boolean,
    onGrant: () -> Unit
) {
    FocusCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.size(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (granted) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = "Granted", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("Granted", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        Spacer(Modifier.size(8.dp))
        Text(why, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (!granted) {
            Spacer(Modifier.size(12.dp))
            Button(onClick = onGrant, shape = RoundedCornerShape(20.dp)) {
                Text("Grant permission", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
