package com.joudjawadtube.player.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.joudjawadtube.player.MainActivity
import com.joudjawadtube.player.data.settings.AppSettingsRepository

/**
 * In-process bridge giving PlaybackScreen access to the service-owned ExoPlayer
 * so Media3 MediaSessionService manages notifications, audio focus, and background playback.
 */
@OptIn(UnstableApi::class)
object JoudPlaybackBridge_Antigravity {
    @Volatile
    var activeService: JoudPlaybackService_Antigravity? = null
        private set

    fun attach(service: JoudPlaybackService_Antigravity) {
        activeService = service
    }

    fun detach(service: JoudPlaybackService_Antigravity) {
        if (activeService === service) {
            activeService = null
        }
    }

    fun activePlayer(): ExoPlayer? = activeService?.exoPlayer
}

/**
 * Media3 MediaSessionService owning the shared ExoPlayer instance for background audio/video
 * playback, media notification controls, and audio focus handling.
 */
@OptIn(UnstableApi::class)
class JoudPlaybackService_Antigravity : MediaSessionService() {

    internal var exoPlayer: ExoPlayer? = null
        private set
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val renderersFactory = DefaultRenderersFactory(this)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            .setEnableDecoderFallback(true)

        val trackSelector = DefaultTrackSelector(this).apply {
            setParameters(
                buildUponParameters()
                    .setTunnelingEnabled(false)
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

        val player = ExoPlayer.Builder(this, renderersFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        exoPlayer = player

        val sessionActivityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            sessionActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityPendingIntent)
            .build()

        JoudPlaybackBridge_Antigravity.attach(this)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        val allowBg = runCatching {
            AppSettingsRepository.getInstance(this).settingsFlow.value.backgroundPlaybackEnabled
        }.getOrDefault(true)

        if (player == null || !player.playWhenReady || !allowBg) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        JoudPlaybackBridge_Antigravity.detach(this)
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        exoPlayer = null
        super.onDestroy()
    }
}
