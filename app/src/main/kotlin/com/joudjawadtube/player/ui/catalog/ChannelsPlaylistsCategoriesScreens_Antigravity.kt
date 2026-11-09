package com.joudjawadtube.player.ui.catalog

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.joudjawadtube.player.data.local.SavedChannelEntity
import com.joudjawadtube.player.data.local.SavedPlaylistEntity
import com.joudjawadtube.player.data.local.VideoItem
import com.joudjawadtube.player.ui.components.VideoCard
import com.joudjawadtube.player.ui.theme.SmartTubeCardBg
import com.joudjawadtube.player.ui.theme.SmartTubeCardFocusedBg
import com.joudjawadtube.player.ui.theme.SmartTubeFocusBorder
import com.joudjawadtube.player.ui.theme.SmartTubeGold4K
import com.joudjawadtube.player.ui.theme.SmartTubeRed
import com.joudjawadtube.player.ui.theme.SmartTubeSidebarBg
import com.joudjawadtube.player.ui.theme.SmartTubeTextPrimary
import com.joudjawadtube.player.ui.theme.SmartTubeTextSecondary
import com.joudjawadtube.player.ui.theme.SmartTubeUsbCyan

val STANDARD_CATEGORIES_ANTIGRAVITY = listOf(
    "General",
    "Islamic Kids",
    "English Learning",
    "Stories & Rhymes",
    "Cartoons & Fun",
    "STEM & Education"
)

// ---------------------------------------------------------------------------
// 1. CHANNELS SCREEN
// ---------------------------------------------------------------------------

@Composable
fun ChannelsScreen_Antigravity(
    channels: List<SavedChannelEntity>,
    allVideos: List<VideoItem>,
    onOpenChannelVideos: (channel: SavedChannelEntity, videos: List<VideoItem>) -> Unit,
    onVideoClick: (VideoItem, List<VideoItem>) -> Unit,
    onToggleFavorite: (VideoItem) -> Unit,
    onOpenVideoOptions: (VideoItem) -> Unit,
    onChangeChannelCategory: (channelId: String, newCategory: String) -> Unit,
    onDeleteChannel: (channelId: String) -> Unit,
    onAddChannelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var channelToEditCategory by remember { mutableStateOf<SavedChannelEntity?>(null) }
    var channelToDelete by remember { mutableStateOf<SavedChannelEntity?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        // Channels Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SmartTubeRed.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = "Channels",
                        tint = SmartTubeRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "Saved Channels",
                        color = SmartTubeTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${channels.size} channels added • Direct in-app playback",
                        color = SmartTubeTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            InteractiveActionButton(
                icon = Icons.Default.AddLink,
                text = "+ Add Channel",
                onClick = onAddChannelClick
            )
        }

        if (channels.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = null,
                        tint = SmartTubeTextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "No channels added yet",
                        color = SmartTubeTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Add YouTube channels via '+ Add Channel' or import from Kids Picks to organize playlists and videos automatically.",
                        color = SmartTubeTextSecondary,
                        fontSize = 13.sp,
                        maxLines = 2
                    )
                    InteractiveActionButton(
                        icon = Icons.Default.AddLink,
                        text = "Add Your First Channel",
                        onClick = onAddChannelClick,
                        isPrimary = true
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(channels, key = { it.channelId }) { channel ->
                    val channelVideos = remember(allVideos, channel.channelId, channel.title) {
                        allVideos.filter {
                            (channel.uploadsPlaylistId.isNotBlank() && it.playlistId == channel.uploadsPlaylistId) ||
                                (channel.title.isNotBlank() && it.playlistTitle.contains(channel.title, ignoreCase = true)) ||
                                (channel.title.isNotBlank() && it.folderName.contains(channel.title, ignoreCase = true))
                        }
                    }

                    ChannelCardSection(
                        channel = channel,
                        videos = channelVideos,
                        onPlayAll = { onOpenChannelVideos(channel, channelVideos) },
                        onVideoClick = { clicked -> onVideoClick(clicked, channelVideos) },
                        onToggleFavorite = onToggleFavorite,
                        onOpenVideoOptions = onOpenVideoOptions,
                        onEditCategory = { channelToEditCategory = channel },
                        onDelete = { channelToDelete = channel }
                    )
                }
            }
        }
    }

    // Edit Category Dialog
    channelToEditCategory?.let { channel ->
        ChangeCategoryDialog_Antigravity(
            title = "Channel Category",
            subtitle = channel.title,
            currentCategory = channel.category,
            onSaveCategory = { newCategory ->
                onChangeChannelCategory(channel.channelId, newCategory)
                channelToEditCategory = null
            },
            onDismiss = { channelToEditCategory = null }
        )
    }

    // Delete Confirmation Dialog
    channelToDelete?.let { channel ->
        DeleteConfirmDialog(
            title = "Delete Channel?",
            message = "Remove '${channel.title}' and its automatically imported playlists and videos?",
            onConfirm = {
                onDeleteChannel(channel.channelId)
                channelToDelete = null
            },
            onDismiss = { channelToDelete = null }
        )
    }
}

