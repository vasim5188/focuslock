package com.focuslock.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.focuslock.app.FocusLockApplication
import com.focuslock.app.service.ProtectionServiceLauncher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Restores protection after reboot: services restart, engine re-reads state. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON" -> {
                val app = FocusLockApplication.from(context)
                CoroutineScope(Dispatchers.Default).launch {
                    // Only bother starting if the user actually has protected apps.
                    if (app.container.repository.blockedCount() > 0) {
                        app.container.protectionEngine.start()
                        app.container.protectionEngine.recover()
                        ProtectionServiceLauncher.start(context)
                    }
                }
            }
        }
    }
}
