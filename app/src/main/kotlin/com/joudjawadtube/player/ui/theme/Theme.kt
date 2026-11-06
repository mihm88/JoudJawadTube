package com.joudjawadtube.player.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SmartTubeColorScheme = darkColorScheme(
    primary = SmartTubeRed,
    onPrimary = SmartTubeTextPrimary,
    secondary = SmartTubeUsbCyan,
    onSecondary = SmartTubeBackground,
    background = SmartTubeBackground,
    onBackground = SmartTubeTextPrimary,
    surface = SmartTubeSurface,
    onSurface = SmartTubeTextPrimary,
    surfaceVariant = SmartTubeCardBg,
    onSurfaceVariant = SmartTubeTextSecondary
)

@Composable
fun JoudJawadTubeTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SmartTubeColorScheme,
        typography = Typography,
        content = content
    )
}
