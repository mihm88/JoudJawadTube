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
import androidx.compose.material.icons.filled.WifiOff
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
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
import com.joudjawadtube.player.ui.components.LocalWindowLayoutInfo
import com.joudjawadtube.player.ui.components.PermissionBanner
import com.joudjawadtube.player.ui.components.QuickChannelsPlaylistsDialog_Antigravity
import com.joudjawadtube.player.ui.components.QuickCollectionTarget_Antigravity
import com.joudjawadtube.player.ui.components.SearchIconBar_Antigravity
import com.joudjawadtube.player.ui.components.SidebarSection
import com.joudjawadtube.player.ui.components.SmartTubeSidebar
import com.joudjawadtube.player.ui.components.VideoCard
import com.joudjawadtube.player.ui.theme.LocalJoudColors
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
    onAction: (CatalogAction) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (LocalJoudColors.current.isLiquidGlass) Color.Transparent else SmartTubeBackground
            )
    ) {
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
            var showQuickChannelsPlaylistsDialog by remember { mutableStateOf(false) }
            var quickCollectionTarget by remember { mutableStateOf<QuickCollectionTarget_Antigravity?>(null) }

            TvTopHeaderBar(
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = { onAction(CatalogAction.UpdateSearchQuery(it)) },
                categories = categoryList,
                selectedCategory = uiState.selectedTopCategory,
                onSelectCategory = { onAction(CatalogAction.SelectTopCategory(it)) },
                onOpenQuickChannelsPlaylists = {
                    quickCollectionTarget = null
                    showQuickChannelsPlaylistsDialog = true
                },
                isScanning = uiState.isScanning
            )

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

                if (uiState.snapshot.folderNames.isNotEmpty() &&
                    (uiState.selectedSection == SidebarSection.FOLDERS || uiState.selectedFolderFilter != null)
                ) {
                    Spacer(modifier = Modifier.height(10.dp))
                    FolderFilterChipRow(
                        folders = uiState.snapshot.folderNames,
                        selectedFolder = uiState.selectedFolderFilter,
                        onSelectFolder = { onAction(CatalogAction.SelectFolderFilter(it)) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (uiState.filteredRows.isEmpty()) {
                    EmptyCatalogState(
                        isScanning = uiState.isScanning,
                        searchQuery = uiState.searchQuery,
                        selectedSection = uiState.selectedSection,
                        onPickSafFolder = { onAction(CatalogAction.PickSafFolder) },
                        onOpenAddYouTube = { onAction(CatalogAction.OpenAddYouTubeDialog) },
                        onRescanStorage = { onAction(CatalogAction.RescanStorage) }
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
}

@Composable
private fun TvTopHeaderBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    categories: List<String>,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    onOpenQuickChannelsPlaylists: () -> Unit,
    isScanning: Boolean
) {
    val focusManager = LocalFocusManager.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: Collapsed Search Icon + Quick Channels & Playlists Picker Button
        SearchIconBar_Antigravity(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            onSubmit = { focusManager.moveFocus(FocusDirection.Down) },
            height = 38.dp,
            expandedWidth = 300.dp
        )

        Spacer(modifier = Modifier.width(8.dp))

        HeaderInteractivePill(
            icon = Icons.Default.VideoLibrary,
            text = stringResource(R.string.quick_channels_playlists_btn),
            onClick = onOpenQuickChannelsPlaylists
        )

        Spacer(modifier = Modifier.width(12.dp))

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
                        text = stringResource(R.string.status_scanning),
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
        else -> SmartTubeTextSecondary.copy(alpha = 0.2f)
    }
    val textColor = when {
        isPrimary -> Color.White
        isFocused -> SmartTubeTextPrimary
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
                label = stringResource(R.string.filter_all_folders),
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
                color = if (isFocused) SmartTubeFocusBorder else SmartTubeTextSecondary.copy(alpha = 0.2f),
                shape = RoundedCornerShape(17.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else SmartTubeTextPrimary,
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
    onRequestThumbnail: (VideoItem) -> Unit = {},
    onPlaylistClick: (String) -> Unit = {},
    onChannelClick: (String) -> Unit = {}
) {
    val isClickable = !row.playlistId.isNullOrBlank() || !row.channelId.isNullOrBlank()
    var isHeaderFocused by remember { mutableStateOf(false) }

    val (displayTitle, displaySubtitle) = when (row.id) {
        "continue_watching", "section_history" ->
            stringResource(R.string.row_continue_watching_title) to
                stringResource(R.string.row_continue_watching_sub, row.videos.size)
        "recommended_for_you" ->
            stringResource(R.string.row_recommended_title) to
                stringResource(R.string.row_recommended_sub, row.videos.size)
        "yt_saved_videos" ->
            stringResource(R.string.row_saved_youtube_title) to
                stringResource(R.string.row_saved_youtube_sub, row.videos.size)
        "favorites", "section_favorites" ->
            stringResource(R.string.row_favorites_title) to
                stringResource(R.string.row_favorites_sub, row.videos.size)
        "section_4k" ->
            stringResource(R.string.row_4k_title) to
                stringResource(R.string.row_4k_sub, row.videos.size)
        else -> row.title to row.subtitle
    }

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
                    text = displayTitle,
                    color = SmartTubeTextPrimary,
                    fontSize = 17.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = displaySubtitle,
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
                    onRequestThumbnail = { onRequestThumbnail(video) },
                    modifier = Modifier.width(if (LocalWindowLayoutInfo.current.isPhone) 200.dp else 260.dp)
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
                .border(1.dp, SmartTubeTextSecondary.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
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
                    isScanning -> stringResource(R.string.empty_scanning_title)
                    searchQuery.isNotBlank() -> stringResource(R.string.empty_search_title, searchQuery)
                    selectedSection in setOf(SidebarSection.CHANNELS, SidebarSection.PLAYLISTS, SidebarSection.CATEGORIES) -> stringResource(R.string.empty_saved_section_title)
                    selectedSection == SidebarSection.USB_DRIVES -> stringResource(R.string.empty_usb_videos_title)
                    selectedSection == SidebarSection.CONTINUE_WATCHING -> stringResource(R.string.empty_history_title)
                    selectedSection == SidebarSection.FAVORITES -> stringResource(R.string.empty_favorites_title)
                    selectedSection == SidebarSection.FOUR_K -> stringResource(R.string.empty_four_k_title)
                    else -> stringResource(R.string.empty_local_title)
                },
                color = SmartTubeTextPrimary,
                fontSize = 19.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = when {
                    isScanning -> stringResource(R.string.empty_scanning_desc)
                    selectedSection in setOf(SidebarSection.CHANNELS, SidebarSection.PLAYLISTS, SidebarSection.CATEGORIES) -> stringResource(R.string.empty_saved_section_desc)
                    selectedSection == SidebarSection.USB_DRIVES -> stringResource(R.string.empty_usb_videos_desc)
                    else -> stringResource(R.string.empty_local_desc)
                },
                color = SmartTubeTextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionPillButton(
                    icon = Icons.Default.AddLink,
                    text = stringResource(R.string.action_add_youtube),
                    isPrimary = selectedSection in setOf(SidebarSection.CHANNELS, SidebarSection.PLAYLISTS, SidebarSection.CATEGORIES),
                    onClick = onOpenAddYouTube
                )
                ActionPillButton(
                    icon = Icons.Default.FolderOpen,
                    text = stringResource(R.string.action_add_folder),
                    isPrimary = selectedSection !in setOf(SidebarSection.CHANNELS, SidebarSection.PLAYLISTS, SidebarSection.CATEGORIES),
                    onClick = onPickSafFolder
                )
                ActionPillButton(
                    icon = Icons.Default.Refresh,
                    text = stringResource(R.string.action_rescan_usb),
                    isPrimary = false,
                    onClick = onRescanStorage
                )
            }
        }
    }
}

@Composable
internal fun OfflineStatusBanner_Antigravity(modifier: Modifier = Modifier) {
    val colors = LocalJoudColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.cardBg)
            .border(1.dp, colors.usbCyan.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Default.WifiOff,
            contentDescription = null,
            tint = colors.usbCyan,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = stringResource(R.string.offline_banner_message),
            color = colors.textPrimary,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
