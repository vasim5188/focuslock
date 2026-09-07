package com.focuslock.app

import android.app.Application
import android.content.Context
import com.focuslock.app.data.SettingsDataStore
import com.focuslock.app.data.db.FocusLockDatabase
import com.focuslock.app.data.repository.FocusRepository
import com.focuslock.app.service.ProtectionEngine
import com.focuslock.app.util.NotificationHelper

/** Manual dependency container (no DI framework needed for this scope). */
class AppContainer(context: Context) {
    val database: FocusLockDatabase = FocusLockDatabase.get(context)
    val repository: FocusRepository = FocusRepository(database)
    val settings: SettingsDataStore = SettingsDataStore(context)
    val protectionEngine: ProtectionEngine = ProtectionEngine(context.applicationContext, repository)
}

class FocusLockApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.ensureChannel(this)
        container.protectionEngine.start()
        container.protectionEngine.recover()
    }

    companion object {
        fun from(context: Context): FocusLockApplication =
            context.applicationContext as FocusLockApplication
    }
}
