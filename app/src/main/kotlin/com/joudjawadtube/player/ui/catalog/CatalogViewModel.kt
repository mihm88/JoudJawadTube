package com.joudjawadtube.player.ui.catalog

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.joudjawadtube.player.data.local.AppDatabase
import com.joudjawadtube.player.data.local.SavedChannelEntity
import com.joudjawadtube.player.data.local.SavedPlaylistEntity
import com.joudjawadtube.player.data.local.VideoEntity
import com.joudjawadtube.player.data.local.VideoItem
import com.joudjawadtube.player.data.repository.CatalogSnapshot
import com.joudjawadtube.player.data.repository.CategoryRow
import com.joudjawadtube.player.data.repository.VideoRepository
import com.joudjawadtube.player.data.scanner.LocalVideoScanner
import com.joudjawadtube.player.data.scanner.MountedVolumeInfo
import com.joudjawadtube.player.data.scanner.UsbMountReceiver
import com.joudjawadtube.player.data.settings.AppSettingsRepository
import com.joudjawadtube.player.data.settings.AppSettingsState
import com.joudjawadtube.player.data.settings.UiModePreference
import com.joudjawadtube.player.data.thumbnail.ThumbnailManager
import com.joudjawadtube.player.data.updater.AppUpdater_Antigravity
import com.joudjawadtube.player.data.updater.UpdateInfo_Antigravity
import com.joudjawadtube.player.data.updater.UpdateState_Antigravity
import com.joudjawadtube.player.data.youtube.YouTubeService
import com.joudjawadtube.player.ui.components.SidebarSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlaybackSession(
    val currentVideo: VideoItem,
    val playlist: List<VideoItem> = listOf(currentVideo)
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
    val updateState: UpdateState_Antigravity = UpdateState_Antigravity.Idle
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
    val isTvModeOverride: Boolean? = null
)

class CatalogViewModel(application: Application) : AndroidViewModel(application) {

    private val appUpdater = AppUpdater_Antigravity(application)
    private val database = AppDatabase.getInstance(application)
    private val repository = VideoRepository(application.applicationContext, database.videoDao())
    private val settingsRepository = AppSettingsRepository.getInstance(application)

