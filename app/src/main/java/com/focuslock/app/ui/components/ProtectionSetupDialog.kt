package com.focuslock.app.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun ProtectionSetupDialog(
    savedMessage: String,
    missingPermissions: List<String>,
    onOpenPermissions: () -> Unit,
    onDismiss: () -> Unit
) {
    val required = missingPermissions.joinToString(separator = " and ")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Finish protection setup") },
        text = {
            Text(
                "$savedMessage Focus Lock cannot block apps until $required " +
                    "${if (missingPermissions.size == 1) "is" else "are"} enabled."
            )
        },
        confirmButton = {
            TextButton(onClick = onOpenPermissions) { Text("Set up now") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Not now") }
        }
    )
}
