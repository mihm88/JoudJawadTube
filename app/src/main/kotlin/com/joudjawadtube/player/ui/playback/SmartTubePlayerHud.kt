package com.joudjawadtube.player.ui.playback

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.res.stringResource
import com.joudjawadtube.player.R
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joudjawadtube.player.data.local.FormatUtils
import com.joudjawadtube.player.data.local.VideoItem
import com.joudjawadtube.player.data.settings.RepeatMode
import com.joudjawadtube.player.ui.components.LocalWindowLayoutInfo
import com.joudjawadtube.player.ui.theme.SmartTubeCardBg
import com.joudjawadtube.player.ui.theme.SmartTubeCardFocusedBg
import com.joudjawadtube.player.ui.theme.SmartTubeFocusBorder
import com.joudjawadtube.player.ui.theme.SmartTubeGold4K
import com.joudjawadtube.player.ui.theme.SmartTubeRed
import com.joudjawadtube.player.ui.theme.SmartTubeTextPrimary
import com.joudjawadtube.player.ui.theme.SmartTubeTextSecondary
import com.joudjawadtube.player.ui.theme.SmartTubeUsbCyan
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun SmartTubePlayerHud(
    visible: Boolean,
    video: VideoItem,
    isPlaying: Boolean,
    positionProvider: () -> Long,
    durationMs: Long,
    seekIntervalSec: Int = 10,
    videoStreamBadge: String?,
    playbackSpeed: Float,
    resizeModeLabel: String,
    hasPrevious: Boolean,
    hasNext: Boolean,
    onPlayPauseToggle: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekRelative: (Long) -> Unit,
    onPreviousVideo: () -> Unit,
    onNextVideo: () -> Unit,
    onOpenSubtitles: () -> Unit,
    onOpenAudioTracks: () -> Unit,
    onOpenSpeedDialog: () -> Unit,
    onCycleResizeMode: () -> Unit,
    onToggleFavorite: () -> Unit,
    currentQuality: String = "Auto",
    repeatMode: RepeatMode = RepeatMode.OFF,
    onOpenQualityDialog: () -> Unit = {},
    onCycleRepeatMode: () -> Unit = {},
    onBack: () -> Unit,
    onUserInteraction: () -> Unit
) {
    val playPauseFocusRequester = remember { FocusRequester() }
    val seekStepMs = (seekIntervalSec.coerceAtLeast(5)) * 1000L
    val layoutInfo = LocalWindowLayoutInfo.current

    LaunchedEffect(visible) {
        if (visible) {
            runCatching { playPauseFocusRequester.requestFocus() }
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.84f),
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.92f)
                        )
                    )
                )
        ) {
            // Top OSD Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    HudControlPill(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        label = "Back",
                        onClick = onBack,
                        onUserInteraction = onUserInteraction
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = video.title,
                            color = SmartTubeTextPrimary,
                            fontSize = if (layoutInfo.isCompact) 15.sp else 18.sp,
                            lineHeight = if (layoutInfo.isCompact) 19.sp else 23.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (video.isYoutube) {
                                Icon(
                                    imageVector = Icons.Default.OndemandVideo,
                                    contentDescription = "YouTube",
                                    tint = SmartTubeRed,
                                    modifier = Modifier.size(14.dp)
                                )
                            } else if (video.isUsb) {
                                Icon(
                                    imageVector = Icons.Default.Usb,
                                    contentDescription = "USB",
                                    tint = SmartTubeUsbCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = "${video.folderName} • ${video.volumeLabel}",
                                color = when {
                                    video.isYoutube -> SmartTubeRed
                                    video.isUsb -> SmartTubeUsbCyan
                                    else -> SmartTubeTextSecondary
                                },
                                fontSize = 12.sp,
                                lineHeight = 15.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Stream Technical Metadata Badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    video.resolutionBadge?.let { badge ->
                        val is4K = badge == "4K" || badge == "2K"
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (is4K) SmartTubeGold4K else SmartTubeCardBg)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = badge,
                                color = if (is4K) Color.Black else Color.White,
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    if (!videoStreamBadge.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SmartTubeCardBg)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = videoStreamBadge,
                                color = SmartTubeTextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // Bottom OSD Controls & Seekbar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                // Isolated Timeline Bar (recomposes at 500ms without recomposing outer HUD)
                PlayerTimelineBar(
                    positionProvider = positionProvider,
                    durationMs = durationMs,
                    seekStepMs = seekStepMs,
                    onSeekTo = onSeekTo,
                    onSeekRelative = onSeekRelative,
                    onUserInteraction = onUserInteraction
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Control Pill Bar
                if (layoutInfo.isCompact) {
                    // Mobile / narrow layout: two compact rows or horizontally scrollable
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HudControlPill(
                            icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            label = if (isPlaying) "Pause" else "Play",
                            isPrimary = true,
                            onClick = onPlayPauseToggle,
                            onUserInteraction = onUserInteraction,
                            modifier = Modifier.focusRequester(playPauseFocusRequester)
                        )
                        HudControlPill(
                            icon = Icons.Default.Replay10,
                            label = "-${seekIntervalSec}s",
                            onClick = { onSeekRelative(-seekStepMs) },
                            onUserInteraction = onUserInteraction
                        )
                        HudControlPill(
                            icon = Icons.Default.Forward10,
                            label = "+${seekIntervalSec}s",
                            onClick = { onSeekRelative(seekStepMs) },
                            onUserInteraction = onUserInteraction
                        )
                        if (hasPrevious) {
                            HudControlPill(
                                icon = Icons.Default.SkipPrevious,
                                label = "Prev",
                                onClick = onPreviousVideo,
                                onUserInteraction = onUserInteraction
                            )
                        }
                        if (hasNext) {
                            HudControlPill(
                                icon = Icons.Default.SkipNext,
                                label = "Next",
                                onClick = onNextVideo,
                                onUserInteraction = onUserInteraction
                            )
                        }
                        HudControlPill(
                            icon = Icons.Default.ClosedCaption,
                            label = "CC",
                            onClick = onOpenSubtitles,
                            onUserInteraction = onUserInteraction
                        )
                        HudControlPill(
                            icon = Icons.Default.Audiotrack,
                            label = "Audio",
                            onClick = onOpenAudioTracks,
                            onUserInteraction = onUserInteraction
                        )
                        HudControlPill(
                            icon = Icons.Default.Speed,
                            label = "${playbackSpeed}x",
                            onClick = onOpenSpeedDialog,
                            onUserInteraction = onUserInteraction
                        )
                        HudControlPill(
                            icon = Icons.Default.AspectRatio,
                            label = resizeModeLabel,
                            onClick = onCycleResizeMode,
                            onUserInteraction = onUserInteraction
                        )
                        HudControlPill(
                            icon = if (video.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            label = if (video.isFavorite) "Saved" else "Favorite",
                            iconTint = if (video.isFavorite) SmartTubeRed else Color.White,
                            onClick = onToggleFavorite,
                            onUserInteraction = onUserInteraction
                        )
                        val (repIconNarrow, repLabelNarrow, repActiveNarrow) = when (repeatMode) {
                            RepeatMode.OFF -> Triple(Icons.Default.Repeat, stringResource(R.string.player_repeat_off), false)
                            RepeatMode.REPEAT_ONE -> Triple(Icons.Default.RepeatOne, stringResource(R.string.player_repeat_one), true)
                            RepeatMode.REPEAT_ALL -> Triple(Icons.Default.Repeat, stringResource(R.string.player_repeat_all), true)
                        }
                        HudControlPill(
                            icon = repIconNarrow,
                            label = repLabelNarrow,
                            iconTint = if (repActiveNarrow) SmartTubeRed else Color.White,
                            onClick = onCycleRepeatMode,
                            onUserInteraction = onUserInteraction
                        )
                        HudControlPill(
                            icon = Icons.Default.Hd,
                            label = currentQuality,
                            onClick = onOpenQualityDialog,
                            onUserInteraction = onUserInteraction
                        )
                    }
                } else {
                    // Standard TV & Tablet wide layout
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left cluster: Transport controls
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HudControlPill(
                                icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                label = if (isPlaying) stringResource(R.string.player_pause) else stringResource(R.string.player_play),
                                isPrimary = true,
                                onClick = onPlayPauseToggle,
                                onUserInteraction = onUserInteraction,
                                modifier = Modifier.focusRequester(playPauseFocusRequester)
                            )

                            HudControlPill(
                                icon = Icons.Default.Replay10,
                                label = "-${seekIntervalSec}s",
                                onClick = { onSeekRelative(-seekStepMs) },
                                onUserInteraction = onUserInteraction
                            )

                            HudControlPill(
                                icon = Icons.Default.Forward10,
                                label = "+${seekIntervalSec}s",
                                onClick = { onSeekRelative(seekStepMs) },
                                onUserInteraction = onUserInteraction
                            )

                            if (hasPrevious) {
                                HudControlPill(
                                    icon = Icons.Default.SkipPrevious,
                                    label = "Prev",
                                    onClick = onPreviousVideo,
                                    onUserInteraction = onUserInteraction
                                )
                            }

                            if (hasNext) {
                                HudControlPill(
                                    icon = Icons.Default.SkipNext,
                                    label = "Next",
                                    onClick = onNextVideo,
                                    onUserInteraction = onUserInteraction
                                )
                            }
                        }

                        // Right cluster: Secondary settings
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HudControlPill(
                                icon = Icons.Default.ClosedCaption,
                                label = "Subtitles",
                                onClick = onOpenSubtitles,
                                onUserInteraction = onUserInteraction
                            )

                            HudControlPill(
                                icon = Icons.Default.Audiotrack,
                                label = "Audio",
                                onClick = onOpenAudioTracks,
                                onUserInteraction = onUserInteraction
                            )

                            HudControlPill(
                                icon = Icons.Default.Speed,
                                label = "${playbackSpeed}x",
                                onClick = onOpenSpeedDialog,
                                onUserInteraction = onUserInteraction
                            )

                            HudControlPill(
                                icon = Icons.Default.AspectRatio,
                                label = resizeModeLabel,
                                onClick = onCycleResizeMode,
                                onUserInteraction = onUserInteraction
                            )

                            HudControlPill(
                                icon = if (video.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                label = if (video.isFavorite) "Saved" else "Favorite",
                                iconTint = if (video.isFavorite) SmartTubeRed else Color.White,
                                onClick = onToggleFavorite,
                                onUserInteraction = onUserInteraction
                            )

                            val (repeatIcon, repeatLabel, repeatActive) = when (repeatMode) {
                                RepeatMode.OFF -> Triple(Icons.Default.Repeat, stringResource(R.string.player_repeat_off), false)
                                RepeatMode.REPEAT_ONE -> Triple(Icons.Default.RepeatOne, stringResource(R.string.player_repeat_one), true)
                                RepeatMode.REPEAT_ALL -> Triple(Icons.Default.Repeat, stringResource(R.string.player_repeat_all), true)
                            }
                            HudControlPill(
                                icon = repeatIcon,
                                label = repeatLabel,
                                iconTint = if (repeatActive) SmartTubeRed else Color.White,
                                onClick = onCycleRepeatMode,
                                onUserInteraction = onUserInteraction
                            )

                            HudControlPill(
                                icon = Icons.Default.Hd,
                                label = currentQuality,
                                onClick = onOpenQualityDialog,
                                onUserInteraction = onUserInteraction
                            )
                        }
                    }
                }
            }
        }
    }
}
}

