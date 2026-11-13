package com.joudjawadtube.player.ui.playback

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.platform.LocalConfiguration
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
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.joudjawadtube.player.data.settings.AppSettingsState
import com.joudjawadtube.player.data.settings.RepeatMode
import com.joudjawadtube.player.data.settings.ResumeMode
import com.joudjawadtube.player.data.youtube.QualitySelectorHelper_Antigravity
import com.joudjawadtube.player.data.youtube.ResolvedYouTubeStream
import com.joudjawadtube.player.data.youtube.YouTubeChunkedHttpDataSource_Antigravity
import com.joudjawadtube.player.data.youtube.YouTubeService
import com.joudjawadtube.player.ui.catalog.PlaybackSession
import com.joudjawadtube.player.ui.theme.SmartTubeRed
import com.joudjawadtube.player.ui.theme.SmartTubeTextPrimary
import com.joudjawadtube.player.ui.theme.SmartTubeTextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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
    val configuration = LocalConfiguration.current
    val hostActivity = remember(context) { context.findActivity() }
    val video = session.currentVideo
    val seekDeltaMs = (settings.seekIntervalSec.coerceAtLeast(5)) * 1000L

    var isPlaying by remember { mutableStateOf(true) }
    var durationMs by remember(video.id) { mutableLongStateOf(video.durationMs.coerceAtLeast(1L)) }
    var videoStreamBadge by remember(video.id) { mutableStateOf<String?>(null) }
    var playbackSpeed by remember { mutableFloatStateOf(settings.defaultPlaybackSpeed) }
    var resizeMode by remember { mutableIntStateOf(settings.defaultResizeMode) }
    var currentRepeatMode by remember { mutableStateOf(settings.repeatMode) }
    var currentQuality by remember(video.id) { mutableStateOf(settings.preferredQuality) }

    var isResolvingYouTube by remember(video.id) { mutableStateOf(video.isYoutube) }
    var resolvedYouTubeStream by remember(video.id) { mutableStateOf<com.joudjawadtube.player.data.youtube.ResolvedYouTubeStream?>(null) }
    var observedTrackResolutions by remember(video.id) { mutableStateOf<List<Pair<Int, Int>>>(emptyList()) }
    var triedFallbackStreamStage by remember(video.id) { mutableIntStateOf(0) }
    var useWebViewYouTubeFallback by remember(video.id) { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val coroutineScope = rememberCoroutineScope()
    var hasAttemptedStreamRefresh by remember(video.id) { mutableStateOf(false) }
    var isRefreshingExpiredStream by remember(video.id) { mutableStateOf(false) }

    val latestVideo by rememberUpdatedState(video)
    val latestDurationMs by rememberUpdatedState(durationMs)
    val latestRepeatMode by rememberUpdatedState(currentRepeatMode)
    val latestHasNext by rememberUpdatedState(session.hasNext)
    val latestOnSaveProgress by rememberUpdatedState(onSaveProgress)
    val latestOnNextVideo by rememberUpdatedState(onNextVideo)
    val latestResolvedStream by rememberUpdatedState(resolvedYouTubeStream)
    val latestCurrentQuality by rememberUpdatedState(currentQuality)

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

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 15_000,
                /* maxBufferMs = */ 50_000,
                /* bufferForPlaybackMs = */ 300,
                /* bufferForPlaybackAfterRebufferMs = */ 800
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        ExoPlayer.Builder(context, renderersFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
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

    // Prepare ExoPlayer MediaSource from a resolved YouTube stream (initial, refresh, or quality change)
    val prepareYouTubeStreamSource: (ResolvedYouTubeStream, String, Long, Boolean) -> Boolean = remember(exoPlayer, context) {
        { resolved, requestedQuality, resumePos, applyPlaybackSpeed ->
            if (!resolved.hasDirectStream) {
                false
            } else {
                val httpFactory = YouTubeChunkedHttpDataSource_Antigravity.Factory(
                    userAgent = resolved.userAgent,
                    connectTimeoutMs = 6_000,
                    readTimeoutMs = 8_000
                )
                val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)
                val fastFailPolicy = DefaultLoadErrorHandlingPolicy(/* minimumLoadableRetryCount = */ 0)

                val preferredVariant = if (!requestedQuality.equals("Auto", ignoreCase = true) && resolved.qualityVariants.isNotEmpty()) {
                    val maxRank = QualitySelectorHelper_Antigravity.qualityRank(requestedQuality)
                    val matched = resolved.qualityVariants.firstOrNull {
                        it.label.equals(requestedQuality, ignoreCase = true) ||
                            QualitySelectorHelper_Antigravity.qualityRank(it.label) == maxRank
                    } ?: resolved.qualityVariants.firstOrNull {
                        QualitySelectorHelper_Antigravity.effectiveVerticalResolution(it.height, it.width) <= maxRank
                    } ?: resolved.qualityVariants.firstOrNull()
                    if (matched != null && resolved.qualityVariants.none { it.label.equals(requestedQuality, ignoreCase = true) }) {
                        currentQuality = matched.label
                    }
                    matched
                } else {
                    null
                }

                var prepared = true
                when {
                    preferredVariant != null && (!preferredVariant.isProgressive && !resolved.audioStreamUrl.isNullOrBlank()) -> {
                        triedFallbackStreamStage = 1
                        val vSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                            .setLoadErrorHandlingPolicy(fastFailPolicy)
                            .createMediaSource(MediaItem.fromUri(preferredVariant.videoUrl))
                        val aSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                            .setLoadErrorHandlingPolicy(fastFailPolicy)
                            .createMediaSource(MediaItem.fromUri(resolved.audioStreamUrl))
                        exoPlayer.setMediaSource(MergingMediaSource(vSource, aSource), resumePos)
                    }
                    preferredVariant != null && preferredVariant.isProgressive -> {
                        triedFallbackStreamStage = 3
                        val pSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                            .setLoadErrorHandlingPolicy(fastFailPolicy)
                            .createMediaSource(MediaItem.fromUri(preferredVariant.videoUrl))
                        exoPlayer.setMediaSource(pSource, resumePos)
                    }
                    !resolved.videoStreamUrl.isNullOrBlank() && !resolved.audioStreamUrl.isNullOrBlank() -> {
                        triedFallbackStreamStage = 1
                        val vSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                            .setLoadErrorHandlingPolicy(fastFailPolicy)
                            .createMediaSource(MediaItem.fromUri(resolved.videoStreamUrl))
                        val aSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                            .setLoadErrorHandlingPolicy(fastFailPolicy)
                            .createMediaSource(MediaItem.fromUri(resolved.audioStreamUrl))
                        exoPlayer.setMediaSource(MergingMediaSource(vSource, aSource), resumePos)
                    }
                    !resolved.hlsManifestUrl.isNullOrBlank() -> {
                        triedFallbackStreamStage = 2
                        val hlsSource = HlsMediaSource.Factory(dataSourceFactory)
                            .setLoadErrorHandlingPolicy(fastFailPolicy)
                            .createMediaSource(MediaItem.fromUri(resolved.hlsManifestUrl))
                        exoPlayer.setMediaSource(hlsSource, resumePos)
                    }
                    !resolved.progressiveUrl.isNullOrBlank() -> {
                        triedFallbackStreamStage = 3
                        val pSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                            .setLoadErrorHandlingPolicy(fastFailPolicy)
                            .createMediaSource(MediaItem.fromUri(resolved.progressiveUrl))
                        exoPlayer.setMediaSource(pSource, resumePos)
                    }
                    else -> prepared = false
                }
                if (prepared) {
                    if (applyPlaybackSpeed) {
                        exoPlayer.setPlaybackSpeed(playbackSpeed)
                    }
                    exoPlayer.prepare()
                    exoPlayer.playWhenReady = true
                }
                prepared
            }
        }
    }

    // Load or switch MediaItem when session.currentVideo changes
    LaunchedEffect(video.id, video.entity.uri) {
        useWebViewYouTubeFallback = false
        hasAttemptedStreamRefresh = false
        isRefreshingExpiredStream = false
        observedTrackResolutions = emptyList()
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

            if (!prepareYouTubeStreamSource(resolved, currentQuality, resumePos, true)) {
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

    // Pre-warm the next YouTube video in the playlist in the background for near-zero transition latency
    LaunchedEffect(video.id, session.currentIndex) {
        delay(2_500L)
        if (session.hasNext) {
            val nextVid = session.playlist.getOrNull(session.currentIndex + 1)
            if (nextVid != null && nextVid.isYoutube && nextVid.youtubeVideoId.isNotBlank()) {
                YouTubeService.preResolveStream(nextVid.youtubeVideoId)
            }
        }
    }

    // Cascade fallback (Stage 2 HLS -> Stage 3 Progressive -> Stage 4 WebView)
    val cascadeYouTubeFallback: (ResolvedYouTubeStream, Long) -> Unit = remember(exoPlayer, context) {
        { resolved, currentPos ->
            val httpFactory = YouTubeChunkedHttpDataSource_Antigravity.Factory(
                userAgent = resolved.userAgent,
                connectTimeoutMs = 6_000,
                readTimeoutMs = 8_000
            )
            val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)
            val fastFailPolicy = DefaultLoadErrorHandlingPolicy(/* minimumLoadableRetryCount = */ 0)

            when (triedFallbackStreamStage) {
                1 -> {
                    if (!resolved.hlsManifestUrl.isNullOrBlank()) {
                        triedFallbackStreamStage = 2
                        val hlsSource = HlsMediaSource.Factory(dataSourceFactory)
                            .setLoadErrorHandlingPolicy(fastFailPolicy)
                            .createMediaSource(MediaItem.fromUri(resolved.hlsManifestUrl))
                        exoPlayer.setMediaSource(hlsSource, currentPos)
                        exoPlayer.prepare()
                        exoPlayer.playWhenReady = true
                    } else if (!resolved.progressiveUrl.isNullOrBlank()) {
                        triedFallbackStreamStage = 3
                        val pSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                            .setLoadErrorHandlingPolicy(fastFailPolicy)
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
                            .setLoadErrorHandlingPolicy(fastFailPolicy)
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

    // Shared helper to refresh an expired/failed YouTube stream and resume seamlessly at resumePos
    val refreshAndResumeYouTubeStream: (Long, (ResolvedYouTubeStream?) -> Unit) -> Unit = remember(
        coroutineScope,
        prepareYouTubeStreamSource
    ) {
        { resumePos, onFallback ->
            if (!isRefreshingExpiredStream) {
                val expectedVideoId = latestVideo.youtubeVideoId
                val requestedQuality = latestCurrentQuality
                if (expectedVideoId.isBlank()) {
                    onFallback(null)
                } else {
                    isRefreshingExpiredStream = true
                    coroutineScope.launch {
                        try {
                            val fresh = YouTubeService.refreshStreamForPlayback(expectedVideoId)
                            if (latestVideo.youtubeVideoId == expectedVideoId) {
                                resolvedYouTubeStream = fresh
                                if (fresh.durationMs > 0L) {
                                    durationMs = fresh.durationMs
                                }
                                if (!prepareYouTubeStreamSource(fresh, requestedQuality, resumePos, true)) {
                                    onFallback(fresh)
                                }
                            }
                        } catch (_: Throwable) {
                            if (latestVideo.youtubeVideoId == expectedVideoId) {
                                onFallback(null)
                            }
                        } finally {
                            if (latestVideo.youtubeVideoId == expectedVideoId) {
                                isRefreshingExpiredStream = false
                            }
                        }
                    }
                }
            }
        }
    }

    // Dynamic error recovery: refresh expired/failing stream URL once before cascading fallbacks
    val tryNextYouTubeFallback: () -> Unit = remember(exoPlayer, refreshAndResumeYouTubeStream, cascadeYouTubeFallback) {
        {
            val resolved = latestResolvedStream
            val activeVideo = latestVideo
            if (activeVideo.isYoutube && resolved != null) {
                val currentPos = exoPlayer.currentPosition.coerceAtLeast(0L)
                val shouldAttemptRefresh = !hasAttemptedStreamRefresh &&
                    !isRefreshingExpiredStream &&
                    activeVideo.youtubeVideoId.isNotBlank() &&
                    (resolved.isExpired(safetyMarginSec = 300L) || currentPos > 5_000L)

                if (shouldAttemptRefresh) {
                    hasAttemptedStreamRefresh = true
                    refreshAndResumeYouTubeStream(currentPos) {
                        cascadeYouTubeFallback(resolved, currentPos)
                    }
                } else {
                    cascadeYouTubeFallback(resolved, currentPos)
                }
            }
        }
    }

    val latestTryNextYouTubeFallback by rememberUpdatedState(tryNextYouTubeFallback)

    // Attach ExoPlayer State and Error Listeners
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    videoStreamBadge = QualitySelectorHelper_Antigravity.formatQualityLabel(
                        height = videoSize.height,
                        width = videoSize.width
                    ).let { if (it.startsWith("4K")) "4K" else if (it.startsWith("2K")) "2K" else it }
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                val resolutions = mutableListOf<Pair<Int, Int>>()
                tracks.groups.forEach { group ->
                    if (group.type == C.TRACK_TYPE_VIDEO) {
                        for (i in 0 until group.length) {
                            if (!group.isTrackSupported(i)) continue
                            val fmt = group.getTrackFormat(i)
                            if (fmt.width > 0 || fmt.height > 0) {
                                resolutions.add(fmt.width to fmt.height)
                            }
                        }
                    }
                }
                if (resolutions.isNotEmpty()) {
                    observedTrackResolutions = resolutions.distinct()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    if (exoPlayer.duration > 0L) {
                        durationMs = exoPlayer.duration
                    }
                } else if (playbackState == Player.STATE_ENDED) {
                    val activeVideo = latestVideo
                    val activeDuration = latestDurationMs
                    latestOnSaveProgress(activeVideo.videoKey, activeDuration, activeDuration)
                    when (latestRepeatMode) {
                        RepeatMode.REPEAT_ONE -> {
                            // Handled by Player.REPEAT_MODE_ONE
                        }
                        RepeatMode.REPEAT_ALL -> {
                            latestOnNextVideo()
                        }
                        RepeatMode.OFF -> {
                            if (settings.autoPlayNext && latestHasNext) {
                                latestOnNextVideo()
                            }
                        }
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                if (latestVideo.isYoutube) {
                    latestTryNextYouTubeFallback()
                }
            }
        }

        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    // Release player and restore orientation when leaving PlaybackScreen completely
    DisposableEffect(Unit) {
        onDispose {
            hostActivity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
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

    val applyQualitySelection: (String) -> Unit = remember(exoPlayer, video, resolvedYouTubeStream, prepareYouTubeStreamSource) {
        { newQuality ->
            currentQuality = newQuality
            val (maxW, maxH) = when {
                newQuality.contains("4K", ignoreCase = true) || newQuality.contains("2160", ignoreCase = true) -> 3840 to 2160
                newQuality.contains("2K", ignoreCase = true) || newQuality.contains("1440", ignoreCase = true) -> 2560 to 1440
                newQuality.contains("1080", ignoreCase = true) -> 1920 to 1080
                newQuality.contains("720", ignoreCase = true) -> 1280 to 720
                newQuality.contains("480", ignoreCase = true) -> 854 to 480
                newQuality.contains("360", ignoreCase = true) -> 640 to 360
                newQuality.contains("240", ignoreCase = true) -> 426 to 240
                else -> Int.MAX_VALUE to Int.MAX_VALUE
            }
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .setMaxVideoSize(maxW, maxH)
                .build()

            val resolved = resolvedYouTubeStream
            if (video.isYoutube && resolved != null && (newQuality == "Auto" || resolved.qualityVariants.isNotEmpty())) {
                val currentPos = exoPlayer.currentPosition.coerceAtLeast(0L)
                prepareYouTubeStreamSource(resolved, newQuality, currentPos, false)
            }
        }
    }

    val performTogglePlayPause: () -> Unit = remember(
        useWebViewYouTubeFallback,
        isPlaying,
        exoPlayer,
        refreshAndResumeYouTubeStream
    ) {
        {
            if (useWebViewYouTubeFallback) {
                isPlaying = !isPlaying
                val js = if (isPlaying) "player.playVideo();" else "player.pauseVideo();"
                webViewRef?.evaluateJavascript(js, null)
            } else if (exoPlayer.isPlaying) {
                exoPlayer.pause()
            } else {
                val activeVideo = latestVideo
                val resolved = latestResolvedStream
                val needsStreamRenewal = activeVideo.isYoutube &&
                    activeVideo.youtubeVideoId.isNotBlank() &&
                    resolved?.isExpired(safetyMarginSec = 120L) == true

                if (needsStreamRenewal) {
                    refreshAndResumeYouTubeStream(exoPlayer.currentPosition.coerceAtLeast(0L)) { fresh ->
                        if (fresh != null) useWebViewYouTubeFallback = true else exoPlayer.play()
                    }
                } else {
                    exoPlayer.play()
                }
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

        if (isResolvingYouTube || isRefreshingExpiredStream) {
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

        val availableQualities = remember(
            resolvedYouTubeStream,
            observedTrackResolutions,
            video.entity.height,
            video.entity.width
        ) {
            QualitySelectorHelper_Antigravity.buildAvailableQualities(
                variants = resolvedYouTubeStream?.qualityVariants.orEmpty(),
                observedTrackResolutions = observedTrackResolutions,
                fallbackHeight = video.entity.height,
                fallbackWidth = video.entity.width
            )
        }
        val effectiveCurrentQuality = if (currentQuality in availableQualities) {
            currentQuality
        } else {
            "Auto"
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
            currentQuality = effectiveCurrentQuality,
            repeatMode = currentRepeatMode,
            onOpenQualityDialog = { activeDialogTab = PlayerDialogTab.QUALITY },
            onCycleRepeatMode = {
                currentRepeatMode = when (currentRepeatMode) {
                    RepeatMode.OFF -> RepeatMode.REPEAT_ONE
                    RepeatMode.REPEAT_ONE -> RepeatMode.REPEAT_ALL
                    RepeatMode.REPEAT_ALL -> RepeatMode.OFF
                }
            },
            onToggleOrientation = {
                val activity = hostActivity ?: return@SmartTubePlayerHud
                val currentlyLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                activity.requestedOrientation = if (currentlyLandscape) {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
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
                currentQuality = effectiveCurrentQuality,
                availableQualities = availableQualities,
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

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
