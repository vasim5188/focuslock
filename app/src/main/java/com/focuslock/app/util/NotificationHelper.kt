package com.focuslock.app.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.focuslock.app.MainActivity
import com.focuslock.app.R

object NotificationHelper {

    const val CHANNEL_ID = "focus_lock_protection"
    const val FOREGROUND_NOTIFICATION_ID = 1001

    fun ensureChannel(context: Context) {
        val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Protection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Focus Lock protecting your apps in the background."
                setShowBadge(false)
            }
            mgr.createNotificationChannel(channel)
        }
    }

    fun buildForegroundNotification(context: Context, contentText: String): Notification {
        ensureChannel(context)
        val pi = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Focus Lock")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pi)
            .build()
    }
}