@Composable
private fun ChannelCardSection(
    channel: SavedChannelEntity,
    videos: List<VideoItem>,
    onPlayAll: () -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    onToggleFavorite: (VideoItem) -> Unit,
    onOpenVideoOptions: (VideoItem) -> Unit,
    onEditCategory: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SmartTubeCardBg)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        // Channel Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Channel Initial Avatar
                val initialLetter = channel.title.take(1).uppercase()
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(SmartTubeRed),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initialLetter.ifBlank { "C" },
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = channel.title,
                            color = SmartTubeTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Category Pill with Edit trigger
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SmartTubeGold4K.copy(alpha = 0.16f))
                                .clickable { onEditCategory() }
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = channel.category,
                                color = SmartTubeGold4K,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit category",
                                tint = SmartTubeGold4K,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }

                    Text(
                        text = "${if (channel.handle.isNotBlank()) "${channel.handle} • " else ""}${videos.size} videos available",
                        color = SmartTubeTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            // Actions: Play All + Delete
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (videos.isNotEmpty()) {
                    InteractiveActionButton(
                        icon = Icons.Default.PlayArrow,
                        text = "Play All",
                        onClick = onPlayAll,
                        isPrimary = true
                    )
                }
                InteractiveActionButton(
                    icon = Icons.Default.Delete,
                    text = "Delete",
                    onClick = onDelete,
                    tint = SmartTubeTextSecondary
                )
            }
        }

        // Horizontal Videos Carousel
        if (videos.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(videos, key = { it.id }) { item ->
                    VideoCard(
                        video = item,
                        onClick = { onVideoClick(item) },
                        onOpenOptions = { onOpenVideoOptions(item) },
                        onToggleFavorite = { onToggleFavorite(item) },
                        modifier = Modifier.width(200.dp)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 2. PLAYLISTS SCREEN
// ---------------------------------------------------------------------------

@Composable
fun PlaylistsScreen_Antigravity(
    playlists: List<SavedPlaylistEntity>,
    allVideos: List<VideoItem>,
    onOpenPlaylistVideos: (playlist: SavedPlaylistEntity, videos: List<VideoItem>) -> Unit,
    onVideoClick: (VideoItem, List<VideoItem>) -> Unit,
    onToggleFavorite: (VideoItem) -> Unit,
    onOpenVideoOptions: (VideoItem) -> Unit,
    onChangePlaylistCategory: (playlistId: String, newCategory: String) -> Unit,
    onDeletePlaylist: (playlistId: String) -> Unit,
    onAddPlaylistClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var playlistToEditCategory by remember { mutableStateOf<SavedPlaylistEntity?>(null) }
    var playlistToDelete by remember { mutableStateOf<SavedPlaylistEntity?>(null) }
    var activeFilter by remember { mutableStateOf("ALL") } // "ALL", "MANUAL", "CHANNEL"

    val filteredPlaylists = remember(playlists, activeFilter) {
        when (activeFilter) {
            "MANUAL" -> playlists.filter { !it.isAutoCreatedFromChannel }
            "CHANNEL" -> playlists.filter { it.isAutoCreatedFromChannel }
            else -> playlists
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SmartTubeUsbCyan.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlaylistPlay,
                        contentDescription = "Playlists",
                        tint = SmartTubeUsbCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "Saved Playlists",
                        color = SmartTubeTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${playlists.size} playlists • Manual and channel uploads",
                        color = SmartTubeTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            InteractiveActionButton(
                icon = Icons.Default.AddLink,
                text = "+ Add Playlist",
                onClick = onAddPlaylistClick
            )
        }

        // Filter Chips (All / Manual / Channel Uploads)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterPillChip(
                label = "All (${playlists.size})",
                isSelected = activeFilter == "ALL",
                onClick = { activeFilter = "ALL" }
            )
            FilterPillChip(
                label = "Manual Playlists (${playlists.count { !it.isAutoCreatedFromChannel }})",
                isSelected = activeFilter == "MANUAL",
                onClick = { activeFilter = "MANUAL" }
            )
            FilterPillChip(
                label = "Channel Uploads (${playlists.count { it.isAutoCreatedFromChannel }})",
                isSelected = activeFilter == "CHANNEL",
                onClick = { activeFilter = "CHANNEL" }
            )
        }

        if (filteredPlaylists.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlaylistPlay,
                        contentDescription = null,
                        tint = SmartTubeTextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = if (activeFilter == "ALL") "No playlists saved yet" else "No matching playlists in this filter",
                        color = SmartTubeTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Import YouTube playlist URLs or add channels to populate this list.",
                        color = SmartTubeTextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(filteredPlaylists, key = { it.playlistId }) { playlist ->
                    val playlistVideos = remember(allVideos, playlist.playlistId) {
                        allVideos.filter { it.playlistId == playlist.playlistId }
                    }

                    PlaylistCardSection(
                        playlist = playlist,
                        videos = playlistVideos,
                        onPlayAll = { onOpenPlaylistVideos(playlist, playlistVideos) },
                        onVideoClick = { clicked -> onVideoClick(clicked, playlistVideos) },
                        onToggleFavorite = onToggleFavorite,
                        onOpenVideoOptions = onOpenVideoOptions,
                        onEditCategory = { playlistToEditCategory = playlist },
                        onDelete = { playlistToDelete = playlist }
                    )
                }
            }
        }
    }

    // Edit Category Dialog
    playlistToEditCategory?.let { playlist ->
        ChangeCategoryDialog_Antigravity(
            title = "Playlist Category",
            subtitle = playlist.title,
            currentCategory = playlist.category,
            onSaveCategory = { newCategory ->
                onChangePlaylistCategory(playlist.playlistId, newCategory)
                playlistToEditCategory = null
            },
            onDismiss = { playlistToEditCategory = null }
        )
    }

    // Delete Confirmation Dialog
    playlistToDelete?.let { playlist ->
        DeleteConfirmDialog(
            title = "Delete Playlist?",
            message = "Remove playlist '${playlist.title}' and its saved videos from your library?",
            onConfirm = {
                onDeletePlaylist(playlist.playlistId)
                playlistToDelete = null
            },
            onDismiss = { playlistToDelete = null }
        )
    }
}

