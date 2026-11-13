package com.joudjawadtube.player.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Icon
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.joudjawadtube.player.data.local.AppDatabase
import com.joudjawadtube.player.data.local.VideoItem
import com.joudjawadtube.player.data.settings.AppSettingsRepository
import com.joudjawadtube.player.data.settings.AppSettingsState
import com.joudjawadtube.player.data.thumbnail.ThumbnailManager
import com.joudjawadtube.player.ui.theme.SmartTubeCardBg
import com.joudjawadtube.player.ui.theme.SmartTubeCardFocusedBg
import com.joudjawadtube.player.ui.theme.SmartTubeFocusBorder
import com.joudjawadtube.player.ui.theme.SmartTubeGold4K
import com.joudjawadtube.player.ui.theme.SmartTubeOverlayDark
import com.joudjawadtube.player.ui.theme.SmartTubeRed
import com.joudjawadtube.player.ui.theme.SmartTubeTextPrimary
import com.joudjawadtube.player.ui.theme.SmartTubeTextSecondary
import com.joudjawadtube.player.ui.theme.SmartTubeUsbCyan
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoCard(
    video: VideoItem,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenOptions: (() -> Unit)? = null,
    settings: AppSettingsState? = null,
    thumbnailRevision: Int = 0,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val layoutInfo = LocalWindowLayoutInfo.current
    val isTouchDevice = layoutInfo.isTouchDevice
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.04f else 1.0f,
        label = "cardScale"
    )
    val context = LocalContext.current
    val effectiveSettings = settings ?: remember(context) {
        AppSettingsRepository.getInstance(context).settingsFlow.value
    }

    // Check fast local flash cache synchronously first (0ms), then resolve/generate asynchronously
    var resolvedThumbFile by remember(
        video.id,
        video.entity.customThumbnailPath,
        effectiveSettings.thumbnailMode,
        effectiveSettings.frameCapturePosition,
        thumbnailRevision
    ) {
        mutableStateOf<File?>(ThumbnailManager.getCachedThumbnailFile(context, video, effectiveSettings))
    }

    LaunchedEffect(
        video.id,
        video.entity.customThumbnailPath,
        effectiveSettings.thumbnailMode,
        effectiveSettings.frameCapturePosition,
        thumbnailRevision
    ) {
        if (resolvedThumbFile == null) {
            val dao = AppDatabase.getInstance(context).videoDao()
            resolvedThumbFile = ThumbnailManager.resolveOrGenerateThumbnail(
                context = context,
                video = video,
                settings = effectiveSettings,
                videoDao = dao
            )
        }
    }

    val imageModel = remember(resolvedThumbFile, video.entity.thumbnailUrl) {
        val modelData: Any? = resolvedThumbFile ?: video.entity.thumbnailUrl.takeIf { it.isNotBlank() }
        if (modelData != null) {
            ImageRequest.Builder(context)
                .data(modelData)
                .size(480, 270)
                .crossfade(true)
                .build()
        } else {
            null
        }
    }

    val cardShape = RoundedCornerShape(12.dp)

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(cardShape)
            .background(if (isFocused) SmartTubeCardFocusedBg else SmartTubeCardBg)
            .border(
                width = if (isFocused) 2.5.dp else 1.dp,
                color = if (isFocused) SmartTubeFocusBorder else Color.White.copy(alpha = 0.06f),
                shape = cardShape
            )
            .onFocusChanged { isFocused = it.isFocused }
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.Menu -> {
                            if (onOpenOptions != null) onOpenOptions() else onToggleFavorite()
                            true
                        }
                        Key.Bookmark, Key.F -> {
                            onToggleFavorite()
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    if (onOpenOptions != null) onOpenOptions() else onToggleFavorite()
                }
            )
    ) {
        // 16:9 Thumbnail Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color(0xFF121212)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (video.isYoutube) Icons.Default.OndemandVideo else Icons.Default.Movie,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.14f),
                modifier = Modifier.size(42.dp)
            )

            if (imageModel != null) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Bottom gradient for badge legibility
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f))
                        )
                    )
            )

            // Top-left badges (YouTube / USB / Sidecar CC)
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(7.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (video.isYoutube) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(SmartTubeRed.copy(alpha = 0.90f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OndemandVideo,
                            contentDescription = "YouTube",
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "YT",
                            color = Color.White,
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                } else if (video.isUsb) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(SmartTubeOverlayDark)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Usb,
                            contentDescription = "USB",
                            tint = SmartTubeUsbCyan,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "USB",
                            color = SmartTubeUsbCyan,
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                if (video.hasSidecarSubtitles) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(SmartTubeOverlayDark)
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ClosedCaption,
                            contentDescription = "Subtitles",
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "CC",
                            color = Color.White,
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // Top-right Favorite & Options action icons
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(7.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (video.isFavorite || isFocused || isTouchDevice) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(SmartTubeOverlayDark)
                            .clickable { onToggleFavorite() }
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (video.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (video.isFavorite) SmartTubeRed else Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                if ((isFocused || isTouchDevice) && onOpenOptions != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(SmartTubeOverlayDark)
                            .clickable { onOpenOptions() }
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Bottom-right Resolution (4K / 1080p) + Duration Pill
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(horizontal = 7.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                video.resolutionBadge?.let { resBadge ->
                    val is4K = resBadge == "4K" || resBadge == "2K"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (is4K) SmartTubeGold4K else SmartTubeOverlayDark)
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = resBadge,
                            color = if (is4K) Color.Black else Color.White,
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                if (video.durationMs > 0L) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(SmartTubeOverlayDark)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = video.formattedDuration,
                            color = Color.White,
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // SmartTube-style Red Playback Progress Bar along bottom of thumbnail
            if (video.progressFraction > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .align(Alignment.BottomCenter)
                        .background(Color.White.copy(alpha = 0.28f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(video.progressFraction)
                            .height(4.dp)
                            .background(SmartTubeRed)
                    )
                }
            }
        }

        // Metadata Area (Title + Folder / Size)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 11.dp, vertical = 9.dp)
        ) {
            Text(
                text = video.title,
                color = SmartTubeTextPrimary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = if (layoutInfo.isPhone) 2 else 1,
                softWrap = layoutInfo.isPhone,
                overflow = TextOverflow.Ellipsis,
                modifier = if (isFocused) Modifier.basicMarquee() else Modifier
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = video.folderName,
                    color = when {
                        video.isYoutube -> SmartTubeRed
                        video.isUsb -> SmartTubeUsbCyan
                        else -> SmartTubeTextSecondary
                    },
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (video.formattedSize.isNotBlank()) {
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = video.formattedSize,
                        color = SmartTubeTextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}
