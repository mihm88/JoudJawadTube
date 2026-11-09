package com.joudjawadtube.player.ui.playback

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import com.joudjawadtube.player.R
import com.joudjawadtube.player.ui.theme.SmartTubeCardBg
import com.joudjawadtube.player.ui.theme.SmartTubeCardFocusedBg
import com.joudjawadtube.player.ui.theme.SmartTubeFocusBorder
import com.joudjawadtube.player.ui.theme.SmartTubeRed
import com.joudjawadtube.player.ui.theme.SmartTubeSidebarBg
import com.joudjawadtube.player.ui.theme.SmartTubeTextPrimary
import com.joudjawadtube.player.ui.theme.SmartTubeTextSecondary
import java.util.Locale

enum class PlayerDialogTab {
    SUBTITLES,
    AUDIO,
    SPEED,
    QUALITY
}

data class TrackOptionUi(
    val id: String,
    val title: String,
    val subtitle: String,
    val isSelected: Boolean,
    val group: Tracks.Group?,
    val trackIndex: Int
)

@OptIn(UnstableApi::class)
@Composable
fun TrackSelectorDialog(
    player: Player,
    initialTab: PlayerDialogTab,
    currentSpeed: Float,
    currentQuality: String = "Auto",
    onSelectSpeed: (Float) -> Unit,
    onSelectQuality: (String) -> Unit = {},
    onDismiss: () -> Unit
) {
    var activeTab by remember { mutableStateOf(initialTab) }
    var refreshRevision by remember { mutableStateOf(0) }

    val tracks = remember(player.currentTracks, refreshRevision) { player.currentTracks }
    val isTextDisabled = remember(player.trackSelectionParameters, refreshRevision) {
        player.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT)
    }

    val subtitleOptions = remember(tracks, isTextDisabled) {
        buildSubtitleTrackOptions(tracks, isTextDisabled)
    }

    val audioOptions = remember(tracks) {
        buildAudioTrackOptions(tracks)
    }

    val speedOptions = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(560.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SmartTubeSidebarBg)
                .border(1.5.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            // Header + Tab Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DialogTabChip(
                        icon = Icons.Default.ClosedCaption,
                        label = "${stringResource(R.string.player_subtitles, "").substringBefore(':').trim()} (${(subtitleOptions.size - 1).coerceAtLeast(0)})",
                        isSelected = activeTab == PlayerDialogTab.SUBTITLES,
                        onClick = { activeTab = PlayerDialogTab.SUBTITLES }
                    )
                    DialogTabChip(
                        icon = Icons.Default.Audiotrack,
                        label = "${stringResource(R.string.player_audio, "").substringBefore(':').trim()} (${audioOptions.size})",
                        isSelected = activeTab == PlayerDialogTab.AUDIO,
                        onClick = { activeTab = PlayerDialogTab.AUDIO }
                    )
                    DialogTabChip(
                        icon = Icons.Default.Speed,
                        label = "${currentSpeed}x",
                        isSelected = activeTab == PlayerDialogTab.SPEED,
                        onClick = { activeTab = PlayerDialogTab.SPEED }
                    )
                    DialogTabChip(
                        icon = Icons.Default.Hd,
                        label = "$currentQuality",
                        isSelected = activeTab == PlayerDialogTab.QUALITY,
                        onClick = { activeTab = PlayerDialogTab.QUALITY }
                    )
                }

                var closeFocused by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (closeFocused) SmartTubeCardFocusedBg else SmartTubeCardBg)
                        .border(
                            width = if (closeFocused) 2.dp else 1.dp,
                            color = if (closeFocused) SmartTubeFocusBorder else Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .onFocusChanged { closeFocused = it.isFocused }
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = SmartTubeTextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (activeTab) {
                PlayerDialogTab.SUBTITLES -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(subtitleOptions, key = { it.id }) { option ->
                            DialogOptionRow(
                                title = option.title,
                                subtitle = option.subtitle,
                                isSelected = option.isSelected,
                                onClick = {
                                    if (option.group == null) {
                                        player.trackSelectionParameters = player.trackSelectionParameters
                                            .buildUpon()
                                            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                                            .build()
                                    } else {
                                        player.trackSelectionParameters = player.trackSelectionParameters
                                            .buildUpon()
                                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                            .setOverrideForType(
                                                TrackSelectionOverride(
                                                    option.group.mediaTrackGroup,
                                                    option.trackIndex
                                                )
                                            )
                                            .build()
                                    }
                                    refreshRevision++
                                    onDismiss()
                                }
                            )
                        }
                    }
                }

                PlayerDialogTab.AUDIO -> {
                    if (audioOptions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No selectable audio tracks found in this video.",
                                color = SmartTubeTextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 320.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(audioOptions, key = { it.id }) { option ->
                                DialogOptionRow(
                                    title = option.title,
                                    subtitle = option.subtitle,
                                    isSelected = option.isSelected,
                                    onClick = {
                                        val grp = option.group ?: return@DialogOptionRow
                                        player.trackSelectionParameters = player.trackSelectionParameters
                                            .buildUpon()
                                            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                                            .setOverrideForType(
                                                TrackSelectionOverride(
                                                    grp.mediaTrackGroup,
                                                    option.trackIndex
                                                )
                                            )
                                            .build()
                                        refreshRevision++
                                        onDismiss()
                                    }
                                )
                            }
                        }
                    }
                }

                PlayerDialogTab.SPEED -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(speedOptions) { speed ->
                            val label = if (speed == 1.0f) "1.0x (Normal)" else "${speed}x"
                            DialogOptionRow(
                                title = label,
                                subtitle = if (speed > 1.0f) "Faster playback" else if (speed < 1.0f) "Slower playback" else "Default speed",
                                isSelected = kotlin.math.abs(currentSpeed - speed) < 0.01f,
                                onClick = {
                                    onSelectSpeed(speed)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }

                PlayerDialogTab.QUALITY -> {
                    val qualityList = listOf("Auto", "4K (2160p)", "2K (1440p)", "1080p", "720p", "480p", "360p")
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(qualityList) { qualityOption ->
                            val isSelected = currentQuality.equals(qualityOption, ignoreCase = true) ||
                                (qualityOption == "Auto" && (currentQuality.isBlank() || currentQuality.equals("Auto", ignoreCase = true)))
                            DialogOptionRow(
                                title = qualityOption,
                                subtitle = when (qualityOption) {
                                    "Auto" -> "Best available adaptive resolution"
                                    "4K (2160p)" -> "3840 x 2160 Ultra HD"
                                    "2K (1440p)" -> "2560 x 1440 Quad HD"
                                    "1080p" -> "1920 x 1080 Full HD"
                                    "720p" -> "1280 x 720 HD"
                                    "480p" -> "Standard Definition"
                                    "360p" -> "Data saver"
                                    else -> ""
                                },
                                isSelected = isSelected,
                                onClick = {
                                    onSelectQuality(qualityOption)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogTabChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val bgColor = when {
        isSelected -> SmartTubeRed
        isFocused -> SmartTubeCardFocusedBg
        else -> SmartTubeCardBg
    }

    Row(
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) SmartTubeFocusBorder else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(10.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .padding(horizontal = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DialogOptionRow(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val bgColor = when {
        isFocused -> SmartTubeCardFocusedBg
        isSelected -> SmartTubeRed.copy(alpha = 0.18f)
        else -> SmartTubeCardBg
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) SmartTubeFocusBorder else if (isSelected) SmartTubeRed else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = SmartTubeTextPrimary,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    color = SmartTubeTextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = SmartTubeRed,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@OptIn(UnstableApi::class)
private fun buildSubtitleTrackOptions(
    tracks: Tracks,
    isTextDisabled: Boolean
): List<TrackOptionUi> {
    val options = mutableListOf<TrackOptionUi>()
    val anySelected = !isTextDisabled && tracks.groups.any { it.type == C.TRACK_TYPE_TEXT && it.isSelected }

    options.add(
        TrackOptionUi(
            id = "sub_off",
            title = "Subtitles Off",
            subtitle = "Disable closed captions and sidecar subtitles",
            isSelected = !anySelected,
            group = null,
            trackIndex = -1
        )
    )

    var trackCounter = 1
    tracks.groups.forEachIndexed { groupIdx, group ->
        if (group.type == C.TRACK_TYPE_TEXT) {
            for (i in 0 until group.length) {
                val format = group.getTrackFormat(i)
                val langName = format.language?.let { Locale(it).displayLanguage }
                    ?.takeIf { it.isNotBlank() } ?: "Track $trackCounter"
                val label = format.label ?: langName
                val mimeInfo = format.sampleMimeType?.substringAfter('/')?.uppercase() ?: "SUB"
                val isSelected = !isTextDisabled && group.isTrackSelected(i)

                options.add(
                    TrackOptionUi(
                        id = "sub_${groupIdx}_$i",
                        title = label,
                        subtitle = "$langName • $mimeInfo",
                        isSelected = isSelected,
                        group = group,
                        trackIndex = i
                    )
                )
                trackCounter++
            }
        }
    }

    return options
}

@OptIn(UnstableApi::class)
private fun buildAudioTrackOptions(tracks: Tracks): List<TrackOptionUi> {
    val options = mutableListOf<TrackOptionUi>()
    var trackCounter = 1

    tracks.groups.forEachIndexed { groupIdx, group ->
        if (group.type == C.TRACK_TYPE_AUDIO) {
            for (i in 0 until group.length) {
                val format = group.getTrackFormat(i)
                val langName = format.language?.let { Locale(it).displayLanguage }
                    ?.takeIf { it.isNotBlank() } ?: "Audio Track $trackCounter"
                val title = format.label ?: langName
                val channelText = when (format.channelCount) {
                    1 -> "Mono"
                    2 -> "Stereo (2.0)"
                    6 -> "5.1 Surround"
                    8 -> "7.1 Surround"
                    else -> if (format.channelCount > 0) "${format.channelCount}ch" else ""
                }
                val codecText = format.sampleMimeType?.substringAfter('/')?.uppercase() ?: ""
                val sampleRateText = if (format.sampleRate > 0) "${format.sampleRate / 1000} kHz" else ""
                val details = listOf(langName, channelText, codecText, sampleRateText)
                    .filter { it.isNotBlank() }
                    .distinct()
                    .joinToString(" • ")

                options.add(
                    TrackOptionUi(
                        id = "aud_${groupIdx}_$i",
                        title = title,
                        subtitle = details,
                        isSelected = group.isTrackSelected(i),
                        group = group,
                        trackIndex = i
                    )
                )
                trackCounter++
            }
        }
    }

    return options
}
