package com.joudjawadtube.player.ui.playback

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.joudjawadtube.player.ui.theme.SmartTubeCardBg
import com.joudjawadtube.player.ui.theme.SmartTubeCardFocusedBg
import com.joudjawadtube.player.ui.theme.SmartTubeFocusBorder
import com.joudjawadtube.player.ui.theme.SmartTubeGold4K
import com.joudjawadtube.player.ui.theme.SmartTubeRed
import com.joudjawadtube.player.ui.theme.SmartTubeTextPrimary
import com.joudjawadtube.player.ui.theme.SmartTubeTextSecondary
import com.joudjawadtube.player.ui.theme.SmartTubeUsbCyan

@Composable
fun SmartTubePlayerHud(
    visible: Boolean,
    video: VideoItem,
    isPlaying: Boolean,
    currentPositionMs: Long,
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
    onBack: () -> Unit,
    onUserInteraction: () -> Unit
) {
    val playPauseFocusRequester = remember { FocusRequester() }
    val seekStepMs = (seekIntervalSec.coerceAtLeast(5)) * 1000L

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
                        onClick = onBack
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = video.title,
                            color = SmartTubeTextPrimary,
                            fontSize = 18.sp,
                            lineHeight = 23.sp,
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
                val safeDuration = durationMs.coerceAtLeast(1L)
                val progressFraction = (currentPositionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
                var seekbarFocused by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = FormatUtils.formatDuration(currentPositionMs),
                        color = SmartTubeTextPrimary,
                        fontSize = 12.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )

                    Slider(
                        value = progressFraction,
                        onValueChange = { frac ->
                            onUserInteraction()
                            onSeekTo((frac * safeDuration).toLong())
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = if (seekbarFocused) Color.White else SmartTubeRed,
                            activeTrackColor = SmartTubeRed,
                            inactiveTrackColor = Color.White.copy(alpha = 0.28f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { seekbarFocused = it.isFocused || it.hasFocus }
                            .onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown) {
                                    when (event.key) {
                                        Key.DirectionLeft -> {
                                            onUserInteraction()
                                            onSeekRelative(-seekStepMs)
                                            true
                                        }
                                        Key.DirectionRight -> {
                                            onUserInteraction()
                                            onSeekRelative(seekStepMs)
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

                Spacer(modifier = Modifier.height(6.dp))

                // Bottom Control Pill Bar (Single focus per button, single-line non-wrapping text)
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
                            label = if (isPlaying) "Pause" else "Play",
                            isPrimary = true,
                            onClick = {
                                onUserInteraction()
                                onPlayPauseToggle()
                            },
                            modifier = Modifier.focusRequester(playPauseFocusRequester)
                        )

                        HudControlPill(
                            icon = Icons.Default.Replay10,
                            label = "-${seekIntervalSec}s",
                            onClick = {
                                onUserInteraction()
                                onSeekRelative(-seekStepMs)
                            }
                        )

                        HudControlPill(
                            icon = Icons.Default.Forward10,
                            label = "+${seekIntervalSec}s",
                            onClick = {
                                onUserInteraction()
                                onSeekRelative(seekStepMs)
                            }
                        )

                        if (hasPrevious) {
                            HudControlPill(
                                icon = Icons.Default.SkipPrevious,
                                label = "Prev",
                                onClick = {
                                    onUserInteraction()
                                    onPreviousVideo()
                                }
                            )
                        }

                        if (hasNext) {
                            HudControlPill(
                                icon = Icons.Default.SkipNext,
                                label = "Next",
                                onClick = {
                                    onUserInteraction()
                                    onNextVideo()
                                }
                            )
                        }
                    }

                    // Right cluster: Subtitles (CC), Audio Tracks, Speed, Aspect Ratio, Favorite
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HudControlPill(
                            icon = Icons.Default.ClosedCaption,
                            label = "CC",
                            onClick = {
                                onUserInteraction()
                                onOpenSubtitles()
                            }
                        )

                        HudControlPill(
                            icon = Icons.Default.Audiotrack,
                            label = "Audio",
                            onClick = {
                                onUserInteraction()
                                onOpenAudioTracks()
                            }
                        )

                        HudControlPill(
                            icon = Icons.Default.Speed,
                            label = "${playbackSpeed}x",
                            onClick = {
                                onUserInteraction()
                                onOpenSpeedDialog()
                            }
                        )

                        HudControlPill(
                            icon = Icons.Default.AspectRatio,
                            label = resizeModeLabel,
                            onClick = {
                                onUserInteraction()
                                onCycleResizeMode()
                            }
                        )

                        HudControlPill(
                            icon = if (video.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            label = if (video.isFavorite) "Saved" else "Favorite",
                            iconTint = if (video.isFavorite) SmartTubeRed else Color.White,
                            onClick = {
                                onUserInteraction()
                                onToggleFavorite()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HudControlPill(
    icon: ImageVector,
    label: String,
    isPrimary: Boolean = false,
    iconTint: Color = Color.White,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
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
            .clickable { onClick() }
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
            fontWeight = if (isFocused || isPrimary) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}
