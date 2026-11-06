package com.joudjawadtube.player.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joudjawadtube.player.data.local.VideoItem
import com.joudjawadtube.player.data.repository.CategoryRow
import com.joudjawadtube.player.data.settings.AppSettingsState
import com.joudjawadtube.player.ui.components.ActionPillButton
import com.joudjawadtube.player.ui.components.PermissionBanner
import com.joudjawadtube.player.ui.components.SidebarSection
import com.joudjawadtube.player.ui.components.SmartTubeSidebar
import com.joudjawadtube.player.ui.components.VideoCard
import com.joudjawadtube.player.ui.theme.SmartTubeBackground
import com.joudjawadtube.player.ui.theme.SmartTubeCardBg
import com.joudjawadtube.player.ui.theme.SmartTubeCardFocusedBg
import com.joudjawadtube.player.ui.theme.SmartTubeFocusBorder
import com.joudjawadtube.player.ui.theme.SmartTubeRed
import com.joudjawadtube.player.ui.theme.SmartTubeTextPrimary
import com.joudjawadtube.player.ui.theme.SmartTubeTextSecondary
import com.joudjawadtube.player.ui.theme.SmartTubeUsbCyan
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TvCatalogScreen(
    uiState: CatalogUiState,
    hasReadVideoPermission: Boolean,
    hasAllFilesAccess: Boolean,
    onSelectSection: (SidebarSection) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectFolderFilter: (String?) -> Unit,
    onVideoClick: (VideoItem, List<VideoItem>) -> Unit,
    onToggleFavorite: (VideoItem) -> Unit,
    onOpenVideoOptions: (VideoItem) -> Unit,
    onOpenAddYouTubeDialog: () -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onRequestReadPermission: () -> Unit,
    onRequestAllFilesAccess: () -> Unit,
    onPickSafFolder: () -> Unit,
    onRescanStorage: () -> Unit,
    onToggleLayoutMode: () -> Unit,
    onUpdateSettings: ((AppSettingsState) -> AppSettingsState) -> Unit,
    onRegenerateAllThumbnails: () -> Unit,
    onClearThumbnailCache: () -> Unit,
    onRemoveSafFolder: (String) -> Unit,
    onClearWatchHistory: () -> Unit,
    onClearFavorites: () -> Unit,
    onClearYouTubeLibrary: () -> Unit,
    onResetSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(SmartTubeBackground)
    ) {
        SmartTubeSidebar(
            selectedSection = uiState.selectedSection,
            usbDriveCount = uiState.usbDriveCount,
            youtubeCount = uiState.snapshot.youtubeVideos.size,
            isScanning = uiState.isScanning,
            isExpanded = false,
            sidebarMode = uiState.settings.sidebarMode,
            onSelectSection = onSelectSection,
            onAddYouTubeLink = onOpenAddYouTubeDialog,
            onAddSafFolder = onPickSafFolder,
            onRescanStorage = onRescanStorage
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 16.dp)
        ) {
            TvTopHeaderBar(
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                usbDriveCount = uiState.usbDriveCount,
                totalVideoCount = uiState.snapshot.allVideos.size,
                isScanning = uiState.isScanning,
                onOpenAddYouTube = onOpenAddYouTubeDialog,
                onClickUsbStatus = { onSelectSection(SidebarSection.USB_DRIVES) },
                onClickVideoCount = { onSelectSection(SidebarSection.HOME) },
                onToggleLayoutMode = onToggleLayoutMode,
                onOpenSettings = { onSelectSection(SidebarSection.SETTINGS) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.selectedSection == SidebarSection.SETTINGS) {
                SettingsScreen(
                    settings = uiState.settings,
                    safFolders = uiState.snapshot.safFolders,
                    savedPlaylists = uiState.snapshot.savedPlaylists,
                    totalVideoCount = uiState.snapshot.allVideos.size,
                    usbDriveCount = uiState.usbDriveCount,
                    isScanning = uiState.isScanning,
                    isGeneratingThumbnails = uiState.isGeneratingThumbnails,
                    thumbnailStatusMessage = uiState.thumbnailStatusMessage,
                    onUpdateSettings = onUpdateSettings,
                    onRegenerateAllThumbnails = onRegenerateAllThumbnails,
                    onClearThumbnailCache = onClearThumbnailCache,
                    onAddFolder = onPickSafFolder,
                    onRemoveSafFolder = onRemoveSafFolder,
                    onAddYouTubeLink = onOpenAddYouTubeDialog,
                    onDeletePlaylist = onDeletePlaylist,
                    onRescanStorage = onRescanStorage,
                    onClearWatchHistory = onClearWatchHistory,
                    onClearFavorites = onClearFavorites,
                    onClearYouTubeLibrary = onClearYouTubeLibrary,
                    onResetSettings = onResetSettings
                )
            } else {
                PermissionBanner(
                    hasReadVideoPermission = hasReadVideoPermission,
                    hasAllFilesAccess = hasAllFilesAccess,
                    onRequestReadPermission = onRequestReadPermission,
                    onRequestAllFilesAccess = onRequestAllFilesAccess,
                    onPickSafFolder = onPickSafFolder
                )

                if (uiState.snapshot.folderNames.isNotEmpty() &&
                    (uiState.selectedSection == SidebarSection.FOLDERS || uiState.selectedFolderFilter != null)
                ) {
                    Spacer(modifier = Modifier.height(10.dp))
                    FolderFilterChipRow(
                        folders = uiState.snapshot.folderNames,
                        selectedFolder = uiState.selectedFolderFilter,
                        onSelectFolder = onSelectFolderFilter
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (uiState.filteredRows.isEmpty()) {
                    EmptyCatalogState(
                        isScanning = uiState.isScanning,
                        searchQuery = uiState.searchQuery,
                        selectedSection = uiState.selectedSection,
                        onPickSafFolder = onPickSafFolder,
                        onOpenAddYouTube = onOpenAddYouTubeDialog,
                        onRescanStorage = onRescanStorage
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(22.dp),
                        contentPadding = PaddingValues(bottom = 36.dp)
                    ) {
                        items(
                            items = uiState.filteredRows,
                            key = { it.id }
                        ) { row ->
                            TvCategoryRowSection(
                                row = row,
                                settings = uiState.settings,
                                thumbnailRevision = uiState.thumbnailRevision,
                                onVideoClick = { video -> onVideoClick(video, row.videos) },
                                onToggleFavorite = onToggleFavorite,
                                onOpenVideoOptions = onOpenVideoOptions,
                                onDeletePlaylist = row.playlistId?.let { plId -> { onDeletePlaylist(plId) } }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TvTopHeaderBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    usbDriveCount: Int,
    totalVideoCount: Int,
    isScanning: Boolean,
    onOpenAddYouTube: () -> Unit,
    onClickUsbStatus: () -> Unit,
    onClickVideoCount: () -> Unit,
    onToggleLayoutMode: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var searchFocused by remember { mutableStateOf(false) }
    val searchFieldFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Search Input Pill + Clear Button (flexible width so right status + clock never clip)
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(19.dp))
                    .background(if (searchFocused) SmartTubeCardFocusedBg else SmartTubeCardBg)
                    .border(
                        width = if (searchFocused) 2.dp else 1.dp,
                        color = if (searchFocused) SmartTubeFocusBorder else Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(19.dp)
                    )
                    .onFocusChanged { searchFocused = it.isFocused || it.hasFocus }
                    .clickable {
                        runCatching { searchFieldFocusRequester.requestFocus() }
                        keyboardController?.show()
                    }
                    .padding(horizontal = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = if (searchFocused) SmartTubeRed else SmartTubeTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Search videos, folders, or YouTube...",
                            color = SmartTubeTextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 16.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        singleLine = true,
                        textStyle = TextStyle(
                            color = SmartTubeTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(SmartTubeRed),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(searchFieldFocusRequester)
                    )
                }
            }

            if (searchQuery.isNotEmpty()) {
                HeaderInteractivePill(
                    icon = Icons.Default.Clear,
                    text = "Clear",
                    onClick = { onSearchQueryChange("") }
                )
            }
        }

        // Right Status & Quick Controls
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isScanning) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(SmartTubeCardBg)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CircularProgressIndicator(
                        color = SmartTubeRed,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Scanning...",
                        color = SmartTubeTextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            // + YouTube Link Pill Button
            HeaderInteractivePill(
                icon = Icons.Default.AddLink,
                text = "+ YouTube",
                isPrimary = true,
                onClick = onOpenAddYouTube
            )

            // Clickable USB Status Pill
            HeaderInteractivePill(
                icon = Icons.Default.Usb,
                text = if (usbDriveCount > 0) "$usbDriveCount USB" else "No USB",
                accentColor = if (usbDriveCount > 0) SmartTubeUsbCyan else null,
                onClick = onClickUsbStatus
            )

            // Clickable Total Video Count Pill
            HeaderInteractivePill(
                icon = null,
                text = "$totalVideoCount Videos",
                onClick = onClickVideoCount
            )

            // Switch to Tablet UI button
            HeaderInteractivePill(
                icon = Icons.Default.GridView,
                text = "Tablet Mode",
                onClick = onToggleLayoutMode
            )

            // Settings button
            HeaderInteractivePill(
                icon = Icons.Default.Settings,
                text = "Settings",
                onClick = onOpenSettings
            )

            // Isolated Live Clock (does not recompose parent header)
            LiveClockText()
        }
    }
}

@Composable
fun HeaderInteractivePill(
    icon: ImageVector?,
    text: String,
    isPrimary: Boolean = false,
    accentColor: Color? = null,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val bgColor = when {
        isFocused && isPrimary -> SmartTubeRed
        isFocused -> SmartTubeCardFocusedBg
        isPrimary -> SmartTubeRed.copy(alpha = 0.88f)
        accentColor != null -> accentColor.copy(alpha = 0.14f)
        else -> SmartTubeCardBg
    }
    val borderColor = when {
        isFocused -> SmartTubeFocusBorder
        accentColor != null -> accentColor.copy(alpha = 0.5f)
        else -> Color.White.copy(alpha = 0.08f)
    }
    val textColor = when {
        isFocused || isPrimary -> Color.White
        accentColor != null -> accentColor
        else -> SmartTubeTextPrimary
    }

    Row(
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(bgColor)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(17.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .padding(horizontal = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = textColor,
                modifier = Modifier.size(14.dp)
            )
        }
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun LiveClockText() {
    var currentTime by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val formatter = SimpleDateFormat("EEE d MMM • HH:mm", Locale.getDefault())
        while (true) {
            currentTime = formatter.format(Date())
            delay(15_000L)
        }
    }
    Text(
        text = currentTime,
        color = SmartTubeTextPrimary,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        softWrap = false
    )
}

@Composable
fun FolderFilterChipRow(
    folders: List<String>,
    selectedFolder: String?,
    onSelectFolder: (String?) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        item {
            FilterChipItem(
                label = "All Folders",
                isSelected = selectedFolder == null,
                onClick = { onSelectFolder(null) }
            )
        }
        items(folders) { folder ->
            FilterChipItem(
                label = folder,
                isSelected = selectedFolder == folder,
                onClick = { onSelectFolder(folder) }
            )
        }
    }
}

@Composable
private fun FilterChipItem(
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

    Box(
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(bgColor)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) SmartTubeFocusBorder else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(17.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            lineHeight = 15.sp,
            fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
fun TvCategoryRowSection(
    row: CategoryRow,
    settings: AppSettingsState,
    thumbnailRevision: Int,
    onVideoClick: (VideoItem) -> Unit,
    onToggleFavorite: (VideoItem) -> Unit,
    onOpenVideoOptions: (VideoItem) -> Unit,
    onDeletePlaylist: (() -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (row.isYoutubeCategory) {
                    Icon(
                        imageVector = Icons.Default.OndemandVideo,
                        contentDescription = "YouTube",
                        tint = SmartTubeRed,
                        modifier = Modifier.size(19.dp)
                    )
                } else if (row.isUsbCategory) {
                    Icon(
                        imageVector = Icons.Default.Usb,
                        contentDescription = "USB Category",
                        tint = SmartTubeUsbCyan,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Text(
                    text = row.title,
                    color = SmartTubeTextPrimary,
                    fontSize = 17.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = row.subtitle,
                    color = when {
                        row.isYoutubeCategory -> SmartTubeRed
                        row.isUsbCategory -> SmartTubeUsbCyan
                        else -> SmartTubeTextSecondary
                    },
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false
                )
            }

            if (onDeletePlaylist != null) {
                HeaderInteractivePill(
                    icon = Icons.Default.Delete,
                    text = "Remove Playlist",
                    onClick = onDeletePlaylist
                )
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
        ) {
            items(
                items = row.videos,
                key = { "${row.id}_${it.id}" }
            ) { video ->
                VideoCard(
                    video = video,
                    onClick = { onVideoClick(video) },
                    onToggleFavorite = { onToggleFavorite(video) },
                    onOpenOptions = { onOpenVideoOptions(video) },
                    settings = settings,
                    thumbnailRevision = thumbnailRevision,
                    modifier = Modifier.width(260.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyCatalogState(
    isScanning: Boolean,
    searchQuery: String,
    selectedSection: SidebarSection,
    onPickSafFolder: () -> Unit,
    onOpenAddYouTube: () -> Unit = {},
    onRescanStorage: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(540.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SmartTubeCardBg)
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                .padding(30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = when (selectedSection) {
                    SidebarSection.USB_DRIVES -> Icons.Default.Usb
                    SidebarSection.YOUTUBE -> Icons.Default.OndemandVideo
                    else -> Icons.Default.VideoLibrary
                },
                contentDescription = null,
                tint = if (selectedSection == SidebarSection.USB_DRIVES) SmartTubeUsbCyan else SmartTubeRed,
                modifier = Modifier.size(52.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = when {
                    isScanning -> "Scanning Local Storage & USB Drives..."
                    searchQuery.isNotBlank() -> "No videos match \"$searchQuery\""
                    selectedSection == SidebarSection.YOUTUBE -> "No Saved YouTube Videos or Playlists Yet"
                    selectedSection == SidebarSection.USB_DRIVES -> "No USB Drive Videos Found"
                    selectedSection == SidebarSection.CONTINUE_WATCHING -> "No Playback History Yet"
                    selectedSection == SidebarSection.FAVORITES -> "No Favorite Videos Saved"
                    selectedSection == SidebarSection.FOUR_K -> "No 4K / UHD Videos Found"
                    else -> "No Local Videos Found Yet"
                },
                color = SmartTubeTextPrimary,
                fontSize = 19.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = when {
                    isScanning -> "Searching internal storage, mounted OTG USB drives, and sidecar subtitles (.srt, .vtt, .ass)..."
                    selectedSection == SidebarSection.YOUTUBE -> "Paste any YouTube video or playlist link to play videos in-app and save both playlists and individual videos for later."
                    selectedSection == SidebarSection.USB_DRIVES -> "Plug in a USB flash drive or external hard drive, or browse a USB directory manually."
                    else -> "Place video files (.mp4, .mkv, .webm, .avi) on your device/USB drive, add a folder, or paste a YouTube link."
                },
                color = SmartTubeTextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionPillButton(
                    icon = Icons.Default.AddLink,
                    text = "+ Add YouTube Link",
                    isPrimary = selectedSection == SidebarSection.YOUTUBE,
                    onClick = onOpenAddYouTube
                )
                ActionPillButton(
                    icon = Icons.Default.FolderOpen,
                    text = "Add Video Folder",
                    isPrimary = selectedSection != SidebarSection.YOUTUBE,
                    onClick = onPickSafFolder
                )
                ActionPillButton(
                    icon = Icons.Default.Refresh,
                    text = "Rescan USB",
                    isPrimary = false,
                    onClick = onRescanStorage
                )
            }
        }
    }
}
