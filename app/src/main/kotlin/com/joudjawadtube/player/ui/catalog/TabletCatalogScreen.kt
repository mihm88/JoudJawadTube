package com.joudjawadtube.player.ui.catalog

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
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.VideoLibrary
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
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
import com.joudjawadtube.player.ui.components.LocalWindowLayoutInfo
import com.joudjawadtube.player.ui.components.PermissionBanner
import com.joudjawadtube.player.ui.components.QuickChannelsPlaylistsDialog_Antigravity
import com.joudjawadtube.player.ui.components.QuickCollectionTarget_Antigravity
import com.joudjawadtube.player.ui.components.SearchIconBar_Antigravity
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
    onAction: (CatalogAction) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(SmartTubeBackground)
    ) {
        val layoutInfo = LocalWindowLayoutInfo.current
        val isCompactWidth = maxWidth < 600.dp
        val isPhone = layoutInfo.isPhone
        val isWideTablet = maxWidth >= 960.dp
        val focusManager = LocalFocusManager.current

        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (!isCompactWidth) {
                    SmartTubeSidebar(
                        selectedSection = uiState.selectedSection,
                        usbDriveCount = uiState.usbDriveCount,
                        channelsCount = uiState.snapshot.savedChannels.size,
                        playlistsCount = uiState.snapshot.savedPlaylists.size,
                        isScanning = uiState.isScanning,
                        isExpanded = false,
                        sidebarMode = uiState.settings.sidebarMode,
                        onSelectSection = { onAction(CatalogAction.SelectSection(it)) },
                        onAddYouTubeLink = { onAction(CatalogAction.OpenAddYouTubeDialog) },
                        onAddSafFolder = { onAction(CatalogAction.PickSafFolder) },
                        onRescanStorage = { onAction(CatalogAction.RescanStorage) }
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .padding(
                            horizontal = when {
                                isCompactWidth -> 12.dp
                                isPhone -> 14.dp
                                else -> 20.dp
                            },
                            vertical = if (isPhone) 10.dp else 14.dp
                        )
                ) {
                    val categoryList = remember(uiState.snapshot.savedChannels, uiState.snapshot.savedPlaylists) {
                        val found = (uiState.snapshot.savedChannels.map { it.category } + uiState.snapshot.savedPlaylists.map { it.category }).filter { it.isNotBlank() }.toSet()
                        listOf("All") + (STANDARD_CATEGORIES_ANTIGRAVITY + found).distinct()
                    }

                    @Composable
                    fun SearchInputField(modifier: Modifier = Modifier, expandedWidth: androidx.compose.ui.unit.Dp = 280.dp) {
                        SearchIconBar_Antigravity(
                            query = uiState.searchQuery,
                            onQueryChange = { onAction(CatalogAction.UpdateSearchQuery(it)) },
                            onSubmit = { focusManager.moveFocus(FocusDirection.Down) },
                            height = if (isCompactWidth) 38.dp else 40.dp,
                            expandedWidth = expandedWidth,
                            modifier = modifier
                        )
                    }

                    @Composable
                    fun CategoryChipsRow(modifier: Modifier = Modifier, includeGridToggle: Boolean = true) {
                        Row(
                            modifier = modifier.horizontalScroll(rememberScrollState()),
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
                                    onClick = { onAction(CatalogAction.SelectTopCategory(cat)) }
                                )
                            }

                            if (includeGridToggle) {
                                HeaderInteractivePill(
                                    icon = if (uiState.tabletUseGridMode) Icons.Default.ViewAgenda else Icons.Default.GridView,
                                    text = stringResource(if (uiState.tabletUseGridMode) R.string.action_view_rows else R.string.action_view_grid),
                                    onClick = { onAction(CatalogAction.ToggleTabletGridMode) }
                                )
                            }
                        }
                    }

                    var showQuickChannelsPlaylistsDialog by remember { mutableStateOf(false) }
                    var quickCollectionTarget by remember { mutableStateOf<QuickCollectionTarget_Antigravity?>(null) }

                    // Top Bar: stacked 2-row compact layout on portrait phones, single-row on landscape/tablet
                    if (isCompactWidth) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    SearchInputField(
                                        expandedWidth = androidx.compose.ui.unit.Dp.Unspecified
                                    )
                                }

                                HeaderInteractivePill(
                                    icon = Icons.Default.VideoLibrary,
                                    text = stringResource(R.string.quick_channels_playlists_btn),
                                    onClick = {
                                        quickCollectionTarget = null
                                        showQuickChannelsPlaylistsDialog = true
                                    }
                                )

                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(SmartTubeCardBg)
                                        .border(1.dp, SmartTubeTextSecondary.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                                        .clickable { onAction(CatalogAction.OpenAddYouTubeDialog) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddLink,
                                        contentDescription = stringResource(R.string.action_add_youtube),
                                        tint = SmartTubeRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(SmartTubeCardBg)
                                        .border(1.dp, SmartTubeTextSecondary.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                                        .clickable { onAction(CatalogAction.PickSafFolder) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CreateNewFolder,
                                        contentDescription = stringResource(R.string.action_add_folder),
                                        tint = SmartTubeUsbCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                HeaderInteractivePill(
                                    icon = if (uiState.tabletUseGridMode) Icons.Default.ViewAgenda else Icons.Default.GridView,
                                    text = stringResource(if (uiState.tabletUseGridMode) R.string.action_view_rows else R.string.action_view_grid),
                                    onClick = { onAction(CatalogAction.ToggleTabletGridMode) }
                                )
                                CategoryChipsRow(
                                    modifier = Modifier.weight(1f),
                                    includeGridToggle = false
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SearchInputField(expandedWidth = 280.dp)
                            HeaderInteractivePill(
                                icon = Icons.Default.VideoLibrary,
                                text = stringResource(R.string.quick_channels_playlists_btn),
                                onClick = {
                                    quickCollectionTarget = null
                                    showQuickChannelsPlaylistsDialog = true
                                }
                            )
                            CategoryChipsRow(modifier = Modifier.weight(1f), includeGridToggle = true)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (!uiState.isOnline) {
                        OfflineStatusBanner_Antigravity()
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    if (uiState.searchQuery.isNotBlank()) {
                        InAppSearchResultsView_Antigravity(
                            query = uiState.searchQuery,
                            matchingChannels = uiState.matchingChannels,
                            matchingPlaylists = uiState.matchingPlaylists,
                            matchingVideos = uiState.filteredVideos,
                            onOpenChannelVideos = { _, videos ->
                                if (videos.isNotEmpty()) onAction(CatalogAction.OpenVideo(videos.first(), videos))
                            },
                            onOpenPlaylistVideos = { _, videos ->
                                if (videos.isNotEmpty()) onAction(CatalogAction.OpenVideo(videos.first(), videos))
                            },
                            onVideoClick = { video, playlist -> onAction(CatalogAction.OpenVideo(video, playlist)) },
                            onToggleFavorite = { onAction(CatalogAction.ToggleFavorite(it)) },
                            onOpenVideoOptions = { onAction(CatalogAction.OpenVideoOptions(it)) },
                            onClearSearch = { onAction(CatalogAction.UpdateSearchQuery("")) }
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
                            onUpdateSettings = { onAction(CatalogAction.UpdateSettings(it)) },
                            onRegenerateAllThumbnails = { onAction(CatalogAction.RegenerateAllThumbnails) },
                            onClearThumbnailCache = { onAction(CatalogAction.ClearThumbnailCache) },
                            onAddFolder = { onAction(CatalogAction.PickSafFolder) },
                            onRemoveSafFolder = { onAction(CatalogAction.RemoveSafFolder(it)) },
                            onAddYouTubeLink = { onAction(CatalogAction.OpenAddYouTubeDialog) },
                            onDeletePlaylist = { onAction(CatalogAction.DeletePlaylist(it)) },
                            onDeleteChannel = { onAction(CatalogAction.DeleteChannel(it)) },
                            onRescanStorage = { onAction(CatalogAction.RescanStorage) },
                            onClearWatchHistory = { onAction(CatalogAction.ClearWatchHistory) },
                            onClearFavorites = { onAction(CatalogAction.ClearFavorites) },
                            onClearYouTubeLibrary = { onAction(CatalogAction.ClearYouTubeLibrary) },
                            onResetSettings = { onAction(CatalogAction.ResetSettings) },
                            updateState = uiState.updateState,
                            onCheckForUpdates = { onAction(CatalogAction.CheckForUpdates) },
                            onDownloadAndInstallUpdate = { onAction(CatalogAction.DownloadAndInstallUpdate(it)) },
                            onInstallDownloadedApk = { onAction(CatalogAction.InstallDownloadedApk(it)) },
                            onDismissUpdateStatus = { onAction(CatalogAction.DismissUpdateStatus) }
                        )
                    } else if (uiState.selectedSection == SidebarSection.KIDS_PICKS) {
                        KidsPicksScreen_Antigravity(
                            channels = uiState.kidsChannels,
                            savedPlaylistIds = uiState.snapshot.savedPlaylists.map { it.playlistId }.toSet(),
                            savedChannelIds = uiState.snapshot.savedChannels.map { it.channelId }.toSet(),
                            appLanguage = uiState.settings.appLanguage,
                            onOpenPlaylist = { plId, _ -> onAction(CatalogAction.OpenKidsPlaylist(plId, playImmediately = true)) },
                            onSavePlaylistToLibrary = { plId -> onAction(CatalogAction.OpenKidsPlaylist(plId, playImmediately = false)) },
                            onAddChannelToLibrary = { onAction(CatalogAction.AddBuiltInChannel(it)) },
                            onRemoveChannelFromLibrary = { onAction(CatalogAction.DeleteChannel(it)) }
                        )
                    } else if (uiState.selectedSection == SidebarSection.CHANNELS) {
                        ChannelsScreen_Antigravity(
                            channels = uiState.snapshot.savedChannels,
                            allVideos = uiState.snapshot.youtubeVideos,
                            onOpenChannelVideos = { _, videos ->
                                if (videos.isNotEmpty()) onAction(CatalogAction.OpenVideo(videos.first(), videos))
                            },
                            onVideoClick = { video, playlist -> onAction(CatalogAction.OpenVideo(video, playlist)) },
                            onToggleFavorite = { onAction(CatalogAction.ToggleFavorite(it)) },
                            onOpenVideoOptions = { onAction(CatalogAction.OpenVideoOptions(it)) },
                            onChangeChannelCategory = { channelId, cat -> onAction(CatalogAction.ChangeChannelCategory(channelId, cat)) },
                            onDeleteChannel = { onAction(CatalogAction.DeleteChannel(it)) },
                            onAddChannelClick = { onAction(CatalogAction.OpenAddYouTubeDialog) }
                        )
                    } else if (uiState.selectedSection == SidebarSection.PLAYLISTS) {
                        PlaylistsScreen_Antigravity(
                            playlists = uiState.snapshot.savedPlaylists,
                            allVideos = uiState.snapshot.youtubeVideos,
                            onOpenPlaylistVideos = { _, videos ->
                                if (videos.isNotEmpty()) onAction(CatalogAction.OpenVideo(videos.first(), videos))
                            },
                            onVideoClick = { video, playlist -> onAction(CatalogAction.OpenVideo(video, playlist)) },
                            onToggleFavorite = { onAction(CatalogAction.ToggleFavorite(it)) },
                            onOpenVideoOptions = { onAction(CatalogAction.OpenVideoOptions(it)) },
                            onChangePlaylistCategory = { playlistId, cat -> onAction(CatalogAction.ChangePlaylistCategory(playlistId, cat)) },
                            onDeletePlaylist = { onAction(CatalogAction.DeletePlaylist(it)) },
                            onAddPlaylistClick = { onAction(CatalogAction.OpenAddYouTubeDialog) }
                        )
                    } else if (uiState.selectedSection == SidebarSection.CATEGORIES) {
                        CategoriesScreen_Antigravity(
                            channels = uiState.snapshot.savedChannels,
                            playlists = uiState.snapshot.savedPlaylists,
                            allVideos = uiState.snapshot.youtubeVideos,
                            onOpenChannelVideos = { _, videos ->
                                if (videos.isNotEmpty()) onAction(CatalogAction.OpenVideo(videos.first(), videos))
                            },
                            onOpenPlaylistVideos = { _, videos ->
                                if (videos.isNotEmpty()) onAction(CatalogAction.OpenVideo(videos.first(), videos))
                            },
                            onVideoClick = { video, playlist -> onAction(CatalogAction.OpenVideo(video, playlist)) },
                            onToggleFavorite = { onAction(CatalogAction.ToggleFavorite(it)) },
                            onOpenVideoOptions = { onAction(CatalogAction.OpenVideoOptions(it)) },
                            onChangeChannelCategory = { channelId, cat -> onAction(CatalogAction.ChangeChannelCategory(channelId, cat)) },
                            onChangePlaylistCategory = { playlistId, cat -> onAction(CatalogAction.ChangePlaylistCategory(playlistId, cat)) }
                        )
                    } else {
                        PermissionBanner(
                            hasReadVideoPermission = hasReadVideoPermission,
                            hasAllFilesAccess = hasAllFilesAccess,
                            onRequestReadPermission = { onAction(CatalogAction.RequestReadPermission) },
                            onRequestAllFilesAccess = { onAction(CatalogAction.RequestAllFilesAccess) },
                            onPickSafFolder = { onAction(CatalogAction.PickSafFolder) }
                        )

                        if (uiState.snapshot.folderNames.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            FolderFilterChipRow(
                                folders = uiState.snapshot.folderNames,
                                selectedFolder = uiState.selectedFolderFilter,
                                onSelectFolder = { onAction(CatalogAction.SelectFolderFilter(it)) }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (uiState.filteredVideos.isEmpty()) {
                            EmptyCatalogState(
                                isScanning = uiState.isScanning,
                                searchQuery = uiState.searchQuery,
                                selectedSection = uiState.selectedSection,
                                onPickSafFolder = { onAction(CatalogAction.PickSafFolder) },
                                onOpenAddYouTube = { onAction(CatalogAction.OpenAddYouTubeDialog) },
                                onRescanStorage = { onAction(CatalogAction.RescanStorage) }
                            )
                        } else if (uiState.tabletUseGridMode) {
                            LazyVerticalGrid(
                                state = lazyGridState,
                                columns = GridCells.Adaptive(
                                    minSize = when {
                                        isCompactWidth -> 156.dp
                                        isPhone -> 184.dp
                                        else -> 244.dp
                                    }
                                ),
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.spacedBy(if (isCompactWidth) 10.dp else 14.dp),
                                verticalArrangement = Arrangement.spacedBy(if (isCompactWidth) 12.dp else 16.dp),
                                contentPadding = PaddingValues(bottom = 32.dp)
                            ) {
                                items(
                                    items = uiState.filteredVideos,
                                    key = { it.id },
                                    contentType = { "video_card" }
                                ) { video ->
                                    VideoCard(
                                        video = video,
                                        onClick = { onAction(CatalogAction.OpenVideo(video, uiState.filteredVideos)) },
                                        onToggleFavorite = { onAction(CatalogAction.ToggleFavorite(video)) },
                                        onOpenOptions = { onAction(CatalogAction.OpenVideoOptions(video)) },
                                        settings = uiState.settings,
                                        thumbnailRevision = uiState.thumbnailRevision,
                                        onRequestThumbnail = { onAction(CatalogAction.ResolveVideoThumbnail(video)) },
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
                                        onVideoClick = { video -> onAction(CatalogAction.OpenVideo(video, row.videos)) },
                                        onToggleFavorite = { onAction(CatalogAction.ToggleFavorite(it)) },
                                        onOpenVideoOptions = { onAction(CatalogAction.OpenVideoOptions(it)) },
                                        onRequestThumbnail = { video -> onAction(CatalogAction.ResolveVideoThumbnail(video)) },
                                        onPlaylistClick = {
                                            quickCollectionTarget = QuickCollectionTarget_Antigravity(
                                                title = row.title,
                                                subtitle = row.subtitle,
                                                videos = row.videos
                                            )
                                            showQuickChannelsPlaylistsDialog = true
                                        },
                                        onChannelClick = {
                                            quickCollectionTarget = QuickCollectionTarget_Antigravity(
                                                title = row.title,
                                                subtitle = row.subtitle,
                                                videos = row.videos
                                            )
                                            showQuickChannelsPlaylistsDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (showQuickChannelsPlaylistsDialog) {
                        QuickChannelsPlaylistsDialog_Antigravity(
                            savedChannels = uiState.snapshot.savedChannels,
                            savedPlaylists = uiState.snapshot.savedPlaylists,
                            allVideos = uiState.snapshot.allVideos,
                            initialTarget = quickCollectionTarget,
                            onPlayVideo = { video, playlist -> onAction(CatalogAction.OpenVideo(video, playlist)) },
                            onAddYouTubeClick = { onAction(CatalogAction.OpenAddYouTubeDialog) },
                            onDismiss = {
                                showQuickChannelsPlaylistsDialog = false
                                quickCollectionTarget = null
                            }
                        )
                    }
                }
            }

            // Compact bottom navigation for phone / narrow portrait screens
            if (isCompactWidth) {
                CompactBottomNav_Antigravity(
                    selectedSection = uiState.selectedSection,
                    channelsCount = uiState.snapshot.savedChannels.size,
                    playlistsCount = uiState.snapshot.savedPlaylists.size,
                    usbDriveCount = uiState.usbDriveCount,
                    onSelectSection = { onAction(CatalogAction.SelectSection(it)) }
                )
            }
        }
    }
}
