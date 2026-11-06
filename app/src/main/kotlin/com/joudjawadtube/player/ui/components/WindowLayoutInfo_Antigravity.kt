package com.joudjawadtube.player.ui.components

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class WindowWidthClass {
    Compact,   // Phones portrait / narrow windows (< 600 dp)
    Medium,    // Small tablets / phones landscape / foldable (600 - 839 dp)
    Expanded;  // TV / large tablets / desktop (>= 840 dp)

    companion object {
        fun fromWidthDp(widthDp: Float): WindowWidthClass = when {
            widthDp < 600f -> Compact
            widthDp < 840f -> Medium
            else -> Expanded
        }
    }
}

data class WindowLayoutInfo(
    val widthClass: WindowWidthClass,
    val isLandscape: Boolean,
    val screenWidthDp: Dp,
    val screenHeightDp: Dp
) {
    val isCompact: Boolean get() = widthClass == WindowWidthClass.Compact
    val isMedium: Boolean get() = widthClass == WindowWidthClass.Medium
    val isExpanded: Boolean get() = widthClass == WindowWidthClass.Expanded
    val isPortrait: Boolean get() = !isLandscape
}

val LocalWindowLayoutInfo = compositionLocalOf {
    WindowLayoutInfo(
        widthClass = WindowWidthClass.Expanded,
        isLandscape = true,
        screenWidthDp = 1920.dp,
        screenHeightDp = 1080.dp
    )
}

@Composable
fun ProvideWindowLayoutInfo(content: @Composable () -> Unit) {
    val configuration = LocalConfiguration.current
    val widthDp = configuration.screenWidthDp.dp
    val heightDp = configuration.screenHeightDp.dp
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val widthClass = when {
        widthDp < 600.dp -> WindowWidthClass.Compact
        widthDp < 840.dp -> WindowWidthClass.Medium
        else -> WindowWidthClass.Expanded
    }

    val info = WindowLayoutInfo(
        widthClass = widthClass,
        isLandscape = isLandscape,
        screenWidthDp = widthDp,
        screenHeightDp = heightDp
    )

    CompositionLocalProvider(LocalWindowLayoutInfo provides info) {
        content()
    }
}
