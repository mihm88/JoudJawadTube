package com.joudjawadtube.player.ui.playback

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.joudjawadtube.player.R
import com.joudjawadtube.player.data.local.VideoItem
import com.joudjawadtube.player.data.repository.RelatedTabMode
import com.joudjawadtube.player.data.repository.RelatedVideos
import com.joudjawadtube.player.data.settings.AppSettingsRepository
import com.joudjawadtube.player.data.thumbnail.ThumbnailManager
import com.joudjawadtube.player.ui.components.LocalWindowLayoutInfo
import com.joudjawadtube.player.ui.theme.SmartTubeCardBg
import com.joudjawadtube.player.ui.theme.SmartTubeCardFocusedBg
import com.joudjawadtube.player.ui.theme.SmartTubeFocusBorder
import com.joudjawadtube.player.ui.theme.SmartTubeOverlayDark
import com.joudjawadtube.player.ui.theme.SmartTubeRed
import com.joudjawadtube.player.ui.theme.SmartTubeTextPrimary
import com.joudjawadtube.player.ui.theme.SmartTubeTextSecondary
import java.util.Locale

@Composable
fun RelatedVideosRow_Antigravity(
    visible: Boolean,
    related: RelatedVideos?,
    currentVideo: VideoItem,
    initialTabMode: RelatedTabMode = RelatedTabMode.CATEGORY,
    layoutDirection: LayoutDirection,
    onSelectVideo: (VideoItem, List<VideoItem>) -> Unit,
    onDismiss: () -> Unit,
    onUserInteraction: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (related == null || (related.videos.isEmpty() && related.sourceVideos.isEmpty())) return

    val hasSourceTab = related.sourceVideos.size > 1 && related.sourceTitle.isNotBlank()
    val hasCategoryTab = related.videos.size > 1 && related.isCategory
    val hasDualTabs = hasSourceTab && hasCategoryTab

    var activeTab by remember(related, initialTabMode, visible) {
        mutableStateOf(
            when {
                initialTabMode == RelatedTabMode.SOURCE && hasSourceTab -> RelatedTabMode.SOURCE
                hasCategoryTab -> RelatedTabMode.CATEGORY
                hasSourceTab -> RelatedTabMode.SOURCE
                else -> RelatedTabMode.CATEGORY
            }
        )
    }

    val activeVideos = remember(related, activeTab, hasSourceTab) {
        if (activeTab == RelatedTabMode.SOURCE && hasSourceTab) {
            related.sourceVideos
        } else {
            related.videos
        }
    }

    val layoutInfo = LocalWindowLayoutInfo.current
    val cardWidth = if (layoutInfo.isCompact) 168.dp else 208.dp
    val listState = rememberLazyListState()
    val currentIndex = remember(activeVideos, currentVideo.id, currentVideo.youtubeVideoId) {
        activeVideos.indexOfFirst {
            it.id == currentVideo.id ||
                (currentVideo.isYoutube && currentVideo.youtubeVideoId.isNotBlank() && it.youtubeVideoId == currentVideo.youtubeVideoId)
        }.coerceAtLeast(0)
    }
    val initialCardFocus = remember { FocusRequester() }
    val sourceTabFocus = remember { FocusRequester() }
    val categoryTabFocus = remember { FocusRequester() }

    BackHandler(enabled = visible) {
        onUserInteraction()
        onDismiss()
    }

    LaunchedEffect(visible, activeTab, currentIndex) {
        if (visible && activeVideos.isNotEmpty()) {
            listState.scrollToItem(currentIndex)
            runCatching { initialCardFocus.requestFocus() }
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
        modifier = modifier
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
            val displayCategoryTitle = if (related.isCategory) {
                localizedCategoryLabel(related.title)
            } else {
                related.title
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.78f),
                                Color(0xFF0C0E1A).copy(alpha = 0.97f)
                            )
                        )
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onUserInteraction() }
                    .padding(top = 12.dp, bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (layoutInfo.isCompact) 12.dp else 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (hasDualTabs) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .onPreviewKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyDown) {
                                        onUserInteraction()
                                        when (event.key) {
                                            Key.DirectionUp -> {
                                                onDismiss()
                                                true
                                            }
                                            Key.DirectionDown -> {
                                                runCatching { initialCardFocus.requestFocus() }
                                                true
                                            }
                                            else -> false
                                        }
                                    } else {
                                        false
                                    }
                                },
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RelatedSheetTabPill(
                                icon = Icons.Default.VideoLibrary,
                                label = stringResource(R.string.player_tab_source, related.sourceTitle),
                                isSelected = activeTab == RelatedTabMode.SOURCE,
                                onClick = {
                                    onUserInteraction()
                                    activeTab = RelatedTabMode.SOURCE
                                },
                                modifier = Modifier.focusRequester(sourceTabFocus)
                            )
                            RelatedSheetTabPill(
                                icon = Icons.Default.Category,
                                label = stringResource(R.string.player_tab_category, displayCategoryTitle),
                                isSelected = activeTab == RelatedTabMode.CATEGORY,
                                onClick = {
                                    onUserInteraction()
                                    activeTab = RelatedTabMode.CATEGORY
                                },
                                modifier = Modifier.focusRequester(categoryTabFocus)
                            )
                        }
                    } else {
                        val singleHeaderTitle = if (activeTab == RelatedTabMode.SOURCE && related.sourceTitle.isNotBlank()) {
                            stringResource(R.string.player_tab_source, related.sourceTitle)
                        } else {
                            stringResource(R.string.player_more_in, displayCategoryTitle)
                        }
                        Text(
                            text = singleHeaderTitle,
                            color = SmartTubeTextPrimary,
                            fontSize = if (layoutInfo.isCompact) 14.sp else 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (layoutInfo.isTouchDevice) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(99.dp))
                                .background(SmartTubeCardBg)
                                .clickable {
                                    onUserInteraction()
                                    onDismiss()
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.action_close),
                                tint = SmartTubeTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    state = listState,
                    modifier = Modifier.onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown) {
                            onUserInteraction()
                            if (event.key == Key.DirectionUp) {
                                if (hasDualTabs) {
                                    val targetFocus = if (activeTab == RelatedTabMode.SOURCE) sourceTabFocus else categoryTabFocus
                                    runCatching { targetFocus.requestFocus() }
                                } else {
                                    onDismiss()
                                }
                                true
                            } else {
                                false
                            }
                        } else {
                            false
                        }
                    },
                    contentPadding = PaddingValues(horizontal = if (layoutInfo.isCompact) 12.dp else 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(
                        items = activeVideos,
                        key = { _, item -> "${activeTab.name}_${item.id}" }
                    ) { index, item ->
                        val isCurrent = index == currentIndex
                        val cardModifier = if (isCurrent) {
                            Modifier.focusRequester(initialCardFocus)
                        } else {
                            Modifier
                        }
                        RelatedPlayerCard(
                            video = item,
                            isCurrent = isCurrent,
                            cardWidth = cardWidth,
                            onClick = {
                                onUserInteraction()
                                onSelectVideo(item, activeVideos)
                            },
                            modifier = cardModifier
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RelatedSheetTabPill(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val bgColor = when {
        isFocused -> SmartTubeCardFocusedBg
        isSelected -> SmartTubeRed
        else -> SmartTubeCardBg
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(99.dp))
            .background(bgColor)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = when {
                    isFocused -> SmartTubeFocusBorder
                    isSelected -> SmartTubeRed
                    else -> Color.White.copy(alpha = 0.14f)
                },
                shape = RoundedCornerShape(99.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = if (isSelected || isFocused) FontWeight.ExtraBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RelatedPlayerCard(
    video: VideoItem,
    isCurrent: Boolean,
    cardWidth: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val settings = remember(context) { AppSettingsRepository.getInstance(context).settingsFlow.value }
    val cachedThumb = remember(video.id, video.entity.customThumbnailPath) {
        ThumbnailManager.getCachedThumbnailFile(context, video, settings)
    }
    val imageModel = remember(cachedThumb, video.entity.thumbnailUrl) {
        val data: Any? = cachedThumb ?: video.entity.thumbnailUrl.takeIf { it.isNotBlank() }
        data?.let {
            ImageRequest.Builder(context)
                .data(it)
                .size(384, 216)
                .crossfade(true)
                .build()
        }
    }

    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier = modifier
            .width(cardWidth)
            .graphicsLayer {
                val s = if (isFocused) 1.04f else 1.0f
                scaleX = s
                scaleY = s
            }
            .clip(shape)
            .background(if (isFocused) SmartTubeCardFocusedBg else SmartTubeCardBg)
            .border(
                width = when {
                    isFocused -> 2.5.dp
                    isCurrent -> 2.dp
                    else -> 1.dp
                },
                color = when {
                    isFocused -> SmartTubeFocusBorder
                    isCurrent -> SmartTubeRed
                    else -> Color.White.copy(alpha = 0.08f)
                },
                shape = shape
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color(0xFF121526)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (video.isYoutube) Icons.Default.OndemandVideo else Icons.Default.Movie,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.16f),
                modifier = Modifier.size(28.dp)
            )
            if (imageModel != null) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(SmartTubeRed)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.player_now_playing_badge),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            if (video.durationMs > 0L) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(SmartTubeOverlayDark)
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = video.formattedDuration,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        Text(
            text = video.title,
            color = SmartTubeTextPrimary,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun localizedCategoryLabel(raw: String): String {
    return when (raw.lowercase(Locale.ROOT)) {
        "all" -> stringResource(R.string.category_all)
        "islamic kids" -> stringResource(R.string.category_islamic_kids)
        "english learning" -> stringResource(R.string.category_english_learning)
        "stories & rhymes" -> stringResource(R.string.category_stories_rhymes)
        "cartoons & fun" -> stringResource(R.string.category_cartoons_fun)
        "stem", "stem & education" -> stringResource(R.string.category_stem_science)
        else -> raw
    }
}
