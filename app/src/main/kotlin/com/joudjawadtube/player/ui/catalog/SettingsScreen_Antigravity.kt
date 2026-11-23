package com.joudjawadtube.player.ui.catalog

import com.joudjawadtube.player.data.updater.UpdateInfo_Antigravity
import com.joudjawadtube.player.data.updater.UpdateState_Antigravity

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import com.joudjawadtube.player.R
import com.joudjawadtube.player.data.settings.AppLanguage
import com.joudjawadtube.player.data.settings.ThemeMode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.ui.AspectRatioFrameLayout
import com.joudjawadtube.player.data.local.FormatUtils
import com.joudjawadtube.player.data.local.SavedChannelEntity
import com.joudjawadtube.player.data.local.SavedPlaylistEntity
import com.joudjawadtube.player.data.local.SavedSafFolderEntity
import com.joudjawadtube.player.data.settings.AppSettingsState
import com.joudjawadtube.player.data.settings.FrameCapturePosition
import com.joudjawadtube.player.data.settings.ResumeMode
import com.joudjawadtube.player.data.settings.SidebarMode
import com.joudjawadtube.player.data.settings.ThumbnailMode
import com.joudjawadtube.player.data.settings.UiFontScale
import com.joudjawadtube.player.data.settings.UiModePreference
import com.joudjawadtube.player.data.thumbnail.ThumbnailManager
import com.joudjawadtube.player.ui.components.ActionPillButton
import com.joudjawadtube.player.ui.components.ConfirmDestructiveDialog_Antigravity
import com.joudjawadtube.player.ui.components.DeviceFormFactor
import com.joudjawadtube.player.ui.components.LocalWindowLayoutInfo
import com.joudjawadtube.player.ui.components.ManageLibraryAndDataDialog_Antigravity
import kotlinx.coroutines.launch
import com.joudjawadtube.player.ui.theme.LocalJoudColors
import com.joudjawadtube.player.ui.theme.SmartTubeCardBg
import com.joudjawadtube.player.ui.theme.SmartTubeCardFocusedBg
import com.joudjawadtube.player.ui.theme.SmartTubeFocusBorder
import com.joudjawadtube.player.ui.theme.SmartTubeRed
import com.joudjawadtube.player.ui.theme.SmartTubeSurface
import com.joudjawadtube.player.ui.theme.SmartTubeTextPrimary
import com.joudjawadtube.player.ui.theme.SmartTubeTextSecondary
import com.joudjawadtube.player.ui.theme.SmartTubeUsbCyan

data class DestructiveAction(
    val title: String,
    val message: String,
    val confirmLabel: String = "Delete",
    val onConfirm: () -> Unit
)

