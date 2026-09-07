package com.focuslock.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.focuslock.app.FocusLockApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** If a protected app is uninstalled, remove it from the protected list. */
class PackageChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) return
        val pkg = intent.data?.schemeSpecificPart ?: return
        val app = FocusLockApplication.from(context)
        CoroutineScope(Dispatchers.Default).launch {
            app.container.repository.removeBlockedApp(pkg)
        }
    }
}
