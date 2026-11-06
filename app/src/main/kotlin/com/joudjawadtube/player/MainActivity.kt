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
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.joudjawadtube.player.ui.components.SidebarSection
import com.joudjawadtube.player.ui.components.TvFolderPickerDialog
import com.joudjawadtube.player.ui.components.VideoOptionsDialog
import com.joudjawadtube.player.ui.playback.PlaybackScreen
import com.joudjawadtube.player.ui.theme.JoudJawadTubeTheme

class MainActivity : ComponentActivity() {

    private val viewModel: CatalogViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                JoudJawadTubeTheme {
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

                    val currentSession = activePlaybackSession
                    if (currentSession != null) {
                        PlaybackScreen(
                            session = currentSession,
                            settings = uiState.settings,
                            onSaveProgress = { videoKey, posMs, durMs ->
                                viewModel.savePlaybackProgress(videoKey, posMs, durMs)
                            },
                            onPreviousVideo = { viewModel.playPreviousInSession() },
                            onNextVideo = { viewModel.playNextInSession() },
                            onToggleFavorite = { viewModel.toggleFavorite(currentSession.currentVideo) },
                            onBack = { viewModel.closePlayer() }
                        )
                    } else {
                        val useTvLayout = when (uiState.settings.uiModePreference) {
                            UiModePreference.FORCE_TV -> true
                            UiModePreference.FORCE_TABLET -> false
                            UiModePreference.AUTO -> uiState.isTvModeOverride ?: defaultIsTv
                        }

                        if (useTvLayout) {
                            TvCatalogScreen(
                                uiState = uiState,
                                hasReadVideoPermission = hasReadPermission,
                                hasAllFilesAccess = hasAllFilesAccess,
                                onSelectSection = { viewModel.selectSection(it) },
                                onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                                onSelectFolderFilter = { viewModel.selectFolderFilter(it) },
                                onVideoClick = { video, playlist ->
                                    viewModel.openVideo(video, playlist)
                                },
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                onOpenVideoOptions = { viewModel.openVideoOptions(it) },
                                onOpenAddYouTubeDialog = { viewModel.openAddYouTubeDialog() },
                                onDeletePlaylist = { viewModel.deleteYouTubePlaylist(it) },
                                onRequestReadPermission = requestReadPermissionAction,
                                onRequestAllFilesAccess = requestAllFilesAccessAction,
                                onPickSafFolder = pickFolderAction,
                                onRescanStorage = { viewModel.rescanStorage() },
                                onToggleLayoutMode = { viewModel.toggleTvTabletMode(defaultIsTv) },
                                onUpdateSettings = { viewModel.updateSettings(it) },
                                onRegenerateAllThumbnails = { viewModel.regenerateAllThumbnails() },
                                onClearThumbnailCache = { viewModel.clearThumbnailCache() },
                                onRemoveSafFolder = { viewModel.removeSafFolder(it) },
                                onClearWatchHistory = { viewModel.clearWatchHistory() },
                                onClearFavorites = { viewModel.clearFavorites() },
                                onClearYouTubeLibrary = { viewModel.clearYouTubeLibrary() },
                                onResetSettings = { viewModel.resetSettingsToDefaults() }
                            )
                        } else {
                            TabletCatalogScreen(
                                uiState = uiState,
                                hasReadVideoPermission = hasReadPermission,
                                hasAllFilesAccess = hasAllFilesAccess,
                                onSelectSection = { viewModel.selectSection(it) },
                                onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                                onSelectFolderFilter = { viewModel.selectFolderFilter(it) },
                                onVideoClick = { video, playlist ->
                                    viewModel.openVideo(video, playlist)
                                },
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                onOpenVideoOptions = { viewModel.openVideoOptions(it) },
                                onOpenAddYouTubeDialog = { viewModel.openAddYouTubeDialog() },
                                onDeletePlaylist = { viewModel.deleteYouTubePlaylist(it) },
                                onRequestReadPermission = requestReadPermissionAction,
                                onRequestAllFilesAccess = requestAllFilesAccessAction,
                                onPickSafFolder = pickFolderAction,
                                onRescanStorage = { viewModel.rescanStorage() },
                                onToggleLayoutMode = { viewModel.toggleTvTabletMode(defaultIsTv) },
                                onToggleTabletGridMode = { viewModel.toggleTabletGridMode() },
                                onUpdateSettings = { viewModel.updateSettings(it) },
                                onRegenerateAllThumbnails = { viewModel.regenerateAllThumbnails() },
                                onClearThumbnailCache = { viewModel.clearThumbnailCache() },
                                onRemoveSafFolder = { viewModel.removeSafFolder(it) },
                                onClearWatchHistory = { viewModel.clearWatchHistory() },
                                onClearFavorites = { viewModel.clearFavorites() },
                                onClearYouTubeLibrary = { viewModel.clearYouTubeLibrary() },
                                onResetSettings = { viewModel.resetSettingsToDefaults() }
                            )
                        }

                        // Handle TV Remote Back Button for modal dialogs & non-Home sections
                        BackHandler(
                            enabled = uiState.showAddYouTubeDialog ||
                                uiState.showFolderPickerDialog ||
                                uiState.selectedVideoForOptions != null ||
                                uiState.selectedSection != SidebarSection.HOME
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
