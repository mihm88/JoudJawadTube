package com.joudjawadtube.player.ui.theme

import android.os.Build
import android.view.WindowManager
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider

/**
 * Tracks active floating modals/sheets so [LiquidGlassRootContainer] can apply real hardware
 * backdrop blur on Android 12+ (API 31+) behind open dialogs and bottom sheets.
 */
@Stable
class LiquidGlassModalBlurController_Antigravity {
    var activeModalCount by mutableIntStateOf(0)
        private set

    fun acquire() {
        activeModalCount += 1
    }

    fun release() {
        activeModalCount = (activeModalCount - 1).coerceAtLeast(0)
    }
}

val LocalLiquidGlassModalBlurController =
    staticCompositionLocalOf<LiquidGlassModalBlurController_Antigravity?> { null }

/**
 * Root container rendering the Liquid Glass aurora mesh backdrop when [ThemeMode.LIQUID_GLASS] is active,
 * or the standard solid background otherwise. When [enableModalBackdropBlur] is true on Android 12+,
 * smoothly blurs the underlying content whenever a modal dialog or bottom sheet is open.
 */
@Composable
fun LiquidGlassRootContainer(
    modifier: Modifier = Modifier,
    enableModalBackdropBlur: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = LocalJoudColors.current
    val blurController = remember { LiquidGlassModalBlurController_Antigravity() }

    val shouldBlurBackdrop = colors.isLiquidGlass &&
        enableModalBackdropBlur &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        blurController.activeModalCount > 0

    val backdropBlurRadius by animateDpAsState(
        targetValue = if (shouldBlurBackdrop) 12.dp else 0.dp,
        animationSpec = tween(durationMillis = 180),
        label = "liquidGlassModalBackdropBlur"
    )

    CompositionLocalProvider(LocalLiquidGlassModalBlurController provides blurController) {
        if (!colors.isLiquidGlass) {
            Box(
                modifier = modifier.background(colors.background),
                content = content
            )
            return@CompositionLocalProvider
        }

        val ambientModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Modifier
                .fillMaxSize()
                .blur(56.dp)
        } else {
            Modifier.fillMaxSize()
        }

        val contentBlurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && backdropBlurRadius > 0.dp) {
            Modifier
                .fillMaxSize()
                .blur(backdropBlurRadius)
        } else {
            Modifier.fillMaxSize()
        }

        Box(
            modifier = modifier.background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF040919),
                        Color(0xFF0A1633),
                        Color(0xFF081B2E),
                        Color(0xFF060B1E)
                    )
                )
            )
        ) {
            Canvas(modifier = ambientModifier) {
                val w = size.width
                val h = size.height
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x6606B6D4), Color.Transparent),
                        center = Offset(w * 0.16f, h * 0.14f),
                        radius = maxOf(w, h) * 0.55f
                    ),
                    radius = maxOf(w, h) * 0.55f,
                    center = Offset(w * 0.16f, h * 0.14f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x553B82F6), Color.Transparent),
                        center = Offset(w * 0.85f, h * 0.28f),
                        radius = maxOf(w, h) * 0.50f
                    ),
                    radius = maxOf(w, h) * 0.50f,
                    center = Offset(w * 0.85f, h * 0.28f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x4D8B5CF6), Color.Transparent),
                        center = Offset(w * 0.50f, h * 0.88f),
                        radius = maxOf(w, h) * 0.58f
                    ),
                    radius = maxOf(w, h) * 0.58f,
                    center = Offset(w * 0.50f, h * 0.88f)
                )
            }
            Box(modifier = contentBlurModifier, content = content)
        }
    }
}

/**
 * Activates real Android 12+ backdrop blur (both Compose root blur and native window blur-behind)
 * plus a darkened dialog scrim while a Dialog or ModalBottomSheet is open in Liquid Glass mode.
 */
@Composable
fun LiquidGlassModalBackdropEffect_Antigravity(
    dimAmount: Float = 0.68f,
    windowBlurRadiusPx: Int = 28
) {
    val isLiquidGlass = LocalJoudColors.current.isLiquidGlass
    val controller = LocalLiquidGlassModalBlurController.current
    val view = LocalView.current

    if (isLiquidGlass && controller != null) {
        DisposableEffect(controller) {
            controller.acquire()
            onDispose { controller.release() }
        }
    }

    if (isLiquidGlass) {
        SideEffect {
            val window = (view.parent as? DialogWindowProvider)?.window ?: return@SideEffect
            runCatching {
                window.setDimAmount(dimAmount)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    val params = window.attributes
                    if (params.blurBehindRadius != windowBlurRadiusPx) {
                        params.blurBehindRadius = windowBlurRadiusPx
                        window.attributes = params
                    }
                }
            }
        }
    }
}

/**
 * Applies a high-legibility frosted glass modal surface with a subtle top specular sheen,
 * crisp luminous border, and elevation shadow in Liquid Glass mode (and clean solid modal styling
 * in Dark/Light themes).
 */
fun Modifier.liquidGlassModalSurface(
    shape: Shape,
    borderWidth: Dp = 1.5.dp,
    elevation: Dp = 20.dp,
    fallbackBorderColor: Color? = null
): Modifier = composed {
    val colors = LocalJoudColors.current
    if (colors.isLiquidGlass) {
        this
            .shadow(elevation = elevation, shape = shape, clip = false)
            .clip(shape)
            .background(colors.modalSurface)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0x28E0F2FE),
                        Color(0x0A38BDF8),
                        Color.Transparent
                    )
                )
            )
            .border(
                width = borderWidth,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xB8E0F2FE),
                        Color(0x527DD3FC),
                        Color(0x7838BDF8)
                    )
                ),
                shape = shape
            )
    } else {
        this
            .clip(shape)
            .background(colors.modalSurface)
            .border(
                width = borderWidth,
                color = fallbackBorderColor ?: colors.textPrimary.copy(alpha = 0.14f),
                shape = shape
            )
    }
}

/**
 * Applies a specular top-left to bottom-right luminous glass border when Liquid Glass theme is active.
 */
fun Modifier.liquidGlassBorder(
    shape: Shape,
    isFocused: Boolean = false,
    focusedColor: Color = Color(0xFF7DD3FC),
    fallbackColor: Color = Color.White.copy(alpha = 0.14f),
    defaultWidth: Dp = 1.dp,
    focusedWidth: Dp = 2.dp
): Modifier = composed {
    val colors = LocalJoudColors.current
    if (isFocused) {
        border(width = focusedWidth, color = focusedColor, shape = shape)
    } else if (colors.isLiquidGlass) {
        border(
            width = defaultWidth,
            brush = Brush.linearGradient(
                colors = listOf(
                    colors.glassBorderHighlight,
                    Color.White.copy(alpha = 0.18f),
                    Color(0x5538BDF8)
                )
            ),
            shape = shape
        )
    } else {
        border(width = defaultWidth, color = fallbackColor, shape = shape)
    }
}
