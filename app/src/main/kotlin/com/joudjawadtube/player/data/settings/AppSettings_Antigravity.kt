package com.joudjawadtube.player.data.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThumbnailMode(val label: String, val description: String) {
    YOUTUBE_FIRST(
        label = "YouTube First → Auto Video Frame",
        description = "Fetches official HD thumbnail from YouTube first; falls back to automatic video frame extraction if unavailable"
    ),
    AUTO_FRAME_ONLY(
        label = "Automatic Video Frame Only",
        description = "Always extracts a frame directly from the video file (100% offline)"
    ),
    YOUTUBE_AND_SIDECAR_ONLY(
        label = "YouTube & Sidecar Cover Only",
        description = "Uses YouTube or local .jpg/.png covers without opening video files on USB"
    )
}

enum class FrameCapturePosition(val label: String, val fraction: Float?) {
    FAST_3S("Fast Keyframe (3s)", null),
    PERCENT_10("10% of Video", 0.10f),
    PERCENT_15("15% of Video", 0.15f),
    PERCENT_30("30% of Video", 0.30f)
}

enum class ResumeMode(val label: String) {
    ALWAYS_RESUME("Resume Where Left Off"),
    START_FROM_BEGINNING("Always Start from Beginning")
}

enum class UiModePreference(val label: String) {
    AUTO("Auto Detect (TV / Tablet)"),
    FORCE_TV("Android TV 10-Foot UI"),
    FORCE_TABLET("Tablet Touch UI")
}

enum class UiFontScale(val label: String, val scaleFactor: Float) {
    COMPACT("Compact (92%)", 0.92f),
    NORMAL("Normal (100%)", 1.0f),
    LARGE("Large (110%)", 1.10f)
}

enum class SidebarMode(val label: String) {
    OVERLAY_ON_FOCUS("Overlay on Focus (Zero Lag)"),
    ALWAYS_COLLAPSED("Always Collapsed (Icons)"),
    ALWAYS_EXPANDED("Always Expanded")
}

enum class RepeatMode(val label: String) {
    OFF("Repeat Off"),
    REPEAT_ONE("Repeat Video"),
    REPEAT_ALL("Repeat Playlist")
}

data class AppSettingsState(
    val appLanguage: AppLanguage = AppLanguage.FOLLOW_SYSTEM,
    val themeMode: ThemeMode = ThemeMode.FOLLOW_SYSTEM,
    val thumbnailMode: ThumbnailMode = ThumbnailMode.YOUTUBE_FIRST,
    val frameCapturePosition: FrameCapturePosition = FrameCapturePosition.FAST_3S,
    val defaultPlaybackSpeed: Float = 1.0f,
    val defaultResizeMode: Int = 0, // 0 = Fit, 4 = Zoom, 3 = Stretch
    val seekIntervalSec: Int = 10,
    val autoPlayNext: Boolean = true,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val preferredQuality: String = "Auto",
    val resumeMode: ResumeMode = ResumeMode.ALWAYS_RESUME,
    val subtitlesEnabledByDefault: Boolean = true,
    val uiModePreference: UiModePreference = UiModePreference.AUTO,
    val tabletUseGridMode: Boolean = true,
    val uiFontScale: UiFontScale = UiFontScale.NORMAL,
    val sidebarMode: SidebarMode = SidebarMode.OVERLAY_ON_FOCUS,
    val autoScanOnStartup: Boolean = true,
    val autoScanOnUsbMount: Boolean = true,
    val hideBootUsbPartitions: Boolean = true,
    val removedBuiltInChannelIds: Set<String> = emptySet()
)

class AppSettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("joud_jawad_settings_prefs", Context.MODE_PRIVATE)

    private val _settingsFlow = MutableStateFlow(loadFromPrefs())
    val settingsFlow: StateFlow<AppSettingsState> = _settingsFlow.asStateFlow()

    private fun loadFromPrefs(): AppSettingsState {
        val langName = prefs.getString("app_language", AppLanguage.FOLLOW_SYSTEM.name)
        val themeName = prefs.getString("theme_mode", ThemeMode.FOLLOW_SYSTEM.name)
        val thumbModeName = prefs.getString("thumbnail_mode", ThumbnailMode.YOUTUBE_FIRST.name)
        val framePosName = prefs.getString("frame_capture_pos", FrameCapturePosition.FAST_3S.name)
        val resumeName = prefs.getString("resume_mode", ResumeMode.ALWAYS_RESUME.name)
        val repeatName = prefs.getString("repeat_mode", RepeatMode.OFF.name)
        val preferredQuality = prefs.getString("preferred_quality", "Auto") ?: "Auto"
        val uiModeName = prefs.getString("ui_mode_pref", UiModePreference.AUTO.name)
        val fontScaleName = prefs.getString("ui_font_scale", UiFontScale.NORMAL.name)
        val sidebarName = prefs.getString("sidebar_mode", SidebarMode.OVERLAY_ON_FOCUS.name)

        val loadedLanguage = runCatching { AppLanguage.valueOf(langName!!) }
            .getOrDefault(AppLanguage.FOLLOW_SYSTEM)
        val loadedTheme = runCatching { ThemeMode.valueOf(themeName!!) }
            .getOrDefault(ThemeMode.FOLLOW_SYSTEM)

        LocaleManager_Antigravity.apply(loadedLanguage)
        LocaleManager_Antigravity.applyTheme(loadedTheme)

        return AppSettingsState(
            appLanguage = loadedLanguage,
            themeMode = loadedTheme,
            thumbnailMode = runCatching { ThumbnailMode.valueOf(thumbModeName!!) }
                .getOrDefault(ThumbnailMode.YOUTUBE_FIRST),
            frameCapturePosition = runCatching { FrameCapturePosition.valueOf(framePosName!!) }
                .getOrDefault(FrameCapturePosition.FAST_3S),
            defaultPlaybackSpeed = prefs.getFloat("default_playback_speed", 1.0f),
            defaultResizeMode = prefs.getInt("default_resize_mode", 0),
            seekIntervalSec = prefs.getInt("seek_interval_sec", 10),
            autoPlayNext = prefs.getBoolean("auto_play_next", true),
            repeatMode = runCatching { RepeatMode.valueOf(repeatName!!) }.getOrDefault(RepeatMode.OFF),
            preferredQuality = preferredQuality,
            resumeMode = runCatching { ResumeMode.valueOf(resumeName!!) }
                .getOrDefault(ResumeMode.ALWAYS_RESUME),
            subtitlesEnabledByDefault = prefs.getBoolean("subtitles_enabled_default", true),
            uiModePreference = runCatching { UiModePreference.valueOf(uiModeName!!) }
                .getOrDefault(UiModePreference.AUTO),
            tabletUseGridMode = prefs.getBoolean("tablet_use_grid", true),
            uiFontScale = runCatching { UiFontScale.valueOf(fontScaleName!!) }
                .getOrDefault(UiFontScale.NORMAL),
            sidebarMode = runCatching { SidebarMode.valueOf(sidebarName!!) }
                .getOrDefault(SidebarMode.OVERLAY_ON_FOCUS),
            autoScanOnStartup = prefs.getBoolean("auto_scan_startup", true),
            autoScanOnUsbMount = prefs.getBoolean("auto_scan_usb_mount", true),
            hideBootUsbPartitions = prefs.getBoolean("hide_boot_usb_partitions", true),
            removedBuiltInChannelIds = prefs.getStringSet("removed_builtin_channel_ids", emptySet())?.toSet() ?: emptySet()
        )
    }

    fun update(transform: (AppSettingsState) -> AppSettingsState) {
        val updated = transform(_settingsFlow.value)
        _settingsFlow.value = updated
        LocaleManager_Antigravity.apply(updated.appLanguage)
        LocaleManager_Antigravity.applyTheme(updated.themeMode)
        prefs.edit()
            .putString("app_language", updated.appLanguage.name)
            .putString("theme_mode", updated.themeMode.name)
            .putString("thumbnail_mode", updated.thumbnailMode.name)
            .putString("frame_capture_pos", updated.frameCapturePosition.name)
            .putFloat("default_playback_speed", updated.defaultPlaybackSpeed)
            .putInt("default_resize_mode", updated.defaultResizeMode)
            .putInt("seek_interval_sec", updated.seekIntervalSec)
            .putBoolean("auto_play_next", updated.autoPlayNext)
            .putString("repeat_mode", updated.repeatMode.name)
            .putString("preferred_quality", updated.preferredQuality)
            .putString("resume_mode", updated.resumeMode.name)
            .putBoolean("subtitles_enabled_default", updated.subtitlesEnabledByDefault)
            .putString("ui_mode_pref", updated.uiModePreference.name)
            .putBoolean("tablet_use_grid", updated.tabletUseGridMode)
            .putString("ui_font_scale", updated.uiFontScale.name)
            .putString("sidebar_mode", updated.sidebarMode.name)
            .putBoolean("auto_scan_startup", updated.autoScanOnStartup)
            .putBoolean("auto_scan_usb_mount", updated.autoScanOnUsbMount)
            .putBoolean("hide_boot_usb_partitions", updated.hideBootUsbPartitions)
            .putStringSet("removed_builtin_channel_ids", updated.removedBuiltInChannelIds.toSet())
            .apply()
    }

    fun markBuiltInChannelRemoved(channelId: String) {
        if (channelId.isBlank()) return
        update { it.copy(removedBuiltInChannelIds = it.removedBuiltInChannelIds + channelId) }
    }

    fun restoreBuiltInChannel(channelId: String) {
        if (channelId.isBlank()) return
        update { it.copy(removedBuiltInChannelIds = it.removedBuiltInChannelIds - channelId) }
    }

    fun resetToDefaults() {
        prefs.edit().clear().apply()
        val defaultState = AppSettingsState()
        LocaleManager_Antigravity.apply(defaultState.appLanguage)
        LocaleManager_Antigravity.applyTheme(defaultState.themeMode)
        _settingsFlow.value = defaultState
    }

    companion object {
        @Volatile
        private var INSTANCE: AppSettingsRepository? = null

        fun getInstance(context: Context): AppSettingsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppSettingsRepository(context).also { INSTANCE = it }
            }
        }
    }
}
