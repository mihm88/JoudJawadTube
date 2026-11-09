package com.joudjawadtube.player.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class JoudColors(
    val background: Color,
    val sidebarBg: Color,
    val surface: Color,
    val cardBg: Color,
    val cardFocusedBg: Color,
    val red: Color,
    val redDark: Color,
    val focusBorder: Color,
    val usbCyan: Color,
    val gold4K: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val overlayDark: Color,
    val isDark: Boolean
)

val DarkJoudColors = JoudColors(
    background = Color(0xFF0F0F0F),
    sidebarBg = Color(0xFF141414),
    surface = Color(0xFF1A1A1A),
    cardBg = Color(0xFF212121),
    cardFocusedBg = Color(0xFF2C2C2C),
    red = Color(0xFFFF0000),
    redDark = Color(0xFFB71C1C),
    focusBorder = Color(0xFFFFFFFF),
    usbCyan = Color(0xFF00E5FF),
    gold4K = Color(0xFFFFB300),
    textPrimary = Color(0xFFF5F5F5),
    textSecondary = Color(0xFFAAAAAA),
    overlayDark = Color(0xCC000000),
    isDark = true
)

val LightJoudColors = JoudColors(
    background = Color(0xFFF8F9FA),
    sidebarBg = Color(0xFFEEEEEE),
    surface = Color(0xFFFFFFFF),
    cardBg = Color(0xFFEBEBEB),
    cardFocusedBg = Color(0xFFD6D6D6),
    red = Color(0xFFE50914),
    redDark = Color(0xFFB71C1C),
    focusBorder = Color(0xFF111111),
    usbCyan = Color(0xFF00838F),
    gold4K = Color(0xFFB26A00),
    textPrimary = Color(0xFF111111),
    textSecondary = Color(0xFF666666),
    overlayDark = Color(0xCC000000),
    isDark = false
)

val LocalJoudColors = staticCompositionLocalOf { DarkJoudColors }

val SmartTubeBackground: Color
    @Composable @ReadOnlyComposable get() = LocalJoudColors.current.background

val SmartTubeSidebarBg: Color
    @Composable @ReadOnlyComposable get() = LocalJoudColors.current.sidebarBg

val SmartTubeSurface: Color
    @Composable @ReadOnlyComposable get() = LocalJoudColors.current.surface

val SmartTubeCardBg: Color
    @Composable @ReadOnlyComposable get() = LocalJoudColors.current.cardBg

val SmartTubeCardFocusedBg: Color
    @Composable @ReadOnlyComposable get() = LocalJoudColors.current.cardFocusedBg

val SmartTubeRed: Color
    @Composable @ReadOnlyComposable get() = LocalJoudColors.current.red

val SmartTubeRedDark: Color
    @Composable @ReadOnlyComposable get() = LocalJoudColors.current.redDark

val SmartTubeFocusBorder: Color
    @Composable @ReadOnlyComposable get() = LocalJoudColors.current.focusBorder

val SmartTubeUsbCyan: Color
    @Composable @ReadOnlyComposable get() = LocalJoudColors.current.usbCyan

val SmartTubeGold4K: Color
    @Composable @ReadOnlyComposable get() = LocalJoudColors.current.gold4K

val SmartTubeTextPrimary: Color
    @Composable @ReadOnlyComposable get() = LocalJoudColors.current.textPrimary

val SmartTubeTextSecondary: Color
    @Composable @ReadOnlyComposable get() = LocalJoudColors.current.textSecondary

val SmartTubeOverlayDark: Color
    @Composable @ReadOnlyComposable get() = LocalJoudColors.current.overlayDark
