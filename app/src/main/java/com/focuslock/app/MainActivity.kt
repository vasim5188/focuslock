package com.focuslock.app

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.focuslock.app.data.AppSettings
import com.focuslock.app.ui.FocusLockApp
import com.focuslock.app.ui.FocusViewModelFactory
import com.focuslock.app.ui.theme.FocusLockTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class MainActivity : FragmentActivity() {
    private var unlocked by mutableStateOf(false)
    private var resumed by mutableStateOf(false)
    private var promptInProgress by mutableStateOf(false)
    private var promptAttempted by mutableStateOf(false)
    private var relockJob: Job? = null
    private var lockError by mutableStateOf<String?>(null)
    private var pendingChange: Boolean? = null
    private lateinit var prompt: BiometricPrompt

    override fun onResume() {
        super.onResume()
        relockJob?.cancel()
        relockJob = null
        resumed = true
        FocusLockApplication.from(this).container.protectionEngine.onHostActivityResumed()
    }

    override fun onStop() {
        resumed = false
        // Android may briefly stop the host while closing device-credential UI.
        // Relock only if the app actually remains in the background.
        relockJob?.cancel()
        relockJob = lifecycleScope.launch {
            delay(750)
            if (!resumed && !promptInProgress) {
                unlocked = false
                promptAttempted = false
            }
        }
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    promptInProgress = false
                    lockError = null
                    val change = pendingChange
                    pendingChange = null
                    unlocked = true
                    if (change != null) {
                        lifecycleScope.launch {
                            FocusLockApplication.from(this@MainActivity).container.settings
                                .setAppLockEnabled(change)
                        }
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    promptInProgress = false
                    pendingChange = null
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                        errorCode != BiometricPrompt.ERROR_CANCELED
                    ) lockError = errString.toString()
                }
            }
        )

        val container = FocusLockApplication.from(this).container
        val factory = FocusViewModelFactory(application, container)
        setContent {
            val settings by produceState<AppSettings?>(initialValue = null) {
                container.settings.settings.collect { value = it }
            }
            settings?.let { s ->
                LaunchedEffect(s.appLockEnabled, resumed, unlocked) {
                    if (s.appLockEnabled && resumed && !unlocked && !promptAttempted) {
                        requestAuthentication()
                    }
                }
                FocusLockTheme(themeMode = s.themeMode) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                        contentColor = MaterialTheme.colorScheme.onBackground
                    ) {
                        if (s.appLockEnabled && !unlocked) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(24.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (promptInProgress || !promptAttempted) {
                                    CircularProgressIndicator()
                                    Spacer(Modifier.size(12.dp))
                                    Text("Opening phone authentication…", textAlign = TextAlign.Center)
                                } else {
                                    Icon(Icons.Rounded.Lock, contentDescription = null)
                                    Spacer(Modifier.size(16.dp))
                                    Text(
                                        "Focus Lock is locked",
                                        style = MaterialTheme.typography.headlineSmall,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(Modifier.size(12.dp))
                                    Text(
                                        "Use your phone's fingerprint, face unlock, PIN, pattern or password.",
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    lockError?.let {
                                        Spacer(Modifier.size(8.dp))
                                        Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                                    }
                                    Spacer(Modifier.size(20.dp))
                                    Button(onClick = { requestAuthentication(retry = true) }) {
                                        Text("Unlock Focus Lock")
                                    }
                                }
                            }
                        } else {
                            FocusLockApp(
                                factory = factory,
                                settingsStore = container.settings,
                                onboardingComplete = s.onboardingComplete,
                                appLockEnabled = s.appLockEnabled,
                                appLockError = lockError,
                                onChangeAppLock = ::changeAppLock
                            )
                        }
                    }
                }
            }
        }
    }

    private fun changeAppLock(enabled: Boolean) {
        val keyguard = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (!keyguard.isDeviceSecure) {
            lockError = "Set a screen lock in your phone's Settings before enabling app lock."
            return
        }
        lockError = null
        pendingChange = enabled
        requestAuthentication(retry = true)
    }

    private fun requestAuthentication(retry: Boolean = false) {
        if (promptInProgress || (promptAttempted && !retry)) return
        promptAttempted = true
        promptInProgress = true
        lockError = null
        val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        }
        try {
            prompt.authenticate(
                BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Unlock Focus Lock")
                    .setSubtitle("Confirm with your phone's screen lock")
                    .setAllowedAuthenticators(authenticators)
                    .build()
            )
        } catch (e: RuntimeException) {
            promptInProgress = false
            pendingChange = null
            lockError = e.message ?: "Could not open phone authentication."
        }
    }
}
