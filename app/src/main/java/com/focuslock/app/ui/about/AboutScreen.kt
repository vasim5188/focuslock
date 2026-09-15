package com.focuslock.app.ui.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.focuslock.app.ui.components.FocusCard
import com.focuslock.app.ui.components.ScreenHeader
import com.focuslock.app.ui.settings.SettingsViewModel

@Composable
fun AboutScreen(
    factory: ViewModelProvider.Factory,
    onBack: () -> Unit
) {
    val vm: SettingsViewModel = viewModel(factory = factory)
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
    ) {
        ScreenHeader(title = "About", onBack = onBack)

        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.size(8.dp))
            Text("Focus Lock", style = MaterialTheme.typography.displaySmall.copy())
            Text("Version ${vm.versionName}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.size(24.dp))
            FocusCard {
                Text(
                    "Focus Lock interrupts automatic app-opening and asks you to make a conscious choice. It never punishes you for opening a blocked app \u2014 it simply adds a moment of friction.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.size(12.dp))
                Text(
                    "All data stays on your device. No account, no cloud, no analytics. Core protection works completely offline.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (vm.supportVisible) {
                Spacer(Modifier.size(20.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f))
                        .clickable { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(vm.donateUrl))) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Coffee, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(Modifier.size(12.dp))
                    Text("Buy me a coffee", style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(Modifier.size(24.dp))
        }
    }
}
