package com.joudjawadtube.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.joudjawadtube.player.R
import com.joudjawadtube.player.data.local.SavedChannelEntity
import com.joudjawadtube.player.data.local.SavedPlaylistEntity
import com.joudjawadtube.player.data.local.VideoItem
import com.joudjawadtube.player.data.settings.AppSettingsRepository
import com.joudjawadtube.player.data.thumbnail.ThumbnailManager
import com.joudjawadtube.player.ui.catalog.DestructiveAction
import com.joudjawadtube.player.ui.theme.LiquidGlassModalBackdropEffect_Antigravity
import com.joudjawadtube.player.ui.theme.SmartTubeCardBg
import com.joudjawadtube.player.ui.theme.SmartTubeCardFocusedBg
import com.joudjawadtube.player.ui.theme.SmartTubeFocusBorder
import com.joudjawadtube.player.ui.theme.SmartTubeOverlayDark
import com.joudjawadtube.player.ui.theme.SmartTubeRed
import com.joudjawadtube.player.ui.theme.SmartTubeSidebarBg
import com.joudjawadtube.player.ui.theme.SmartTubeTextPrimary
import com.joudjawadtube.player.ui.theme.SmartTubeTextSecondary
import com.joudjawadtube.player.ui.theme.liquidGlassModalSurface

data class QuickCollectionTarget_Antigravity(
    val title: String,
    val subtitle: String,
    val videos: List<VideoItem>
)

/**
 * Grid modal dialog opened from the Home top bar ("Channels & Playlists"), row headers,
 * or from a Video Card's "Go to Channel / Playlist" option.
 */
