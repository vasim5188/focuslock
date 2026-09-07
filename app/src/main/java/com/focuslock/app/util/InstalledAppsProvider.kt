package com.focuslock.app.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap?
)

/** Lists launchable, user-facing apps for the picker, excluding sensitive ones. */
object InstalledAppsProvider {

    private fun Drawable.toImageBitmap(size: Int = 128): ImageBitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        setBounds(0, 0, size, size)
        draw(canvas)
        return bmp.asImageBitmap()
    }

    suspend fun loadApps(context: Context): List<InstalledApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val self = context.packageName

        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val launchable = pm.queryIntentActivities(launcherIntent, 0)
            .mapNotNull { it.activityInfo?.packageName }
            .toSet()

        // Resolve the default home + dialer packages to exclude them.
        val homePkg = pm.resolveActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0
        )?.activityInfo?.packageName
        val dialerPkg = pm.resolveActivity(
            Intent(Intent.ACTION_DIAL), 0
        )?.activityInfo?.packageName

        val excluded = setOfNotNull(self, homePkg, dialerPkg, "com.android.systemui", "android")

        launchable
            .filter { it !in excluded }
            .mapNotNull { pkg ->
                runCatching {
                    val ai = pm.getApplicationInfo(pkg, 0)
                    InstalledApp(
                        packageName = pkg,
                        label = pm.getApplicationLabel(ai).toString(),
                        icon = runCatching { pm.getApplicationIcon(ai).toImageBitmap() }.getOrNull()
                    )
                }.getOrNull()
            }
            .sortedBy { it.label.lowercase() }
    }
}