@Composable
fun SettingsScreen(
    settings: AppSettingsState,
    safFolders: List<SavedSafFolderEntity>,
    savedPlaylists: List<SavedPlaylistEntity>,
    savedChannels: List<SavedChannelEntity> = emptyList(),
    totalVideoCount: Int,
    usbDriveCount: Int,
    isScanning: Boolean,
    isGeneratingThumbnails: Boolean,
    thumbnailStatusMessage: String?,
    onUpdateSettings: ((AppSettingsState) -> AppSettingsState) -> Unit,
    onRegenerateAllThumbnails: () -> Unit,
    onClearThumbnailCache: () -> Unit,
    onAddFolder: () -> Unit,
    onRemoveSafFolder: (String) -> Unit,
    onAddYouTubeLink: () -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onDeleteChannel: (String) -> Unit = {},
    onRescanStorage: () -> Unit,
    onClearWatchHistory: () -> Unit,
    onClearFavorites: () -> Unit,
    onClearYouTubeLibrary: () -> Unit,
    onResetSettings: () -> Unit,
    updateState: UpdateState_Antigravity = UpdateState_Antigravity.Idle,
    onCheckForUpdates: () -> Unit = {},
    onDownloadAndInstallUpdate: (UpdateInfo_Antigravity) -> Unit = {},
    onInstallDownloadedApk: (java.io.File) -> Unit = {},
    onDismissUpdateStatus: () -> Unit = {}
) {
    val context = LocalContext.current
    val layoutInfo = LocalWindowLayoutInfo.current
    val isCompact = layoutInfo.isCompact
    val isDark = LocalJoudColors.current.isDark
    val detectedModeLabel = when (layoutInfo.detectedFormFactor) {
        DeviceFormFactor.TV -> stringResource(R.string.settings_device_tv)
        DeviceFormFactor.TABLET -> stringResource(R.string.settings_device_tablet)
        DeviceFormFactor.PHONE -> stringResource(R.string.settings_device_phone)
    }
    var pendingDestructiveAction by remember { mutableStateOf<DestructiveAction?>(null) }
    var showManageLibraryDataDialog by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var cacheSizeBytes by remember { mutableLongStateOf(0L) }
    LaunchedEffect(isGeneratingThumbnails, thumbnailStatusMessage) {
        cacheSizeBytes = ThumbnailManager.getCacheSizeBytesAsync(context)
    }

    val resizeLabel = when (settings.defaultResizeMode) {
        AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> stringResource(R.string.player_aspect_zoom)
        AspectRatioFrameLayout.RESIZE_MODE_FILL -> stringResource(R.string.player_aspect_stretch)
        else -> stringResource(R.string.player_aspect_fit)
    }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // Header Banner
        item(key = "settings_header") {
            val bannerModifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SmartTubeCardBg)
                .border(1.dp, SmartTubeTextSecondary.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
                .padding(if (isCompact) 14.dp else 18.dp)

            @Composable
            fun HeaderTitleGroup(rowModifier: Modifier = Modifier) {
                Row(
                    modifier = rowModifier,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SmartTubeRed.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = SmartTubeRed,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.settings_header_title),
                            color = SmartTubeTextPrimary,
                            fontSize = if (isCompact) 17.sp else 19.sp,
                            lineHeight = 23.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = stringResource(
                                R.string.settings_header_summary,
                                totalVideoCount,
                                usbDriveCount,
                                savedPlaylists.size
                            ),
                            color = SmartTubeTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            @Composable
            fun HeaderActionsRow(rowModifier: Modifier = Modifier) {
                Row(
                    modifier = rowModifier,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ActionPillButton(
                        icon = Icons.Default.AddLink,
                        text = stringResource(R.string.action_add_youtube),
                        isPrimary = true,
                        onClick = onAddYouTubeLink
                    )
                    ActionPillButton(
                        icon = Icons.Default.Refresh,
                        text = if (isScanning) stringResource(R.string.status_scanning) else stringResource(R.string.settings_rescan_storage),
                        isPrimary = false,
                        onClick = onRescanStorage
                    )
                }
            }

            if (isCompact) {
                Column(
                    modifier = bannerModifier,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HeaderTitleGroup(rowModifier = Modifier.fillMaxWidth())
                    HeaderActionsRow(
                        rowModifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    )
                }
            } else {
                Row(
                    modifier = bannerModifier,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    HeaderTitleGroup(rowModifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(12.dp))
                    HeaderActionsRow()
                }
            }
        }

        // Section: Appearance & Language
        item(key = "settings_appearance_language") {
            SettingsCategoryCard(
                title = stringResource(R.string.settings_section_appearance),
                subtitle = stringResource(R.string.settings_language_desc)
            ) {
                val currentLangLabel = when (settings.appLanguage) {
                    AppLanguage.FOLLOW_SYSTEM -> stringResource(R.string.settings_language_system)
                    AppLanguage.ENGLISH -> stringResource(R.string.settings_language_english)
                    AppLanguage.ARABIC -> stringResource(R.string.settings_language_arabic)
                }

                SettingsCycleOptionRow(
                    icon = Icons.Default.Language,
                    title = stringResource(R.string.settings_language),
                    subtitle = stringResource(R.string.settings_language_desc),
                    valueLabel = currentLangLabel,
                    onClick = {
                        val langs = AppLanguage.entries
                        val next = langs[(langs.indexOf(settings.appLanguage) + 1) % langs.size]
                        onUpdateSettings { it.copy(appLanguage = next) }
                    }
                )

                val currentThemeLabel = when (settings.themeMode) {
                    ThemeMode.FOLLOW_SYSTEM -> stringResource(R.string.settings_theme_system)
                    ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
                    ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
                }

                SettingsCycleOptionRow(
                    icon = Icons.Default.Brightness4,
                    title = stringResource(R.string.settings_theme),
                    subtitle = stringResource(R.string.settings_theme_desc),
                    valueLabel = currentThemeLabel,
                    onClick = {
                        val themes = ThemeMode.entries
                        val next = themes[(themes.indexOf(settings.themeMode) + 1) % themes.size]
                        onUpdateSettings { it.copy(themeMode = next) }
                    }
                )
            }
        }

        // Section: Parental Controls & Kids Safety
        item(key = "settings_parental") {
            SettingsCategoryCard(
                title = stringResource(R.string.settings_parental_section_title),
                subtitle = stringResource(R.string.settings_parental_section_sub)
            ) {
                SettingsCycleOptionRow(
                    icon = Icons.Default.Lock,
                    title = stringResource(R.string.settings_parental_gate_title),
                    subtitle = stringResource(R.string.settings_parental_gate_sub),
                    valueLabel = if (settings.parentalGateEnabled) {
                        stringResource(R.string.settings_enabled)
                    } else {
                        stringResource(R.string.settings_disabled)
                    },
                    isAccent = settings.parentalGateEnabled,
                    onClick = {
                        onUpdateSettings { it.copy(parentalGateEnabled = !it.parentalGateEnabled) }
                    }
                )

                val watchLimits = listOf(0, 15, 30, 45, 60)
                SettingsCycleOptionRow(
                    icon = Icons.Default.Timer,
                    title = stringResource(R.string.settings_watch_timer_title),
                    subtitle = stringResource(R.string.settings_watch_timer_sub),
                    valueLabel = if (settings.watchTimeLimitMinutes <= 0) {
                        stringResource(R.string.watch_timer_off)
                    } else {
                        stringResource(R.string.watch_timer_minutes, settings.watchTimeLimitMinutes)
                    },
                    isAccent = settings.watchTimeLimitMinutes > 0,
                    onClick = {
                        val idx = watchLimits.indexOf(settings.watchTimeLimitMinutes)
                        val next = watchLimits[(idx + 1).coerceAtLeast(0) % watchLimits.size]
                        onUpdateSettings { it.copy(watchTimeLimitMinutes = next) }
                    }
                )

                SettingsCycleOptionRow(
                    icon = Icons.Default.Headphones,
                    title = stringResource(R.string.settings_background_playback_title),
                    subtitle = stringResource(R.string.settings_background_playback_sub),
                    valueLabel = if (settings.backgroundPlaybackEnabled) {
                        stringResource(R.string.settings_enabled)
                    } else {
                        stringResource(R.string.settings_disabled)
                    },
                    isAccent = settings.backgroundPlaybackEnabled,
                    onClick = {
                        onUpdateSettings { it.copy(backgroundPlaybackEnabled = !it.backgroundPlaybackEnabled) }
                    }
                )

                SettingsCycleOptionRow(
                    icon = Icons.Default.Security,
                    title = stringResource(R.string.settings_general_fallback_title),
                    subtitle = stringResource(R.string.settings_general_fallback_sub),
                    valueLabel = if (settings.allowGeneralFallbackForSavedChannels) {
                        stringResource(R.string.settings_enabled)
                    } else {
                        stringResource(R.string.settings_disabled)
                    },
                    isAccent = settings.allowGeneralFallbackForSavedChannels,
                    onClick = {
                        onUpdateSettings {
                            it.copy(allowGeneralFallbackForSavedChannels = !it.allowGeneralFallbackForSavedChannels)
                        }
                    }
                )

                SettingsCycleOptionRow(
                    icon = Icons.Default.Public,
                    title = stringResource(R.string.settings_webview_fallback_title),
                    subtitle = stringResource(R.string.settings_webview_fallback_sub),
                    valueLabel = if (settings.allowWebViewFallback) {
                        stringResource(R.string.settings_enabled)
                    } else {
                        stringResource(R.string.settings_disabled)
                    },
                    isAccent = settings.allowWebViewFallback,
                    onClick = {
                        onUpdateSettings { it.copy(allowWebViewFallback = !it.allowWebViewFallback) }
                    }
                )
            }
        }

        // Section 1: Thumbnails & Performance
        item(key = "settings_thumbnails") {
            SettingsCategoryCard(
                title = stringResource(R.string.settings_section_thumbnails),
                subtitle = thumbnailStatusMessage
                    ?: stringResource(R.string.settings_thumbnails_sub)
            ) {
                SettingsCycleOptionRow(
                    icon = Icons.Default.Image,
                    title = stringResource(R.string.settings_thumbnail_source_title),
                    subtitle = settings.thumbnailMode.description,
                    valueLabel = settings.thumbnailMode.label,
                    onClick = {
                        val modes = ThumbnailMode.entries
                        val next = modes[(modes.indexOf(settings.thumbnailMode) + 1) % modes.size]
                        onUpdateSettings { it.copy(thumbnailMode = next) }
                    }
                )

                SettingsCycleOptionRow(
                    icon = Icons.Default.PlayArrow,
                    title = stringResource(R.string.settings_frame_timestamp_title),
                    subtitle = stringResource(R.string.settings_frame_timestamp_sub),
                    valueLabel = settings.frameCapturePosition.label,
                    onClick = {
                        val positions = FrameCapturePosition.entries
                        val next = positions[(positions.indexOf(settings.frameCapturePosition) + 1) % positions.size]
                        onUpdateSettings { it.copy(frameCapturePosition = next) }
                    }
                )

                SettingsCycleOptionRow(
                    icon = Icons.Default.Refresh,
                    title = if (isGeneratingThumbnails) stringResource(R.string.settings_regen_thumbs_running) else stringResource(R.string.settings_regen_thumbs_title),
                    subtitle = stringResource(R.string.settings_regen_thumbs_sub, totalVideoCount),
                    valueLabel = if (isGeneratingThumbnails) stringResource(R.string.settings_running) else stringResource(R.string.settings_run_now),
                    isAccent = true,
                    onClick = onRegenerateAllThumbnails
                )

                val formattedCache = FormatUtils.formatFileSize(cacheSizeBytes).ifBlank { "0 MB" }
                SettingsCycleOptionRow(
                    icon = Icons.Default.Delete,
                    title = stringResource(R.string.settings_clear_thumb_cache_title),
                    subtitle = stringResource(R.string.settings_clear_thumb_cache_sub, formattedCache),
                    valueLabel = stringResource(R.string.settings_clear_cache_btn, formattedCache),
                    onClick = {
                        onClearThumbnailCache()
                        cacheSizeBytes = 0L
                    }
                )
            }
        }

        // Section 2: Player & Playback Controls
        item(key = "settings_playback") {
            SettingsCategoryCard(
                title = stringResource(R.string.settings_section_playback_numbered),
                subtitle = stringResource(R.string.settings_playback_sub)
            ) {
                val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                SettingsCycleOptionRow(
                    icon = Icons.Default.Speed,
                    title = stringResource(R.string.settings_default_speed_title),
                    subtitle = stringResource(R.string.settings_default_speed_sub),
                    valueLabel = "${settings.defaultPlaybackSpeed}x",
                    onClick = {
                        val idx = speeds.indexOfFirst { kotlin.math.abs(it - settings.defaultPlaybackSpeed) < 0.01f }
                        val next = speeds[(idx + 1).coerceAtLeast(0) % speeds.size]
                        onUpdateSettings { it.copy(defaultPlaybackSpeed = next) }
                    }
                )

                SettingsCycleOptionRow(
                    icon = Icons.Default.AspectRatio,
                    title = stringResource(R.string.settings_default_aspect_title),
                    subtitle = stringResource(R.string.settings_default_aspect_sub),
                    valueLabel = resizeLabel,
                    onClick = {
                        val nextMode = when (settings.defaultResizeMode) {
                            AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                        onUpdateSettings { it.copy(defaultResizeMode = nextMode) }
                    }
                )

                val seekSteps = listOf(5, 10, 15, 30, 60)
                SettingsCycleOptionRow(
                    icon = Icons.Default.History,
                    title = stringResource(R.string.settings_seek_interval_title),
                    subtitle = stringResource(R.string.settings_seek_interval_sub),
                    valueLabel = stringResource(R.string.settings_seek_seconds, settings.seekIntervalSec),
                    onClick = {
                        val idx = seekSteps.indexOf(settings.seekIntervalSec)
                        val next = seekSteps[(idx + 1).coerceAtLeast(0) % seekSteps.size]
                        onUpdateSettings { it.copy(seekIntervalSec = next) }
                    }
                )

                SettingsCycleOptionRow(
                    icon = Icons.Default.PlayArrow,
                    title = stringResource(R.string.settings_auto_play_next),
                    subtitle = stringResource(R.string.settings_auto_play_next_sub),
                    valueLabel = if (settings.autoPlayNext) stringResource(R.string.settings_enabled) else stringResource(R.string.settings_disabled),
                    isAccent = settings.autoPlayNext,
                    onClick = {
                        onUpdateSettings { it.copy(autoPlayNext = !it.autoPlayNext) }
                    }
                )

                SettingsCycleOptionRow(
                    icon = Icons.Default.History,
                    title = stringResource(R.string.settings_resume_mode),
                    subtitle = stringResource(R.string.settings_resume_mode_sub),
                    valueLabel = settings.resumeMode.label,
                    onClick = {
                        val modes = ResumeMode.entries
                        val next = modes[(modes.indexOf(settings.resumeMode) + 1) % modes.size]
                        onUpdateSettings { it.copy(resumeMode = next) }
                    }
                )

                SettingsCycleOptionRow(
                    icon = Icons.Default.ClosedCaption,
                    title = stringResource(R.string.settings_subtitles_default),
                    subtitle = stringResource(R.string.settings_subtitles_default_sub),
                    valueLabel = if (settings.subtitlesEnabledByDefault) stringResource(R.string.settings_enabled) else stringResource(R.string.settings_disabled),
                    isAccent = settings.subtitlesEnabledByDefault,
                    onClick = {
                        onUpdateSettings { it.copy(subtitlesEnabledByDefault = !it.subtitlesEnabledByDefault) }
                    }
                )
            }
        }

        // Section 3: UI, Layout & Fonts
        item(key = "settings_ui") {
            SettingsCategoryCard(
                title = stringResource(R.string.settings_section_ui_numbered),
                subtitle = stringResource(R.string.settings_ui_sub)
            ) {
                SettingsCycleOptionRow(
                    icon = Icons.Default.Tv,
                    title = stringResource(R.string.settings_ui_mode),
                    subtitle = stringResource(R.string.settings_ui_mode_detected_sub, detectedModeLabel),
                    valueLabel = settings.uiModePreference.label,
                    onClick = {
                        val modes = UiModePreference.entries
                        val next = modes[(modes.indexOf(settings.uiModePreference) + 1) % modes.size]
                        onUpdateSettings { it.copy(uiModePreference = next) }
                    }
                )

                SettingsCycleOptionRow(
                    icon = Icons.Default.Settings,
                    title = stringResource(R.string.settings_sidebar_mode),
                    subtitle = stringResource(R.string.settings_sidebar_mode_sub),
                    valueLabel = settings.sidebarMode.label,
                    onClick = {
                        val modes = SidebarMode.entries
                        val next = modes[(modes.indexOf(settings.sidebarMode) + 1) % modes.size]
                        onUpdateSettings { it.copy(sidebarMode = next) }
                    }
                )

                SettingsCycleOptionRow(
                    icon = Icons.Default.AspectRatio,
                    title = stringResource(R.string.settings_font_scale),
                    subtitle = stringResource(R.string.settings_font_scale_sub),
                    valueLabel = settings.uiFontScale.label,
                    onClick = {
                        val scales = UiFontScale.entries
                        val next = scales[(scales.indexOf(settings.uiFontScale) + 1) % scales.size]
                        onUpdateSettings { it.copy(uiFontScale = next) }
                    }
                )
            }
        }

        // Section 4: Storage, USB & Saved Folders
        item(key = "settings_storage") {
            SettingsCategoryCard(
                title = stringResource(R.string.settings_section_storage_numbered),
                subtitle = stringResource(R.string.settings_storage_sub)
            ) {
                SettingsCycleOptionRow(
                    icon = Icons.Default.Storage,
                    title = stringResource(R.string.settings_auto_scan_startup),
                    subtitle = stringResource(R.string.settings_auto_scan_startup_sub),
                    valueLabel = if (settings.autoScanOnStartup) stringResource(R.string.settings_enabled) else stringResource(R.string.settings_disabled),
                    isAccent = settings.autoScanOnStartup,
                    onClick = {
                        onUpdateSettings { it.copy(autoScanOnStartup = !it.autoScanOnStartup) }
                    }
                )

                SettingsCycleOptionRow(
                    icon = Icons.Default.Usb,
                    title = stringResource(R.string.settings_auto_scan_usb),
                    subtitle = stringResource(R.string.settings_auto_scan_usb_sub),
                    valueLabel = if (settings.autoScanOnUsbMount) stringResource(R.string.settings_enabled) else stringResource(R.string.settings_disabled),
                    isAccent = settings.autoScanOnUsbMount,
                    onClick = {
                        onUpdateSettings { it.copy(autoScanOnUsbMount = !it.autoScanOnUsbMount) }
                    }
                )

                SettingsCycleOptionRow(
                    icon = Icons.Default.Usb,
                    title = stringResource(R.string.settings_hide_boot_partitions),
                    subtitle = stringResource(R.string.settings_hide_boot_partitions_sub),
                    valueLabel = if (settings.hideBootUsbPartitions) stringResource(R.string.settings_hidden) else stringResource(R.string.settings_shown),
                    isAccent = settings.hideBootUsbPartitions,
                    onClick = {
                        onUpdateSettings { it.copy(hideBootUsbPartitions = !it.hideBootUsbPartitions) }
                        onRescanStorage()
                    }
                )

                SettingsCycleOptionRow(
                    icon = Icons.Default.FolderOpen,
                    title = stringResource(R.string.settings_add_folder_title),
                    subtitle = stringResource(R.string.settings_add_folder_sub),
                    valueLabel = stringResource(R.string.settings_browse_folders),
                    isAccent = true,
                    onClick = onAddFolder
                )

                if (safFolders.isNotEmpty()) {
                    HorizontalDivider(color = SmartTubeTextSecondary.copy(alpha = 0.16f))
                    Text(
                        text = stringResource(R.string.settings_saved_folders_count, safFolders.size),
                        color = SmartTubeTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    safFolders.forEach { folder ->
                        SettingsCycleOptionRow(
                            icon = Icons.Default.FolderOpen,
                            title = folder.displayName,
                            subtitle = folder.uriString,
                            valueLabel = stringResource(R.string.settings_remove),
                            onClick = { onRemoveSafFolder(folder.uriString) }
                        )
                    }
                }
            }
        }

        // Section 5: YouTube Playlists, History & Data Reset (Single button opening modal manager)
        item(key = "settings_data") {
            SettingsCategoryCard(
                title = stringResource(R.string.settings_section_data_numbered),
                subtitle = stringResource(R.string.settings_data_sub)
            ) {
                SettingsCycleOptionRow(
                    icon = Icons.Default.AddLink,
                    title = stringResource(R.string.settings_manage_library_data_title),
                    subtitle = stringResource(
                        R.string.settings_manage_library_data_sub,
                        savedChannels.size,
                        savedPlaylists.size
                    ),
                    valueLabel = stringResource(R.string.settings_manage_library_data_btn),
                    isAccent = true,
                    onClick = { showManageLibraryDataDialog = true }
                )
            }
        }

        // Section 6: App Updates & GitHub Sync
        item(key = "settings_updates") {
            val versionName = remember {
                runCatching {
                    val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    pInfo.versionName ?: "1.0.0"
                }.getOrDefault("1.0.0")
            }
            var updatesCardHasFocus by remember { mutableStateOf(false) }

            LaunchedEffect(updatesCardHasFocus, updateState) {
                if (updatesCardHasFocus) {
                    val lastIndex = (lazyListState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)
                    runCatching { lazyListState.animateScrollToItem(lastIndex) }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { updatesCardHasFocus = it.hasFocus }
            ) {
                SettingsCategoryCard(
                    title = stringResource(R.string.settings_section_updater_numbered),
                    subtitle = stringResource(R.string.settings_updater_sub)
                ) {
                    SettingsCycleOptionRow(
                        icon = Icons.Default.SystemUpdate,
                        title = stringResource(R.string.settings_installed_version, versionName),
                        subtitle = stringResource(R.string.settings_repo_sub),
                        valueLabel = if (updateState is UpdateState_Antigravity.Checking) {
                            stringResource(R.string.settings_checking_short)
                        } else {
                            stringResource(R.string.settings_check_updates)
                        },
                        isAccent = true,
                        onClick = {
                            coroutineScope.launch {
                                val lastIndex = (lazyListState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)
                                runCatching { lazyListState.animateScrollToItem(lastIndex) }
                            }
                            onCheckForUpdates()
                        }
                    )

                    val successBg = if (isDark) Color(0xFF16321F) else Color(0xFFE8F5E9)
                    val successText = if (isDark) Color(0xFFE8F5E9) else Color(0xFF1B5E20)
                    val successIcon = if (isDark) Color(0xFF4CAF50) else Color(0xFF2E7D32)
                    val warnBg = if (isDark) Color(0xFF261D10) else Color(0xFFFFF3E0)
                    val warnBorder = if (isDark) Color(0xFFFF9800).copy(alpha = 0.5f) else Color(0xFFEF6C00).copy(alpha = 0.5f)
                    val warnIcon = if (isDark) Color(0xFFFF9800) else Color(0xFFE65100)
                    val errBg = if (isDark) Color(0xFF331414) else Color(0xFFFFEBEE)
                    val errText = if (isDark) Color(0xFFFFCDD2) else Color(0xFFB71C1C)

                    when (updateState) {
                        is UpdateState_Antigravity.Idle -> { /* Idle */ }
                        is UpdateState_Antigravity.Checking -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SmartTubeSurface)
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.settings_checking_updates),
                                    color = SmartTubeTextSecondary,
                                    fontSize = 13.sp
                                )
                            }
                        }
                        is UpdateState_Antigravity.UpToDate -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(successBg)
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = successIcon, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = stringResource(R.string.settings_up_to_date_version, updateState.currentVersion),
                                    color = successText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        is UpdateState_Antigravity.UpdateAvailable -> {
                            val info = updateState.info
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(warnBg)
                                    .border(1.dp, warnBorder, RoundedCornerShape(10.dp))
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = warnIcon, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = stringResource(R.string.settings_update_available_detail, info.tagName, info.releaseTitle),
                                            color = SmartTubeTextPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        val sizeMb = if (info.fileSize > 0) String.format(java.util.Locale.US, " • %.1f MB", info.fileSize / (1024f * 1024f)) else ""
                                        Text(
                                            text = "${info.fileName}$sizeMb",
                                            color = SmartTubeTextSecondary,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                if (info.releaseNotes.isNotBlank()) {
                                    Text(
                                        text = info.releaseNotes,
                                        color = SmartTubeTextSecondary,
                                        fontSize = 12.sp,
                                        maxLines = 4,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    ActionPillButton(
                                        icon = Icons.Default.CloudDownload,
                                        text = stringResource(R.string.settings_download_update),
                                        isPrimary = true,
                                        onClick = { onDownloadAndInstallUpdate(info) }
                                    )
                                }
                            }
                        }
                        is UpdateState_Antigravity.Downloading -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SmartTubeSurface)
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val downloadedMb = String.format(java.util.Locale.US, "%.1f", updateState.downloadedBytes / (1024f * 1024f))
                                val totalMb = String.format(java.util.Locale.US, "%.1f", updateState.totalBytes / (1024f * 1024f))
                                Text(
                                    text = stringResource(R.string.settings_downloading_update, updateState.progressPercent, downloadedMb, totalMb),
                                    color = SmartTubeTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                LinearProgressIndicator(
                                    progress = { updateState.progressPercent / 100f },
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                    color = SmartTubeRed,
                                    trackColor = SmartTubeTextSecondary.copy(alpha = 0.25f)
                                )
                            }
                        }
                        is UpdateState_Antigravity.ReadyToInstall -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(successBg)
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.settings_download_complete_msg),
                                    color = successText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                ActionPillButton(
                                    icon = Icons.Default.SystemUpdate,
                                    text = stringResource(R.string.settings_launch_installer),
                                    isPrimary = true,
                                    onClick = { onInstallDownloadedApk(updateState.apkFile) }
                                )
                            }
                        }
                        is UpdateState_Antigravity.Error -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(errBg)
                                    .border(1.dp, SmartTubeRed.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.settings_update_error, updateState.message),
                                    color = errText,
                                    fontSize = 12.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                ActionPillButton(
                                    icon = Icons.Default.Refresh,
                                    text = stringResource(R.string.action_dismiss),
                                    isPrimary = false,
                                    onClick = onDismissUpdateStatus
                                )
                            }
                        }
                    }
                }
            }
        }

        // Trailing spacer item so Card 6 can scroll well above the bottom edge of the screen
        item(key = "settings_bottom_spacer") {
            Spacer(modifier = Modifier.height(56.dp))
        }
    }

    if (showManageLibraryDataDialog) {
        ManageLibraryAndDataDialog_Antigravity(
            savedChannels = savedChannels,
            savedPlaylists = savedPlaylists,
            onAddYouTubeLink = onAddYouTubeLink,
            onDeleteChannel = onDeleteChannel,
            onDeletePlaylist = onDeletePlaylist,
            onClearWatchHistory = onClearWatchHistory,
            onClearFavorites = onClearFavorites,
            onClearYouTubeLibrary = onClearYouTubeLibrary,
            onResetSettings = onResetSettings,
            onDismiss = { showManageLibraryDataDialog = false }
        )
    }

    pendingDestructiveAction?.let { action ->
        ConfirmDestructiveDialog_Antigravity(
            title = action.title,
            message = action.message,
            confirmActionLabel = action.confirmLabel,
            onConfirmed = action.onConfirm,
            onDismiss = { pendingDestructiveAction = null }
        )
    }
}

