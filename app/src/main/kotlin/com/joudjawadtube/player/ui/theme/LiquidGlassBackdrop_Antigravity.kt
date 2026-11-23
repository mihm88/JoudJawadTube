package com.joudjawadtube.player.ui.theme

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Root container rendering the Liquid Glass aurora mesh backdrop when [ThemeMode.LIQUID_GLASS] is active,
 * or the standard solid background otherwise.
 */
@Composable
fun LiquidGlassRootContainer(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = LocalJoudColors.current
    if (!colors.isLiquidGlass) {
        Box(
            modifier = modifier.background(colors.background),
            content = content
        )
        return
    }

    val ambientModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier
            .fillMaxSize()
            .blur(56.dp)
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
        content()
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
                    Color.White.copy(alpha = 0.14f),
                    Color(0x4438BDF8)
                )
            ),
            shape = shape
        )
    } else {
        border(width = defaultWidth, color = fallbackColor, shape = shape)
    }
}