/**
 * Isolated leaf composable for the player timeline seekbar and duration labels.
 * Only this composable recomposes when time advances; the parent HUD remains untouched.
 */
@Composable
private fun PlayerTimelineBar(
    positionProvider: () -> Long,
    durationMs: Long,
    seekStepMs: Long,
    onSeekTo: (Long) -> Unit,
    onSeekRelative: (Long) -> Unit,
    onUserInteraction: () -> Unit
) {
    var currentPosMs by remember { mutableLongStateOf(positionProvider()) }
    var seekbarFocused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (isActive) {
            currentPosMs = positionProvider().coerceAtLeast(0L)
            delay(500L)
        }
    }

    val safeDuration = durationMs.coerceAtLeast(1L)
    val progressFraction = (currentPosMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = FormatUtils.formatDuration(currentPosMs),
            color = SmartTubeTextPrimary,
            fontSize = 12.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
        )

        val sliderColors = SliderDefaults.colors(
            thumbColor = if (seekbarFocused) Color.White else SmartTubeRed,
            activeTrackColor = SmartTubeRed,
            inactiveTrackColor = Color.White.copy(alpha = 0.28f)
        )

        Slider(
            value = progressFraction,
            onValueChange = { frac ->
                onUserInteraction()
                val targetMs = (frac * safeDuration).toLong()
                currentPosMs = targetMs
                onSeekTo(targetMs)
            },
            colors = sliderColors,
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { seekbarFocused = it.isFocused || it.hasFocus }
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        when (event.key) {
                            Key.DirectionLeft -> {
                                onUserInteraction()
                                onSeekRelative(-seekStepMs)
                                currentPosMs = (currentPosMs - seekStepMs).coerceAtLeast(0L)
                                true
                            }
                            Key.DirectionRight -> {
                                onUserInteraction()
                                onSeekRelative(seekStepMs)
                                currentPosMs = (currentPosMs + seekStepMs).coerceAtMost(safeDuration)
                                true
                            }
                            else -> false
                        }
                    } else {
                        false
                    }
                }
        )

        Text(
            text = FormatUtils.formatDuration(durationMs),
            color = SmartTubeTextSecondary,
            fontSize = 12.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * Lightweight, zero-relayout control pill.
 * Uses a constant FontWeight (SemiBold) and avoids runtime text re-measurement on focus.
 * Uses indication = null to prevent double-ripple jank.
 */
@Composable
private fun HudControlPill(
    icon: ImageVector,
    label: String,
    isPrimary: Boolean = false,
    iconTint: Color = Color.White,
    onClick: () -> Unit,
    onUserInteraction: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    val bgColor = when {
        isFocused && isPrimary -> SmartTubeRed
        isFocused -> SmartTubeCardFocusedBg
        isPrimary -> SmartTubeRed.copy(alpha = 0.88f)
        else -> SmartTubeCardBg.copy(alpha = 0.88f)
    }

    Row(
        modifier = modifier
            .height(38.dp)
            .clip(CircleShape)
            .background(bgColor)
            .border(
                width = if (isFocused) 2.5.dp else 1.dp,
                color = if (isFocused) SmartTubeFocusBorder else Color.White.copy(alpha = 0.14f),
                shape = CircleShape
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                onUserInteraction()
                onClick()
            }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconTint,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.SemiBold, // Constant weight prevents re-measurement & layout freeze
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}
