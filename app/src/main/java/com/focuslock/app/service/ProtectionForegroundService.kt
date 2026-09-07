package com.focuslock.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import com.focuslock.app.FocusLockApplication
import com.focuslock.app.util.NotificationHelper

/**
 * Keeps the process alive so protection survives in the background, exposes the
 * persistent notification Android requires, and listens for clock/timezone
 * changes at runtime (implicit TIME_SET can't be declared in the manifest).
 */
class ProtectionForegroundService : LifecycleService() {

    private val timeChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // Re-evaluate on time/timezone/date changes so schedules stay correct.
            FocusLockApplication.from(context).container.protectionEngine.recover()
        }
    }

    override fun onCreate() {
        super.onCreate()
        val notification = NotificationHelper.buildForegroundNotification(
            this, "Protecting your focus"
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NotificationHelper.FOREGROUND_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NotificationHelper.FOREGROUND_NOTIFICATION_ID, notification)
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_DATE_CHANGED)
        }
        registerReceiver(timeChangeReceiver, filter)

        FocusLockApplication.from(this).container.protectionEngine.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(timeChangeReceiver) }
        super.onDestroy()
    }
}
