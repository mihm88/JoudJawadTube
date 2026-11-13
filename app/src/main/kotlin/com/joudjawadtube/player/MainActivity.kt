package com.joudjawadtube.player

import android.Manifest
import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.Density
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.joudjawadtube.player.data.settings.UiModePreference
import com.joudjawadtube.player.ui.catalog.CatalogViewModel
import com.joudjawadtube.player.ui.catalog.TabletCatalogScreen
import com.joudjawadtube.player.ui.catalog.TvCatalogScreen
import com.joudjawadtube.player.ui.components.AddYouTubeLinkDialog
import com.joudjawadtube.player.ui.components.LocalWindowLayoutInfo
import com.joudjawadtube.player.ui.components.ProvideWindowLayoutInfo
import com.joudjawadtube.player.ui.components.SidebarSection
import com.joudjawadtube.player.ui.components.TvFolderPickerDialog
import com.joudjawadtube.player.ui.components.VideoOptionsDialog
import com.joudjawadtube.player.ui.playback.PlaybackScreen
import com.joudjawadtube.player.ui.theme.JoudJawadTubeTheme

class MainActivity : AppCompatActivity() {

    private val viewModel: CatalogViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIncomingVideoIntent(intent)

        val defaultIsTv = detectIsTelevision()

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val activePlaybackSession by viewModel.activePlaybackSession.collectAsState()
            val baseDensity = LocalDensity.current
            val scaledDensity = remember(baseDensity, uiState.settings.uiFontScale) {
                Density(
                    density = baseDensity.density,
                    fontScale = baseDensity.fontScale * uiState.settings.uiFontScale.scaleFactor
                )
            }

