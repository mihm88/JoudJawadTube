package com.joudjawadtube.player.ui.components

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.joudjawadtube.player.data.settings.UiModePreference

enum class DeviceFormFactor {
    TV,
    TABLET,
    PHONE;

    companion object {
        /**
         * Automatically classifies the current device into TV, Tablet, or Phone.
         * Uses [smallestScreenWidthDp] (< 600dp) so phones stay detected as PHONE
         * in both portrait and landscape orientations.
         */
        fun detect(
            isTelevisionHardware: Boolean,
            smallestScreenWidthDp: Int,
            screenWidthDp: Int
        ): DeviceFormFactor = when {
            isTelevisionHardware -> TV
            smallestScreenWidthDp in 1 until 600 -> PHONE
            screenWidthDp in 1 until 600 -> PHONE
            else -> TABLET
        }

        fun resolveEffectiveFormFactor(
            preference: UiModePreference,
            detected: DeviceFormFactor
        ): DeviceFormFactor = when (preference) {
            UiModePreference.AUTO -> detected
            UiModePreference.FORCE_TV -> TV
            UiModePreference.FORCE_TABLET -> TABLET
            UiModePreference.FORCE_PHONE -> PHONE
        }
    }
}

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
    val screenHeightDp: Dp,
    val smallestScreenWidthDp: Dp = screenWidthDp,
    val detectedFormFactor: DeviceFormFactor = DeviceFormFactor.TV,
    val effectiveFormFactor: DeviceFormFactor = DeviceFormFactor.TV
) {
    val isCompact: Boolean get() = widthClass == WindowWidthClass.Compact
    val isMedium: Boolean get() = widthClass == WindowWidthClass.Medium
    val isExpanded: Boolean get() = widthClass == WindowWidthClass.Expanded
    val isPortrait: Boolean get() = !isLandscape
    val isTv: Boolean get() = effectiveFormFactor == DeviceFormFactor.TV
    val isTablet: Boolean get() = effectiveFormFactor == DeviceFormFactor.TABLET
    val isPhone: Boolean get() = effectiveFormFactor == DeviceFormFactor.PHONE
    val isTouchDevice: Boolean get() = !isTv
}

val LocalWindowLayoutInfo = compositionLocalOf {
    WindowLayoutInfo(
        widthClass = WindowWidthClass.Expanded,
        isLandscape = true,
        screenWidthDp = 1920.dp,
        screenHeightDp = 1080.dp,
        smallestScreenWidthDp = 1080.dp,
        detectedFormFactor = DeviceFormFactor.TV,
        effectiveFormFactor = DeviceFormFactor.TV
    )
}

@Composable
fun ProvideWindowLayoutInfo(
    isTelevisionHardware: Boolean = false,
    uiModePreference: UiModePreference = UiModePreference.AUTO,
    content: @Composable () -> Unit
) {
    val configuration = LocalConfiguration.current
    val widthDp = configuration.screenWidthDp.dp
    val heightDp = configuration.screenHeightDp.dp
    val smallestWidthDpInt = configuration.smallestScreenWidthDp
        .takeIf { it != Configuration.SMALLEST_SCREEN_WIDTH_DP_UNDEFINED && it > 0 }
        ?: minOf(configuration.screenWidthDp, configuration.screenHeightDp)
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val widthClass = when {
        widthDp < 600.dp -> WindowWidthClass.Compact
        widthDp < 840.dp -> WindowWidthClass.Medium
        else -> WindowWidthClass.Expanded
    }

    val detected = DeviceFormFactor.detect(
        isTelevisionHardware = isTelevisionHardware,
        smallestScreenWidthDp = smallestWidthDpInt,
        screenWidthDp = configuration.screenWidthDp
    )
    val effective = DeviceFormFactor.resolveEffectiveFormFactor(
        preference = uiModePreference,
        detected = detected
    )

    val info = WindowLayoutInfo(
        widthClass = widthClass,
        isLandscape = isLandscape,
        screenWidthDp = widthDp,
        screenHeightDp = heightDp,
        smallestScreenWidthDp = smallestWidthDpInt.dp,
        detectedFormFactor = detected,
        effectiveFormFactor = effective
    )

    CompositionLocalProvider(LocalWindowLayoutInfo provides info) {
        content()
    }
}
