package com.joudjawadtube.player.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.joudjawadtube.player.data.settings.ThemeMode

private fun createDarkColorScheme(colors: JoudColors) = darkColorScheme(
    primary = colors.red,
    onPrimary = Color.White,
    secondary = colors.usbCyan,
    onSecondary = colors.background,
    background = colors.background,
    onBackground = colors.textPrimary,
    surface = colors.surface,
    onSurface = colors.textPrimary,
    surfaceVariant = colors.cardBg,
    onSurfaceVariant = colors.textSecondary
)

private fun createLightColorScheme(colors: JoudColors) = lightColorScheme(
    primary = colors.red,
    onPrimary = Color.White,
    secondary = colors.usbCyan,
    onSecondary = Color.White,
    background = colors.background,
    onBackground = colors.textPrimary,
    surface = colors.surface,
    onSurface = colors.textPrimary,
    surfaceVariant = colors.cardBg,
    onSurfaceVariant = colors.textSecondary
)

@Composable
fun JoudJawadTubeTheme(
    themeMode: ThemeMode = ThemeMode.FOLLOW_SYSTEM,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.FOLLOW_SYSTEM -> isSystemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val joudColors = if (isDark) DarkJoudColors else LightJoudColors
    val colorScheme = if (isDark) createDarkColorScheme(joudColors) else createLightColorScheme(joudColors)

    CompositionLocalProvider(LocalJoudColors provides joudColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
