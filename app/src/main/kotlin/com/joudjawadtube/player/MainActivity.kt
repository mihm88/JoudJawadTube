package com.joudjawadtube.player

import android.Manifest
import android.app.PictureInPictureParams
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
import android.util.Rational
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joudjawadtube.player.data.local.VideoItem
import com.joudjawadtube.player.data.repository.RelatedVideosResolver_Antigravity
import com.joudjawadtube.player.data.settings.UiModePreference
import com.joudjawadtube.player.ui.catalog.CatalogAction
import com.joudjawadtube.player.ui.catalog.CatalogViewModel
import com.joudjawadtube.player.ui.catalog.TabletCatalogScreen
import com.joudjawadtube.player.ui.catalog.TvCatalogScreen
import com.joudjawadtube.player.ui.components.AddYouTubeLinkDialog
import com.joudjawadtube.player.ui.components.LocalWindowLayoutInfo
import com.joudjawadtube.player.ui.components.ParentalGateDialog_Antigravity
import com.joudjawadtube.player.ui.components.ProvideWindowLayoutInfo
import com.joudjawadtube.player.ui.components.QuickChannelsPlaylistsDialog_Antigravity
import com.joudjawadtube.player.ui.components.QuickCollectionTarget_Antigravity
import com.joudjawadtube.player.ui.components.SidebarSection
import com.joudjawadtube.player.ui.components.TvFolderPickerDialog
import com.joudjawadtube.player.ui.components.VideoOptionsDialog
import com.joudjawadtube.player.ui.playback.PlaybackScreen
import com.joudjawadtube.player.ui.theme.JoudJawadTubeTheme
import com.joudjawadtube.player.ui.theme.LiquidGlassRootContainer

class MainActivity : AppCompatActivity() {

    private val viewModel: CatalogViewModel by viewModels {
        (application as JoudJawadTubeApp).appContainer.catalogViewModelFactory
    }
    private var pendingIncomingUri by mutableStateOf<Uri?>(null)
    private var pendingIncomingShareText by mutableStateOf<String?>(null)
    private var isInPipMode by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIncomingVideoIntent(intent)

        val defaultIsTv = detectIsTelevision()

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val activePlaybackSession by viewModel.activePlaybackSession.collectAsStateWithLifecycle()
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

                    var pendingParentalAction by remember { mutableStateOf<(() -> Unit)?>(null) }
                    var lastParentUnlockAtMs by remember { mutableLongStateOf(0L) }

                    val runWithParentalGate: (() -> Unit) -> Unit = { action ->
                        val now = System.currentTimeMillis()
                        val withinUnlockWindow =
                            lastParentUnlockAtMs > 0L && (now - lastParentUnlockAtMs) < 120_000L
                        if (!uiState.settings.parentalGateEnabled || withinUnlockWindow) {
                            action()
                        } else {
                            pendingParentalAction = action
                        }
                    }

                    LaunchedEffect(
                        pendingIncomingUri,
                        pendingIncomingShareText,
                        uiState.settings.parentalGateEnabled
                    ) {
                        val incomingUri = pendingIncomingUri
                        if (incomingUri != null) {
                            pendingIncomingUri = null
                            runWithParentalGate {
                                viewModel.handleIncomingExternalOrYouTubeUri(incomingUri)
                            }
                        } else {
                            val incomingText = pendingIncomingShareText
                            if (!incomingText.isNullOrBlank()) {
                                pendingIncomingShareText = null
                                runWithParentalGate {
                                    viewModel.importYouTubeLink(incomingText, playImmediately = true)
                                }
                            }
                        }
                    }

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

                    var requestedNotificationPermission by rememberSaveable { mutableStateOf(false) }
                    val notificationPermissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { }

