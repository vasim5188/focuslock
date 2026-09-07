package com.focuslock.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.focuslock.app.data.ThemeMode

private val DarkColors = darkColorScheme(
    primary = Mint,
    onPrimary = DarkBg,
    secondary = MintDark,
    onSecondary = DarkBg,
    tertiary = Amber,
    background = DarkBg,
    onBackground = DarkOnBg,
    surface = DarkSurface,
    onSurface = DarkOnBg,
    surfaceVariant = DarkSurfaceAlt,
    onSurfaceVariant = DarkOnMuted,
    outline = DarkOutline,
    error = Danger,
    onError = DarkBg
)

private val LightColors = lightColorScheme(
    primary = MintDark,
    onPrimary = LightSurface,
    secondary = Mint,
    onSecondary = LightOnBg,
    tertiary = Amber,
    background = LightBg,
    onBackground = LightOnBg,
    surface = LightSurface,
    onSurface = LightOnBg,
    surfaceVariant = LightSurfaceAlt,
    onSurfaceVariant = LightOnMuted,
    outline = LightOutline,
    error = Danger,
    onError = LightSurface
)

@Composable
fun FocusLockTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = FocusTypography,
        content = content
    )
}
