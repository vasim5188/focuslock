package com.focuslock.app.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.focuslock.app.FocusLockApplication

/**
 * Detects foreground-app changes via accessibility events (not polling) and
 * forwards them to the ProtectionEngine, which decides whether to block.
 */
class FocusAccessibilityService : AccessibilityService() {

    private val engine get() = FocusLockApplication.from(this).container.protectionEngine

    override fun onServiceConnected() {
        super.onServiceConnected()
        engine.accessibilityConnected = true
        engine.start()
        engine.recover()
        ProtectionServiceLauncher.start(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) return

        // The active window's package is the true current foreground app.
        val pkg = runCatching { rootInActiveWindow?.packageName?.toString() }.getOrNull()
            ?: event.packageName?.toString()
            ?: return

        engine.onForegroundPackage(pkg)
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        engine.accessibilityConnected = false
        return super.onUnbind(intent)
    }
}