                    LaunchedEffect(uiState.settings.backgroundPlaybackEnabled) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            uiState.settings.backgroundPlaybackEnabled &&
                            !requestedNotificationPermission
                        ) {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                            if (!granted) {
                                requestedNotificationPermission = true
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
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
                        runWithParentalGate { viewModel.openFolderPickerDialog() }
                    }

                    val handleCatalogAction: (CatalogAction) -> Unit = { action ->
                        when (action) {
                            is CatalogAction.SelectSection -> {
                                if (action.section == SidebarSection.SETTINGS) {
                                    runWithParentalGate { viewModel.onAction(action) }
                                } else {
                                    viewModel.onAction(action)
                                }
                            }
                            CatalogAction.OpenAddYouTubeDialog,
                            CatalogAction.PickSafFolder,
                            is CatalogAction.DeletePlaylist,
                            is CatalogAction.DeleteChannel,
                            is CatalogAction.DownloadAndInstallUpdate,
                            is CatalogAction.InstallDownloadedApk -> {
                                runWithParentalGate { viewModel.onAction(action) }
                            }
                            CatalogAction.RequestReadPermission -> requestReadPermissionAction()
                            CatalogAction.RequestAllFilesAccess -> requestAllFilesAccessAction()
                            else -> viewModel.onAction(action)
                        }
                    }

                    // Hoisted lazy list / grid states for persistent scroll preservation across playback exits
                    val tvHomeListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
                    val tabletHomeListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
                    val tabletGridState = rememberSaveable(saver = LazyGridState.Saver) { LazyGridState() }

                    val layoutInfo = LocalWindowLayoutInfo.current
                    val useTvLayout = layoutInfo.isTv

                    LiquidGlassRootContainer(
                        enableModalBackdropBlur = activePlaybackSession == null,
                        modifier = Modifier
                            .fillMaxSize()
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
                                    onAction = handleCatalogAction
                                )
                            } else {
                                TabletCatalogScreen(
                                    uiState = uiState,
                                    lazyListState = tabletHomeListState,
                                    lazyGridState = tabletGridState,
                                    hasReadVideoPermission = hasReadPermission,
                                    hasAllFilesAccess = hasAllFilesAccess,
                                    onAction = handleCatalogAction
                                )
                            }
                        }

                        // Playback overlay when an active playback session is running
                        activePlaybackSession?.let { currentSession ->
                            PlaybackScreen(
                                session = currentSession,
                                settings = uiState.settings,
                                isOnline = uiState.isOnline,
                                isInPictureInPictureMode = isInPipMode,
                                onEnterPictureInPicture = if (
                                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                                    layoutInfo.isTouchDevice &&
                                    packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
                                ) {
                                    { enterPipMode() }
                                } else {
                                    null
                                },
                                onSaveProgress = { videoKey, posMs, durMs ->
                                    viewModel.savePlaybackProgress(videoKey, posMs, durMs)
                                },
                                onPreviousVideo = { viewModel.playPreviousInSession() },
                                onNextVideo = {
                                    val wrap = uiState.settings.repeatMode == com.joudjawadtube.player.data.settings.RepeatMode.REPEAT_ALL
                                    viewModel.playNextInSession(wrapAround = wrap)
                                },
                                onPlayRelated = { selected, queue -> viewModel.playFromRelated(selected, queue) },
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

                        var videoCardSourceTarget by remember { mutableStateOf<QuickCollectionTarget_Antigravity?>(null) }

                        // Video Options & Thumbnail Control Dialog
                        uiState.selectedVideoForOptions?.let { selectedVideo ->
                            val resolvedSource = remember(
                                selectedVideo,
                                uiState.snapshot.allVideos,
                                uiState.snapshot.savedPlaylists,
                                uiState.snapshot.savedChannels
                            ) {
                                RelatedVideosResolver_Antigravity.resolveSourceCollection(
                                    current = selectedVideo,
                                    allVideos = uiState.snapshot.allVideos,
                                    savedPlaylists = uiState.snapshot.savedPlaylists,
                                    savedChannels = uiState.snapshot.savedChannels
                                )
                            }
                            VideoOptionsDialog(
                                video = selectedVideo,
                                onPlay = { viewModel.openVideo(selectedVideo, uiState.filteredVideos) },
                                sourceLabel = resolvedSource?.first,
                                onOpenSourceCollection = resolvedSource?.let { (label, list) ->
                                    {
                                        videoCardSourceTarget = QuickCollectionTarget_Antigravity(
                                            title = label,
                                            subtitle = "${list.size} videos",
                                            videos = list
                                        )
                                    }
                                },
                                onToggleFavorite = { viewModel.toggleFavorite(selectedVideo) },
                                onFetchYouTubeThumbnail = {
                                    viewModel.forceYouTubeThumbnailForVideo(selectedVideo)
                                },
                                onExtractFrameAtFraction = { frac ->
                                    viewModel.extractCustomFrameThumbnailForVideo(selectedVideo, frac)
                                },
                                onDeleteVideo = if (selectedVideo.isYoutube) {
                                    { runWithParentalGate { viewModel.deleteYouTubeVideo(selectedVideo) } }
                                } else {
                                    null
                                },
                                onDismiss = { viewModel.closeVideoOptions() }
                            )
                        }

                        videoCardSourceTarget?.let { target ->
                            QuickChannelsPlaylistsDialog_Antigravity(
                                savedChannels = uiState.snapshot.savedChannels,
                                savedPlaylists = uiState.snapshot.savedPlaylists,
                                allVideos = uiState.snapshot.allVideos,
                                initialTarget = target,
                                onPlayVideo = { video, playlist -> viewModel.openVideo(video, playlist) },
                                onAddYouTubeClick = {
                                    runWithParentalGate { viewModel.openAddYouTubeDialog() }
                                },
                                onDismiss = { videoCardSourceTarget = null }
                            )
                        }

                        pendingParentalAction?.let { action ->
                            ParentalGateDialog_Antigravity(
                                onSuccess = {
                                    lastParentUnlockAtMs = System.currentTimeMillis()
                                    pendingParentalAction = null
                                    action()
                                },
                                onDismiss = {
                                    pendingParentalAction = null
                                }
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

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
    }

    private fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
        ) {
            runCatching {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                enterPictureInPictureMode(params)
            }
        }
    }

    private fun handleIncomingVideoIntent(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_VIEW -> {
                val dataUri = intent.data
                if (dataUri != null) {
                    intent.action = Intent.ACTION_MAIN
                    intent.data = null
                    pendingIncomingUri = dataUri
                }
            }
            Intent.ACTION_SEND -> {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (!sharedText.isNullOrBlank()) {
                    intent.action = Intent.ACTION_MAIN
                    intent.removeExtra(Intent.EXTRA_TEXT)
                    pendingIncomingShareText = sharedText
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
