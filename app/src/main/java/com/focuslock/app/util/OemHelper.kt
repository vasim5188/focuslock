package com.focuslock.app.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * OEM-specific battery-optimization guidance. Aggressive vendor power savers
 * frequently kill background services and stop protection.
 */
object OemHelper {

    data class OemGuidance(
        val manufacturer: String,
        val title: String,
        val steps: List<String>,
        val autoStartIntent: Intent?
    )

    private fun component(pkg: String, cls: String): Intent =
        Intent().apply { component = ComponentName(pkg, cls) }

    fun guidanceFor(): OemGuidance {
        val mfg = Build.MANUFACTURER.lowercase()
        return when {
            mfg.contains("xiaomi") || mfg.contains("redmi") || mfg.contains("poco") -> OemGuidance(
                "Xiaomi / Redmi / POCO",
                "MIUI can stop background apps aggressively",
                listOf(
                    "Open Settings > Apps > Manage apps > Focus Lock",
                    "Enable \"Autostart\"",
                    "Set Battery saver to \"No restrictions\"",
                    "Lock Focus Lock in Recents (pull down on the card)"
                ),
                component("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
            )
            mfg.contains("oppo") -> OemGuidance(
                "Oppo",
                "ColorOS may stop background apps",
                listOf(
                    "Settings > Battery > Focus Lock > Allow background activity",
                    "Enable \"Auto-launch\" for Focus Lock",
                    "Lock Focus Lock in Recents"
                ),
                component("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")
            )
            mfg.contains("realme") -> OemGuidance(
                "Realme",
                "Realme UI may stop background apps",
                listOf(
                    "Settings > Battery > Focus Lock > Allow background activity",
                    "Enable \"Auto-launch\"",
                    "Lock Focus Lock in Recents"
                ),
                component("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")
            )
            mfg.contains("vivo") || mfg.contains("iqoo") -> OemGuidance(
                "Vivo / iQOO",
                "Funtouch/OriginOS may stop background apps",
                listOf(
                    "Settings > Battery > Background power consumption > allow Focus Lock",
                    "Settings > Apps > Autostart > enable Focus Lock",
                    "Lock Focus Lock in Recents"
                ),
                component("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")
            )
            mfg.contains("samsung") -> OemGuidance(
                "Samsung",
                "One UI may put Focus Lock to sleep",
                listOf(
                    "Settings > Apps > Focus Lock > Battery",
                    "Set to \"Unrestricted\"",
                    "Settings > Battery > Background usage limits > remove Focus Lock from sleeping apps"
                ),
                component("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity")
            )
            mfg.contains("oneplus") -> OemGuidance(
                "OnePlus",
                "OxygenOS may stop background apps",
                listOf(
                    "Settings > Battery > Battery optimization > Focus Lock > Don't optimize",
                    "Enable \"Allow auto-launch\"",
                    "Lock Focus Lock in Recents"
                ),
                component("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity")
            )
            mfg.contains("huawei") || mfg.contains("honor") -> OemGuidance(
                "Huawei / Honor",
                "EMUI/MagicOS may stop background apps",
                listOf(
                    "Settings > Apps > Focus Lock > Battery > App launch",
                    "Turn off \"Manage automatically\" and enable all switches",
                    "Lock Focus Lock in Recents"
                ),
                component("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")
            )
            else -> OemGuidance(
                Build.MANUFACTURER.ifBlank { "Your device" },
                "Battery optimization may stop background protection",
                listOf(
                    "Open your system Battery settings",
                    "Allow Focus Lock to run in the background without restrictions",
                    "Lock Focus Lock in Recents if your device supports it"
                ),
                null
            )
        }
    }

    /** Some vendor intents don't exist on all firmware; verify before launching. */
    fun canOpen(context: Context, intent: Intent?): Boolean =
        intent != null && intent.resolveActivity(context.packageManager) != null
}
