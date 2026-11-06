package com.joudjawadtube.player.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.joudjawadtube.player.ui.theme.SmartTubeCardBg
import com.joudjawadtube.player.ui.theme.SmartTubeCardFocusedBg
import com.joudjawadtube.player.ui.theme.SmartTubeFocusBorder
import com.joudjawadtube.player.ui.theme.SmartTubeRed
import com.joudjawadtube.player.ui.theme.SmartTubeTextPrimary
import com.joudjawadtube.player.ui.theme.SmartTubeTextSecondary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ConfirmDestructiveDialog_Antigravity(
    title: String,
    message: String,
    confirmActionLabel: String = "Delete",
    holdDurationMs: Long = 1500L,
    onConfirmed: () -> Unit,
    onDismiss: () -> Unit
) {
    val cancelFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        // Safe default: focus Cancel button on launch to prevent accidental triggers
        runCatching { cancelFocusRequester.requestFocus() }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF161922),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 480.dp)
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Warning icon badge
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(SmartTubeRed.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = "Warning",
                        tint = SmartTubeRed,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = title,
                    color = SmartTubeTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = message,
                    color = SmartTubeTextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Actions row: Cancel (default focus) + HoldToConfirmButton
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cancel button
                    var cancelFocused by remember { mutableStateOf(false) }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .focusRequester(cancelFocusRequester)
                            .onFocusChanged { cancelFocused = it.isFocused }
                            .border(
                                width = if (cancelFocused) 2.dp else 1.dp,
                                color = if (cancelFocused) SmartTubeFocusBorder else Color.White.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (cancelFocused) SmartTubeCardFocusedBg else SmartTubeCardBg,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Hold-to-confirm button
                    HoldToConfirmButton_Antigravity(
                        label = confirmActionLabel,
                        holdDurationMs = holdDurationMs,
                        onConfirmed = {
                            onConfirmed()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1.2f)
                    )
                }
            }
        }
    }
}

/**
 * Button that requires holding for [holdDurationMs] (default 1500 ms).
 * Supports both D-Pad Center / Enter key holding and Touch pointer holding.
 * Shows an animated red progress fill that smoothly resets if released early.
 */
@Composable
fun HoldToConfirmButton_Antigravity(
    label: String,
    holdDurationMs: Long = 1500L,
    onConfirmed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var isFocused by remember { mutableStateOf(false) }
    var isHolding by remember { mutableStateOf(false) }
    val progressAnim = remember { Animatable(0f) }
    var holdJob by remember { mutableStateOf<Job?>(null) }

    fun startHolding() {
        if (isHolding) return
        isHolding = true
        holdJob?.cancel()
        holdJob = scope.launch {
            progressAnim.snapTo(0f)
            progressAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = holdDurationMs.toInt(),
                    easing = LinearEasing
                )
            )
            // If completed fully without cancellation:
            if (isHolding && progressAnim.value >= 0.99f) {
                isHolding = false
                onConfirmed()
            }
        }
    }

    fun stopHolding() {
        if (!isHolding) return
        isHolding = false
        holdJob?.cancel()
        scope.launch {
            progressAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(150, easing = LinearEasing)
            )
        }
    }

    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF281014))
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) SmartTubeRed else SmartTubeRed.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .onFocusChanged {
                isFocused = it.isFocused
                if (!it.isFocused) stopHolding()
            }
            .onPreviewKeyEvent { event ->
                if (event.key == Key.DirectionCenter || event.key == Key.Enter) {
                    if (event.type == KeyEventType.KeyDown) {
                        if (!isHolding) startHolding()
                        true
                    } else if (event.type == KeyEventType.KeyUp) {
                        stopHolding()
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    startHolding()
                    // Wait until released or cancelled
                    do {
                        val event = awaitPointerEvent()
                    } while (event.changes.any { it.pressed })
                    stopHolding()
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        // Red fill layer showing hold progress
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progressAnim.value)
                .background(SmartTubeRed)
        )

        // Text label
        Text(
            text = if (isHolding) "Hold to Confirm..." else "Hold $label (1.5s)",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