@Composable
private fun PlaylistCardSection(
    playlist: SavedPlaylistEntity,
    videos: List<VideoItem>,
    onPlayAll: () -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    onToggleFavorite: (VideoItem) -> Unit,
    onOpenVideoOptions: (VideoItem) -> Unit,
    onEditCategory: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SmartTubeCardBg)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SmartTubeUsbCyan.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlaylistPlay,
                        contentDescription = null,
                        tint = SmartTubeUsbCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = playlist.title,
                            color = SmartTubeTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Badge: Manual vs Auto Channel
                        val sourceBadgeText = if (playlist.isAutoCreatedFromChannel) "Channel" else "Manual"
                        val sourceBadgeColor = if (playlist.isAutoCreatedFromChannel) SmartTubeRed else SmartTubeUsbCyan
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(sourceBadgeColor.copy(alpha = 0.16f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = sourceBadgeText,
                                color = sourceBadgeColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Category Pill with Edit trigger
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(SmartTubeGold4K.copy(alpha = 0.16f))
                                .clickable { onEditCategory() }
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = playlist.category,
                                color = SmartTubeGold4K,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit category",
                                tint = SmartTubeGold4K,
                                modifier = Modifier.size(9.dp)
                            )
                        }
                    }

                    Text(
                        text = "${videos.size} videos in playlist",
                        color = SmartTubeTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            // Actions: Play All + Delete
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (videos.isNotEmpty()) {
                    InteractiveActionButton(
                        icon = Icons.Default.PlayArrow,
                        text = "Play All",
                        onClick = onPlayAll,
                        isPrimary = true
                    )
                }
                InteractiveActionButton(
                    icon = Icons.Default.Delete,
                    text = "Delete",
                    onClick = onDelete,
                    tint = SmartTubeTextSecondary
                )
            }
        }

        // Horizontal Videos Carousel
        if (videos.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(videos, key = { it.id }) { item ->
                    VideoCard(
                        video = item,
                        onClick = { onVideoClick(item) },
                        onOpenOptions = { onOpenVideoOptions(item) },
                        onToggleFavorite = { onToggleFavorite(item) },
                        modifier = Modifier.width(200.dp)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 3. CATEGORIES SCREEN
// ---------------------------------------------------------------------------

@Composable
fun CategoriesScreen_Antigravity(
    channels: List<SavedChannelEntity>,
    playlists: List<SavedPlaylistEntity>,
    allVideos: List<VideoItem>,
    onOpenChannelVideos: (SavedChannelEntity, List<VideoItem>) -> Unit,
    onOpenPlaylistVideos: (SavedPlaylistEntity, List<VideoItem>) -> Unit,
    onVideoClick: (VideoItem, List<VideoItem>) -> Unit,
    onToggleFavorite: (VideoItem) -> Unit,
    onOpenVideoOptions: (VideoItem) -> Unit,
    onChangeChannelCategory: (channelId: String, newCategory: String) -> Unit,
    onChangePlaylistCategory: (playlistId: String, newCategory: String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Build list of distinct categories
    val allCategories = remember(channels, playlists) {
        val found = (channels.map { it.category } + playlists.map { it.category }).toSet()
        val combined = (STANDARD_CATEGORIES_ANTIGRAVITY + found).distinct()
        listOf("All") + combined
    }

    var selectedCategory by remember { mutableStateOf("All") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(SmartTubeGold4K.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Category,
                    contentDescription = "Categories",
                    tint = SmartTubeGold4K,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = "Categories",
                    color = SmartTubeTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Channels & Playlists organized by topic",
                    color = SmartTubeTextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        // Horizontal Category Filter Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            allCategories.forEach { cat ->
                FilterPillChip(
                    label = cat,
                    isSelected = selectedCategory == cat,
                    onClick = { selectedCategory = cat }
                )
            }
        }

        // Display sections
        val activeCategories = if (selectedCategory == "All") {
            allCategories.filter { it != "All" }
        } else {
            listOf(selectedCategory)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(22.dp),
            contentPadding = PaddingValues(bottom = 36.dp)
        ) {
            items(activeCategories, key = { it }) { cat ->
                val catChannels = remember(channels, cat) {
                    channels.filter { it.category.equals(cat, ignoreCase = true) }
                }
                val catPlaylists = remember(playlists, cat) {
                    playlists.filter { it.category.equals(cat, ignoreCase = true) }
                }

                if (catChannels.isNotEmpty() || catPlaylists.isNotEmpty()) {
                    CategoryGroupSection(
                        categoryName = cat,
                        channels = catChannels,
                        playlists = catPlaylists,
                        allVideos = allVideos,
                        onOpenChannelVideos = onOpenChannelVideos,
                        onOpenPlaylistVideos = onOpenPlaylistVideos,
                        onVideoClick = onVideoClick,
                        onToggleFavorite = onToggleFavorite,
                        onOpenVideoOptions = onOpenVideoOptions
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryGroupSection(
    categoryName: String,
    channels: List<SavedChannelEntity>,
    playlists: List<SavedPlaylistEntity>,
    allVideos: List<VideoItem>,
    onOpenChannelVideos: (SavedChannelEntity, List<VideoItem>) -> Unit,
    onOpenPlaylistVideos: (SavedPlaylistEntity, List<VideoItem>) -> Unit,
    onVideoClick: (VideoItem, List<VideoItem>) -> Unit,
    onToggleFavorite: (VideoItem) -> Unit,
    onOpenVideoOptions: (VideoItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SmartTubeCardBg)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(SmartTubeGold4K)
            )
            Text(
                text = categoryName,
                color = SmartTubeTextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "(${channels.size} channels • ${playlists.size} playlists)",
                color = SmartTubeTextSecondary,
                fontSize = 12.sp
            )
        }

        // Sub-section: Channels in Category
        if (channels.isNotEmpty()) {
            Text(
                text = "Channels",
                color = SmartTubeTextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(channels, key = { it.channelId }) { channel ->
                    val cVideos = remember(allVideos, channel.channelId, channel.title) {
                        allVideos.filter {
                            (channel.uploadsPlaylistId.isNotBlank() && it.playlistId == channel.uploadsPlaylistId) ||
                                (channel.title.isNotBlank() && it.playlistTitle.contains(channel.title, ignoreCase = true)) ||
                                (channel.title.isNotBlank() && it.folderName.contains(channel.title, ignoreCase = true))
                        }
                    }
                    CategoryChannelPill(
                        channel = channel,
                        videoCount = cVideos.size,
                        onClick = { onOpenChannelVideos(channel, cVideos) }
                    )
                }
            }
        }

        // Sub-section: Playlists in Category
        if (playlists.isNotEmpty()) {
            Text(
                text = "Playlists",
                color = SmartTubeTextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(playlists, key = { it.playlistId }) { playlist ->
                    val plVideos = remember(allVideos, playlist.playlistId) {
                        allVideos.filter { it.playlistId == playlist.playlistId }
                    }
                    CategoryPlaylistPill(
                        playlist = playlist,
                        videoCount = plVideos.size,
                        onClick = { onOpenPlaylistVideos(playlist, plVideos) }
                    )
                }
            }
        }
    }
}

@Composable
internal fun CategoryChannelPill(
    channel: SavedChannelEntity,
    videoCount: Int,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isFocused) SmartTubeCardFocusedBg else SmartTubeSidebarBg)
            .border(
                width = if (isFocused) 1.5.dp else 1.dp,
                color = if (isFocused) SmartTubeFocusBorder else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .onFocusChanged { isFocused = it.isFocused || it.hasFocus }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val initialLetter = channel.title.take(1).uppercase()
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(SmartTubeRed),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initialLetter.ifBlank { "C" },
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Column {
            Text(
                text = channel.title,
                color = SmartTubeTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = "$videoCount videos",
                color = SmartTubeTextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
internal fun CategoryPlaylistPill(
    playlist: SavedPlaylistEntity,
    videoCount: Int,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isFocused) SmartTubeCardFocusedBg else SmartTubeSidebarBg)
            .border(
                width = if (isFocused) 1.5.dp else 1.dp,
                color = if (isFocused) SmartTubeFocusBorder else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .onFocusChanged { isFocused = it.isFocused || it.hasFocus }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.PlaylistPlay,
            contentDescription = null,
            tint = SmartTubeUsbCyan,
            modifier = Modifier.size(20.dp)
        )
        Column {
            Text(
                text = playlist.title,
                color = SmartTubeTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = "$videoCount videos",
                color = SmartTubeTextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 4. SHARED DIALOGS & COMPONENTS
// ---------------------------------------------------------------------------

@Composable
fun ChangeCategoryDialog_Antigravity(
    title: String,
    subtitle: String,
    currentCategory: String,
    onSaveCategory: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCat by remember { mutableStateOf(currentCategory) }
    var customText by remember { mutableStateOf("") }
    var isCustomActive by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(440.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SmartTubeSidebarBg)
                .border(1.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = title,
                        color = SmartTubeTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        color = SmartTubeTextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = SmartTubeTextSecondary,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onDismiss() }
                )
            }

            Text(
                text = "Choose a Category:",
                color = SmartTubeTextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            // Category options
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                STANDARD_CATEGORIES_ANTIGRAVITY.forEach { cat ->
                    val isSelected = !isCustomActive && selectedCat.equals(cat, ignoreCase = true)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) SmartTubeRed.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f))
                            .clickable {
                                selectedCat = cat
                                isCustomActive = false
                            }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = cat,
                            color = if (isSelected) SmartTubeRed else SmartTubeTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = SmartTubeRed,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Custom category input
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Or Custom Category:",
                    color = SmartTubeTextSecondary,
                    fontSize = 12.sp
                )
                BasicTextField(
                    value = customText,
                    onValueChange = {
                        customText = it
                        if (it.isNotBlank()) isCustomActive = true
                    },
                    singleLine = true,
                    textStyle = TextStyle(color = SmartTubeTextPrimary, fontSize = 13.sp),
                    cursorBrush = SolidColor(SmartTubeRed),
                    decorationBox = { inner ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(1.dp, if (isCustomActive) SmartTubeRed else Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (customText.isEmpty()) {
                                Text(
                                    text = "Type new category name...",
                                    color = SmartTubeTextSecondary.copy(alpha = 0.6f),
                                    fontSize = 12.sp
                                )
                            }
                            inner()
                        }
                    }
                )
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                InteractiveActionButton(
                    icon = Icons.Default.Close,
                    text = "Cancel",
                    onClick = onDismiss,
                    tint = SmartTubeTextSecondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                InteractiveActionButton(
                    icon = Icons.Default.Check,
                    text = "Save",
                    onClick = {
                        val finalCategory = if (isCustomActive && customText.isNotBlank()) customText.trim() else selectedCat
                        onSaveCategory(finalCategory)
                    },
                    isPrimary = true
                )
            }
        }
    }
}

@Composable
private fun DeleteConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(400.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SmartTubeSidebarBg)
                .border(1.5.dp, SmartTubeRed.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = title,
                color = SmartTubeRed,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = message,
                color = SmartTubeTextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                InteractiveActionButton(
                    icon = Icons.Default.Close,
                    text = "Cancel",
                    onClick = onDismiss,
                    tint = SmartTubeTextSecondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                InteractiveActionButton(
                    icon = Icons.Default.Delete,
                    text = "Delete",
                    onClick = onConfirm,
                    isPrimary = true
                )
            }
        }
    }
}

@Composable
private fun FilterPillChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                when {
                    isSelected -> SmartTubeRed
                    isFocused -> SmartTubeCardFocusedBg
                    else -> SmartTubeCardBg
                }
            )
            .border(
                width = if (isFocused) 1.5.dp else 1.dp,
                color = if (isFocused) SmartTubeFocusBorder else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .onFocusChanged { isFocused = it.isFocused || it.hasFocus }
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else SmartTubeTextPrimary,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun InteractiveActionButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    isPrimary: Boolean = false,
    tint: Color = SmartTubeTextPrimary
) {
    var isFocused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isPrimary -> SmartTubeRed
                    isFocused -> SmartTubeCardFocusedBg
                    else -> Color.White.copy(alpha = 0.06f)
                }
            )
            .border(
                width = if (isFocused) 1.5.dp else 1.dp,
                color = if (isFocused) SmartTubeFocusBorder else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .onFocusChanged { isFocused = it.isFocused || it.hasFocus }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = if (isPrimary) Color.White else tint,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            color = if (isPrimary) Color.White else tint,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ---------------------------------------------------------------------------
// 5. IN-APP SEARCH RESULTS VIEW
// ---------------------------------------------------------------------------

@Composable
fun InAppSearchResultsView_Antigravity(
    query: String,
    matchingChannels: List<SavedChannelEntity>,
    matchingPlaylists: List<SavedPlaylistEntity>,
    matchingVideos: List<VideoItem>,
    onOpenChannelVideos: (SavedChannelEntity, List<VideoItem>) -> Unit,
    onOpenPlaylistVideos: (SavedPlaylistEntity, List<VideoItem>) -> Unit,
    onVideoClick: (VideoItem, List<VideoItem>) -> Unit,
    onToggleFavorite: (VideoItem) -> Unit,
    onOpenVideoOptions: (VideoItem) -> Unit,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalMatches = matchingChannels.size + matchingPlaylists.size + matchingVideos.size

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "In-App Search Results",
                    color = SmartTubeTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$totalMatches results for \"$query\" (Saved Content Only)",
                    color = SmartTubeTextSecondary,
                    fontSize = 12.sp
                )
            }
            InteractiveActionButton(
                icon = Icons.Default.Close,
                text = "Close Search",
                onClick = onClearSearch
            )
        }

        if (totalMatches == 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = SmartTubeTextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(54.dp)
                    )
                    Text(
                        text = "No in-app content found matching \"$query\"",
                        color = SmartTubeTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Search is scoped strictly to channels, playlists, and videos already saved in your app.",
                        color = SmartTubeTextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(bottom = 36.dp)
            ) {
                if (matchingChannels.isNotEmpty()) {
                    item {
                        Text(
                            text = "Channels (${matchingChannels.size})",
                            color = SmartTubeTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(matchingChannels, key = { it.channelId }) { channel ->
                                val cVideos = matchingVideos.filter {
                                    (channel.uploadsPlaylistId.isNotBlank() && it.playlistId == channel.uploadsPlaylistId) ||
                                        (channel.title.isNotBlank() && it.playlistTitle.contains(channel.title, ignoreCase = true)) ||
                                        (channel.title.isNotBlank() && it.folderName.contains(channel.title, ignoreCase = true))
                                }
                                CategoryChannelPill(
                                    channel = channel,
                                    videoCount = cVideos.size,
                                    onClick = { onOpenChannelVideos(channel, cVideos) }
                                )
                            }
                        }
                    }
                }

                if (matchingPlaylists.isNotEmpty()) {
                    item {
                        Text(
                            text = "Playlists (${matchingPlaylists.size})",
                            color = SmartTubeTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(matchingPlaylists, key = { it.playlistId }) { playlist ->
                                val pVideos = matchingVideos.filter { it.playlistId == playlist.playlistId }
                                CategoryPlaylistPill(
                                    playlist = playlist,
                                    videoCount = pVideos.size,
                                    onClick = { onOpenPlaylistVideos(playlist, pVideos) }
                                )
                            }
                        }
                    }
                }

                if (matchingVideos.isNotEmpty()) {
                    item {
                        Text(
                            text = "Videos (${matchingVideos.size})",
                            color = SmartTubeTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(matchingVideos.chunked(3)) { chunk ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            chunk.forEach { video ->
                                VideoCard(
                                    video = video,
                                    onClick = { onVideoClick(video, matchingVideos) },
                                    onOpenOptions = { onOpenVideoOptions(video) },
                                    onToggleFavorite = { onToggleFavorite(video) },
                                    modifier = Modifier.width(220.dp)
                                )
                            }
                            repeat(3 - chunk.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}