@Composable
private fun SettingsCategoryCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SmartTubeCardBg)
            .border(1.dp, SmartTubeTextSecondary.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column {
            Text(
                text = title,
                color = SmartTubeTextPrimary,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = SmartTubeTextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
        HorizontalDivider(color = SmartTubeTextSecondary.copy(alpha = 0.16f))
        content()
    }
}

@Composable
private fun SettingsCycleOptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    valueLabel: String,
    isAccent: Boolean = false,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val bgColor = when {
        isFocused -> SmartTubeCardFocusedBg
        else -> SmartTubeSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) SmartTubeFocusBorder else SmartTubeTextSecondary.copy(alpha = 0.14f),
                shape = RoundedCornerShape(10.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isFocused || isAccent) SmartTubeRed else SmartTubeTextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                val isCompact = LocalWindowLayoutInfo.current.isCompact
                Text(
                    text = title,
                    color = SmartTubeTextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = if (isCompact) 2 else 1,
                    softWrap = isCompact,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    color = SmartTubeTextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                    when {
                        isFocused -> SmartTubeRed
                        isAccent -> SmartTubeUsbCyan.copy(alpha = 0.16f)
                        else -> SmartTubeCardBg
                    }
                )
                .border(
                    width = 1.dp,
                    color = when {
                        isAccent && !isFocused -> SmartTubeUsbCyan.copy(alpha = 0.5f)
                        !isFocused -> SmartTubeTextSecondary.copy(alpha = 0.2f)
                        else -> Color.Transparent
                    },
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = valueLabel,
                color = when {
                    isFocused -> Color.White
                    isAccent -> SmartTubeUsbCyan
                    else -> SmartTubeTextPrimary
                },
                fontSize = 12.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
