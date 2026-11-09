package com.joudjawadtube.player.ui.catalog

import com.joudjawadtube.player.data.updater.UpdateInfo_Antigravity

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import com.joudjawadtube.player.R
import java.util.Locale
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joudjawadtube.player.data.local.VideoItem
import com.joudjawadtube.player.data.settings.AppSettingsState
import com.joudjawadtube.player.ui.components.CompactBottomNav_Antigravity
import com.joudjawadtube.player.ui.components.PermissionBanner
import com.joudjawadtube.player.ui.components.SidebarSection
import com.joudjawadtube.player.ui.components.SmartTubeSidebar
import com.joudjawadtube.player.ui.components.VideoCard
import com.joudjawadtube.player.ui.theme.SmartTubeBackground
import com.joudjawadtube.player.ui.theme.SmartTubeCardBg
import com.joudjawadtube.player.ui.theme.SmartTubeCardFocusedBg
import com.joudjawadtube.player.ui.theme.SmartTubeRed
import com.joudjawadtube.player.ui.theme.SmartTubeTextPrimary
import com.joudjawadtube.player.ui.theme.SmartTubeTextSecondary
import com.joudjawadtube.player.ui.theme.SmartTubeUsbCyan

@Composable
fun TabletCatalogScreen(
    uiState: CatalogUiState,
    lazyListState: LazyListState = rememberLazyListState(),
    lazyGridState: LazyGridState = rememberLazyGridState(),
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
    onToggleTabletGridMode: () -> Unit,
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
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(SmartTubeBackground)
    ) {
        val isCompactWidth = maxWidth < 600.dp
        val isWideTablet = maxWidth >= 960.dp
        var searchFocused by remember { mutableStateOf(false) }
        val searchFocusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current

        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (!isCompactWidth) {
                    SmartTubeSidebar(
                        selectedSection = uiState.selectedSection,
                        usbDriveCount = uiState.usbDriveCount,
                        channelsCount = uiState.snapshot.savedChannels.size,
                        playlistsCount = uiState.snapshot.savedPlaylists.size,
                        isScanning = uiState.isScanning,
                        isExpanded = isWideTablet,
                        sidebarMode = uiState.settings.sidebarMode,
                        onSelectSection = onSelectSection,
                        onAddYouTubeLink = onOpenAddYouTubeDialog,
                        onAddSafFolder = onPickSafFolder,
                        onRescanStorage = onRescanStorage
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .padding(horizontal = if (isCompactWidth) 12.dp else 20.dp, vertical = 14.dp)
                ) {
                    val categoryList = remember(uiState.snapshot.savedChannels, uiState.snapshot.savedPlaylists) {
                        val found = (uiState.snapshot.savedChannels.map { it.category } + uiState.snapshot.savedPlaylists.map { it.category }).filter { it.isNotBlank() }.toSet()
                        listOf("All") + (STANDARD_CATEGORIES_ANTIGRAVITY + found).distinct()
                    }

                    // Top Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Focus-Safe Search Input
                        Row(
                            modifier = Modifier
                                .weight(if (isCompactWidth) 1f else 0.45f)
                                .height(40.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            BasicTextField(
                                value = uiState.searchQuery,
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
                                    .fillMaxHeight()
                                    .focusRequester(searchFocusRequester)
                                    .onFocusChanged { searchFocused = it.isFocused },
                                decorationBox = { innerTextField ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(if (searchFocused) SmartTubeCardFocusedBg else SmartTubeCardBg)
                                            .border(
                                                width = if (searchFocused) 1.8.dp else 1.dp,
                                                color = if (searchFocused) SmartTubeRed else Color.White.copy(alpha = 0.08f),
                                                shape = RoundedCornerShape(20.dp)
                                            )
                                            .padding(horizontal = 14.dp),
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
                                            if (uiState.searchQuery.isEmpty()) {
                                                Text(
                                                    text = stringResource(R.string.search_placeholder),
                                                    color = SmartTubeTextSecondary,
                                                    fontSize = 13.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            innerTextField()
                                        }
                                    }
                                }
                            )

                            if (uiState.searchQuery.isNotEmpty()) {
                                HeaderInteractivePill(
                                    icon = Icons.Default.Clear,
                                    text = stringResource(R.string.search_clear),
                                    onClick = { onSearchQueryChange("") }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Category Navigation Pills (Replaces old top-bar buttons!)
                        Row(
                            modifier = Modifier
                                .weight(if (isCompactWidth) 0.5f else 0.55f)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            categoryList.forEach { cat ->
                                val isSelected = uiState.selectedTopCategory.equals(cat, ignoreCase = true)
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
                                    onClick = { onSelectTopCategory(cat) }
                                )
                            }

                            HeaderInteractivePill(
                                icon = if (uiState.tabletUseGridMode) Icons.Default.ViewAgenda else Icons.Default.GridView,
                                text = if (uiState.tabletUseGridMode) "Rows" else "Grid",
                                onClick = onToggleTabletGridMode
                            )
                        }
                    }

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

                        if (uiState.snapshot.folderNames.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            FolderFilterChipRow(
                                folders = uiState.snapshot.folderNames,
                                selectedFolder = uiState.selectedFolderFilter,
                                onSelectFolder = onSelectFolderFilter
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (uiState.filteredVideos.isEmpty()) {
                            EmptyCatalogState(
                                isScanning = uiState.isScanning,
                                searchQuery = uiState.searchQuery,
                                selectedSection = uiState.selectedSection,
                                onPickSafFolder = onPickSafFolder,
                                onOpenAddYouTube = onOpenAddYouTubeDialog,
                                onRescanStorage = onRescanStorage
                            )
                        } else if (uiState.tabletUseGridMode) {
                            LazyVerticalGrid(
                                state = lazyGridState,
                                columns = GridCells.Adaptive(minSize = if (isCompactWidth) 160.dp else 244.dp),
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(bottom = 32.dp)
                            ) {
                                items(
                                    items = uiState.filteredVideos,
                                    key = { it.id },
                                    contentType = { "video_card" }
                                ) { video ->
                                    VideoCard(
                                        video = video,
                                        onClick = { onVideoClick(video, uiState.filteredVideos) },
                                        onToggleFavorite = { onToggleFavorite(video) },
                                        onOpenOptions = { onOpenVideoOptions(video) },
                                        settings = uiState.settings,
                                        thumbnailRevision = uiState.thumbnailRevision,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                state = lazyListState,
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(22.dp),
                                contentPadding = PaddingValues(bottom = 32.dp)
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

            // Compact bottom navigation for phone / narrow portrait screens
            if (isCompactWidth) {
                CompactBottomNav_Antigravity(
                    selectedSection = uiState.selectedSection,
                    onSelectSection = onSelectSection
                )
            }
        }
    }
}
