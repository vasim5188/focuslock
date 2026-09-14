package com.focuslock.app.ui.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.focuslock.app.ui.components.FocusCard
import com.focuslock.app.ui.components.ScreenHeader

@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    val sections = listOf(
        "What stays on your device" to "Focus Lock stores your selected apps, schedule, preferences, wait progress, temporary access grants and a history of blocking and unlock events locally. This version has no account, analytics or advertising SDK, and does not send these records to a server.",
        "Accessibility access" to "Android Accessibility can access screen content. Focus Lock uses window events and the active window's app identity to detect protected apps. It does not store screen text, messages, passwords or screenshots. You choose whether to enable Accessibility and can turn it off in Android settings.",
        "Other permissions" to "Display over other apps lets Focus Lock show the blocking screen. Notifications show protection status. Access to installed app information lets you choose apps to protect. Background battery settings help protection keep running. Usage Access is not needed by the current blocking engine.",
        "Backups" to "The protection database is excluded from Android backup and device transfer. Basic preferences, such as your theme, may be included in your phone's Android backup or transfer settings.",
        "Your control" to "You can change your protected apps and schedule in Focus Lock and revoke permissions in Android settings. Clearing app storage or uninstalling removes the app's local data. Turning off required permissions stops automatic blocking."
    )
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState())) {
        ScreenHeader(title = "Privacy policy", onBack = onBack)
        Column(Modifier.padding(horizontal = 20.dp)) {
            sections.forEach { (title, body) ->
                FocusCard {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.size(8.dp))
                    Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.size(16.dp))
            }
        }
    }
}
