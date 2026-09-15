package com.focuslock.app.util

import android.content.Context
import android.content.Intent
import android.content.ComponentName
import android.net.Uri
import android.provider.Settings
import android.text.TextUtils
import androidx.core.app.NotificationManagerCompat
import com.focuslock.app.service.FocusAccessibilityService

/** Reads and reports the health of every system permission Focus Lock needs. */
object PermissionChecker {

    fun hasOverlay(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun isAccessibilityEnabled(context: Context): Boolean {
        val expected = ComponentName(context, FocusAccessibilityService::class.java)
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabled)
        while (splitter.hasNext()) {
            if (ComponentName.unflattenFromString(splitter.next()) == expected) return true
        }
        return false
    }

    fun hasNotifications(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** The minimum set required for blocking to actually function. */
    fun protectionOperational(context: Context): Boolean =
        hasOverlay(context) && isAccessibilityEnabled(context)

    fun missingProtectionPermissions(context: Context): List<String> = buildList {
        if (!isAccessibilityEnabled(context)) add("Accessibility Service")
        if (!hasOverlay(context)) add("Display over other apps")
    }

    // ---- Intents to the relevant system settings screens ----------------

    fun overlaySettingsIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )

    fun accessibilitySettingsIntent(): Intent =
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)

    fun appInfoIntent(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))

    fun notificationSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

}
