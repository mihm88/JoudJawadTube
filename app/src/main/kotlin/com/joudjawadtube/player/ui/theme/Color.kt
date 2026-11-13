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
    background = Color(0xFF0C0E1A),
    sidebarBg = Color(0xFF121526),
    surface = Color(0xFF171B30),
    cardBg = Color(0xFF1E243D),
    cardFocusedBg = Color(0xFF283152),
    red = Color(0xFF06B6D4),
    redDark = Color(0xFF0E7490),
    focusBorder = Color(0xFF38BDF8),
    usbCyan = Color(0xFFF97316),
    gold4K = Color(0xFFFBBF24),
    textPrimary = Color(0xFFF5F8FA),
    textSecondary = Color(0xFF94A3B8),
    overlayDark = Color(0xCC060812),
    isDark = true
)

val LightJoudColors = JoudColors(
    background = Color(0xFFF4F8FB),
    sidebarBg = Color(0xFFE6F0F6),
    surface = Color(0xFFFFFFFF),
    cardBg = Color(0xFFEAF2F7),
    cardFocusedBg = Color(0xFFD4E6F1),
    red = Color(0xFF00899A),
    redDark = Color(0xFF006978),
    focusBorder = Color(0xFF007786),
    usbCyan = Color(0xFFEA580C),
    gold4K = Color(0xFFD97706),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF475569),
    overlayDark = Color(0xCC060812),
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
