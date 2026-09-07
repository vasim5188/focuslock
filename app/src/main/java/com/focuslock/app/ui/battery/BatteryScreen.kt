package com.focuslock.app.ui.battery

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
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.focuslock.app.ui.components.FocusCard
import com.focuslock.app.ui.components.ScreenHeader
import com.focuslock.app.util.OemHelper
import com.focuslock.app.util.PermissionChecker

@Composable
fun BatteryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val guidance = remember { OemHelper.guidanceFor() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
    ) {
        ScreenHeader(title = "Battery", onBack = onBack)

        Column(Modifier.padding(horizontal = 20.dp)) {
            FocusCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.BatteryAlert, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.size(12.dp))
                    Text(guidance.manufacturer, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.size(8.dp))
                Text(
                    "${guidance.title}. Aggressive battery optimization can stop Focus Lock in the background, which turns protection off without warning.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.size(16.dp))

            FocusCard {
                Text("Recommended steps", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.size(12.dp))
                guidance.steps.forEachIndexed { i, step ->
                    Row {
                        Text("${i + 1}. ", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                        Text(step, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(Modifier.size(8.dp))
                }
            }

            Spacer(Modifier.size(24.dp))

            Button(
                onClick = {
                    val vendor = guidance.autoStartIntent
                    val intent = if (OemHelper.canOpen(context, vendor)) vendor
                    else PermissionChecker.batteryOptimizationIntent(context)
                    runCatching { context.startActivity(intent) }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
            ) { Text("Fix battery optimization", style = MaterialTheme.typography.labelLarge) }

            Spacer(Modifier.size(12.dp))

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) { Text("Skip") }

            Spacer(Modifier.size(8.dp))
            Text(
                "If you skip this, Focus Lock may be stopped by your device and your apps could become unprotected.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.size(24.dp))
        }
    }
}