    private val _selectedSection = MutableStateFlow(SidebarSection.HOME)
    private val _selectedTopCategory = MutableStateFlow("All")
    private val _searchQuery = MutableStateFlow("")
    private val _selectedFolderFilter = MutableStateFlow<String?>(null)
    private val _dialogState = MutableStateFlow(DialogAndThumbState())

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
            _dialogState
        ) { section, topCat, query, folder, dialog ->
            FilterAndDialogBundle(section, topCat, query, folder, dialog.isTvModeOverride, dialog)
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
            updateState = dlg.updateState
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
        val dialogState: DialogAndThumbState
    )

    init {
        viewModelScope.launch {
            val currentSettings = settingsRepository.settingsFlow.value
            repository.refreshVolumesOnly(currentSettings.hideBootUsbPartitions)
            val existingCount = database.videoDao().getAllVideosOnce().size
            if (currentSettings.autoScanOnStartup || existingCount == 0) {
                repository.refreshStorageAndScan(currentSettings.hideBootUsbPartitions)
            }
        }

        viewModelScope.launch {
            UsbMountReceiver.usbEvents.collectLatest { event ->
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

        viewModelScope.launch {
            delay(3500)
            checkForAppUpdates(isManual = false)
        }
    }

    fun checkForAppUpdates(isManual: Boolean = false) {
        viewModelScope.launch {
            _dialogState.value = _dialogState.value.copy(updateState = UpdateState_Antigravity.Checking)
            val currentVersion = runCatching {
                val pInfo = getApplication<Application>().packageManager.getPackageInfo(getApplication<Application>().packageName, 0)
                pInfo.versionName ?: "1.0.0"
            }.getOrDefault("1.0.0")

            val result = appUpdater.checkForUpdate(currentVersion)
            result.onSuccess { info ->
                if (info == null) {
                    _dialogState.value = _dialogState.value.copy(
                        updateState = UpdateState_Antigravity.UpToDate(currentVersion)
                    )
                } else if (info.isNewer) {
                    _dialogState.value = _dialogState.value.copy(
                        updateState = UpdateState_Antigravity.UpdateAvailable(info)
                    )
                } else {
                    _dialogState.value = _dialogState.value.copy(
                        updateState = UpdateState_Antigravity.UpToDate(currentVersion)
                    )
                }
            }.onFailure { err ->
                if (isManual) {
                    _dialogState.value = _dialogState.value.copy(
                        updateState = UpdateState_Antigravity.Error(err.message ?: "Failed to check for updates")
                    )
                } else {
                    _dialogState.value = _dialogState.value.copy(
                        updateState = UpdateState_Antigravity.Idle
                    )
                }
            }
        }
    }

    fun downloadAndInstallUpdate(info: UpdateInfo_Antigravity) {
        viewModelScope.launch {
            _dialogState.value = _dialogState.value.copy(
                updateState = UpdateState_Antigravity.Downloading(0, 0, info.fileSize)
            )
            val result = appUpdater.downloadApk(info) { percent, downloaded, total ->
                _dialogState.value = _dialogState.value.copy(
                    updateState = UpdateState_Antigravity.Downloading(percent, downloaded, total)
                )
            }
            result.onSuccess { apkFile ->
                _dialogState.value = _dialogState.value.copy(
                    updateState = UpdateState_Antigravity.ReadyToInstall(apkFile, info)
                )
                appUpdater.installApk(apkFile).onFailure { err ->
                    _dialogState.value = _dialogState.value.copy(
                        updateState = UpdateState_Antigravity.Error("Failed to launch installer: ${err.message}")
                    )
                }
            }.onFailure { err ->
                _dialogState.value = _dialogState.value.copy(
                    updateState = UpdateState_Antigravity.Error("Download failed: ${err.message}")
                )
            }
        }
    }

    fun dismissUpdateStatus() {
        _dialogState.value = _dialogState.value.copy(updateState = UpdateState_Antigravity.Idle)
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
            UiModePreference.FORCE_TABLET -> false
            UiModePreference.AUTO -> _dialogState.value.isTvModeOverride ?: defaultIsTv
        }
        val nextIsTv = !currentEffective
        _dialogState.value = _dialogState.value.copy(isTvModeOverride = nextIsTv)
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
        _dialogState.value = _dialogState.value.copy(isTvModeOverride = null)
        settingsRepository.resetToDefaults()
    }

    // YouTube Link Dialog & Import
    fun openAddYouTubeDialog() {
        _dialogState.value = _dialogState.value.copy(
            showAddYouTubeDialog = true,
            youtubeErrorMessage = null,
            youtubeStatusMessage = null
        )
    }

    fun closeAddYouTubeDialog() {
        _dialogState.value = _dialogState.value.copy(
            showAddYouTubeDialog = false,
            isImportingYouTube = false,
            youtubeErrorMessage = null,
            youtubeStatusMessage = null
        )
    }

    fun importYouTubeLink(rawInput: String, playImmediately: Boolean) {
        viewModelScope.launch {
            _dialogState.value = _dialogState.value.copy(
                isImportingYouTube = true,
                youtubeErrorMessage = null,
                youtubeStatusMessage = null
            )
            val result = repository.importYouTubeUrl(rawInput)
            result.onSuccess { imported ->
                _dialogState.value = _dialogState.value.copy(
                    isImportingYouTube = false,
                    showAddYouTubeDialog = false,
                    youtubeErrorMessage = null,
                    youtubeStatusMessage = imported.summaryMessage,
                    thumbnailRevision = _dialogState.value.thumbnailRevision + 1
                )
                if (playImmediately) {
                    openVideo(imported.firstVideo, imported.playlist)
                } else {
                    _selectedSection.value = SidebarSection.PLAYLISTS
                }
            }.onFailure { err ->
                _dialogState.value = _dialogState.value.copy(
                    isImportingYouTube = false,
                    youtubeErrorMessage = err.message ?: "Failed to import YouTube link."
                )
            }
        }
    }

    // In-App TV Folder Picker Dialog
    fun openFolderPickerDialog() {
        _dialogState.value = _dialogState.value.copy(showFolderPickerDialog = true)
    }

    fun closeFolderPickerDialog() {
        _dialogState.value = _dialogState.value.copy(showFolderPickerDialog = false)
    }

    // Video Options & Thumbnail Controls
    fun openVideoOptions(video: VideoItem) {
        _dialogState.value = _dialogState.value.copy(selectedVideoForOptions = video)
    }

    fun closeVideoOptions() {
        _dialogState.value = _dialogState.value.copy(selectedVideoForOptions = null)
    }

    fun forceYouTubeThumbnailForVideo(video: VideoItem) {
        viewModelScope.launch {
            val ctx = getApplication<Application>().applicationContext
            val settings = settingsRepository.settingsFlow.value
            ThumbnailManager.resolveOrGenerateThumbnail(
                context = ctx,
                video = video,
                settings = settings,
                videoDao = database.videoDao(),
                forceRefresh = true
            )
            _dialogState.value = _dialogState.value.copy(
                thumbnailRevision = _dialogState.value.thumbnailRevision + 1
            )
        }
    }

    fun extractCustomFrameThumbnailForVideo(video: VideoItem, fraction: Float) {
        viewModelScope.launch {
            val ctx = getApplication<Application>().applicationContext
            val targetMs = if (video.durationMs > 5_000L) {
                (video.durationMs * fraction).toLong()
            } else {
                3_000L
            }
            ThumbnailManager.generateCustomFrameThumbnailAt(
                context = ctx,
                video = video,
                positionMs = targetMs,
                videoDao = database.videoDao()
            )
            _dialogState.value = _dialogState.value.copy(
                thumbnailRevision = _dialogState.value.thumbnailRevision + 1
            )
        }
    }

    fun regenerateAllThumbnails() {
        if (_dialogState.value.isGeneratingThumbnails) return
        viewModelScope.launch {
            val ctx = getApplication<Application>().applicationContext
            val settings = settingsRepository.settingsFlow.value
            val allVideos = uiState.value.snapshot.allVideos
            _dialogState.value = _dialogState.value.copy(
                isGeneratingThumbnails = true,
                thumbnailStatusMessage = "Generating thumbnails (0/${allVideos.size})..."
            )
            var done = 0
            for (video in allVideos) {
                ThumbnailManager.resolveOrGenerateThumbnail(
                    context = ctx,
                    video = video,
                    settings = settings,
                    videoDao = database.videoDao(),
                    forceRefresh = true
                )
                done++
                _dialogState.value = _dialogState.value.copy(
                    thumbnailStatusMessage = "Generated $done/${allVideos.size} thumbnails (YouTube first -> Auto Frame)...",
                    thumbnailRevision = _dialogState.value.thumbnailRevision + 1
                )
            }
            _dialogState.value = _dialogState.value.copy(
                isGeneratingThumbnails = false,
                thumbnailStatusMessage = "Completed all $done thumbnails!",
                thumbnailRevision = _dialogState.value.thumbnailRevision + 1
            )
        }
    }

    fun clearThumbnailCache() {
        viewModelScope.launch {
            val ctx = getApplication<Application>().applicationContext
            ThumbnailManager.clearThumbnailCache(ctx)
            _dialogState.value = _dialogState.value.copy(
                thumbnailStatusMessage = "Thumbnail disk cache cleared.",
                thumbnailRevision = _dialogState.value.thumbnailRevision + 1
            )
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
            repository.deleteYouTubeChannel(channelId)
        }
    }

    fun openOrImportKidsPlaylist(playlistId: String, playImmediately: Boolean = true) {
        viewModelScope.launch {
            _dialogState.value = _dialogState.value.copy(
                isImportingYouTube = true,
                youtubeStatusMessage = "Loading Kids Playlist..."
            )
            val res = repository.importYouTubeUrl("https://www.youtube.com/playlist?list=$playlistId")
            _dialogState.value = _dialogState.value.copy(
                isImportingYouTube = false,
                youtubeStatusMessage = null
            )
            res.onSuccess { imported ->
                if (playImmediately) {
                    _activePlaybackSession.value = PlaybackSession(
                        currentVideo = imported.firstVideo,
                        playlist = imported.playlist
                    )
                }
            }.onFailure { err ->
                _dialogState.value = _dialogState.value.copy(
                    youtubeErrorMessage = err.message ?: "Could not open kids playlist"
                )
            }
        }
    }

    fun clearYouTubeLibrary() {
        viewModelScope.launch {
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
        viewModelScope.launch {
            val freshVideo = repository.getVideoItemById(video.id) ?: video
            val playlist = if (contextPlaylist.isNotEmpty()) {
                contextPlaylist
            } else {
                repository.getFolderPlaylist(freshVideo.entity.folderPath).ifEmpty { listOf(freshVideo) }
            }
            _activePlaybackSession.value = PlaybackSession(
                currentVideo = freshVideo,
                playlist = playlist
            )
        }
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
