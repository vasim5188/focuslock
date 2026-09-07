package com.focuslock.app.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

object ProtectionServiceLauncher {
    fun start(context: Context) {
        val intent = Intent(context, ProtectionForegroundService::class.java)
        runCatching { ContextCompat.startForegroundService(context, intent) }
    }

    fun stop(context: Context) {
        runCatching { context.stopService(Intent(context, ProtectionForegroundService::class.java)) }
    }
}
