package com.focuslock.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.focuslock.app.data.SettingsDataStore
import com.focuslock.app.ui.about.AboutScreen
import com.focuslock.app.ui.about.PrivacyScreen
import com.focuslock.app.ui.battery.BatteryScreen
import com.focuslock.app.ui.home.HomeScreen
import com.focuslock.app.ui.onboarding.OnboardingScreen
import com.focuslock.app.ui.permissions.PermissionsScreen
import com.focuslock.app.ui.picker.AppPickerScreen
import com.focuslock.app.ui.schedule.ScheduleScreen
import com.focuslock.app.ui.settings.SettingsScreen
import kotlinx.coroutines.launch

object Routes {
    const val ONBOARDING = "onboarding"
    const val PERMISSIONS = "permissions"
    const val HOME = "home"
    const val PICKER = "picker"
    const val SCHEDULE = "schedule"
    const val SETTINGS = "settings"
    const val BATTERY = "battery"
    const val ABOUT = "about"
    const val PRIVACY = "privacy"
}

@Composable
fun FocusLockApp(
    factory: ViewModelProvider.Factory,
    settingsStore: SettingsDataStore,
    onboardingComplete: Boolean,
    appLockEnabled: Boolean,
    appLockError: String?,
    onChangeAppLock: (Boolean) -> Unit
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val start = rememberSaveable { if (onboardingComplete) Routes.HOME else Routes.ONBOARDING }

    NavHost(navController = navController, startDestination = start) {

        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onGetStarted = {
                    navController.navigate("${Routes.PERMISSIONS}?onboarding=true")
                }
            )
        }

        composable(
            route = "${Routes.PERMISSIONS}?onboarding={onboarding}",
            arguments = listOf(navArgument("onboarding") {
                type = NavType.BoolType; defaultValue = false
            })
        ) { backStack ->
            val isOnboarding = backStack.arguments?.getBoolean("onboarding") ?: false
            PermissionsScreen(
                factory = factory,
                isOnboarding = isOnboarding,
                onBack = { navController.popBackStack() },
                onFinishOnboarding = {
                    scope.launch { settingsStore.setOnboardingComplete(true) }
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                factory = factory,
                onEditApps = { navController.navigate(Routes.PICKER) },
                onEditSchedule = { navController.navigate(Routes.SCHEDULE) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
                onFixProtection = { navController.navigate("${Routes.PERMISSIONS}?onboarding=false") }
            )
        }

        composable(Routes.PICKER) {
            AppPickerScreen(
                factory = factory,
                onBack = { navController.popBackStack() },
                onOpenPermissions = { navController.navigate("${Routes.PERMISSIONS}?onboarding=false") }
            )
        }

        composable(Routes.SCHEDULE) {
            ScheduleScreen(
                factory = factory,
                onBack = { navController.popBackStack() },
                onOpenPermissions = { navController.navigate("${Routes.PERMISSIONS}?onboarding=false") }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                factory = factory,
                onBack = { navController.popBackStack() },
                onPermissions = { navController.navigate("${Routes.PERMISSIONS}?onboarding=false") },
                onBattery = { navController.navigate(Routes.BATTERY) },
                onPrivacy = { navController.navigate(Routes.PRIVACY) },
                onAbout = { navController.navigate(Routes.ABOUT) },
                appLockEnabled = appLockEnabled,
                appLockError = appLockError,
                onChangeAppLock = onChangeAppLock
            )
        }

        composable(Routes.BATTERY) {
            BatteryScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.ABOUT) {
            AboutScreen(factory = factory, onBack = { navController.popBackStack() }, onPrivacy = { navController.navigate(Routes.PRIVACY) })
        }
        composable(Routes.PRIVACY) {
            PrivacyScreen(onBack = { navController.popBackStack() })
        }
    }
}
