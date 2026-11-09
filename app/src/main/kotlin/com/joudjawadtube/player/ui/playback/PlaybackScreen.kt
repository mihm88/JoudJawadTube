package com.joudjawadtube.player.ui.playback

import android.annotation.SuppressLint
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.joudjawadtube.player.data.settings.AppSettingsState
import com.joudjawadtube.player.data.settings.RepeatMode
import com.joudjawadtube.player.data.settings.ResumeMode
import com.joudjawadtube.player.data.youtube.YouTubeService
import com.joudjawadtube.player.ui.catalog.PlaybackSession
import com.joudjawadtube.player.ui.theme.SmartTubeRed
import com.joudjawadtube.player.ui.theme.SmartTubeTextPrimary
import com.joudjawadtube.player.ui.theme.SmartTubeTextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeoutOrNull

@SuppressLint("SetJavaScriptEnabled")
@OptIn(UnstableApi::class)
@Composable
fun PlaybackScreen(
    session: PlaybackSession,
    settings: AppSettingsState = AppSettingsState(),
    onSaveProgress: (videoKey: String, positionMs: Long, durationMs: Long) -> Unit,
    onPreviousVideo: () -> Unit,
    onNextVideo: () -> Unit,
    onToggleFavorite: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val video = session.currentVideo
    val seekDeltaMs = (settings.seekIntervalSec.coerceAtLeast(5)) * 1000L

    var isPlaying by remember { mutableStateOf(true) }
    var durationMs by remember(video.id) { mutableLongStateOf(video.durationMs.coerceAtLeast(1L)) }
    var videoStreamBadge by remember(video.id) { mutableStateOf<String?>(null) }
    var playbackSpeed by remember { mutableFloatStateOf(settings.defaultPlaybackSpeed) }
    var resizeMode by remember { mutableIntStateOf(settings.defaultResizeMode) }
    var currentRepeatMode by remember { mutableStateOf(settings.repeatMode) }
    var currentQuality by remember { mutableStateOf(settings.preferredQuality) }

    var isResolvingYouTube by remember(video.id) { mutableStateOf(video.isYoutube) }
    var resolvedYouTubeStream by remember(video.id) { mutableStateOf<com.joudjawadtube.player.data.youtube.ResolvedYouTubeStream?>(null) }
    var triedFallbackStreamStage by remember(video.id) { mutableIntStateOf(0) }
    var useWebViewYouTubeFallback by remember(video.id) { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    var hudVisible by remember { mutableStateOf(true) }
    val interactionSignal = remember { MutableSharedFlow<Unit>(extraBufferCapacity = 1) }
    var activeDialogTab by remember { mutableStateOf<PlayerDialogTab?>(null) }

    val exoPlayer = remember(context) {
        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            .setEnableDecoderFallback(true)

        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setTunnelingEnabled(false)
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !settings.subtitlesEnabledByDefault)
            )
        }

        ExoPlayer.Builder(context, renderersFactory)
            .setTrackSelector(trackSelector)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
    }

    // Load or switch MediaItem when session.currentVideo changes
    LaunchedEffect(video.id, video.entity.uri) {
        useWebViewYouTubeFallback = false
        val resumePos = if (settings.resumeMode == ResumeMode.ALWAYS_RESUME &&
            video.positionMs > 3_000L &&
            video.progressFraction < 0.95f
        ) {
            video.positionMs
        } else {
            0L
        }

        if (video.isYoutube && video.youtubeVideoId.isNotBlank()) {
            isResolvingYouTube = true
            triedFallbackStreamStage = 0
            val resolved = YouTubeService.resolveStreamForPlayback(video.youtubeVideoId)
            resolvedYouTubeStream = resolved
            isResolvingYouTube = false

            if (resolved.durationMs > 0L) {
                durationMs = resolved.durationMs
            }

            if (resolved.hasDirectStream) {
                val httpFactory = DefaultHttpDataSource.Factory()
                    .setUserAgent(resolved.userAgent)
                    .setConnectTimeoutMs(10_000)
                    .setReadTimeoutMs(12_000)
                    .setAllowCrossProtocolRedirects(true)
                val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)

                when {
                    !resolved.hlsManifestUrl.isNullOrBlank() -> {
                        triedFallbackStreamStage = 1
                        val hlsSource = HlsMediaSource.Factory(dataSourceFactory)
                            .createMediaSource(MediaItem.fromUri(resolved.hlsManifestUrl))
                        exoPlayer.setMediaSource(hlsSource, resumePos)
                    }
                    !resolved.videoStreamUrl.isNullOrBlank() && !resolved.audioStreamUrl.isNullOrBlank() -> {
                        triedFallbackStreamStage = 2
                        val vSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                            .createMediaSource(MediaItem.fromUri(resolved.videoStreamUrl))
                        val aSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                            .createMediaSource(MediaItem.fromUri(resolved.audioStreamUrl))
                        val merged = MergingMediaSource(vSource, aSource)
                        exoPlayer.setMediaSource(merged, resumePos)
                    }
                    !resolved.progressiveUrl.isNullOrBlank() -> {
                        triedFallbackStreamStage = 3
                        val pSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                            .createMediaSource(MediaItem.fromUri(resolved.progressiveUrl))
                        exoPlayer.setMediaSource(pSource, resumePos)
                    }
                }
                exoPlayer.setPlaybackSpeed(playbackSpeed)
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            } else {
                useWebViewYouTubeFallback = true
            }
        } else {
            isResolvingYouTube = false
            val subtitleConfigs = video.sidecarSubtitleList.mapIndexed { index, sub ->
                MediaItem.SubtitleConfiguration.Builder(Uri.parse(sub.uriOrPath))
                    .setMimeType(sub.mimeType)
                    .setLanguage(sub.language)
                    .setLabel("${sub.label} (External)")
                    .setSelectionFlags(if (index == 0 && settings.subtitlesEnabledByDefault) C.SELECTION_FLAG_DEFAULT else 0)
                    .build()
            }

            val mediaItem = MediaItem.Builder()
                .setUri(Uri.parse(video.entity.uri))
                .setSubtitleConfigurations(subtitleConfigs)
                .build()

            exoPlayer.setMediaItem(mediaItem, resumePos)
            exoPlayer.setPlaybackSpeed(playbackSpeed)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }
    }

    // Dynamic error recovery for YouTube streams
    val tryNextYouTubeFallback: () -> Unit = remember(video.youtubeVideoId, resolvedYouTubeStream) {
        {
            val resolved = resolvedYouTubeStream
            if (video.isYoutube && resolved != null) {
                val httpFactory = DefaultHttpDataSource.Factory()
                    .setUserAgent(resolved.userAgent)
                    .setConnectTimeoutMs(10_000)
                    .setReadTimeoutMs(12_000)
                    .setAllowCrossProtocolRedirects(true)
                val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)
                val currentPos = exoPlayer.currentPosition.coerceAtLeast(0L)

                when (triedFallbackStreamStage) {
                    1 -> {
                        if (!resolved.videoStreamUrl.isNullOrBlank() && !resolved.audioStreamUrl.isNullOrBlank()) {
                            triedFallbackStreamStage = 2
                            val vSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                                .createMediaSource(MediaItem.fromUri(resolved.videoStreamUrl))
                            val aSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                                .createMediaSource(MediaItem.fromUri(resolved.audioStreamUrl))
                            val merged = MergingMediaSource(vSource, aSource)
                            exoPlayer.setMediaSource(merged, currentPos)
                            exoPlayer.prepare()
                            exoPlayer.playWhenReady = true
                        } else if (!resolved.progressiveUrl.isNullOrBlank()) {
                            triedFallbackStreamStage = 3
                            val pSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                                .createMediaSource(MediaItem.fromUri(resolved.progressiveUrl))
                            exoPlayer.setMediaSource(pSource, currentPos)
                            exoPlayer.prepare()
                            exoPlayer.playWhenReady = true
                        } else {
                            triedFallbackStreamStage = 4
                            useWebViewYouTubeFallback = true
                        }
                    }
                    2 -> {
                        if (!resolved.progressiveUrl.isNullOrBlank()) {
                            triedFallbackStreamStage = 3
                            val pSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                                .createMediaSource(MediaItem.fromUri(resolved.progressiveUrl))
                            exoPlayer.setMediaSource(pSource, currentPos)
                            exoPlayer.prepare()
                            exoPlayer.playWhenReady = true
                        } else {
                            triedFallbackStreamStage = 4
                            useWebViewYouTubeFallback = true
                        }
                    }
                    else -> {
                        triedFallbackStreamStage = 4
                        useWebViewYouTubeFallback = true
                    }
                }
            }
        }
    }

    // Attach ExoPlayer State and Error Listeners
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    videoStreamBadge = when {
                        videoSize.height >= 2160 || videoSize.width >= 3840 -> "4K"
                        videoSize.height >= 1440 || videoSize.width >= 2560 -> "2K"
                        videoSize.height >= 1080 || videoSize.width >= 1920 -> "1080p"
                        videoSize.height >= 720 || videoSize.width >= 1280 -> "720p"
                        else -> "${videoSize.height}p"
                    }
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    if (exoPlayer.duration > 0L) {
                        durationMs = exoPlayer.duration
                    }
                } else if (playbackState == Player.STATE_ENDED) {
                    onSaveProgress(video.videoKey, durationMs, durationMs)
                    when (currentRepeatMode) {
                        RepeatMode.REPEAT_ONE -> {
                            // Handled by Player.REPEAT_MODE_ONE
                        }
                        RepeatMode.REPEAT_ALL -> {
                            onNextVideo()
                        }
                        RepeatMode.OFF -> {
                            if (settings.autoPlayNext && session.hasNext) {
                                onNextVideo()
                            }
                        }
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                if (video.isYoutube) {
                    tryNextYouTubeFallback()
                }
            }
        }

        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    // Release player when leaving PlaybackScreen completely
    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.destroy()
            exoPlayer.release()
        }
    }

    // Periodic progress saving (runs every 20s in background, no UI recomposition)
    LaunchedEffect(exoPlayer, video.videoKey, useWebViewYouTubeFallback) {
        if (useWebViewYouTubeFallback) return@LaunchedEffect
        while (isActive) {
            delay(20_000L)
            if (exoPlayer.isPlaying) {
                val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
                val dur = exoPlayer.duration.takeIf { it > 0L } ?: durationMs
                onSaveProgress(video.videoKey, pos, dur)
            }
        }
    }

    // Auto-hide OSD HUD after 4.5 seconds of inactivity while video is playing
    // Uses interactionSignal so moving focus between pills does NOT trigger recomposition
    LaunchedEffect(hudVisible, isPlaying, activeDialogTab) {
        if (hudVisible && isPlaying && activeDialogTab == null) {
            while (isActive) {
                val touched = withTimeoutOrNull(4_500L) {
                    interactionSignal.first()
                }
                if (touched == null) {
                    hudVisible = false
                    break
                }
            }
        }
    }

    val playerRootFocusRequester = remember { FocusRequester() }

    LaunchedEffect(hudVisible, activeDialogTab) {
        if (!hudVisible && activeDialogTab == null) {
            try {
                playerRootFocusRequester.requestFocus()
            } catch (_: Throwable) {}
        }
    }

    LaunchedEffect(currentRepeatMode) {
        exoPlayer.repeatMode = if (currentRepeatMode == RepeatMode.REPEAT_ONE) {
            Player.REPEAT_MODE_ONE
        } else {
            Player.REPEAT_MODE_OFF
        }
    }

    val applyQualitySelection: (String) -> Unit = remember(exoPlayer, video, resolvedYouTubeStream) {
        { newQuality ->
            currentQuality = newQuality
            val (maxW, maxH) = when {
                newQuality.contains("4K", ignoreCase = true) || newQuality.contains("2160", ignoreCase = true) -> 3840 to 2160
                newQuality.contains("2K", ignoreCase = true) || newQuality.contains("1440", ignoreCase = true) -> 2560 to 1440
                newQuality.contains("1080", ignoreCase = true) -> 1920 to 1080
                newQuality.contains("720", ignoreCase = true) -> 1280 to 720
                newQuality.contains("480", ignoreCase = true) -> 854 to 480
                newQuality.contains("360", ignoreCase = true) -> 640 to 360
                else -> Int.MAX_VALUE to Int.MAX_VALUE
            }
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .setMaxVideoSize(maxW, maxH)
                .build()

            val resolved = resolvedYouTubeStream
            if (video.isYoutube && resolved != null) {
                if (newQuality == "Auto" && !resolved.hlsManifestUrl.isNullOrBlank()) {
                    val currentPos = exoPlayer.currentPosition.coerceAtLeast(0L)
                    val httpFactory = DefaultHttpDataSource.Factory()
                        .setUserAgent(resolved.userAgent)
                        .setConnectTimeoutMs(10_000)
                        .setReadTimeoutMs(12_000)
                        .setAllowCrossProtocolRedirects(true)
                    val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)
                    val hlsSource = HlsMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(resolved.hlsManifestUrl))
                    exoPlayer.setMediaSource(hlsSource, currentPos)
                    exoPlayer.prepare()
                    exoPlayer.playWhenReady = true
                } else if (resolved.qualityVariants.isNotEmpty() && newQuality != "Auto") {
                    val match = resolved.qualityVariants.firstOrNull { it.label.contains(newQuality.take(4), ignoreCase = true) }
                        ?: resolved.qualityVariants.firstOrNull { it.height <= maxH }
                    if (match != null && match.videoUrl.isNotBlank()) {
                        val currentPos = exoPlayer.currentPosition.coerceAtLeast(0L)
                        val httpFactory = DefaultHttpDataSource.Factory()
                            .setUserAgent(resolved.userAgent)
                            .setConnectTimeoutMs(10_000)
                            .setReadTimeoutMs(12_000)
                            .setAllowCrossProtocolRedirects(true)
                        val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)

                        val newSource = if (match.isProgressive || resolved.audioStreamUrl.isNullOrBlank()) {
                            ProgressiveMediaSource.Factory(dataSourceFactory)
                                .createMediaSource(MediaItem.fromUri(match.videoUrl))
                        } else {
                            val vSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                                .createMediaSource(MediaItem.fromUri(match.videoUrl))
                            val aSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                                .createMediaSource(MediaItem.fromUri(resolved.audioStreamUrl))
                            MergingMediaSource(vSource, aSource)
                        }

                        exoPlayer.setMediaSource(newSource, currentPos)
                        exoPlayer.prepare()
                        exoPlayer.playWhenReady = true
                    }
                }
            }
        }
    }

    val performTogglePlayPause: () -> Unit = remember(useWebViewYouTubeFallback, isPlaying) {
        {
            if (useWebViewYouTubeFallback) {
                isPlaying = !isPlaying
                val js = if (isPlaying) "player.playVideo();" else "player.pauseVideo();"
                webViewRef?.evaluateJavascript(js, null)
            } else {
                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
            }
        }
    }

    val performRelativeSeek: (Long) -> Unit = remember(useWebViewYouTubeFallback, durationMs) {
        { deltaMs ->
            if (useWebViewYouTubeFallback) {
                val deltaSec = deltaMs / 1000
                webViewRef?.evaluateJavascript(
                    "if(window.player && player.getCurrentTime){ player.seekTo(Math.max(0, player.getCurrentTime() + ($deltaSec)), true); }",
                    null
                )
            } else {
                val target = (exoPlayer.currentPosition + deltaMs).coerceIn(0L, durationMs)
                exoPlayer.seekTo(target)
            }
        }
    }

    BackHandler {
        if (activeDialogTab != null) {
            activeDialogTab = null
            interactionSignal.tryEmit(Unit)
        } else if (hudVisible) {
            hudVisible = false
        } else {
            if (!useWebViewYouTubeFallback) {
                onSaveProgress(video.videoKey, exoPlayer.currentPosition, durationMs)
            }
            onBack()
        }
    }

    val resizeModeLabel = when (resizeMode) {
        AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> stringResource(com.joudjawadtube.player.R.string.player_aspect_zoom)
        AspectRatioFrameLayout.RESIZE_MODE_FILL -> stringResource(com.joudjawadtube.player.R.string.player_aspect_stretch)
        else -> stringResource(com.joudjawadtube.player.R.string.player_aspect_fit)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(playerRootFocusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && activeDialogTab == null) {
                    when (event.key) {
                        Key.Back, Key.Escape -> {
                            if (hudVisible) {
                                hudVisible = false
                                true
                            } else {
                                false
                            }
                        }
                        Key.MediaPlayPause, Key.Spacebar -> {
                            performTogglePlayPause()
                            hudVisible = true
                            interactionSignal.tryEmit(Unit)
                            true
                        }
                        Key.MediaFastForward -> {
                            performRelativeSeek(seekDeltaMs)
                            hudVisible = true
                            interactionSignal.tryEmit(Unit)
                            true
                        }
                        Key.MediaRewind -> {
                            performRelativeSeek(-seekDeltaMs)
                            hudVisible = true
                            interactionSignal.tryEmit(Unit)
                            true
                        }
                        Key.DirectionLeft -> {
                            if (!hudVisible) {
                                performRelativeSeek(-seekDeltaMs)
                                hudVisible = true
                                interactionSignal.tryEmit(Unit)
                                true
                            } else {
                                // HUD is visible: let focus move smoothly, NO recomposition
                                interactionSignal.tryEmit(Unit)
                                false
                            }
                        }
                        Key.DirectionRight -> {
                            if (!hudVisible) {
                                performRelativeSeek(seekDeltaMs)
                                hudVisible = true
                                interactionSignal.tryEmit(Unit)
                                true
                            } else {
                                // HUD is visible: let focus move smoothly, NO recomposition
                                interactionSignal.tryEmit(Unit)
                                false
                            }
                        }
                        Key.DirectionUp, Key.DirectionDown -> {
                            if (!hudVisible) {
                                hudVisible = true
                                interactionSignal.tryEmit(Unit)
                                true
                            } else {
                                interactionSignal.tryEmit(Unit)
                                false
                            }
                        }
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                            if (!hudVisible) {
                                hudVisible = true
                                interactionSignal.tryEmit(Unit)
                                true
                            } else {
                                interactionSignal.tryEmit(Unit)
                                false
                            }
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                hudVisible = !hudVisible
                interactionSignal.tryEmit(Unit)
            }
    ) {
        if (useWebViewYouTubeFallback && video.youtubeVideoId.isNotBlank()) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        setBackgroundColor(android.graphics.Color.BLACK)
                        isFocusable = false
                        isFocusableInTouchMode = false
                        this.settings.apply {
                            javaScriptEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            domStorageEnabled = true
                            cacheMode = WebSettings.LOAD_DEFAULT
                            userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
                        }
                        webChromeClient = WebChromeClient()
                        webViewClient = WebViewClient()
                        webViewRef = this
                        val html = buildYouTubeEmbedHtml(video.youtubeVideoId)
                        loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "UTF-8", null)
                    }
                },
                update = { wv ->
                    webViewRef = wv
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AndroidView(
                factory = { ctx ->
                    (android.view.LayoutInflater.from(ctx).inflate(
                        com.joudjawadtube.player.R.layout.player_texture_view_antigravity,
                        null,
                        false
                    ) as PlayerView).apply {
                        useController = false
                        keepScreenOn = true
                        isFocusable = false
                        this.resizeMode = resizeMode
                        player = exoPlayer
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { playerView ->
                    playerView.player = exoPlayer
                    playerView.resizeMode = resizeMode
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (isResolvingYouTube) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = SmartTubeRed,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Loading YouTube HD Stream...",
                    color = SmartTubeTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = video.title,
                    color = SmartTubeTextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        SmartTubePlayerHud(
            visible = hudVisible,
            video = video,
            isPlaying = isPlaying,
            positionProvider = { if (useWebViewYouTubeFallback) 0L else exoPlayer.currentPosition },
            durationMs = durationMs,
            seekIntervalSec = settings.seekIntervalSec,
            videoStreamBadge = videoStreamBadge,
            playbackSpeed = playbackSpeed,
            resizeModeLabel = resizeModeLabel,
            hasPrevious = session.hasPrevious,
            hasNext = session.hasNext,
            onPlayPauseToggle = performTogglePlayPause,
            onSeekTo = { position ->
                if (useWebViewYouTubeFallback) {
                    val sec = position / 1000
                    webViewRef?.evaluateJavascript(
                        "if(window.player && player.seekTo){ player.seekTo($sec, true); }",
                        null
                    )
                } else {
                    exoPlayer.seekTo(position)
                }
            },
            onSeekRelative = performRelativeSeek,
            onPreviousVideo = onPreviousVideo,
            onNextVideo = onNextVideo,
            onOpenSubtitles = { activeDialogTab = PlayerDialogTab.SUBTITLES },
            onOpenAudioTracks = { activeDialogTab = PlayerDialogTab.AUDIO },
            onOpenSpeedDialog = { activeDialogTab = PlayerDialogTab.SPEED },
            onCycleResizeMode = {
                resizeMode = when (resizeMode) {
                    AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            onToggleFavorite = onToggleFavorite,
            currentQuality = currentQuality,
            repeatMode = currentRepeatMode,
            onOpenQualityDialog = { activeDialogTab = PlayerDialogTab.QUALITY },
            onCycleRepeatMode = {
                currentRepeatMode = when (currentRepeatMode) {
                    RepeatMode.OFF -> RepeatMode.REPEAT_ONE
                    RepeatMode.REPEAT_ONE -> RepeatMode.REPEAT_ALL
                    RepeatMode.REPEAT_ALL -> RepeatMode.OFF
                }
            },
            onBack = {
                if (!useWebViewYouTubeFallback) {
                    onSaveProgress(video.videoKey, exoPlayer.currentPosition, durationMs)
                }
                onBack()
            },
            onUserInteraction = {
                interactionSignal.tryEmit(Unit)
            }
        )

        activeDialogTab?.let { tab ->
            TrackSelectorDialog(
                player = exoPlayer,
                initialTab = tab,
                currentSpeed = playbackSpeed,
                currentQuality = currentQuality,
                onSelectSpeed = { newSpeed ->
                    playbackSpeed = newSpeed
                    exoPlayer.setPlaybackSpeed(newSpeed)
                    webViewRef?.evaluateJavascript(
                        "if(window.player && player.setPlaybackRate){ player.setPlaybackRate($newSpeed); }",
                        null
                    )
                },
                onSelectQuality = applyQualitySelection,
                onDismiss = {
                    activeDialogTab = null
                    interactionSignal.tryEmit(Unit)
                }
            )
        }
    }
}

private fun buildYouTubeEmbedHtml(videoId: String): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
        <style>
            html, body { margin: 0; padding: 0; width: 100%; height: 100%; background-color: #000; overflow: hidden; }
            #player { width: 100%; height: 100%; border: 0; }
        </style>
        </head>
        <body>
        <div id="player"></div>
        <script src="https://www.youtube.com/iframe_api"></script>
        <script>
            var player;
            function onYouTubeIframeAPIReady() {
                player = new YT.Player('player', {
                    width: '100%',
                    height: '100%',
                    videoId: '$videoId',
                    playerVars: {
                        'autoplay': 1,
                        'controls': 0,
                        'rel': 0,
                        'playsinline': 1,
                        'modestbranding': 1,
                        'iv_load_policy': 3
                    },
                    events: {
                        'onReady': function(event) { event.target.playVideo(); }
                    }
                });
            }
        </script>
        </body>
        </html>
    """.trimIndent()
}
