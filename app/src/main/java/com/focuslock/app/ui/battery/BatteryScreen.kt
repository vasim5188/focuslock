package com.focuslock.app.ui.battery

import android.content.ActivityNotFoundException
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.focuslock.app.ui.components.FocusCard
import com.focuslock.app.ui.components.ScreenHeader
import com.focuslock.app.util.PermissionChecker

@Composable
fun BatteryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var error by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState())) {
        ScreenHeader(title = "Background protection", onBack = onBack)
        Column(Modifier.padding(horizontal = 20.dp)) {
            FocusCard {
                Text("Let Focus Lock run in the background", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.size(12.dp))
                Text("How to enable it", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.size(12.dp))
                listOf(
                    "1. Tap Open app settings below. Focus Lock's App info page opens.",
                    "2. Tap Battery usage.",
                    "3. Turn on Allow background activity. On your OnePlus, the switch is blue when enabled.",
                    "4. Use your phone's Back button to return to Focus Lock."
                ).forEach { step ->
                    Text(step, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.size(12.dp))
                }
                Text(
                    "Already switched on? You're done. Leave Allow foreground activity as it is, even if it is greyed out.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.size(12.dp))
                Text(
                    "This helps protection keep working when you're using other apps. On other phones, Battery usage may be called Battery, and the option may be called Unrestricted.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.size(20.dp))
            Button(
                onClick = {
                    error = false
                    try { context.startActivity(PermissionChecker.appInfoIntent(context)) }
                    catch (_: ActivityNotFoundException) { error = true }
                    catch (_: SecurityException) { error = true }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) { Text("Open app settings") }
            if (error) {
                Text("Open phone Settings > Apps > Focus Lock > Battery usage manually.", color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.size(24.dp))
        }
    }
}