@Composable
fun QuickChannelsPlaylistsDialog_Antigravity(
    savedChannels: List<SavedChannelEntity>,
    savedPlaylists: List<SavedPlaylistEntity>,
    allVideos: List<VideoItem>,
    initialTarget: QuickCollectionTarget_Antigravity? = null,
    onPlayVideo: (VideoItem, List<VideoItem>) -> Unit,
    onAddYouTubeClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val layoutInfo = LocalWindowLayoutInfo.current
    val isCompact = layoutInfo.isCompact
    var activeTab by remember { mutableIntStateOf(0) } // 0 = Channels, 1 = Playlists
    var selectedCategory by remember { mutableStateOf("All") }
    var openedTarget by remember(initialTarget) { mutableStateOf(initialTarget) }

    val allCategories = remember(savedChannels, savedPlaylists) {
        val cats = (savedChannels.map { it.category } + savedPlaylists.map { it.category })
            .filter { it.isNotBlank() }
            .distinct()
        listOf("All") + cats
    }

    val filteredChannels = remember(savedChannels, selectedCategory) {
        if (selectedCategory == "All") savedChannels
        else savedChannels.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }

    val filteredPlaylists = remember(savedPlaylists, selectedCategory) {
        if (selectedCategory == "All") savedPlaylists
        else savedPlaylists.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        LiquidGlassModalBackdropEffect_Antigravity()
        Column(
            modifier = Modifier
                .fillMaxWidth(if (isCompact) 0.96f else 0.90f)
                .fillMaxHeight(if (isCompact) 0.90f else 0.86f)
                .widthIn(max = 980.dp)
                .liquidGlassModalSurface(RoundedCornerShape(18.dp))
                .padding(if (isCompact) 14.dp else 22.dp)
        ) {
            val currentTarget = openedTarget
            if (currentTarget != null) {
                // Collection Videos View (inside a selected Channel / Playlist / Folder)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (initialTarget == null) {
                            ActionPillButton(
                                icon = Icons.AutoMirrored.Filled.ArrowBack,
                                text = stringResource(R.string.action_back_to_list),
                                isPrimary = false,
                                onClick = { openedTarget = null }
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentTarget.title,
                                color = SmartTubeTextPrimary,
                                fontSize = if (isCompact) 16.sp else 19.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = currentTarget.subtitle,
                                color = SmartTubeTextSecondary,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentTarget.videos.isNotEmpty()) {
                            ActionPillButton(
                                icon = Icons.Default.PlayArrow,
                                text = stringResource(R.string.action_play_all),
                                isPrimary = true,
                                onClick = {
                                    onPlayVideo(currentTarget.videos.first(), currentTarget.videos)
                                    onDismiss()
                                }
                            )
                        }
                        DialogIconCloseButton(onClick = onDismiss)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(12.dp))

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = if (isCompact) 155.dp else 210.dp),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(
                        items = currentTarget.videos,
                        key = { it.id }
                    ) { video ->
                        QuickDialogVideoCard(
                            video = video,
                            onClick = {
                                onPlayVideo(video, currentTarget.videos)
                                onDismiss()
                            }
                        )
                    }
                }
            } else {
                // Grid Picker View (Channels & Playlists)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = null,
                            tint = SmartTubeRed,
                            modifier = Modifier.size(26.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.quick_picker_title),
                                color = SmartTubeTextPrimary,
                                fontSize = if (isCompact) 16.sp else 19.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = stringResource(R.string.quick_picker_subtitle),
                                color = SmartTubeTextSecondary,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ActionPillButton(
                            icon = Icons.Default.AddLink,
                            text = stringResource(R.string.action_add_youtube),
                            isPrimary = false,
                            onClick = {
                                onDismiss()
                                onAddYouTubeClick()
                            }
                        )
                        DialogIconCloseButton(onClick = onDismiss)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Top Tabs (Channels vs Playlists) + Category Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ActionPillButton(
                        icon = Icons.Default.Subscriptions,
                        text = "${stringResource(R.string.nav_channels)} (${filteredChannels.size})",
                        isPrimary = activeTab == 0,
                        onClick = { activeTab = 0 }
                    )
                    ActionPillButton(
                        icon = Icons.Default.VideoLibrary,
                        text = "${stringResource(R.string.nav_playlists)} (${filteredPlaylists.size})",
                        isPrimary = activeTab == 1,
                        onClick = { activeTab = 1 }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    allCategories.forEach { cat ->
                        QuickCategoryChip(
                            label = cat,
                            isSelected = selectedCategory.equals(cat, ignoreCase = true),
                            onClick = { selectedCategory = cat }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(12.dp))

                if (activeTab == 0) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = if (isCompact) 160.dp else 220.dp),
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 20.dp)
                    ) {
                        items(
                            items = filteredChannels,
                            key = { it.channelId }
                        ) { channel ->
                            val cVideos = remember(channel, allVideos) {
                                allVideos.filter {
                                    it.isYoutube && (
                                        (channel.uploadsPlaylistId.isNotBlank() && it.playlistId == channel.uploadsPlaylistId) ||
                                            it.entity.folderPath == "youtube_channel_${channel.channelId}" ||
                                            (channel.title.isNotBlank() && it.playlistTitle.contains(channel.title, ignoreCase = true))
                                        )
                                }
                            }
                            val thumb = channel.avatarUrl.ifBlank {
                                cVideos.firstOrNull()?.entity?.thumbnailUrl.orEmpty()
                            }
                            QuickCollectionGridCard(
                                title = channel.title,
                                subtitle = "${cVideos.size} videos • ${channel.category}",
                                thumbnailUrl = thumb,
                                isChannel = true,
                                onClick = {
                                    if (cVideos.isNotEmpty()) {
                                        openedTarget = QuickCollectionTarget_Antigravity(
                                            title = channel.title,
                                            subtitle = "${cVideos.size} videos • ${channel.category}",
                                            videos = cVideos
                                        )
                                    }
                                }
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = if (isCompact) 160.dp else 220.dp),
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 20.dp)
                    ) {
                        items(
                            items = filteredPlaylists,
                            key = { it.playlistId }
                        ) { playlist ->
                            val plVideos = remember(playlist, allVideos) {
                                allVideos.filter { it.isYoutube && it.playlistId == playlist.playlistId }
                            }
                            val thumb = playlist.thumbnailUrl.ifBlank {
                                plVideos.firstOrNull()?.entity?.thumbnailUrl.orEmpty()
                            }
                            QuickCollectionGridCard(
                                title = playlist.title,
                                subtitle = "${plVideos.size} videos • ${playlist.category}",
                                thumbnailUrl = thumb,
                                isChannel = false,
                                onClick = {
                                    if (plVideos.isNotEmpty()) {
                                        openedTarget = QuickCollectionTarget_Antigravity(
                                            title = playlist.title,
                                            subtitle = "${plVideos.size} videos • ${playlist.category}",
                                            videos = plVideos
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Modal window opened from Settings Card 5 ("Manage Channels, Playlists & Data").
 * Combines Saved Channels, Saved Playlists, and Watch History / Favorites / Reset actions.
 */
@Composable
fun ManageLibraryAndDataDialog_Antigravity(
    savedChannels: List<SavedChannelEntity>,
    savedPlaylists: List<SavedPlaylistEntity>,
    onAddYouTubeLink: () -> Unit,
    onDeleteChannel: (String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onClearWatchHistory: () -> Unit,
    onClearFavorites: () -> Unit,
    onClearYouTubeLibrary: () -> Unit,
    onResetSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val layoutInfo = LocalWindowLayoutInfo.current
    val isCompact = layoutInfo.isCompact
    var activeTab by remember { mutableIntStateOf(0) } // 0 = Channels, 1 = Playlists, 2 = Data & Reset
    var pendingDestructiveAction by remember { mutableStateOf<DestructiveAction?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        LiquidGlassModalBackdropEffect_Antigravity()
        Column(
            modifier = Modifier
                .fillMaxWidth(if (isCompact) 0.96f else 0.86f)
                .fillMaxHeight(if (isCompact) 0.88f else 0.82f)
                .widthIn(max = 860.dp)
                .liquidGlassModalSurface(RoundedCornerShape(18.dp))
                .padding(if (isCompact) 14.dp else 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_manage_library_data_title),
                        color = SmartTubeTextPrimary,
                        fontSize = if (isCompact) 16.sp else 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(
                            R.string.settings_manage_library_data_sub,
                            savedChannels.size,
                            savedPlaylists.size
                        ),
                        color = SmartTubeTextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ActionPillButton(
                        icon = Icons.Default.AddLink,
                        text = stringResource(R.string.action_add_youtube),
                        isPrimary = true,
                        onClick = {
                            onDismiss()
                            onAddYouTubeLink()
                        }
                    )
                    DialogIconCloseButton(onClick = onDismiss)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3 Tabs: Channels | Playlists | Data & Reset
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ActionPillButton(
                    icon = Icons.Default.Subscriptions,
                    text = "${stringResource(R.string.nav_channels)} (${savedChannels.size})",
                    isPrimary = activeTab == 0,
                    onClick = { activeTab = 0 }
                )
                ActionPillButton(
                    icon = Icons.Default.VideoLibrary,
                    text = "${stringResource(R.string.nav_playlists)} (${savedPlaylists.size})",
                    isPrimary = activeTab == 1,
                    onClick = { activeTab = 1 }
                )
                ActionPillButton(
                    icon = Icons.Default.Restore,
                    text = stringResource(R.string.settings_data_tab_reset),
                    isPrimary = activeTab == 2,
                    onClick = { activeTab = 2 }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                when (activeTab) {
                    0 -> {
                        items(
                            items = savedChannels,
                            key = { it.channelId }
                        ) { chan ->
                            ManagerActionRow(
                                icon = Icons.Default.Subscriptions,
                                title = chan.title,
                                subtitle = "${chan.handle} • ${chan.category}",
                                actionLabel = stringResource(R.string.action_delete),
                                onClick = {
                                    pendingDestructiveAction = DestructiveAction(
                                        title = "Delete Channel",
                                        message = "Are you sure you want to delete channel \"${chan.title}\"? All its uploads and playlists will be removed.",
                                        confirmLabel = "Delete",
                                        onConfirm = { onDeleteChannel(chan.channelId) }
                                    )
                                }
                            )
                        }
                    }
                    1 -> {
                        items(
                            items = savedPlaylists,
                            key = { it.playlistId }
                        ) { pl ->
                            ManagerActionRow(
                                icon = Icons.Default.VideoLibrary,
                                title = pl.title,
                                subtitle = "${pl.videoCount} videos • ${pl.category}",
                                actionLabel = stringResource(R.string.action_delete),
                                onClick = {
                                    pendingDestructiveAction = DestructiveAction(
                                        title = "Delete Playlist",
                                        message = "Are you sure you want to delete playlist \"${pl.title}\"? Its ${pl.videoCount} videos will be removed from your library.",
                                        confirmLabel = "Delete",
                                        onConfirm = { onDeletePlaylist(pl.playlistId) }
                                    )
                                }
                            )
                        }
                    }
                    else -> {
                        item(key = "clear_history") {
                            ManagerActionRow(
                                icon = Icons.Default.History,
                                title = "Clear Watch History & Playback Progress",
                                subtitle = "Resets Continue Watching positions for all videos",
                                actionLabel = "Clear History",
                                onClick = {
                                    pendingDestructiveAction = DestructiveAction(
                                        title = "Clear Watch History",
                                        message = "Are you sure you want to clear your watch history? Continue Watching progress will be reset for all videos.",
                                        confirmLabel = "Clear History",
                                        onConfirm = onClearWatchHistory
                                    )
                                }
                            )
                        }
                        item(key = "clear_favorites") {
                            ManagerActionRow(
                                icon = Icons.Default.Delete,
                                title = "Clear All Favorites",
                                subtitle = "Removes all videos from your Favorites list",
                                actionLabel = "Clear Favorites",
                                onClick = {
                                    pendingDestructiveAction = DestructiveAction(
                                        title = "Clear All Favorites",
                                        message = "Are you sure you want to remove all saved favorite videos?",
                                        confirmLabel = "Clear Favorites",
                                        onConfirm = onClearFavorites
                                    )
                                }
                            )
                        }
                        item(key = "clear_yt") {
                            ManagerActionRow(
                                icon = Icons.Default.Delete,
                                title = "Remove All Saved YouTube Links & Playlists",
                                subtitle = "Deletes imported YouTube links while keeping all local & USB videos untouched",
                                actionLabel = "Clear YouTube",
                                onClick = {
                                    pendingDestructiveAction = DestructiveAction(
                                        title = "Clear YouTube Library",
                                        message = "Are you sure you want to delete all saved YouTube playlists, channels, and video links? Local and USB files will remain safe.",
                                        confirmLabel = "Clear All",
                                        onConfirm = onClearYouTubeLibrary
                                    )
                                }
                            )
                        }
                        item(key = "reset_defaults") {
                            ManagerActionRow(
                                icon = Icons.Default.Restore,
                                title = "Reset All Settings to Default",
                                subtitle = "Restores all player, thumbnail, and layout settings to factory defaults",
                                actionLabel = "Reset Defaults",
                                onClick = {
                                    pendingDestructiveAction = DestructiveAction(
                                        title = "Reset All Settings",
                                        message = "Are you sure you want to restore all settings to factory default values?",
                                        confirmLabel = "Reset Defaults",
                                        onConfirm = onResetSettings
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    pendingDestructiveAction?.let { action ->
        ConfirmDestructiveDialog_Antigravity(
            title = action.title,
            message = action.message,
            confirmActionLabel = action.confirmLabel,
            onConfirmed = action.onConfirm,
            onDismiss = { pendingDestructiveAction = null }
        )
    }
}

@Composable
private fun QuickCategoryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(
                when {
                    isSelected -> SmartTubeRed
                    isFocused -> SmartTubeCardFocusedBg
                    else -> SmartTubeCardBg
                }
            )
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) SmartTubeFocusBorder else Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(17.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun QuickCollectionGridCard(
    title: String,
    subtitle: String,
    thumbnailUrl: String,
    isChannel: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                val s = if (isFocused) 1.03f else 1.0f
                scaleX = s
                scaleY = s
            }
            .clip(shape)
            .background(if (isFocused) SmartTubeCardFocusedBg else SmartTubeCardBg)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) SmartTubeFocusBorder else Color.White.copy(alpha = 0.08f),
                shape = shape
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(if (isChannel) CircleShape else RoundedCornerShape(8.dp))
                .background(Color(0xFF141828)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isChannel) Icons.Default.Subscriptions else Icons.Default.VideoLibrary,
                contentDescription = null,
                tint = SmartTubeRed,
                modifier = Modifier.size(22.dp)
            )
            if (thumbnailUrl.isNotBlank()) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = SmartTubeTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = SmartTubeTextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun QuickDialogVideoCard(
    video: VideoItem,
    onClick: () -> Unit
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
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                val s = if (isFocused) 1.03f else 1.0f
                scaleX = s
                scaleY = s
            }
            .clip(shape)
            .background(if (isFocused) SmartTubeCardFocusedBg else SmartTubeCardBg)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) SmartTubeFocusBorder else Color.White.copy(alpha = 0.08f),
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
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Text(
            text = video.title,
            color = SmartTubeTextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun ManagerActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isFocused) SmartTubeCardFocusedBg else Color(0xFF191919))
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) SmartTubeFocusBorder else Color.White.copy(alpha = 0.06f),
                shape = RoundedCornerShape(10.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isFocused) SmartTubeRed else SmartTubeTextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = SmartTubeTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    color = SmartTubeTextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (isFocused) SmartTubeRed else Color(0xFF2C2C2C))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = actionLabel,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
