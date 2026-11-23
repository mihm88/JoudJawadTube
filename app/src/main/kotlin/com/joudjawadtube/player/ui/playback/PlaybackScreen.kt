package com.joudjawadtube.player.ui.playback

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.joudjawadtube.player.R
import com.joudjawadtube.player.data.local.SidecarSubtitle
import com.joudjawadtube.player.data.local.VideoItem
import com.joudjawadtube.player.data.repository.RelatedTabMode
import com.joudjawadtube.player.data.settings.AppSettingsState
import com.joudjawadtube.player.data.settings.RepeatMode
import com.joudjawadtube.player.data.settings.ResumeMode
import com.joudjawadtube.player.data.settings.WatchTimerManager_Antigravity
import com.joudjawadtube.player.data.youtube.QualitySelectorHelper_Antigravity
import com.joudjawadtube.player.data.youtube.ResolvedYouTubeStream
import com.joudjawadtube.player.data.youtube.YouTubeChunkedHttpDataSource_Antigravity
import com.joudjawadtube.player.data.youtube.YouTubeService
import com.joudjawadtube.player.playback.JoudPlaybackBridge_Antigravity
import com.joudjawadtube.player.playback.JoudPlaybackService_Antigravity
import com.joudjawadtube.player.ui.catalog.PlaybackSession
import com.joudjawadtube.player.ui.components.ParentalGateDialog_Antigravity
import com.joudjawadtube.player.ui.theme.SmartTubeCardBg
import com.joudjawadtube.player.ui.theme.SmartTubeFocusBorder
import com.joudjawadtube.player.ui.theme.SmartTubeRed
import com.joudjawadtube.player.ui.theme.SmartTubeSurface
import com.joudjawadtube.player.ui.theme.SmartTubeTextPrimary
import com.joudjawadtube.player.ui.theme.SmartTubeTextSecondary
import kotlinx.coroutines.Job
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
    isOnline: Boolean = true,
    isInPictureInPictureMode: Boolean = false,
    onEnterPictureInPicture: (() -> Unit)? = null,
    onSaveProgress: (videoKey: String, positionMs: Long, durationMs: Long) -> Unit,
    onPreviousVideo: () -> Unit,
    onNextVideo: () -> Unit,
    onPlayRelated: (VideoItem, List<VideoItem>) -> Unit = { _, _ -> },
    onToggleFavorite: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val lifecycleOwner = LocalLifecycleOwner.current
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
    var playbackErrorResId by remember(video.id) { mutableStateOf<Int?>(null) }
    var playbackErrorDetail by remember(video.id) { mutableStateOf<String?>(null) }
    var retryNonce by remember(video.id) { mutableIntStateOf(0) }
    var wasOfflineDuringPlaybackError by remember(video.id) { mutableStateOf(false) }
    var disableSubtitlesForCurrentVideo by remember(video.id) { mutableStateOf(false) }

    val isTrustedSavedSource = remember(video.id, video.playlistId, video.entity.folderPath) {
        video.playlistId.isNotBlank() || video.entity.folderPath.startsWith("youtube_")
    }
    val allowGeneralForVideo = settings.allowGeneralFallbackForSavedChannels && isTrustedSavedSource

    var isScreenLocked by remember { mutableStateOf(false) }
    var showUnlockedBanner by remember { mutableStateOf(false) }
    var showExtendParentalGate by remember { mutableStateOf(false) }

    val watchTimerManager = WatchTimerManager_Antigravity.shared
    val isTimeUp by watchTimerManager.isTimeUp.collectAsState()

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
    val latestAllowWebViewFallback by rememberUpdatedState(settings.allowWebViewFallback)
    val latestAllowGeneralFallback by rememberUpdatedState(allowGeneralForVideo)
    val latestSubtitlesEnabledByDefault by rememberUpdatedState(settings.subtitlesEnabledByDefault)
    val latestDisableSubtitlesForCurrentVideo by rememberUpdatedState(disableSubtitlesForCurrentVideo)
    val latestIsOnline by rememberUpdatedState(isOnline)

    val handleYouTubePlaybackFailure: (ResolvedYouTubeStream?, String?) -> Unit = remember {
        { resolved, detail ->
            if (!latestIsOnline) {
                useWebViewYouTubeFallback = false
                playbackErrorResId = R.string.player_error_offline
                playbackErrorDetail = detail
                wasOfflineDuringPlaybackError = true
            } else {
                val canUseWebView = latestAllowWebViewFallback &&
                    YouTubeService.SAFE_VIDEO_ID_REGEX.matches(latestVideo.youtubeVideoId)
                if (canUseWebView) {
                    playbackErrorResId = null
                    playbackErrorDetail = null
                    useWebViewYouTubeFallback = true
                } else {
                    useWebViewYouTubeFallback = false
                    playbackErrorResId = if (resolved?.restrictionReason == "KIDS_RESTRICTED") {
                        R.string.player_error_kids_restricted
                    } else {
                        R.string.player_error_youtube
                    }
                    playbackErrorDetail = detail
                }
            }
        }
    }

    var hudVisible by remember { mutableStateOf(true) }
    var relatedRowVisible by remember(video.id) { mutableStateOf(false) }
    var initialRelatedTabMode by remember(video.id) { mutableStateOf(RelatedTabMode.CATEGORY) }
    val interactionSignal = remember { MutableSharedFlow<Unit>(extraBufferCapacity = 1) }
    var activeDialogTab by remember { mutableStateOf<PlayerDialogTab?>(null) }

    var activeExoPlayer by remember { mutableStateOf(JoudPlaybackBridge_Antigravity.activePlayer()) }
    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }

    val latestExoPlayer by rememberUpdatedState(activeExoPlayer)
    val latestIsInPip by rememberUpdatedState(isInPictureInPictureMode)
    val latestBackgroundPlaybackEnabled by rememberUpdatedState(settings.backgroundPlaybackEnabled)
    val latestUseWebViewFallback by rememberUpdatedState(useWebViewYouTubeFallback)

    // Bind MediaSessionService via MediaController so background playback & media notification work
    DisposableEffect(context) {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, JoudPlaybackService_Antigravity::class.java)
        )
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener(
            {
                activeExoPlayer = JoudPlaybackBridge_Antigravity.activePlayer()
            },
            ContextCompat.getMainExecutor(context)
        )
        onDispose {
            MediaController.releaseFuture(controllerFuture)
        }
    }

    // Prepare ExoPlayer MediaSource from a resolved YouTube stream (initial, refresh, or quality change)
    val prepareYouTubeStreamSource: (ResolvedYouTubeStream, String, Long, Boolean, Boolean) -> Boolean = remember(activeExoPlayer, context) {
        { resolved, requestedQuality, resumePos, applyPlaybackSpeed, includeSubtitles ->
            val exoPlayer = activeExoPlayer
            if (exoPlayer == null || !resolved.hasDirectStream) {
                false
            } else {
                val httpFactory = YouTubeChunkedHttpDataSource_Antigravity.Factory(
                    userAgent = resolved.userAgent,
                    connectTimeoutMs = 6_000,
                    readTimeoutMs = 8_000
                )
                val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)
                val fastFailPolicy = DefaultLoadErrorHandlingPolicy(/* minimumLoadableRetryCount = */ 3)
                val videoMediaMetadata = buildVideoMediaMetadata(latestVideo)
                val subtitleConfigs = if (includeSubtitles) {
                    selectPreferredSubtitleConfigs(resolved.subtitles, latestSubtitlesEnabledByDefault)
                } else {
                    emptyList()
                }
                fun createVideoSource(url: String): MediaSource {
                    val item = MediaItem.Builder()
                        .setMediaId(latestVideo.videoKey)
                        .setUri(url)
                        .setMediaMetadata(videoMediaMetadata)
                        .setSubtitleConfigurations(subtitleConfigs)
                        .build()
                    return if (subtitleConfigs.isEmpty()) {
                        ProgressiveMediaSource.Factory(dataSourceFactory)
                            .setLoadErrorHandlingPolicy(fastFailPolicy)
                            .createMediaSource(item)
                    } else {
                        DefaultMediaSourceFactory(context)
                            .setDataSourceFactory(dataSourceFactory)
                            .setLoadErrorHandlingPolicy(fastFailPolicy)
                            .createMediaSource(item)
                    }
                }
                fun taggedMediaItem(url: String): MediaItem = MediaItem.Builder()
                    .setMediaId(latestVideo.videoKey)
                    .setUri(url)
                    .setMediaMetadata(videoMediaMetadata)
                    .build()

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

                var finalSource: MediaSource? = null
                when {
                    preferredVariant != null && (!preferredVariant.isProgressive && !resolved.audioStreamUrl.isNullOrBlank()) -> {
                        triedFallbackStreamStage = 1
                        val vSource = createVideoSource(preferredVariant.videoUrl)
                        val aSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                            .setLoadErrorHandlingPolicy(fastFailPolicy)
                            .createMediaSource(MediaItem.fromUri(resolved.audioStreamUrl))
                        finalSource = MergingMediaSource(vSource, aSource)
                    }
                    preferredVariant != null && preferredVariant.isProgressive -> {
                        triedFallbackStreamStage = 3
                        finalSource = createVideoSource(preferredVariant.videoUrl)
                    }
                    !resolved.videoStreamUrl.isNullOrBlank() && !resolved.audioStreamUrl.isNullOrBlank() -> {
                        triedFallbackStreamStage = 1
                        val vSource = createVideoSource(resolved.videoStreamUrl)
                        val aSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                            .setLoadErrorHandlingPolicy(fastFailPolicy)
                            .createMediaSource(MediaItem.fromUri(resolved.audioStreamUrl))
                        finalSource = MergingMediaSource(vSource, aSource)
                    }
                    !resolved.hlsManifestUrl.isNullOrBlank() -> {
                        triedFallbackStreamStage = 2
                        finalSource = HlsMediaSource.Factory(dataSourceFactory)
                            .setLoadErrorHandlingPolicy(fastFailPolicy)
                            .createMediaSource(taggedMediaItem(resolved.hlsManifestUrl))
                    }
                    !resolved.progressiveUrl.isNullOrBlank() -> {
                        triedFallbackStreamStage = 3
                        finalSource = createVideoSource(resolved.progressiveUrl)
                    }
                }
                val prepared = finalSource != null
                if (finalSource != null) {
                    exoPlayer.setMediaSource(finalSource, resumePos)
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

    // Load or switch MediaItem when session.currentVideo changes, service binds, or user retries
    LaunchedEffect(activeExoPlayer, video.id, video.entity.uri, retryNonce) {
        val exoPlayer = activeExoPlayer ?: return@LaunchedEffect
        playbackErrorResId = null
        playbackErrorDetail = null
        wasOfflineDuringPlaybackError = false
        disableSubtitlesForCurrentVideo = false
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
            if (retryNonce > 0) {
                YouTubeService.invalidateStreamCache(video.youtubeVideoId)
            }
            val resolved = YouTubeService.resolveStreamForPlayback(
                videoId = video.youtubeVideoId,
                allowGeneralClientFallback = latestAllowGeneralFallback
            )
            resolvedYouTubeStream = resolved
            isResolvingYouTube = false

            if (resolved.durationMs > 0L) {
                durationMs = resolved.durationMs
            }

            if (!prepareYouTubeStreamSource(resolved, currentQuality, resumePos, true, !disableSubtitlesForCurrentVideo)) {
                handleYouTubePlaybackFailure(resolved, null)
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
                .setMediaId(video.videoKey)
                .setUri(Uri.parse(video.entity.uri))
                .setMediaMetadata(buildVideoMediaMetadata(video))
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
                YouTubeService.preResolveStream(
                    videoId = nextVid.youtubeVideoId,
                    allowGeneralClientFallback = latestAllowGeneralFallback
                )
            }
        }
    }

    // Cascade fallback (Stage 2 HLS -> Stage 3 Progressive -> Stage 4 WebView or Kid-Friendly Error)
    val cascadeYouTubeFallback: (ResolvedYouTubeStream, Long, String?) -> Unit = remember(activeExoPlayer, context) {
        { resolved, currentPos, detail ->
            val exoPlayer = activeExoPlayer
            if (exoPlayer == null) {
                handleYouTubePlaybackFailure(resolved, detail)
            } else {
                val httpFactory = YouTubeChunkedHttpDataSource_Antigravity.Factory(
                    userAgent = resolved.userAgent,
                    connectTimeoutMs = 6_000,
                    readTimeoutMs = 8_000
                )
                val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)
                val fastFailPolicy = DefaultLoadErrorHandlingPolicy(/* minimumLoadableRetryCount = */ 3)
                val metadata = buildVideoMediaMetadata(latestVideo)
                val subtitleConfigs = if (!latestDisableSubtitlesForCurrentVideo) {
                    selectPreferredSubtitleConfigs(resolved.subtitles, latestSubtitlesEnabledByDefault)
                } else {
                    emptyList()
                }
                fun taggedMediaItem(url: String, withSubtitles: Boolean = false): MediaItem = MediaItem.Builder()
                    .setMediaId(latestVideo.videoKey)
                    .setUri(url)
                    .setMediaMetadata(metadata)
                    .apply {
                        if (withSubtitles && subtitleConfigs.isNotEmpty()) {
                            setSubtitleConfigurations(subtitleConfigs)
                        }
                    }
                    .build()

                when (triedFallbackStreamStage) {
                    1 -> {
                        if (!resolved.hlsManifestUrl.isNullOrBlank()) {
                            triedFallbackStreamStage = 2
                            val hlsSource = HlsMediaSource.Factory(dataSourceFactory)
                                .setLoadErrorHandlingPolicy(fastFailPolicy)
                                .createMediaSource(taggedMediaItem(resolved.hlsManifestUrl))
                            exoPlayer.setMediaSource(hlsSource, currentPos)
                            exoPlayer.prepare()
                            exoPlayer.playWhenReady = true
                        } else if (!resolved.progressiveUrl.isNullOrBlank()) {
                            triedFallbackStreamStage = 3
                            val pItem = taggedMediaItem(resolved.progressiveUrl, withSubtitles = true)
                            val pSource = if (subtitleConfigs.isEmpty()) {
                                ProgressiveMediaSource.Factory(dataSourceFactory)
                                    .setLoadErrorHandlingPolicy(fastFailPolicy)
                                    .createMediaSource(pItem)
                            } else {
                                DefaultMediaSourceFactory(context)
                                    .setDataSourceFactory(dataSourceFactory)
                                    .setLoadErrorHandlingPolicy(fastFailPolicy)
                                    .createMediaSource(pItem)
                            }
                            exoPlayer.setMediaSource(pSource, currentPos)
                            exoPlayer.prepare()
                            exoPlayer.playWhenReady = true
                        } else {
                            triedFallbackStreamStage = 4
                            handleYouTubePlaybackFailure(resolved, detail)
                        }
                    }
                    2 -> {
                        if (!resolved.progressiveUrl.isNullOrBlank()) {
                            triedFallbackStreamStage = 3
                            val pItem = taggedMediaItem(resolved.progressiveUrl, withSubtitles = true)
                            val pSource = if (subtitleConfigs.isEmpty()) {
                                ProgressiveMediaSource.Factory(dataSourceFactory)
                                    .setLoadErrorHandlingPolicy(fastFailPolicy)
                                    .createMediaSource(pItem)
                            } else {
                                DefaultMediaSourceFactory(context)
                                    .setDataSourceFactory(dataSourceFactory)
                                    .setLoadErrorHandlingPolicy(fastFailPolicy)
                                    .createMediaSource(pItem)
                            }
                            exoPlayer.setMediaSource(pSource, currentPos)
                            exoPlayer.prepare()
                            exoPlayer.playWhenReady = true
                        } else {
                            triedFallbackStreamStage = 4
                            handleYouTubePlaybackFailure(resolved, detail)
                        }
                    }
                    else -> {
                        triedFallbackStreamStage = 4
                        handleYouTubePlaybackFailure(resolved, detail)
                    }
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
                            val fresh = YouTubeService.refreshStreamForPlayback(
                                videoId = expectedVideoId,
                                allowGeneralClientFallback = latestAllowGeneralFallback
                            )
                            if (latestVideo.youtubeVideoId == expectedVideoId) {
                                resolvedYouTubeStream = fresh
                                if (fresh.durationMs > 0L) {
                                    durationMs = fresh.durationMs
                                }
                                if (!prepareYouTubeStreamSource(fresh, requestedQuality, resumePos, true, !latestDisableSubtitlesForCurrentVideo)) {
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

    // Dynamic error recovery: retry without subtitles first, then refresh/cascade fallbacks
    val tryNextYouTubeFallback: (String?) -> Unit = remember(activeExoPlayer, refreshAndResumeYouTubeStream, cascadeYouTubeFallback, prepareYouTubeStreamSource) {
        { detail ->
            val resolved = latestResolvedStream
            val activeVideo = latestVideo
            val exoPlayer = activeExoPlayer
            if (activeVideo.isYoutube && resolved != null) {
                val currentPos = exoPlayer?.currentPosition?.coerceAtLeast(0L) ?: 0L
                var handledWithoutSubtitles = false
                if (!latestDisableSubtitlesForCurrentVideo && resolved.subtitles.isNotEmpty()) {
                    disableSubtitlesForCurrentVideo = true
                    handledWithoutSubtitles = prepareYouTubeStreamSource(resolved, latestCurrentQuality, currentPos, false, false)
                }
                if (!handledWithoutSubtitles) {
                    val shouldAttemptRefresh = !hasAttemptedStreamRefresh &&
                        !isRefreshingExpiredStream &&
                        activeVideo.youtubeVideoId.isNotBlank() &&
                        (resolved.isExpired(safetyMarginSec = 300L) || currentPos > 5_000L)

                    if (shouldAttemptRefresh) {
                        hasAttemptedStreamRefresh = true
                        refreshAndResumeYouTubeStream(currentPos) { fresh ->
                            cascadeYouTubeFallback(fresh ?: resolved, currentPos, detail)
                        }
                    } else {
                        cascadeYouTubeFallback(resolved, currentPos, detail)
                    }
                }
            } else if (activeVideo.isYoutube) {
                handleYouTubePlaybackFailure(null, detail)
            }
        }
    }

    val latestTryNextYouTubeFallback by rememberUpdatedState(tryNextYouTubeFallback)

    // Attach ExoPlayer State and Error Listeners to service-owned ExoPlayer
    DisposableEffect(activeExoPlayer) {
        val exoPlayer = activeExoPlayer ?: return@DisposableEffect onDispose {}
        isPlaying = exoPlayer.isPlaying
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
                    latestTryNextYouTubeFallback(error.localizedMessage)
                } else {
                    playbackErrorResId = R.string.player_error_local
                    playbackErrorDetail = error.localizedMessage
                }
            }
        }

        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    // Lifecycle transitions for background playback, surface detachment, PiP, and WebView pause/resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    if (!latestUseWebViewFallback) {
                        playerViewRef?.player = latestExoPlayer
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (latestUseWebViewFallback) {
                        webViewRef?.onResume()
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    if (latestUseWebViewFallback) {
                        webViewRef?.onPause()
                    }
                }
                Lifecycle.Event.ON_STOP -> {
                    if (!latestUseWebViewFallback) {
                        latestExoPlayer?.let { player ->
                            latestOnSaveProgress(
                                latestVideo.videoKey,
                                player.currentPosition.coerceAtLeast(0L),
                                latestDurationMs
                            )
                        }
                    }
                    if (!latestIsInPip) {
                        if (latestBackgroundPlaybackEnabled) {
                            playerViewRef?.player = null
                        } else {
                            latestExoPlayer?.pause()
                        }
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Do NOT release service-owned ExoPlayer on dispose; detach surface and clean up WebView/orientation
    DisposableEffect(Unit) {
        onDispose {
            hostActivity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            if (!latestBackgroundPlaybackEnabled) {
                latestExoPlayer?.pause()
            }
            playerViewRef?.player = null
            playerViewRef = null
            webViewRef?.let { wv ->
                wv.stopLoading()
                wv.loadUrl("about:blank")
                wv.removeAllViews()
                wv.destroy()
                webViewRef = null
            }
        }
    }

    // Periodic progress saving (runs every 20s in background, no UI recomposition)
    LaunchedEffect(activeExoPlayer, video.videoKey, useWebViewYouTubeFallback) {
        val exoPlayer = activeExoPlayer ?: return@LaunchedEffect
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
    // Pauses while the related category row is open so users can browse cards comfortably
    LaunchedEffect(hudVisible, isPlaying, activeDialogTab, relatedRowVisible) {
        if (hudVisible && isPlaying && activeDialogTab == null && !relatedRowVisible) {
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

    LaunchedEffect(activeExoPlayer, currentRepeatMode) {
        val exoPlayer = activeExoPlayer ?: return@LaunchedEffect
        exoPlayer.repeatMode = if (currentRepeatMode == RepeatMode.REPEAT_ONE) {
            Player.REPEAT_MODE_ONE
        } else {
            Player.REPEAT_MODE_OFF
        }
    }

    LaunchedEffect(activeExoPlayer, settings.subtitlesEnabledByDefault) {
        val exoPlayer = activeExoPlayer ?: return@LaunchedEffect
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !settings.subtitlesEnabledByDefault)
            .build()
    }

    // T09 Auto-Reconnect: automatically refresh/retry YouTube playback when connectivity returns
    LaunchedEffect(isOnline, playbackErrorResId, video.id) {
        if (video.isYoutube && playbackErrorResId != null) {
            if (!isOnline) {
                wasOfflineDuringPlaybackError = true
            } else if (wasOfflineDuringPlaybackError) {
                wasOfflineDuringPlaybackError = false
                playbackErrorResId = null
                playbackErrorDetail = null
                val currentPos = activeExoPlayer?.currentPosition?.coerceAtLeast(0L) ?: 0L
                if (currentPos > 3_000L && resolvedYouTubeStream != null) {
                    refreshAndResumeYouTubeStream(currentPos) { fresh ->
                        handleYouTubePlaybackFailure(fresh, null)
                    }
                } else {
                    retryNonce++
                }
            }
        }
    }

    // T12 Watch Timer: sync configured limit, tick every second while playing, and pause on time-up
    LaunchedEffect(settings.watchTimeLimitMinutes) {
        watchTimerManager.setLimitMinutes(settings.watchTimeLimitMinutes)
    }

    LaunchedEffect(isPlaying, settings.watchTimeLimitMinutes, useWebViewYouTubeFallback, isTimeUp) {
        if (settings.watchTimeLimitMinutes > 0 && !isTimeUp) {
            while (isActive) {
                delay(1_000L)
                watchTimerManager.onPlaybackTick(deltaSeconds = 1L, isPlaying = isPlaying)
            }
        }
    }

    LaunchedEffect(isTimeUp) {
        if (isTimeUp) {
            hudVisible = false
            relatedRowVisible = false
            activeDialogTab = null
            if (useWebViewYouTubeFallback) {
                isPlaying = false
                webViewRef?.evaluateJavascript(
                    "if(window.player && player.pauseVideo){ player.pauseVideo(); }",
                    null
                )
            } else {
                activeExoPlayer?.pause()
            }
        }
    }

    LaunchedEffect(showUnlockedBanner) {
        if (showUnlockedBanner) {
            delay(2_200L)
            showUnlockedBanner = false
        }
    }

    val applyQualitySelection: (String) -> Unit = remember(activeExoPlayer, video, resolvedYouTubeStream, prepareYouTubeStreamSource) {
        { newQuality ->
            currentQuality = newQuality
            val exoPlayer = activeExoPlayer
            if (exoPlayer != null) {
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
                    prepareYouTubeStreamSource(resolved, newQuality, currentPos, false, !latestDisableSubtitlesForCurrentVideo)
                }
            }
        }
    }

    val performTogglePlayPause: () -> Unit = remember(
        useWebViewYouTubeFallback,
        isPlaying,
        activeExoPlayer,
        isTimeUp,
        refreshAndResumeYouTubeStream
    ) {
        {
            if (isTimeUp) {
                // Keep paused until parent extends watch time
            } else {
                val exoPlayer = activeExoPlayer
                if (useWebViewYouTubeFallback) {
                    isPlaying = !isPlaying
                    val js = if (isPlaying) "player.playVideo();" else "player.pauseVideo();"
                    webViewRef?.evaluateJavascript(js, null)
                } else if (exoPlayer != null) {
                    if (exoPlayer.isPlaying) {
                        exoPlayer.pause()
                    } else {
                        val activeVideo = latestVideo
                        val resolved = latestResolvedStream
                        val needsStreamRenewal = activeVideo.isYoutube &&
                            activeVideo.youtubeVideoId.isNotBlank() &&
                            resolved?.isExpired(safetyMarginSec = 120L) == true

                        if (needsStreamRenewal) {
                            refreshAndResumeYouTubeStream(exoPlayer.currentPosition.coerceAtLeast(0L)) { fresh ->
                                handleYouTubePlaybackFailure(fresh, null)
                            }
                        } else {
                            exoPlayer.play()
                        }
                    }
                }
            }
        }
    }

    val performRelativeSeek: (Long) -> Unit = remember(useWebViewYouTubeFallback, activeExoPlayer, durationMs) {
        { deltaMs ->
            if (useWebViewYouTubeFallback) {
                val deltaSec = deltaMs / 1000
                webViewRef?.evaluateJavascript(
                    "if(window.player && player.getCurrentTime){ player.seekTo(Math.max(0, player.getCurrentTime() + ($deltaSec)), true); }",
                    null
                )
            } else {
                activeExoPlayer?.let { exoPlayer ->
                    val target = (exoPlayer.currentPosition + deltaMs).coerceIn(0L, durationMs)
                    exoPlayer.seekTo(target)
                }
            }
        }
    }

    val exitPlayback: () -> Unit = {
        if (!useWebViewYouTubeFallback) {
            activeExoPlayer?.let { exoPlayer ->
                onSaveProgress(video.videoKey, exoPlayer.currentPosition.coerceAtLeast(0L), durationMs)
                exoPlayer.pause()
                exoPlayer.stop()
            }
        }
        onBack()
    }

    BackHandler(enabled = isScreenLocked && !isTimeUp) {
        // Consume Back while Kid Screen Lock is active so accidental taps/presses cannot exit playback
    }

    BackHandler(enabled = !isScreenLocked || isTimeUp) {
        if (showExtendParentalGate) {
            showExtendParentalGate = false
        } else if (isTimeUp) {
            exitPlayback()
        } else if (activeDialogTab != null) {
            activeDialogTab = null
            interactionSignal.tryEmit(Unit)
        } else if (hudVisible && !isInPictureInPictureMode) {
            hudVisible = false
        } else {
            exitPlayback()
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
                if (isTimeUp) {
                    false
                } else if (isScreenLocked) {
                    // Allow D-Pad Center/Enter through to the corner Lock pill for 2.5s hold-to-unlock
                    when (event.key) {
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> false
                        else -> true
                    }
                } else if (event.type == KeyEventType.KeyDown && activeDialogTab == null) {
                    when (event.key) {
                        Key.Back, Key.Escape -> {
                            if (relatedRowVisible) {
                                relatedRowVisible = false
                                interactionSignal.tryEmit(Unit)
                                true
                            } else if (hudVisible) {
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
                enabled = !isScreenLocked && !isTimeUp,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (hudVisible) {
                    relatedRowVisible = false
                    hudVisible = false
                } else {
                    hudVisible = true
                }
                interactionSignal.tryEmit(Unit)
            }
    ) {
        if (useWebViewYouTubeFallback &&
            settings.allowWebViewFallback &&
            YouTubeService.SAFE_VIDEO_ID_REGEX.matches(video.youtubeVideoId)
        ) {
            key(video.youtubeVideoId) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            val webView = this
                            setBackgroundColor(android.graphics.Color.BLACK)
                            isFocusable = false
                            isFocusableInTouchMode = false
                            CookieManager.getInstance().apply {
                                setAcceptCookie(false)
                                setAcceptThirdPartyCookies(webView, false)
                            }
                            this.settings.apply {
                                javaScriptEnabled = true
                                mediaPlaybackRequiresUserGesture = false
                                domStorageEnabled = false
                                allowFileAccess = false
                                allowContentAccess = false
                                cacheMode = WebSettings.LOAD_DEFAULT
                                userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
                            }
                            webChromeClient = WebChromeClient()
                            webViewClient = object : WebViewClient() {
                                private fun isAllowedEmbedUrl(rawUrl: String?): Boolean {
                                    val uri = rawUrl?.let { runCatching { Uri.parse(it) }.getOrNull() } ?: return false
                                    if (!uri.scheme.equals("https", ignoreCase = true)) return false
                                    val host = uri.host?.lowercase() ?: return false
                                    val path = uri.path ?: ""
                                    val allowedHost = host == "www.youtube-nocookie.com" ||
                                        host == "youtube-nocookie.com" ||
                                        host == "www.youtube.com" ||
                                        host == "youtube.com"
                                    return allowedHost && path.startsWith("/embed/")
                                }

                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean = !isAllowedEmbedUrl(request?.url?.toString())

                                @Deprecated("Deprecated in Java")
                                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean =
                                    !isAllowedEmbedUrl(url)
                            }
                            webViewRef = this
                            val html = buildYouTubeEmbedHtml(video.youtubeVideoId)
                            loadDataWithBaseURL(
                                "https://www.youtube-nocookie.com",
                                html,
                                "text/html",
                                "UTF-8",
                                null
                            )
                        }
                    },
                    update = { wv ->
                        webViewRef = wv
                    },
                    onRelease = { wv ->
                        wv.stopLoading()
                        wv.loadUrl("about:blank")
                        wv.removeAllViews()
                        wv.destroy()
                        if (webViewRef === wv) {
                            webViewRef = null
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
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
                        player = activeExoPlayer
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        playerViewRef = this
                    }
                },
                update = { playerView ->
                    playerViewRef = playerView
                    if (playerView.player !== activeExoPlayer) {
                        playerView.player = activeExoPlayer
                    }
                    playerView.resizeMode = resizeMode
                },
                onRelease = { playerView ->
                    playerView.player = null
                    if (playerViewRef === playerView) {
                        playerViewRef = null
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (!isInPictureInPictureMode && (isResolvingYouTube || isRefreshingExpiredStream)) {
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
                    text = stringResource(R.string.player_loading_stream),
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = video.title,
                    color = Color(0xFFD1DAE6),
                    fontSize = 12.sp
                )
            }
        }

        val activeErrorResId = playbackErrorResId
        if (!isInPictureInPictureMode &&
            !isScreenLocked &&
            !isTimeUp &&
            activeErrorResId != null &&
            !isResolvingYouTube &&
            !isRefreshingExpiredStream
        ) {
            PlaybackErrorOverlay(
                errorResId = activeErrorResId,
                errorDetail = playbackErrorDetail,
                onRetry = { retryNonce++ },
                onBack = exitPlayback
            )
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
            visible = hudVisible && !isInPictureInPictureMode && !isScreenLocked && !isTimeUp,
            video = video,
            isPlaying = isPlaying,
            positionProvider = { if (useWebViewYouTubeFallback) 0L else activeExoPlayer?.currentPosition ?: 0L },
            durationMs = durationMs,
            seekIntervalSec = settings.seekIntervalSec,
            videoStreamBadge = videoStreamBadge,
            hasPrevious = session.hasPrevious,
            hasNext = session.hasNext,
            relatedVideos = session.related,
            relatedRowVisible = relatedRowVisible,
            initialRelatedTabMode = initialRelatedTabMode,
            onShowRelatedRow = { tabMode ->
                initialRelatedTabMode = tabMode
                relatedRowVisible = true
                interactionSignal.tryEmit(Unit)
            },
            onHideRelatedRow = {
                relatedRowVisible = false
                interactionSignal.tryEmit(Unit)
            },
            onSelectRelatedVideo = { selected, queue ->
                relatedRowVisible = false
                if (!useWebViewYouTubeFallback) {
                    activeExoPlayer?.let { player ->
                        onSaveProgress(video.videoKey, player.currentPosition, durationMs)
                    }
                }
                onPlayRelated(selected, queue)
            },
            onPlayPauseToggle = performTogglePlayPause,
            onSeekTo = { position ->
                if (useWebViewYouTubeFallback) {
                    val sec = position / 1000
                    webViewRef?.evaluateJavascript(
                        "if(window.player && player.seekTo){ player.seekTo($sec, true); }",
                        null
                    )
                } else {
                    activeExoPlayer?.seekTo(position)
                }
            },
            onSeekRelative = performRelativeSeek,
            onPreviousVideo = onPreviousVideo,
            onNextVideo = onNextVideo,
            onToggleFavorite = onToggleFavorite,
            onOpenPlayerOptions = { activeDialogTab = PlayerDialogTab.MENU },
            onToggleOrientation = {
                val activity = hostActivity ?: return@SmartTubePlayerHud
                val currentlyLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                activity.requestedOrientation = if (currentlyLandscape) {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                }
            },
            onLockScreen = {
                isScreenLocked = true
                hudVisible = false
                relatedRowVisible = false
                activeDialogTab = null
            },
            onEnterPictureInPicture = if (!useWebViewYouTubeFallback) onEnterPictureInPicture else null,
            onBack = exitPlayback,
            onUserInteraction = {
                interactionSignal.tryEmit(Unit)
            }
        )

        if (!isInPictureInPictureMode && !isScreenLocked && !isTimeUp) {
            activeDialogTab?.let { tab ->
                activeExoPlayer?.let { exoPlayer ->
                    TrackSelectorDialog(
                        player = exoPlayer,
                        initialTab = tab,
                        currentSpeed = playbackSpeed,
                        currentQuality = effectiveCurrentQuality,
                        actualDisplayedQuality = videoStreamBadge,
                        availableQualities = availableQualities,
                        currentResizeMode = resizeMode,
                        currentRepeatMode = currentRepeatMode,
                        onSelectSpeed = { newSpeed ->
                            playbackSpeed = newSpeed
                            exoPlayer.setPlaybackSpeed(newSpeed)
                            webViewRef?.evaluateJavascript(
                                "if(window.player && player.setPlaybackRate){ player.setPlaybackRate($newSpeed); }",
                                null
                            )
                        },
                        onSelectQuality = applyQualitySelection,
                        onSelectResizeMode = { newMode -> resizeMode = newMode },
                        onSelectRepeatMode = { newRepeat -> currentRepeatMode = newRepeat },
                        onDismiss = {
                            activeDialogTab = null
                            interactionSignal.tryEmit(Unit)
                        }
                    )
                }
            }
        }

        if (isScreenLocked && !isInPictureInPictureMode && !isTimeUp) {
            KidScreenLockOverlay(
                onUnlocked = {
                    isScreenLocked = false
                    showUnlockedBanner = true
                    hudVisible = true
                    interactionSignal.tryEmit(Unit)
                }
            )
        }

        if (showUnlockedBanner && !isInPictureInPictureMode) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = SmartTubeSurface.copy(alpha = 0.92f),
                border = BorderStroke(1.dp, SmartTubeFocusBorder.copy(alpha = 0.45f)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .systemBarsPadding()
                    .padding(top = 18.dp)
            ) {
                Text(
                    text = stringResource(R.string.kid_lock_unlocked),
                    color = SmartTubeTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }

        if (isTimeUp && !isInPictureInPictureMode) {
            WatchTimerTimesUpOverlay(
                onExtendClick = { showExtendParentalGate = true },
                onBack = exitPlayback
            )
        }

        if (showExtendParentalGate) {
            ParentalGateDialog_Antigravity(
                onSuccess = {
                    showExtendParentalGate = false
                    watchTimerManager.extendByMinutes(15)
                    if (useWebViewYouTubeFallback) {
                        isPlaying = true
                        webViewRef?.evaluateJavascript(
                            "if(window.player && player.playVideo){ player.playVideo(); }",
                            null
                        )
                    } else {
                        activeExoPlayer?.play()
                    }
                },
                onDismiss = {
                    showExtendParentalGate = false
                }
            )
        }
    }
}

private fun buildVideoMediaMetadata(video: VideoItem): MediaMetadata {
    val builder = MediaMetadata.Builder()
        .setTitle(video.title)
        .setArtist(video.playlistTitle.ifBlank { video.folderName })
    val thumb = video.entity.thumbnailUrl.ifBlank { null }
    if (thumb != null) {
        runCatching { Uri.parse(thumb) }.getOrNull()?.let { builder.setArtworkUri(it) }
    }
    return builder.build()
}

private fun selectPreferredSubtitleConfigs(
    subtitles: List<SidecarSubtitle>,
    subtitlesEnabledByDefault: Boolean
): List<MediaItem.SubtitleConfiguration> {
    if (subtitles.isEmpty()) return emptyList()
    val valid = subtitles.filter {
        it.uriOrPath.startsWith("https://", ignoreCase = true) ||
            it.uriOrPath.startsWith("http://", ignoreCase = true)
    }
    if (valid.isEmpty()) return emptyList()

    val prioritized = valid.sortedBy { sub ->
        val lang = sub.language.lowercase(java.util.Locale.ROOT)
        when {
            lang.startsWith("ar") -> 0
            lang.startsWith("en") -> 1
            else -> 2
        }
    }.take(4)

    return prioritized.mapIndexed { index, sub ->
        val mime = when {
            sub.mimeType.equals("text/vtt", ignoreCase = true) -> MimeTypes.TEXT_VTT
            else -> sub.mimeType.ifBlank { MimeTypes.TEXT_VTT }
        }
        MediaItem.SubtitleConfiguration.Builder(Uri.parse(sub.uriOrPath))
            .setMimeType(mime)
            .setLanguage(sub.language)
            .setLabel(sub.label)
            .setSelectionFlags(if (index == 0 && subtitlesEnabledByDefault) C.SELECTION_FLAG_DEFAULT else 0)
            .build()
    }
}

@Composable
private fun PlaybackErrorOverlay(
    errorResId: Int,
    errorDetail: String?,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    val retryFocusRequester = remember { FocusRequester() }
    var retryFocused by remember { mutableStateOf(false) }
    var backFocused by remember { mutableStateOf(false) }

    LaunchedEffect(errorResId) {
        runCatching { retryFocusRequester.requestFocus() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.86f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SmartTubeSurface,
            border = BorderStroke(1.dp, SmartTubeFocusBorder.copy(alpha = 0.25f)),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .widthIn(max = 520.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ErrorOutline,
                    contentDescription = null,
                    tint = SmartTubeRed,
                    modifier = Modifier.size(52.dp)
                )
                Text(
                    text = stringResource(R.string.player_error_title),
                    color = SmartTubeTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(errorResId),
                    color = SmartTubeTextSecondary,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
                if (!errorDetail.isNullOrBlank()) {
                    Text(
                        text = errorDetail,
                        color = SmartTubeTextSecondary.copy(alpha = 0.75f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onRetry,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SmartTubeRed,
                            contentColor = Color.White
                        ),
                        border = if (retryFocused) BorderStroke(2.dp, SmartTubeFocusBorder) else null,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .focusRequester(retryFocusRequester)
                            .onFocusChanged { retryFocused = it.isFocused }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.player_retry),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    OutlinedButton(
                        onClick = onBack,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = SmartTubeCardBg,
                            contentColor = SmartTubeTextPrimary
                        ),
                        border = BorderStroke(
                            width = if (backFocused) 2.dp else 1.dp,
                            color = if (backFocused) SmartTubeFocusBorder else SmartTubeTextSecondary.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .onFocusChanged { backFocused = it.isFocused }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.player_back),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KidScreenLockOverlay(
    onUnlocked: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val holdProgress = remember { Animatable(0f) }
    var holdJob by remember { mutableStateOf<Job?>(null) }
    var isHolding by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    val pillFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        runCatching { pillFocusRequester.requestFocus() }
    }

    fun startHold() {
        if (isHolding) return
        isHolding = true
        holdJob?.cancel()
        holdJob = scope.launch {
            holdProgress.snapTo(0f)
            holdProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 2500, easing = LinearEasing)
            )
            isHolding = false
            onUnlocked()
        }
    }

    fun cancelHold() {
        if (!isHolding) return
        isHolding = false
        holdJob?.cancel()
        holdJob = null
        scope.launch {
            holdProgress.snapTo(0f)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = Color.Black.copy(alpha = 0.65f),
            border = BorderStroke(
                width = if (isFocused || isHolding) 2.dp else 1.dp,
                color = if (isFocused || isHolding) SmartTubeFocusBorder else Color.White.copy(alpha = 0.28f)
            ),
            modifier = Modifier
                .align(Alignment.TopStart)
                .heightIn(min = 48.dp)
                .focusRequester(pillFocusRequester)
                .onFocusChanged { isFocused = it.isFocused }
                .focusable()
                .onPreviewKeyEvent { event ->
                    when (event.key) {
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                            when (event.type) {
                                KeyEventType.KeyDown -> {
                                    startHold()
                                    true
                                }
                                KeyEventType.KeyUp -> {
                                    cancelHold()
                                    true
                                }
                                else -> true
                            }
                        }
                        else -> false
                    }
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        startHold()
                        waitForUpOrCancellation()
                        cancelHold()
                    }
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(24.dp)
                ) {
                    if (isHolding) {
                        CircularProgressIndicator(
                            progress = { holdProgress.value },
                            color = SmartTubeFocusBorder,
                            trackColor = Color.White.copy(alpha = 0.25f),
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = stringResource(R.string.kid_lock_label),
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.kid_lock_hold_to_unlock),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun WatchTimerTimesUpOverlay(
    onExtendClick: () -> Unit,
    onBack: () -> Unit
) {
    val extendFocusRequester = remember { FocusRequester() }
    var extendFocused by remember { mutableStateOf(false) }
    var backFocused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        runCatching { extendFocusRequester.requestFocus() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SmartTubeSurface,
            border = BorderStroke(1.dp, SmartTubeFocusBorder.copy(alpha = 0.3f)),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .widthIn(max = 520.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Timer,
                    contentDescription = null,
                    tint = SmartTubeRed,
                    modifier = Modifier.size(52.dp)
                )
                Text(
                    text = stringResource(R.string.watch_timer_times_up_title),
                    color = SmartTubeTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.watch_timer_times_up_message),
                    color = SmartTubeTextSecondary,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onExtendClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SmartTubeRed,
                            contentColor = Color.White
                        ),
                        border = if (extendFocused) BorderStroke(2.dp, SmartTubeFocusBorder) else null,
                        modifier = Modifier
                            .weight(1.25f)
                            .height(56.dp)
                            .focusRequester(extendFocusRequester)
                            .onFocusChanged { extendFocused = it.isFocused }
                    ) {
                        Text(
                            text = stringResource(R.string.watch_timer_extend_parent),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                    OutlinedButton(
                        onClick = onBack,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = SmartTubeCardBg,
                            contentColor = SmartTubeTextPrimary
                        ),
                        border = BorderStroke(
                            width = if (backFocused) 2.dp else 1.dp,
                            color = if (backFocused) SmartTubeFocusBorder else SmartTubeTextSecondary.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .weight(0.85f)
                            .height(56.dp)
                            .onFocusChanged { backFocused = it.isFocused }
                    ) {
                        Text(
                            text = stringResource(R.string.player_back),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

private fun buildYouTubeEmbedHtml(videoId: String): String {
    require(YouTubeService.SAFE_VIDEO_ID_REGEX.matches(videoId)) { "Invalid YouTube video ID" }
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
        <iframe id="player"
            src="https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&controls=0&rel=0&playsinline=1&modestbranding=1&iv_load_policy=3&enablejsapi=1"
            allow="autoplay; encrypted-media"
            allowfullscreen></iframe>
        <script>
            var frame = document.getElementById('player');
            window.player = {
                playVideo: function() {
                    if (frame && frame.contentWindow) {
                        frame.contentWindow.postMessage('{"event":"command","func":"playVideo","args":""}', 'https://www.youtube-nocookie.com');
                    }
                },
                pauseVideo: function() {
                    if (frame && frame.contentWindow) {
                        frame.contentWindow.postMessage('{"event":"command","func":"pauseVideo","args":""}', 'https://www.youtube-nocookie.com');
                    }
                },
                seekTo: function() {}
            };
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
