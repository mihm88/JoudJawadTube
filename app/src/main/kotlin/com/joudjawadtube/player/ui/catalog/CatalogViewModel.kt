package com.joudjawadtube.player.ui.catalog

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joudjawadtube.player.data.local.SavedChannelEntity
import com.joudjawadtube.player.data.local.SavedPlaylistEntity
import com.joudjawadtube.player.data.local.VideoEntity
import com.joudjawadtube.player.data.local.VideoItem
import com.joudjawadtube.player.data.network.NetworkMonitor_Antigravity
import com.joudjawadtube.player.data.repository.CatalogSnapshot
import com.joudjawadtube.player.data.repository.CategoryRow
import com.joudjawadtube.player.data.repository.KidsChannelItem
import com.joudjawadtube.player.data.repository.RelatedVideos
import com.joudjawadtube.player.data.repository.RelatedVideosResolver_Antigravity
import com.joudjawadtube.player.data.repository.VideoRepository
import com.joudjawadtube.player.data.scanner.LocalVideoScanner
import com.joudjawadtube.player.data.scanner.MountedVolumeInfo
import com.joudjawadtube.player.data.scanner.UsbMountReceiver
import com.joudjawadtube.player.data.settings.AppLanguage
import com.joudjawadtube.player.data.settings.AppSettingsRepository
import com.joudjawadtube.player.data.settings.AppSettingsState
import com.joudjawadtube.player.data.settings.UiModePreference
import com.joudjawadtube.player.data.updater.AppUpdater_Antigravity
import com.joudjawadtube.player.data.updater.UpdateInfo_Antigravity
import com.joudjawadtube.player.data.updater.UpdateState_Antigravity
import com.joudjawadtube.player.data.youtube.YouTubeService
import com.joudjawadtube.player.ui.components.SidebarSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Collections

data class PlaybackSession(
    val currentVideo: VideoItem,
    val playlist: List<VideoItem> = listOf(currentVideo),
    val related: RelatedVideos? = null
) {
    val currentIndex: Int
        get() = playlist.indexOfFirst { it.id == currentVideo.id }.coerceAtLeast(0)

    val hasPrevious: Boolean
        get() = currentIndex > 0

    val hasNext: Boolean
        get() = currentIndex < playlist.lastIndex
}

data class CatalogUiState(
    val snapshot: CatalogSnapshot = CatalogSnapshot(),
    val settings: AppSettingsState = AppSettingsState(),
    val isScanning: Boolean = false,
    val mountedVolumes: List<MountedVolumeInfo> = emptyList(),
    val selectedSection: SidebarSection = SidebarSection.HOME,
    val selectedTopCategory: String = "All",
    val searchQuery: String = "",
    val selectedFolderFilter: String? = null,
    val isTvModeOverride: Boolean? = null,
    val tabletUseGridMode: Boolean = true,
    val filteredRows: List<CategoryRow> = emptyList(),
    val filteredVideos: List<VideoItem> = emptyList(),
    val matchingChannels: List<SavedChannelEntity> = emptyList(),
    val matchingPlaylists: List<SavedPlaylistEntity> = emptyList(),
    val showAddYouTubeDialog: Boolean = false,
    val isImportingYouTube: Boolean = false,
    val youtubeErrorMessage: String? = null,
    val youtubeStatusMessage: String? = null,
    val showFolderPickerDialog: Boolean = false,
    val selectedVideoForOptions: VideoItem? = null,
    val isGeneratingThumbnails: Boolean = false,
    val thumbnailStatusMessage: String? = null,
    val thumbnailRevision: Int = 0,
    val updateState: UpdateState_Antigravity = UpdateState_Antigravity.Idle,
    val isHomeUpdateBannerDismissed: Boolean = false,
    val isOnline: Boolean = true,
    val kidsChannels: List<KidsChannelItem> = emptyList()
) {
    val usbDriveCount: Int
        get() = mountedVolumes.count { it.isUsbOrRemovable }
}

private data class DialogAndThumbState(
    val showAddYouTubeDialog: Boolean = false,
    val isImportingYouTube: Boolean = false,
    val youtubeErrorMessage: String? = null,
    val youtubeStatusMessage: String? = null,
    val showFolderPickerDialog: Boolean = false,
    val selectedVideoForOptions: VideoItem? = null,
    val isGeneratingThumbnails: Boolean = false,
    val thumbnailStatusMessage: String? = null,
    val thumbnailRevision: Int = 0,
    val updateState: UpdateState_Antigravity = UpdateState_Antigravity.Idle,
    val isHomeUpdateBannerDismissed: Boolean = false,
    val isTvModeOverride: Boolean? = null
)