            CompositionLocalProvider(LocalDensity provides scaledDensity) {
                JoudJawadTubeTheme(themeMode = uiState.settings.themeMode) {
                    ProvideWindowLayoutInfo(
                        isTelevisionHardware = defaultIsTv,
                        uiModePreference = uiState.settings.uiModePreference
                    ) {
                        val context = LocalContext.current
                    val lifecycleOwner = LocalLifecycleOwner.current

                    var hasReadPermission by remember {
                        mutableStateOf(checkReadVideoPermission(context))
                    }
                    var hasAllFilesAccess by remember {
                        mutableStateOf(checkAllFilesAccess())
                    }

                    // Refresh permissions & rescan only when permissions actually changed in system settings
                    DisposableEffect(lifecycleOwner) {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_RESUME) {
                                val newRead = checkReadVideoPermission(context)
                                val newAllFiles = checkAllFilesAccess()
                                if (newRead != hasReadPermission || newAllFiles != hasAllFilesAccess) {
                                    hasReadPermission = newRead
                                    hasAllFilesAccess = newAllFiles
                                    viewModel.rescanStorage()
                                }
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose {
                            lifecycleOwner.lifecycle.removeObserver(observer)
                        }
                    }

                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestMultiplePermissions()
                    ) {
                        hasReadPermission = checkReadVideoPermission(context)
                        viewModel.rescanStorage()
                    }

                    val safFolderLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.OpenDocumentTree()
                    ) { treeUri: Uri? ->
                        if (treeUri != null) {
                            runCatching {
                                contentResolver.takePersistableUriPermission(
                                    treeUri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                                )
                            }
                            val docName = DocumentFile.fromTreeUri(context, treeUri)?.name
                                ?: treeUri.lastPathSegment
                                ?: "USB / Local Folder"
                            viewModel.addSafFolder(treeUri.toString(), docName)
                        }
                    }

                    val requestReadPermissionAction: () -> Unit = {
                        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
                        } else {
                            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                        permissionLauncher.launch(perms)
                    }

                    val requestAllFilesAccessAction: () -> Unit = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            val perAppIntent = Intent(
                                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                Uri.parse("package:$packageName")
                            )
                            val genericIntent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                            runCatching {
                                startActivity(perAppIntent)
                            }.onFailure {
                                runCatching { startActivity(genericIntent) }
                            }
                        } else {
                            requestReadPermissionAction()
                        }
                    }

                    // Open the in-app D-Pad TV & USB Folder Browser so it works reliably on Android TV
                    val pickFolderAction: () -> Unit = {
                        viewModel.openFolderPickerDialog()
                    }

                    // Hoisted lazy list / grid states for persistent scroll preservation across playback exits
                    val tvHomeListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
                    val tabletHomeListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
                    val tabletGridState = rememberSaveable(saver = LazyGridState.Saver) { LazyGridState() }

                    val layoutInfo = LocalWindowLayoutInfo.current
                    val useTvLayout = layoutInfo.isTv

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .then(
                                if (activePlaybackSession == null && layoutInfo.isTouchDevice) {
                                    Modifier.systemBarsPadding()
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                        if (activePlaybackSession == null) {
                            if (useTvLayout) {
                                TvCatalogScreen(
                                    uiState = uiState,
                                    lazyListState = tvHomeListState,
                                    hasReadVideoPermission = hasReadPermission,
                                    hasAllFilesAccess = hasAllFilesAccess,
                                    onSelectSection = { viewModel.selectSection(it) },
                                    onSelectTopCategory = { viewModel.selectTopCategory(it) },
                                    onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                                    onSelectFolderFilter = { viewModel.selectFolderFilter(it) },
                                    onVideoClick = { video, playlist ->
                                        viewModel.openVideo(video, playlist)
                                    },
                                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                                    onOpenVideoOptions = { viewModel.openVideoOptions(it) },
                                    onOpenAddYouTubeDialog = { viewModel.openAddYouTubeDialog() },
                                    onDeletePlaylist = { viewModel.deleteYouTubePlaylist(it) },
                                    onDeleteChannel = { viewModel.deleteYouTubeChannel(it) },
                                    onChangeChannelCategory = { channelId, cat -> viewModel.updateChannelCategory(channelId, cat) },
                                    onChangePlaylistCategory = { playlistId, cat -> viewModel.updatePlaylistCategory(playlistId, cat) },
                                    onOpenKidsPlaylist = { plId, _ -> viewModel.openOrImportKidsPlaylist(plId, playImmediately = true) },
                                    onSaveKidsPlaylist = { plId -> viewModel.openOrImportKidsPlaylist(plId, playImmediately = false) },
                                    onAddBuiltInChannel = { ch -> viewModel.addBuiltInChannelToLibrary(ch) },
                                    onRequestReadPermission = requestReadPermissionAction,
                                    onRequestAllFilesAccess = requestAllFilesAccessAction,
                                    onPickSafFolder = pickFolderAction,
                                    onRescanStorage = { viewModel.rescanStorage() },
                                    onUpdateSettings = { viewModel.updateSettings(it) },
                                    onRegenerateAllThumbnails = { viewModel.regenerateAllThumbnails() },
                                    onClearThumbnailCache = { viewModel.clearThumbnailCache() },
                                    onRemoveSafFolder = { viewModel.removeSafFolder(it) },
                                    onClearWatchHistory = { viewModel.clearWatchHistory() },
                                    onClearFavorites = { viewModel.clearFavorites() },
                                    onClearYouTubeLibrary = { viewModel.clearYouTubeLibrary() },
                                    onResetSettings = { viewModel.resetSettingsToDefaults() },
                                    onCheckForUpdates = { viewModel.checkForAppUpdates(isManual = true) },
                                    onDownloadAndInstallUpdate = { viewModel.downloadAndInstallUpdate(it) },
                                    onDismissUpdateStatus = { viewModel.dismissUpdateStatus() }
                                )
                            } else {
                                TabletCatalogScreen(
                                    uiState = uiState,
                                    lazyListState = tabletHomeListState,
                                    lazyGridState = tabletGridState,
                                    hasReadVideoPermission = hasReadPermission,
                                    hasAllFilesAccess = hasAllFilesAccess,
                                    onSelectSection = { viewModel.selectSection(it) },
                                    onSelectTopCategory = { viewModel.selectTopCategory(it) },
                                    onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                                    onSelectFolderFilter = { viewModel.selectFolderFilter(it) },
                                    onVideoClick = { video, playlist ->
                                        viewModel.openVideo(video, playlist)
                                    },
                                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                                    onOpenVideoOptions = { viewModel.openVideoOptions(it) },
                                    onOpenAddYouTubeDialog = { viewModel.openAddYouTubeDialog() },
                                    onDeletePlaylist = { viewModel.deleteYouTubePlaylist(it) },
                                    onDeleteChannel = { viewModel.deleteYouTubeChannel(it) },
                                    onChangeChannelCategory = { channelId, cat -> viewModel.updateChannelCategory(channelId, cat) },
                                    onChangePlaylistCategory = { playlistId, cat -> viewModel.updatePlaylistCategory(playlistId, cat) },
                                    onOpenKidsPlaylist = { plId, _ -> viewModel.openOrImportKidsPlaylist(plId, playImmediately = true) },
                                    onSaveKidsPlaylist = { plId -> viewModel.openOrImportKidsPlaylist(plId, playImmediately = false) },
                                    onAddBuiltInChannel = { ch -> viewModel.addBuiltInChannelToLibrary(ch) },
                                    onRequestReadPermission = requestReadPermissionAction,
                                    onRequestAllFilesAccess = requestAllFilesAccessAction,
                                    onPickSafFolder = pickFolderAction,
                                    onRescanStorage = { viewModel.rescanStorage() },
                                    onToggleTabletGridMode = { viewModel.toggleTabletGridMode() },
                                    onUpdateSettings = { viewModel.updateSettings(it) },
                                    onRegenerateAllThumbnails = { viewModel.regenerateAllThumbnails() },
                                    onClearThumbnailCache = { viewModel.clearThumbnailCache() },
                                    onRemoveSafFolder = { viewModel.removeSafFolder(it) },
                                    onClearWatchHistory = { viewModel.clearWatchHistory() },
                                    onClearFavorites = { viewModel.clearFavorites() },
                                    onClearYouTubeLibrary = { viewModel.clearYouTubeLibrary() },
                                    onResetSettings = { viewModel.resetSettingsToDefaults() },
                                    onCheckForUpdates = { viewModel.checkForAppUpdates(isManual = true) },
                                    onDownloadAndInstallUpdate = { viewModel.downloadAndInstallUpdate(it) },
                                    onDismissUpdateStatus = { viewModel.dismissUpdateStatus() }
                                )
                            }
                        }

                        // Playback overlay when an active playback session is running
                        activePlaybackSession?.let { currentSession ->
                            PlaybackScreen(
                                session = currentSession,
                                settings = uiState.settings,
                                onSaveProgress = { videoKey, posMs, durMs ->
                                    viewModel.savePlaybackProgress(videoKey, posMs, durMs)
                                },
                                onPreviousVideo = { viewModel.playPreviousInSession() },
                                onNextVideo = {
                                    val wrap = uiState.settings.repeatMode == com.joudjawadtube.player.data.settings.RepeatMode.REPEAT_ALL
                                    viewModel.playNextInSession(wrapAround = wrap)
                                },
                                onToggleFavorite = { viewModel.toggleFavorite(currentSession.currentVideo) },
                                onBack = { viewModel.closePlayer() }
                            )
                        }

                        // Handle TV Remote Back Button for modal dialogs & non-Home sections when player is not active
                        BackHandler(
                            enabled = activePlaybackSession == null && (
                                uiState.showAddYouTubeDialog ||
                                uiState.showFolderPickerDialog ||
                                uiState.selectedVideoForOptions != null ||
                                uiState.selectedSection != SidebarSection.HOME
                            )
                        ) {
                            when {
                                uiState.selectedVideoForOptions != null -> viewModel.closeVideoOptions()
                                uiState.showAddYouTubeDialog -> viewModel.closeAddYouTubeDialog()
                                uiState.showFolderPickerDialog -> viewModel.closeFolderPickerDialog()
                                uiState.selectedSection != SidebarSection.HOME -> {
                                    viewModel.selectSection(SidebarSection.HOME)
                                }
                            }
                        }

                        // YouTube Link Import Dialog
                        if (uiState.showAddYouTubeDialog) {
                            AddYouTubeLinkDialog(
                                isLoading = uiState.isImportingYouTube,
                                errorMessage = uiState.youtubeErrorMessage,
                                statusMessage = uiState.youtubeStatusMessage,
                                onSubmitUrl = { url, playNow ->
                                    viewModel.importYouTubeLink(url, playNow)
                                },
                                onDismiss = { viewModel.closeAddYouTubeDialog() }
                            )
                        }

                        // In-App TV & USB Folder Picker Dialog
                        if (uiState.showFolderPickerDialog) {
                            TvFolderPickerDialog(
                                onSelectFolder = { path, displayName ->
                                    viewModel.addSafFolder(path, displayName)
                                },
                                onLaunchSystemSafPicker = {
                                    runCatching { safFolderLauncher.launch(null) }
                                },
                                onDismiss = { viewModel.closeFolderPickerDialog() }
                            )
                        }

                        // Video Options & Thumbnail Control Dialog
                        uiState.selectedVideoForOptions?.let { selectedVideo ->
                            VideoOptionsDialog(
                                video = selectedVideo,
                                onPlay = { viewModel.openVideo(selectedVideo, uiState.filteredVideos) },
                                onToggleFavorite = { viewModel.toggleFavorite(selectedVideo) },
                                onFetchYouTubeThumbnail = {
                                    viewModel.forceYouTubeThumbnailForVideo(selectedVideo)
                                },
                                onExtractFrameAtFraction = { frac ->
                                    viewModel.extractCustomFrameThumbnailForVideo(selectedVideo, frac)
                                },
                                onDeleteVideo = if (selectedVideo.isYoutube) {
                                    { viewModel.deleteYouTubeVideo(selectedVideo) }
                                } else {
                                    null
                                },
                                onDismiss = { viewModel.closeVideoOptions() }
                            )
                        }
                    }
                }
            }
        }
    }
}

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingVideoIntent(intent)
    }

    private fun handleIncomingVideoIntent(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_VIEW -> {
                val dataUri = intent.data
                if (dataUri != null) {
                    intent.action = Intent.ACTION_MAIN
                    intent.data = null
                    viewModel.handleIncomingExternalOrYouTubeUri(dataUri)
                }
            }
            Intent.ACTION_SEND -> {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (!sharedText.isNullOrBlank()) {
                    intent.action = Intent.ACTION_MAIN
                    intent.removeExtra(Intent.EXTRA_TEXT)
                    viewModel.importYouTubeLink(sharedText, playImmediately = true)
                }
            }
        }
    }

    private fun detectIsTelevision(): Boolean {
        val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        val isTvUiMode = uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
        val hasLeanback = packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        return isTvUiMode || hasLeanback
    }

    private fun checkReadVideoPermission(context: Context): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkAllFilesAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }
}
