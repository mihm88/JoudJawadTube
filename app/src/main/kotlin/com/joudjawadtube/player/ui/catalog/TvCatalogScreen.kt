package com.joudjawadtube.player.ui.catalog

import com.joudjawadtube.player.data.updater.UpdateInfo_Antigravity

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import com.joudjawadtube.player.R
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
import com.joudjawadtube.player.data.local.SavedChannelEntity
import com.joudjawadtube.player.data.local.SavedPlaylistEntity
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
    lazyListState: LazyListState = rememberLazyListState(),
    hasReadVideoPermission: Boolean,
    hasAllFilesAccess: Boolean,
    onSelectSection: (SidebarSection) -> Unit,
    onSelectTopCategory: (String) -> Unit = {},
    onSearchQueryChange: (String) -> Unit,
    onSelectFolderFilter: (String?) -> Unit,
    onVideoClick: (VideoItem, List<VideoItem>) -> Unit,
    onToggleFavorite: (VideoItem) -> Unit,
    onOpenVideoOptions: (VideoItem) -> Unit,
    onOpenAddYouTubeDialog: () -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onDeleteChannel: (String) -> Unit = {},
    onChangeChannelCategory: (channelId: String, newCategory: String) -> Unit = { _, _ -> },
    onChangePlaylistCategory: (playlistId: String, newCategory: String) -> Unit = { _, _ -> },
    onOpenKidsPlaylist: (String, String) -> Unit = { _, _ -> },
    onSaveKidsPlaylist: (String) -> Unit = {},
    onRequestReadPermission: () -> Unit,
    onRequestAllFilesAccess: () -> Unit,
    onPickSafFolder: () -> Unit,
    onRescanStorage: () -> Unit,
    onUpdateSettings: ((AppSettingsState) -> AppSettingsState) -> Unit,
    onRegenerateAllThumbnails: () -> Unit,
    onClearThumbnailCache: () -> Unit,
    onRemoveSafFolder: (String) -> Unit,
    onClearWatchHistory: () -> Unit,
    onClearFavorites: () -> Unit,
    onClearYouTubeLibrary: () -> Unit,
    onResetSettings: () -> Unit,
    onCheckForUpdates: () -> Unit = {},
    onDownloadAndInstallUpdate: (UpdateInfo_Antigravity) -> Unit = {},
    onDismissUpdateStatus: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(SmartTubeBackground)
    ) {
        SmartTubeSidebar(
            selectedSection = uiState.selectedSection,
            usbDriveCount = uiState.usbDriveCount,
            channelsCount = uiState.snapshot.savedChannels.size,
            playlistsCount = uiState.snapshot.savedPlaylists.size,
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
            val categoryList = remember(uiState.snapshot.savedChannels, uiState.snapshot.savedPlaylists) {
                val found = (uiState.snapshot.savedChannels.map { it.category } + uiState.snapshot.savedPlaylists.map { it.category }).filter { it.isNotBlank() }.toSet()
                listOf("All") + (STANDARD_CATEGORIES_ANTIGRAVITY + found).distinct()
            }

            TvTopHeaderBar(
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                categories = categoryList,
                selectedCategory = uiState.selectedTopCategory,
                onSelectCategory = onSelectTopCategory,
                isScanning = uiState.isScanning
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.searchQuery.isNotBlank()) {
                InAppSearchResultsView_Antigravity(
                    query = uiState.searchQuery,
                    matchingChannels = uiState.matchingChannels,
                    matchingPlaylists = uiState.matchingPlaylists,
                    matchingVideos = uiState.filteredVideos,
                    onOpenChannelVideos = { channel, videos ->
                        if (videos.isNotEmpty()) onVideoClick(videos.first(), videos)
                    },
                    onOpenPlaylistVideos = { playlist, videos ->
                        if (videos.isNotEmpty()) onVideoClick(videos.first(), videos)
                    },
                    onVideoClick = onVideoClick,
                    onToggleFavorite = onToggleFavorite,
                    onOpenVideoOptions = onOpenVideoOptions,
                    onClearSearch = { onSearchQueryChange("") }
                )
            } else if (uiState.selectedSection == SidebarSection.SETTINGS) {
                SettingsScreen(
                    settings = uiState.settings,
                    safFolders = uiState.snapshot.safFolders,
                    savedPlaylists = uiState.snapshot.savedPlaylists,
                    savedChannels = uiState.snapshot.savedChannels,
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
                    onDeleteChannel = onDeleteChannel,
                    onRescanStorage = onRescanStorage,
                    onClearWatchHistory = onClearWatchHistory,
                    onClearFavorites = onClearFavorites,
                    onClearYouTubeLibrary = onClearYouTubeLibrary,
                    onResetSettings = onResetSettings,
                    updateState = uiState.updateState,
                    onCheckForUpdates = onCheckForUpdates,
                    onDownloadAndInstallUpdate = onDownloadAndInstallUpdate,
                    onDismissUpdateStatus = onDismissUpdateStatus
                )
            } else if (uiState.selectedSection == SidebarSection.KIDS_PICKS) {
                KidsPicksScreen_Antigravity(
                    savedPlaylistIds = uiState.snapshot.savedPlaylists.map { it.playlistId }.toSet(),
                    onOpenPlaylist = onOpenKidsPlaylist,
                    onSavePlaylistToLibrary = onSaveKidsPlaylist
                )
            } else if (uiState.selectedSection == SidebarSection.CHANNELS) {
                ChannelsScreen_Antigravity(
                    channels = uiState.snapshot.savedChannels,
                    allVideos = uiState.snapshot.youtubeVideos,
                    onOpenChannelVideos = { channel, videos ->
                        if (videos.isNotEmpty()) onVideoClick(videos.first(), videos)
                    },
                    onVideoClick = onVideoClick,
                    onToggleFavorite = onToggleFavorite,
                    onOpenVideoOptions = onOpenVideoOptions,
                    onChangeChannelCategory = onChangeChannelCategory,
                    onDeleteChannel = onDeleteChannel,
                    onAddChannelClick = onOpenAddYouTubeDialog
                )
            } else if (uiState.selectedSection == SidebarSection.PLAYLISTS) {
                PlaylistsScreen_Antigravity(
                    playlists = uiState.snapshot.savedPlaylists,
                    allVideos = uiState.snapshot.youtubeVideos,
                    onOpenPlaylistVideos = { playlist, videos ->
                        if (videos.isNotEmpty()) onVideoClick(videos.first(), videos)
                    },
                    onVideoClick = onVideoClick,
                    onToggleFavorite = onToggleFavorite,
                    onOpenVideoOptions = onOpenVideoOptions,
                    onChangePlaylistCategory = onChangePlaylistCategory,
                    onDeletePlaylist = onDeletePlaylist,
                    onAddPlaylistClick = onOpenAddYouTubeDialog
                )
            } else if (uiState.selectedSection == SidebarSection.CATEGORIES) {
                CategoriesScreen_Antigravity(
                    channels = uiState.snapshot.savedChannels,
                    playlists = uiState.snapshot.savedPlaylists,
                    allVideos = uiState.snapshot.youtubeVideos,
                    onOpenChannelVideos = { channel, videos ->
                        if (videos.isNotEmpty()) onVideoClick(videos.first(), videos)
                    },
                    onOpenPlaylistVideos = { playlist, videos ->
                        if (videos.isNotEmpty()) onVideoClick(videos.first(), videos)
                    },
                    onVideoClick = onVideoClick,
                    onToggleFavorite = onToggleFavorite,
                    onOpenVideoOptions = onOpenVideoOptions,
                    onChangeChannelCategory = onChangeChannelCategory,
                    onChangePlaylistCategory = onChangePlaylistCategory
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
                        state = lazyListState,
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
                                onPlaylistClick = { onSelectSection(SidebarSection.PLAYLISTS) },
                                onChannelClick = { onSelectSection(SidebarSection.CHANNELS) }
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
    categories: List<String>,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    isScanning: Boolean
) {
    var searchFocused by remember { mutableStateOf(false) }
    val searchFieldFocusRequester = remember { FocusRequester() }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: Clean Focus-Safe Search Input Field
        Row(
            modifier = Modifier.width(300.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
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
                    .weight(1f)
                    .height(38.dp)
                    .focusRequester(searchFieldFocusRequester)
                    .onFocusChanged { searchFocused = it.isFocused || it.hasFocus },
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(19.dp))
                            .background(if (searchFocused) SmartTubeCardFocusedBg else SmartTubeCardBg)
                            .border(
                                width = if (searchFocused) 2.dp else 1.dp,
                                color = if (searchFocused) SmartTubeFocusBorder else Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(19.dp)
                            )
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
                                    text = stringResource(R.string.search_placeholder),
                                    color = SmartTubeTextSecondary,
                                    fontSize = 13.sp,
                                    lineHeight = 16.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            innerTextField()
                        }
                    }
                }
            )

            if (searchQuery.isNotEmpty()) {
                HeaderInteractivePill(
                    icon = Icons.Default.Clear,
                    text = stringResource(R.string.search_clear),
                    onClick = { onSearchQueryChange("") }
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Center / Right: Category Navigation Bar (Replaces old top bar buttons)
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            categories.forEach { cat ->
                val isSelected = selectedCategory.equals(cat, ignoreCase = true)
                val catDisplay = when (cat.lowercase(Locale.ROOT)) {
                    "all" -> stringResource(R.string.category_all)
                    "islamic kids" -> stringResource(R.string.category_islamic_kids)
                    "english learning" -> stringResource(R.string.category_english_learning)
                    "stories & rhymes" -> stringResource(R.string.category_stories_rhymes)
                    "cartoons & fun" -> stringResource(R.string.category_cartoons_fun)
                    "stem", "stem & education" -> stringResource(R.string.category_stem_science)
                    else -> cat
                }
                FilterChipItem(
                    label = catDisplay,
                    isSelected = isSelected,
                    onClick = { onSelectCategory(cat) }
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Right Status: Scanning Indicator + Live Clock
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
                        maxLines = 1
                    )
                }
            }

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
fun FilterChipItem(
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
    onPlaylistClick: (String) -> Unit = {},
    onChannelClick: (String) -> Unit = {}
) {
    val isClickable = !row.playlistId.isNullOrBlank() || !row.channelId.isNullOrBlank()
    var isHeaderFocused by remember { mutableStateOf(false) }

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
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isHeaderFocused) SmartTubeCardFocusedBg else Color.Transparent)
                    .border(
                        width = if (isHeaderFocused) 1.5.dp else 0.dp,
                        color = if (isHeaderFocused) SmartTubeFocusBorder else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable(enabled = isClickable) {
                        if (!row.playlistId.isNullOrBlank()) {
                            onPlaylistClick(row.playlistId)
                        } else if (!row.channelId.isNullOrBlank()) {
                            onChannelClick(row.channelId)
                        }
                    }
                    .onFocusChanged { isHeaderFocused = it.isFocused || it.hasFocus }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                if (row.isYoutubeCategory) {
                    Icon(
                        imageVector = if (!row.playlistId.isNullOrBlank()) Icons.Default.PlaylistPlay else Icons.Default.VideoLibrary,
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
                    color = if (isHeaderFocused) Color.White else SmartTubeTextPrimary,
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
                if (isClickable) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open",
                        tint = if (isHeaderFocused) SmartTubeRed else SmartTubeTextSecondary.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
        ) {
            items(
                items = row.videos,
                key = { "${row.id}_${it.id}" },
                contentType = { "video_card" }
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
                .fillMaxWidth(0.92f)
                .widthIn(max = 540.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SmartTubeCardBg)
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                .padding(30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = when (selectedSection) {
                    SidebarSection.USB_DRIVES -> Icons.Default.Usb
                    SidebarSection.CHANNELS, SidebarSection.PLAYLISTS, SidebarSection.CATEGORIES -> Icons.Default.OndemandVideo
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
                    selectedSection in setOf(SidebarSection.CHANNELS, SidebarSection.PLAYLISTS, SidebarSection.CATEGORIES) -> "No Saved Content in This Section Yet"
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
                    selectedSection in setOf(SidebarSection.CHANNELS, SidebarSection.PLAYLISTS, SidebarSection.CATEGORIES) -> "Paste any channel, playlist or video link to save it and enjoy in-app playback."
                    selectedSection == SidebarSection.USB_DRIVES -> "Plug in a USB flash drive or external hard drive, or browse a USB directory manually."
                    else -> "Place video files (.mp4, .mkv, .webm, .avi) on your device/USB drive, add a folder, or paste a video link."
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
                    isPrimary = selectedSection in setOf(SidebarSection.CHANNELS, SidebarSection.PLAYLISTS, SidebarSection.CATEGORIES),
                    onClick = onOpenAddYouTube
                )
                ActionPillButton(
                    icon = Icons.Default.FolderOpen,
                    text = "Add Video Folder",
                    isPrimary = selectedSection !in setOf(SidebarSection.CHANNELS, SidebarSection.PLAYLISTS, SidebarSection.CATEGORIES),
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