@OptIn(FlowPreview::class)
class CatalogViewModel(
    private val repository: VideoRepository,
    private val settingsRepository: AppSettingsRepository,
    private val appUpdater: AppUpdater_Antigravity,
    private val networkMonitor: NetworkMonitor_Antigravity,
    private val kidsChannelsLoader: suspend (AppLanguage) -> List<KidsChannelItem>,
    private val isBuiltInChannelCheck: suspend (String, AppLanguage) -> Boolean,
    private val currentVersionProvider: () -> String,
    private val thumbnailResolver: suspend (VideoItem, AppSettingsState, Boolean) -> Unit = { _, _, _ -> },
    private val thumbnailCacheClearer: suspend () -> Unit = {},
    private val customFrameExtractor: suspend (VideoItem, Float) -> Unit = { _, _ -> },
    private val observeUsbEvents: Boolean = true,
    private val videoCountProvider: suspend () -> Int = { repository.catalogFlow.first().allVideos.size },
    private val autoCheckUpdatesOnInit: Boolean = true,
    private val autoScanOnInit: Boolean = true,
    private val onlineStateFlow: StateFlow<Boolean> = networkMonitor.isOnline,
    private val updateChecker: suspend (String) -> Result<UpdateInfo_Antigravity?> = { currentVersion ->
        appUpdater.checkForUpdate(currentVersion)
    },
    private val autoUpdateCheckDelayMs: Long = 2_500L
) : ViewModel() {

    private val _selectedSection = MutableStateFlow(SidebarSection.HOME)
    private val _selectedTopCategory = MutableStateFlow("All")
    private val _searchQuery = MutableStateFlow("")
    private val _selectedFolderFilter = MutableStateFlow<String?>(null)
    private val _dialogState = MutableStateFlow(DialogAndThumbState())
    private val _kidsChannels = MutableStateFlow<List<KidsChannelItem>>(emptyList())
    private val inFlightThumbnails: MutableSet<String> = Collections.synchronizedSet(mutableSetOf())

    @Volatile
    private var hasCompletedAutoUpdateCheck = false

    private val _activePlaybackSession = MutableStateFlow<PlaybackSession?>(null)
    val activePlaybackSession: StateFlow<PlaybackSession?> = _activePlaybackSession.asStateFlow()

    val uiState: StateFlow<CatalogUiState> = combine(
        repository.catalogFlow,
        repository.isScanning,
        repository.mountedVolumes,
        settingsRepository.settingsFlow,
        combine(
            _selectedSection,
            _selectedTopCategory,
            _searchQuery,
            _selectedFolderFilter,
            combine(_dialogState, onlineStateFlow, _kidsChannels) { dlg, online, kids ->
                Triple(dlg, online, kids)
            }
        ) { section, topCat, query, folder, (dialog, online, kids) ->
            FilterAndDialogBundle(
                selectedSection = section,
                selectedTopCategory = topCat,
                searchQuery = query,
                selectedFolderFilter = folder,
                isTvModeOverride = dialog.isTvModeOverride,
                dialogState = dialog,
                isOnline = online,
                kidsChannels = kids
            )
        }
    ) { snapshot, isScanning, volumes, settings, bundle ->
        val section = bundle.selectedSection
        val topCategory = bundle.selectedTopCategory
        val query = bundle.searchQuery.trim()
        val folderFilter = bundle.selectedFolderFilter

        val baseVideos = when (section) {
            SidebarSection.HOME -> snapshot.allVideos
            SidebarSection.KIDS_PICKS -> emptyList()
            SidebarSection.CHANNELS -> snapshot.youtubeVideos
            SidebarSection.PLAYLISTS -> snapshot.youtubeVideos
            SidebarSection.CATEGORIES -> snapshot.youtubeVideos
            SidebarSection.CONTINUE_WATCHING -> snapshot.continueWatching
            SidebarSection.USB_DRIVES -> snapshot.usbVideos
            SidebarSection.FOLDERS -> snapshot.allVideos.filter { !it.isYoutube }
            SidebarSection.FOUR_K -> snapshot.fourKVideos
            SidebarSection.FAVORITES -> snapshot.favoriteVideos
            SidebarSection.SETTINGS -> snapshot.allVideos
        }

        // Strictly in-app scoped search: filter saved channels, playlists, and videos
        val matchingChannels = if (query.isEmpty()) {
            snapshot.savedChannels
        } else {
            snapshot.savedChannels.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.handle.contains(query, ignoreCase = true) ||
                    it.category.contains(query, ignoreCase = true)
            }
        }

        val matchingPlaylists = if (query.isEmpty()) {
            snapshot.savedPlaylists
        } else {
            snapshot.savedPlaylists.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.category.contains(query, ignoreCase = true)
            }
        }

        val queryFilteredVideos = baseVideos.filter { item ->
            val matchesQuery = query.isEmpty() ||
                item.title.contains(query, ignoreCase = true) ||
                item.folderName.contains(query, ignoreCase = true) ||
                item.volumeLabel.contains(query, ignoreCase = true)
            val matchesFolder = folderFilter == null || item.folderName == folderFilter
            matchesQuery && matchesFolder
        }

        val rows = when (section) {
            SidebarSection.HOME -> {
                val candidateRows = if (topCategory == "All") {
                    snapshot.categoryRows
                } else {
                    snapshot.categoryRows.filter { row ->
                        row.category.equals(topCategory, ignoreCase = true) ||
                            (topCategory.equals("Islamic Kids", ignoreCase = true) && (row.title.contains("Islamic", ignoreCase = true) || row.title.contains("Quran", ignoreCase = true))) ||
                            (topCategory.equals("English Learning", ignoreCase = true) && (row.title.contains("English", ignoreCase = true) || row.title.contains("ABC", ignoreCase = true))) ||
                            (topCategory.equals("Cartoons & Fun", ignoreCase = true) && (row.title.contains("Cartoon", ignoreCase = true) || row.title.contains("Animation", ignoreCase = true))) ||
                            (topCategory.equals("Stories & Rhymes", ignoreCase = true) && (row.title.contains("Stories", ignoreCase = true) || row.title.contains("Rhymes", ignoreCase = true)))
                    }
                }
                candidateRows.mapNotNull { row ->
                    val matching = row.videos.filter { item ->
                        val matchesQuery = query.isEmpty() ||
                            item.title.contains(query, ignoreCase = true) ||
                            item.folderName.contains(query, ignoreCase = true)
                        val matchesFolder = folderFilter == null || item.folderName == folderFilter
                        matchesQuery && matchesFolder
                    }
                    if (matching.isEmpty()) null else row.copy(videos = matching)
                }
            }
            SidebarSection.KIDS_PICKS -> emptyList()
            SidebarSection.CHANNELS -> {
                matchingChannels.map { channel ->
                    val cVideos = queryFilteredVideos.filter {
                        (channel.uploadsPlaylistId.isNotBlank() && it.playlistId == channel.uploadsPlaylistId) ||
                            (channel.title.isNotBlank() && it.playlistTitle.contains(channel.title, ignoreCase = true)) ||
                            (channel.title.isNotBlank() && it.folderName.contains(channel.title, ignoreCase = true))
                    }
                    CategoryRow(
                        id = "ch_row_${channel.channelId}",
                        title = channel.title,
                        subtitle = "${cVideos.size} videos • ${channel.category}",
                        isYoutubeCategory = true,
                        channelId = channel.channelId,
                        category = channel.category,
                        videos = cVideos
                    )
                }
            }
            SidebarSection.PLAYLISTS -> {
                matchingPlaylists.map { pl ->
                    val plVideos = queryFilteredVideos.filter { it.playlistId == pl.playlistId }
                    CategoryRow(
                        id = "pl_row_${pl.playlistId}",
                        title = pl.title,
                        subtitle = "${plVideos.size} videos • ${pl.category}" + if (pl.isAutoCreatedFromChannel) " • From Channel" else "",
                        isYoutubeCategory = true,
                        playlistId = pl.playlistId,
                        videos = plVideos
                    )
                }
            }
            SidebarSection.CATEGORIES -> {
                val catRows = mutableListOf<CategoryRow>()
                val allCats = (snapshot.savedChannels.map { it.category } + snapshot.savedPlaylists.map { it.category }).distinct()
                for (cat in allCats) {
                    val catPlaylists = snapshot.savedPlaylists.filter { it.category.equals(cat, ignoreCase = true) }.map { it.playlistId }.toSet()
                    val catVideos = queryFilteredVideos.filter { it.playlistId in catPlaylists }
                    if (catVideos.isNotEmpty()) {
                        catRows.add(
                            CategoryRow(
                                id = "cat_row_$cat",
                                title = cat,
                                subtitle = "${catVideos.size} videos in $cat",
                                isYoutubeCategory = true,
                                videos = catVideos
                            )
                        )
                    }
                }
                catRows
            }
            SidebarSection.CONTINUE_WATCHING -> {
                if (queryFilteredVideos.isEmpty()) emptyList()
                else listOf(
                    CategoryRow(
                        id = "section_history",
                        title = "Continue Watching",
                        subtitle = "${queryFilteredVideos.size} videos in progress",
                        videos = queryFilteredVideos
                    )
                )
            }
            SidebarSection.USB_DRIVES -> {
                queryFilteredVideos.groupBy { it.folderName }.map { (folder, list) ->
                    val vol = list.firstOrNull()?.volumeLabel ?: "USB Drive"
                    CategoryRow(
                        id = "usb_sec_$folder",
                        title = folder,
                        subtitle = "${list.size} videos • $vol",
                        isUsbCategory = true,
                        videos = list
                    )
                }
            }
            SidebarSection.FOLDERS -> {
                queryFilteredVideos.groupBy { it.folderName }.map { (folder, list) ->
                    val isUsb = list.any { it.isUsb }
                    CategoryRow(
                        id = "folder_sec_$folder",
                        title = folder,
                        subtitle = "${list.size} videos" + if (isUsb) " • USB" else "",
                        isUsbCategory = isUsb,
                        videos = list
                    )
                }
            }
            SidebarSection.FOUR_K -> {
                if (queryFilteredVideos.isEmpty()) emptyList()
                else listOf(
                    CategoryRow(
                        id = "section_4k",
                        title = "4K & QHD Ultra HD Videos",
                        subtitle = "${queryFilteredVideos.size} high-resolution videos",
                        videos = queryFilteredVideos
                    )
                )
            }
            SidebarSection.FAVORITES -> {
                if (queryFilteredVideos.isEmpty()) emptyList()
                else listOf(
                    CategoryRow(
                        id = "section_favorites",
                        title = "Favorite Videos",
                        subtitle = "${queryFilteredVideos.size} saved videos",
                        videos = queryFilteredVideos
                    )
                )
            }
            SidebarSection.SETTINGS -> emptyList()
        }

        val dlg = bundle.dialogState
        CatalogUiState(
            snapshot = snapshot,
            settings = settings,
            isScanning = isScanning,
            mountedVolumes = volumes,
            selectedSection = section,
            selectedTopCategory = topCategory,
            searchQuery = bundle.searchQuery,
            selectedFolderFilter = folderFilter,
            isTvModeOverride = bundle.isTvModeOverride,
            tabletUseGridMode = settings.tabletUseGridMode,
            filteredRows = rows,
            filteredVideos = queryFilteredVideos,
            matchingChannels = matchingChannels,
            matchingPlaylists = matchingPlaylists,
            showAddYouTubeDialog = dlg.showAddYouTubeDialog,
            isImportingYouTube = dlg.isImportingYouTube,
            youtubeErrorMessage = dlg.youtubeErrorMessage,
            youtubeStatusMessage = dlg.youtubeStatusMessage,
            showFolderPickerDialog = dlg.showFolderPickerDialog,
            selectedVideoForOptions = dlg.selectedVideoForOptions,
            isGeneratingThumbnails = dlg.isGeneratingThumbnails,
            thumbnailStatusMessage = dlg.thumbnailStatusMessage,
            thumbnailRevision = dlg.thumbnailRevision,
            updateState = dlg.updateState,
            isHomeUpdateBannerDismissed = dlg.isHomeUpdateBannerDismissed,
            isOnline = bundle.isOnline,
            kidsChannels = bundle.kidsChannels
        )
    }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CatalogUiState()
    )

    private data class FilterAndDialogBundle(
        val selectedSection: SidebarSection,
        val selectedTopCategory: String,
        val searchQuery: String,
        val selectedFolderFilter: String?,
        val isTvModeOverride: Boolean?,
        val dialogState: DialogAndThumbState,
        val isOnline: Boolean,
        val kidsChannels: List<KidsChannelItem>
    )

    init {
        if (autoScanOnInit) {
            viewModelScope.launch(Dispatchers.IO) {
                val currentSettings = settingsRepository.settingsFlow.value
                repository.refreshVolumesOnly(currentSettings.hideBootUsbPartitions)
                // Seed the 21 bilingual built-in channels (respecting any user removals) first
                launch {
                    repository.seedAndSyncBuiltInChannels(
                        removedChannelIds = currentSettings.removedBuiltInChannelIds,
                        appLanguage = currentSettings.appLanguage
                    )
                }
                val existingCount = videoCountProvider()
                if (currentSettings.autoScanOnStartup || existingCount == 0) {
                    repository.refreshStorageAndScan(currentSettings.hideBootUsbPartitions)
                }
            }
        }

        // Load localized Kids Picks catalog off the main thread whenever appLanguage changes
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.settingsFlow
                .map { it.appLanguage }
                .distinctUntilChanged()
                .collectLatest { lang ->
                    _kidsChannels.value = kidsChannelsLoader(lang)
                }
        }

        // Re-localize untouched built-in channels if user toggles EN <-> AR in settings
        viewModelScope.launch {
            settingsRepository.settingsFlow
                .map { it.appLanguage }
                .distinctUntilChanged()
                .drop(1)
                .collectLatest { newLang ->
                    val removed = settingsRepository.settingsFlow.value.removedBuiltInChannelIds
                    repository.seedAndSyncBuiltInChannels(
                        removedChannelIds = removed,
                        appLanguage = newLang
                    )
                }
        }

        // Background pre-warm top 3 visible YouTube streams so clicking any card starts instantaneously
        viewModelScope.launch(Dispatchers.IO) {
            uiState
                .map { state ->
                    state.filteredVideos
                        .filter { it.isYoutube && it.youtubeVideoId.isNotBlank() }
                        .take(3)
                        .map { it.youtubeVideoId to (it.playlistId.isNotBlank() || it.entity.folderPath.startsWith("youtube_")) }
                }
                .distinctUntilChanged()
                .collectLatest { topItems ->
                    if (topItems.isEmpty()) return@collectLatest
                    delay(1_200L)
                    val allowFallbackSetting = settingsRepository.settingsFlow.value.allowGeneralFallbackForSavedChannels
                    topItems.forEach { (vidId, isTrusted) ->
                        YouTubeService.preResolveStream(
                            videoId = vidId,
                            allowGeneralClientFallback = allowFallbackSetting && isTrusted
                        )
                    }
                }
        }

        if (observeUsbEvents) {
            viewModelScope.launch {
                UsbMountReceiver.usbEvents
                    .debounce(1_500L)
                    .collectLatest { event ->
                        if (event != null) {
                            val currentSettings = settingsRepository.settingsFlow.value
                            if (currentSettings.autoScanOnUsbMount || event.action == "MANUAL_RESCAN") {
                                repository.refreshStorageAndScan(currentSettings.hideBootUsbPartitions)
                            } else {
                                repository.refreshVolumesOnly(currentSettings.hideBootUsbPartitions)
                            }
                        }
                    }
            }
        }

        if (autoCheckUpdatesOnInit) {
            viewModelScope.launch {
                onlineStateFlow.collectLatest { online ->
                    if (online && !hasCompletedAutoUpdateCheck) {
                        delay(autoUpdateCheckDelayMs)
                        if (onlineStateFlow.value && !hasCompletedAutoUpdateCheck) {
                            checkForAppUpdates(isManual = false)
                        }
                    }
                }
            }
        }
    }

    fun onAction(action: CatalogAction) {
        when (action) {
            is CatalogAction.SelectSection -> selectSection(action.section)
            is CatalogAction.SelectTopCategory -> selectTopCategory(action.category)
            is CatalogAction.UpdateSearchQuery -> updateSearchQuery(action.query)
            is CatalogAction.SelectFolderFilter -> selectFolderFilter(action.folderName)
            is CatalogAction.OpenVideo -> openVideo(action.video, action.playlist)
            is CatalogAction.ToggleFavorite -> toggleFavorite(action.video)
            is CatalogAction.OpenVideoOptions -> openVideoOptions(action.video)
            CatalogAction.OpenAddYouTubeDialog -> openAddYouTubeDialog()
            is CatalogAction.DeletePlaylist -> deleteYouTubePlaylist(action.playlistId)
            is CatalogAction.DeleteChannel -> deleteYouTubeChannel(action.channelId)
            is CatalogAction.ChangeChannelCategory -> updateChannelCategory(action.channelId, action.newCategory)
            is CatalogAction.ChangePlaylistCategory -> updatePlaylistCategory(action.playlistId, action.newCategory)
            is CatalogAction.OpenKidsPlaylist -> openOrImportKidsPlaylist(action.playlistId, action.playImmediately)
            is CatalogAction.AddBuiltInChannel -> addBuiltInChannelToLibrary(action.channel)
            CatalogAction.RequestReadPermission -> Unit
            CatalogAction.RequestAllFilesAccess -> Unit
            CatalogAction.PickSafFolder -> openFolderPickerDialog()
            CatalogAction.RescanStorage -> rescanStorage()
            CatalogAction.ToggleTabletGridMode -> toggleTabletGridMode()
            is CatalogAction.UpdateSettings -> updateSettings(action.transform)
            CatalogAction.RegenerateAllThumbnails -> regenerateAllThumbnails()
            CatalogAction.ClearThumbnailCache -> clearThumbnailCache()
            is CatalogAction.RemoveSafFolder -> removeSafFolder(action.uriString)
            CatalogAction.ClearWatchHistory -> clearWatchHistory()
            CatalogAction.ClearFavorites -> clearFavorites()
            CatalogAction.ClearYouTubeLibrary -> clearYouTubeLibrary()
            CatalogAction.ResetSettings -> resetSettingsToDefaults()
            CatalogAction.CheckForUpdates -> checkForAppUpdates(isManual = true)
            is CatalogAction.DownloadAndInstallUpdate -> downloadAndInstallUpdate(action.info)
            is CatalogAction.InstallDownloadedApk -> installDownloadedApk(action.apkFile)
            CatalogAction.DismissUpdateStatus -> dismissUpdateStatus()
            CatalogAction.DismissHomeUpdateBanner -> dismissHomeUpdateBanner()
            is CatalogAction.ResolveVideoThumbnail -> resolveVideoThumbnail(action.video)
        }
    }

    fun checkForAppUpdates(isManual: Boolean = false) {
        viewModelScope.launch {
            if (isManual) {
                _dialogState.update { it.copy(updateState = UpdateState_Antigravity.Checking) }
            }
            val currentVersion = currentVersionProvider()

            val result = updateChecker(currentVersion)
            result.onSuccess { info ->
                hasCompletedAutoUpdateCheck = true
                if (info != null && info.isNewer) {
                    _dialogState.update {
                        it.copy(
                            updateState = UpdateState_Antigravity.UpdateAvailable(info),
                            isHomeUpdateBannerDismissed = false
                        )
                    }
                } else if (isManual) {
                    _dialogState.update {
                        it.copy(updateState = UpdateState_Antigravity.UpToDate(currentVersion))
                    }
                } else {
                    _dialogState.update { state ->
                        if (state.updateState is UpdateState_Antigravity.Idle) state
                        else state.copy(updateState = UpdateState_Antigravity.Idle)
                    }
                }
            }.onFailure { err ->
                if (isManual) {
                    hasCompletedAutoUpdateCheck = true
                    _dialogState.update {
                        it.copy(updateState = UpdateState_Antigravity.Error(err.message ?: "Failed to check for updates"))
                    }
                } else if (onlineStateFlow.value) {
                    hasCompletedAutoUpdateCheck = true
                }
            }
        }
    }

    fun downloadAndInstallUpdate(info: UpdateInfo_Antigravity) {
        viewModelScope.launch {
            _dialogState.update {
                it.copy(updateState = UpdateState_Antigravity.Downloading(0, 0, info.fileSize))
            }
            val result = appUpdater.downloadApk(info) { percent, downloaded, total ->
                _dialogState.update {
                    it.copy(updateState = UpdateState_Antigravity.Downloading(percent, downloaded, total))
                }
            }
            result.onSuccess { apkFile ->
                _dialogState.update {
                    it.copy(updateState = UpdateState_Antigravity.ReadyToInstall(apkFile, info))
                }
                appUpdater.installApk(apkFile).onFailure { err ->
                    val msg = err.message ?: "Failed to launch installer"
                    _dialogState.update {
                        it.copy(updateState = UpdateState_Antigravity.Error(msg))
                    }
                }
            }.onFailure { err ->
                _dialogState.update {
                    it.copy(updateState = UpdateState_Antigravity.Error("Download failed: ${err.message}"))
                }
            }
        }
    }

    fun installDownloadedApk(apkFile: java.io.File) {
        appUpdater.installApk(apkFile).onFailure { err ->
            val msg = err.message ?: "Failed to launch installer"
            _dialogState.update {
                it.copy(updateState = UpdateState_Antigravity.Error(msg))
            }
        }
    }

    fun dismissUpdateStatus() {
        _dialogState.update { it.copy(updateState = UpdateState_Antigravity.Idle) }
    }

    fun dismissHomeUpdateBanner() {
        _dialogState.update { it.copy(isHomeUpdateBannerDismissed = true) }
    }

    fun rescanStorage() {
        viewModelScope.launch {
            val hideBoot = settingsRepository.settingsFlow.value.hideBootUsbPartitions
            repository.refreshStorageAndScan(hideBoot)
        }
    }

    fun selectSection(section: SidebarSection) {
        _selectedSection.value = section
        if (section != SidebarSection.FOLDERS && section != SidebarSection.HOME) {
            _selectedFolderFilter.value = null
        }
    }

    fun selectTopCategory(category: String) {
        _selectedTopCategory.value = category
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectFolderFilter(folderName: String?) {
        _selectedFolderFilter.value = if (_selectedFolderFilter.value == folderName) null else folderName
    }

    fun toggleTvTabletMode(defaultIsTv: Boolean) {
        val currentSettings = settingsRepository.settingsFlow.value
        val currentEffective = when (currentSettings.uiModePreference) {
            UiModePreference.FORCE_TV -> true
            UiModePreference.FORCE_TABLET,
            UiModePreference.FORCE_PHONE -> false
            UiModePreference.AUTO -> _dialogState.value.isTvModeOverride ?: defaultIsTv
        }
        val nextIsTv = !currentEffective
        _dialogState.update { it.copy(isTvModeOverride = nextIsTv) }
        settingsRepository.update {
            it.copy(uiModePreference = if (nextIsTv) UiModePreference.FORCE_TV else UiModePreference.FORCE_TABLET)
        }
    }

    fun toggleTabletGridMode() {
        settingsRepository.update { it.copy(tabletUseGridMode = !it.tabletUseGridMode) }
    }

    fun updateSettings(transform: (AppSettingsState) -> AppSettingsState) {
        settingsRepository.update(transform)
    }

    fun resetSettingsToDefaults() {
        _dialogState.update { it.copy(isTvModeOverride = null) }
        settingsRepository.resetToDefaults()
        viewModelScope.launch {
            repository.seedAndSyncBuiltInChannels(
                removedChannelIds = emptySet(),
                appLanguage = AppLanguage.FOLLOW_SYSTEM
            )
        }
    }

    // YouTube Link Dialog & Import
    fun openAddYouTubeDialog() {
        _dialogState.update {
            it.copy(
                showAddYouTubeDialog = true,
                youtubeErrorMessage = null,
                youtubeStatusMessage = null
            )
        }
    }

    fun closeAddYouTubeDialog() {
        _dialogState.update {
            it.copy(
                showAddYouTubeDialog = false,
                isImportingYouTube = false,
                youtubeErrorMessage = null,
                youtubeStatusMessage = null
            )
        }
    }

    fun importYouTubeLink(rawInput: String, playImmediately: Boolean) {
        viewModelScope.launch {
            _dialogState.update {
                it.copy(
                    isImportingYouTube = true,
                    youtubeErrorMessage = null,
                    youtubeStatusMessage = null
                )
            }
            val result = repository.importYouTubeUrl(rawInput)
            result.onSuccess { imported ->
                val chFolder = imported.firstVideo.entity.folderPath
                if (chFolder.startsWith("youtube_channel_")) {
                    settingsRepository.restoreBuiltInChannel(chFolder.removePrefix("youtube_channel_"))
                }
                _dialogState.update {
                    it.copy(
                        isImportingYouTube = false,
                        showAddYouTubeDialog = false,
                        youtubeErrorMessage = null,
                        youtubeStatusMessage = imported.summaryMessage,
                        thumbnailRevision = it.thumbnailRevision + 1
                    )
                }
                if (playImmediately) {
                    openVideo(imported.firstVideo, imported.playlist)
                } else {
                    _selectedSection.value = SidebarSection.PLAYLISTS
                }
            }.onFailure { err ->
                _dialogState.update {
                    it.copy(
                        isImportingYouTube = false,
                        youtubeErrorMessage = err.message ?: "Failed to import YouTube link."
                    )
                }
            }
        }
    }

    // In-App TV Folder Picker Dialog
    fun openFolderPickerDialog() {
        _dialogState.update { it.copy(showFolderPickerDialog = true) }
    }

    fun closeFolderPickerDialog() {
        _dialogState.update { it.copy(showFolderPickerDialog = false) }
    }

    // Video Options & Thumbnail Controls
    fun openVideoOptions(video: VideoItem) {
        _dialogState.update { it.copy(selectedVideoForOptions = video) }
    }

    fun closeVideoOptions() {
        _dialogState.update { it.copy(selectedVideoForOptions = null) }
    }

    fun resolveVideoThumbnail(video: VideoItem) {
        if (!inFlightThumbnails.add(video.id)) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val settings = settingsRepository.settingsFlow.value
                thumbnailResolver(video, settings, false)
                _dialogState.update {
                    it.copy(thumbnailRevision = it.thumbnailRevision + 1)
                }
            } finally {
                inFlightThumbnails.remove(video.id)
            }
        }
    }

    fun forceYouTubeThumbnailForVideo(video: VideoItem) {
        viewModelScope.launch(Dispatchers.IO) {
            val settings = settingsRepository.settingsFlow.value
            thumbnailResolver(video, settings, true)
            _dialogState.update {
                it.copy(thumbnailRevision = it.thumbnailRevision + 1)
            }
        }
    }

    fun extractCustomFrameThumbnailForVideo(video: VideoItem, fraction: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            customFrameExtractor(video, fraction)
            _dialogState.update {
                it.copy(thumbnailRevision = it.thumbnailRevision + 1)
            }
        }
    }

    fun regenerateAllThumbnails() {
        if (_dialogState.value.isGeneratingThumbnails) return
        viewModelScope.launch(Dispatchers.IO) {
            val settings = settingsRepository.settingsFlow.value
            val allVideos = uiState.value.snapshot.allVideos
            _dialogState.update {
                it.copy(
                    isGeneratingThumbnails = true,
                    thumbnailStatusMessage = "Generating thumbnails (0/${allVideos.size})..."
                )
            }
            var done = 0
            for (video in allVideos) {
                thumbnailResolver(video, settings, true)
                done++
                _dialogState.update {
                    it.copy(
                        thumbnailStatusMessage = "Generated $done/${allVideos.size} thumbnails (YouTube first -> Auto Frame)...",
                        thumbnailRevision = it.thumbnailRevision + 1
                    )
                }
            }
            _dialogState.update {
                it.copy(
                    isGeneratingThumbnails = false,
                    thumbnailStatusMessage = "Completed all $done thumbnails!",
                    thumbnailRevision = it.thumbnailRevision + 1
                )
            }
        }
    }

    fun clearThumbnailCache() {
        viewModelScope.launch(Dispatchers.IO) {
            thumbnailCacheClearer()
            _dialogState.update {
                it.copy(
                    thumbnailStatusMessage = "Thumbnail disk cache cleared.",
                    thumbnailRevision = it.thumbnailRevision + 1
                )
            }
        }
    }

    fun deleteYouTubeVideo(video: VideoItem) {
        viewModelScope.launch {
            repository.deleteYouTubeVideo(video.id)
        }
    }

    fun deleteYouTubePlaylist(playlistId: String) {
        viewModelScope.launch {
            repository.deleteYouTubePlaylist(playlistId)
        }
    }

    fun deleteYouTubeChannel(channelId: String) {
        viewModelScope.launch {
            val isBuiltIn = isBuiltInChannelCheck(
                channelId,
                settingsRepository.settingsFlow.value.appLanguage
            )
            if (isBuiltIn) {
                settingsRepository.markBuiltInChannelRemoved(channelId)
            }
            repository.deleteYouTubeChannel(channelId)
        }
    }

    fun addBuiltInChannelToLibrary(channel: KidsChannelItem) {
        viewModelScope.launch {
            settingsRepository.restoreBuiltInChannel(channel.channelId)
            _dialogState.update {
                it.copy(
                    isImportingYouTube = true,
                    youtubeStatusMessage = "Adding ${channel.channelName}..."
                )
            }
            repository.addBuiltInChannelToLibrary(channel)
            _dialogState.update {
                it.copy(
                    isImportingYouTube = false,
                    youtubeStatusMessage = "${channel.channelName} added to Library"
                )
            }
        }
    }

    fun openOrImportKidsPlaylist(playlistId: String, playImmediately: Boolean = true) {
        viewModelScope.launch {
            _dialogState.update {
                it.copy(
                    isImportingYouTube = true,
                    youtubeStatusMessage = "Loading Kids Playlist..."
                )
            }
            val res = repository.importYouTubeUrl("https://www.youtube.com/playlist?list=$playlistId")
            _dialogState.update {
                it.copy(
                    isImportingYouTube = false,
                    youtubeStatusMessage = null
                )
            }
            res.onSuccess { imported ->
                if (playImmediately) {
                    openVideo(imported.firstVideo, imported.playlist)
                }
            }.onFailure { err ->
                _dialogState.update {
                    it.copy(youtubeErrorMessage = err.message ?: "Could not open kids playlist")
                }
            }
        }
    }

    fun clearYouTubeLibrary() {
        viewModelScope.launch {
            val builtInIds = kidsChannelsLoader(settingsRepository.settingsFlow.value.appLanguage)
                .map { it.channelId }
                .toSet()
            settingsRepository.update { it.copy(removedBuiltInChannelIds = it.removedBuiltInChannelIds + builtInIds) }
            repository.clearAllYouTubeContent()
        }
    }

    fun clearWatchHistory() {
        viewModelScope.launch {
            repository.clearWatchHistory()
        }
    }

    fun clearFavorites() {
        viewModelScope.launch {
            repository.clearAllFavorites()
        }
    }

    fun toggleFavorite(video: VideoItem) {
        viewModelScope.launch {
            repository.toggleFavorite(video.videoKey)
            val session = _activePlaybackSession.value
            if (session != null && session.currentVideo.videoKey == video.videoKey) {
                val updated = repository.getVideoItemById(video.id)
                if (updated != null) {
                    _activePlaybackSession.value = session.copy(currentVideo = updated)
                }
            }
        }
    }

    fun openVideo(video: VideoItem, contextPlaylist: List<VideoItem> = emptyList()) {
        // Immediate 0ms UI transition so stream resolution starts right away without waiting for SQLite locks
        val initialPlaylist = if (contextPlaylist.isNotEmpty()) contextPlaylist else listOf(video)
        _activePlaybackSession.value = PlaybackSession(
            currentVideo = video,
            playlist = initialPlaylist
        )

        if (contextPlaylist.isEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                val folderPlaylist = repository.getFolderPlaylist(video.entity.folderPath)
                if (folderPlaylist.size > 1) {
                    val current = _activePlaybackSession.value
                    if (current != null && current.currentVideo.id == video.id) {
                        _activePlaybackSession.value = current.copy(playlist = folderPlaylist)
                    }
                }
            }
        }

        viewModelScope.launch(Dispatchers.Default) {
            val snap = uiState.value.snapshot
            val resolvedRelated = RelatedVideosResolver_Antigravity.resolve(
                current = video,
                allVideos = snap.allVideos,
                savedPlaylists = snap.savedPlaylists,
                savedChannels = snap.savedChannels
            )
            if (resolvedRelated != null) {
                val current = _activePlaybackSession.value
                if (current != null && current.currentVideo.id == video.id) {
                    _activePlaybackSession.value = current.copy(related = resolvedRelated)
                }
            }
        }
    }

    fun playFromRelated(video: VideoItem, customQueue: List<VideoItem> = emptyList()) {
        val related = _activePlaybackSession.value?.related
        val queue = when {
            customQueue.isNotEmpty() -> customQueue
            related != null -> related.videos
            else -> listOf(video)
        }
        openVideo(video, queue)
    }

    fun playPreviousInSession() {
        val session = _activePlaybackSession.value ?: return
        if (session.hasPrevious) {
            val prev = session.playlist[session.currentIndex - 1]
            openVideo(prev, session.playlist)
        }
    }

    fun playNextInSession(wrapAround: Boolean = false) {
        val session = _activePlaybackSession.value ?: return
        if (session.hasNext) {
            val next = session.playlist[session.currentIndex + 1]
            openVideo(next, session.playlist)
        } else if (wrapAround && session.playlist.isNotEmpty()) {
            openVideo(session.playlist.first(), session.playlist)
        }
    }

    fun updateChannelCategory(channelId: String, newCategory: String) {
        viewModelScope.launch {
            repository.updateChannelCategory(channelId, newCategory)
        }
    }

    fun updatePlaylistCategory(playlistId: String, newCategory: String) {
        viewModelScope.launch {
            repository.updatePlaylistCategory(playlistId, newCategory)
        }
    }

    fun handleIncomingExternalOrYouTubeUri(uri: Uri) {
        val uriStr = uri.toString()
        if (uriStr.contains("youtube.com", ignoreCase = true) ||
            uriStr.contains("youtu.be", ignoreCase = true)
        ) {
            importYouTubeLink(uriStr, playImmediately = true)
            return
        }

        val pathSeg = uri.lastPathSegment ?: "External Video"
        val cleanTitle = LocalVideoScanner.prettifyVideoTitle(pathSeg.substringAfterLast('/'))
        val key = "ext:${uriStr.hashCode()}"
        val entity = VideoEntity(
            id = key,
            videoKey = key,
            uri = uriStr,
            filePath = "",
            title = cleanTitle,
            fileName = pathSeg,
            folderName = "External Intent",
            folderPath = "external",
            volumeLabel = "External",
            isUsb = false,
            durationMs = 0L,
            sizeBytes = 0L,
            width = 0,
            height = 0,
            mimeType = "video/*",
            dateModified = System.currentTimeMillis(),
            sidecarSubtitles = ""
        )
        val item = VideoItem(entity = entity, progress = null)
        _activePlaybackSession.value = PlaybackSession(currentVideo = item, playlist = listOf(item))
    }

    fun savePlaybackProgress(videoKey: String, positionMs: Long, durationMs: Long) {
        viewModelScope.launch {
            repository.savePlaybackProgress(videoKey, positionMs, durationMs)
        }
    }

    fun closePlayer() {
        _activePlaybackSession.value = null
    }

    fun addSafFolder(uriString: String, displayName: String) {
        viewModelScope.launch {
            val hideBoot = settingsRepository.settingsFlow.value.hideBootUsbPartitions
            repository.addSafFolder(uriString, displayName, hideBoot)
        }
    }

    fun removeSafFolder(uriString: String) {
        viewModelScope.launch {
            val hideBoot = settingsRepository.settingsFlow.value.hideBootUsbPartitions
            repository.removeSafFolder(uriString, hideBoot)
        }
    }
}
