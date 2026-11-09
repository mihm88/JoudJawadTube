package com.joudjawadtube.player.ui.catalog

import com.joudjawadtube.player.data.updater.UpdateInfo_Antigravity
import com.joudjawadtube.player.data.updater.UpdateState_Antigravity

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SystemUpdate
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.joudjawadtube.player.ui.theme.SmartTubeCardBg
import com.joudjawadtube.player.ui.theme.SmartTubeCardFocusedBg
import com.joudjawadtube.player.ui.theme.SmartTubeFocusBorder
import com.joudjawadtube.player.ui.theme.SmartTubeRed
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
    onDismissUpdateStatus: () -> Unit = {}
) {
    val context = LocalContext.current
    var pendingDestructiveAction by remember { mutableStateOf<DestructiveAction?>(null) }
    var cacheSizeBytes by remember(isGeneratingThumbnails, thumbnailStatusMessage) {
        mutableLongStateOf(ThumbnailManager.getCacheSizeBytes(context))
    }

    val resizeLabel = when (settings.defaultResizeMode) {
        AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "Zoom (Crop to Fill)"
        AspectRatioFrameLayout.RESIZE_MODE_FILL -> "Stretch (Full Screen)"
        else -> "Fit (Original Aspect)"
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = PaddingValues(bottom = 44.dp)
    ) {
        // Header Banner
        item(key = "settings_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SmartTubeCardBg)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
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
                            text = "Settings & Full Control",
                            color = SmartTubeTextPrimary,
                            fontSize = 19.sp,
                            lineHeight = 23.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "$totalVideoCount Videos in Catalog • $usbDriveCount USB Connected • ${savedPlaylists.size} YouTube Playlists",
                            color = SmartTubeTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionPillButton(
                        icon = Icons.Default.AddLink,
                        text = "+ YouTube Link",
                        isPrimary = true,
                        onClick = onAddYouTubeLink
                    )
                    ActionPillButton(
                        icon = Icons.Default.Refresh,
                        text = if (isScanning) "Scanning..." else "Rescan Storage",
                        isPrimary = false,
                        onClick = onRescanStorage
                    )
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

        // Section 1: Thumbnails & Performance
        item(key = "settings_thumbnails") {
            SettingsCategoryCard(
                title = "1. Thumbnail Generation & Priority",
                subtitle = thumbnailStatusMessage
                    ?: "Priority: Official YouTube HD Thumbnail first, automatic video frame extraction if unavailable"
            ) {
                SettingsCycleOptionRow(
                    icon = Icons.Default.Image,
                    title = "Thumbnail Source Priority",
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
                    title = "Automatic Video Frame Timestamp (Fallback)",
                    subtitle = "Timestamp used when generating thumbnail directly from a local/USB video file",
                    valueLabel = settings.frameCapturePosition.label,
                    onClick = {
                        val positions = FrameCapturePosition.entries
                        val next = positions[(positions.indexOf(settings.frameCapturePosition) + 1) % positions.size]
                        onUpdateSettings { it.copy(frameCapturePosition = next) }
                    }
                )

                SettingsCycleOptionRow(
                    icon = Icons.Default.Refresh,
                    title = if (isGeneratingThumbnails) "Generating Thumbnails in Background..." else "Generate / Refresh All Thumbnails Now",
                    subtitle = "Fetches YouTube thumbnails first for all $totalVideoCount videos and falls back to automatic frame capture",
                    valueLabel = if (isGeneratingThumbnails) "Running..." else "Run Now",
                    isAccent = true,
                    onClick = onRegenerateAllThumbnails
                )

                val formattedCache = FormatUtils.formatFileSize(cacheSizeBytes).ifBlank { "0 MB" }
                SettingsCycleOptionRow(
                    icon = Icons.Default.Delete,
                    title = "Clear Persistent Thumbnail Disk Cache",
                    subtitle = "Currently storing $formattedCache of fast-loading 16:9 thumbnails on internal flash",
                    valueLabel = "Clear ($formattedCache)",
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
                title = "2. Video Player & Playback",
                subtitle = "Configure default speed, seek interval, aspect ratio, auto-play, and subtitles"
            ) {
                val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                SettingsCycleOptionRow(
                    icon = Icons.Default.Speed,
                    title = "Default Playback Speed",
                    subtitle = "Initial playback speed when opening any video",
                    valueLabel = "${settings.defaultPlaybackSpeed}x",
                    onClick = {
                        val idx = speeds.indexOfFirst { kotlin.math.abs(it - settings.defaultPlaybackSpeed) < 0.01f }
                        val next = speeds[(idx + 1).coerceAtLeast(0) % speeds.size]
                        onUpdateSettings { it.copy(defaultPlaybackSpeed = next) }
                    }
                )

                SettingsCycleOptionRow(
                    icon = Icons.Default.AspectRatio,
                    title = "Default Aspect Ratio / Resize Mode",
                    subtitle = "Default video scaling inside the player",
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
                    title = "D-Pad Left / Right Seek Interval",
                    subtitle = "Seconds to jump when pressing Rewind / Fast-Forward or Left / Right",
                    valueLabel = "${settings.seekIntervalSec} Seconds",
                    onClick = {
                        val idx = seekSteps.indexOf(settings.seekIntervalSec)
                        val next = seekSteps[(idx + 1).coerceAtLeast(0) % seekSteps.size]
                        onUpdateSettings { it.copy(seekIntervalSec = next) }
                    }
                )

                SettingsCycleOptionRow(
                    icon = Icons.Default.PlayArrow,
                    title = "Auto-Play Next Video in Folder / Playlist",
                    subtitle = "Automatically play the next episode when the current video finishes",
                    valueLabel = if (settings.autoPlayNext) "Enabled" else "Disabled",
                    isAccent = settings.autoPlayNext,
                    onClick = {
                        onUpdateSettings { it.copy(autoPlayNext = !it.autoPlayNext) }
                    }
                )

                SettingsCycleOptionRow(
                    icon = Icons.Default.History,
                    title = "Resume Playback Behavior",
                    subtitle = "Whether to resume from your saved position or start from 00:00",
                    valueLabel = settings.resumeMode.label,
                    onClick = {
                        val modes = ResumeMode.entries
                        val next = modes[(modes.indexOf(settings.resumeMode) + 1) % modes.size]
                        onUpdateSettings { it.copy(resumeMode = next) }
                    }
                )

                SettingsCycleOptionRow(
                    icon = Icons.Default.ClosedCaption,
                    title = "Subtitles Enabled by Default",
                    subtitle = "Automatically enable external (.srt/.vtt/.ass) or YouTube subtitles when available",
                    valueLabel = if (settings.subtitlesEnabledByDefault) "Enabled" else "Disabled",
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
                title = "3. Interface, Layout & Font Fitting",
                subtitle = "Customize TV vs Tablet layout, sidebar behavior, and button/font scale"
            ) {
                SettingsCycleOptionRow(
                    icon = Icons.Default.Tv,
                    title = "Interface Layout Mode",
                    subtitle = "Switch between Android TV 10-foot rows and Tablet touch grid",
                    valueLabel = settings.uiModePreference.label,
                    onClick = {
                        val modes = UiModePreference.entries
                        val next = modes[(modes.indexOf(settings.uiModePreference) + 1) % modes.size]
                        onUpdateSettings { it.copy(uiModePreference = next) }
                    }
                )

                SettingsCycleOptionRow(
                    icon = Icons.Default.Settings,
                    title = "Sidebar Navigation Behavior",
                    subtitle = "Overlay mode expands over content without resizing the video grid (eliminates D-Pad lag)",
                    valueLabel = settings.sidebarMode.label,
                    onClick = {
                        val modes = SidebarMode.entries
                        val next = modes[(modes.indexOf(settings.sidebarMode) + 1) % modes.size]
                        onUpdateSettings { it.copy(sidebarMode = next) }
                    }
                )

                SettingsCycleOptionRow(
                    icon = Icons.Default.AspectRatio,
                    title = "Button & Text Scale",
                    subtitle = "Adjust text and button density across the app",
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
                title = "4. Storage, USB Drives & Custom Folders",
                subtitle = "Control automatic scanning and manage custom USB/local folders"
            ) {
                SettingsCycleOptionRow(
                    icon = Icons.Default.Storage,
                    title = "Auto-Scan Storage on App Startup",
                    subtitle = "Automatically scan internal storage and USB drives when opening the app",
                    valueLabel = if (settings.autoScanOnStartup) "Enabled" else "Disabled",
                    isAccent = settings.autoScanOnStartup,
                    onClick = {
                        onUpdateSettings { it.copy(autoScanOnStartup = !it.autoScanOnStartup) }
                    }
                )

                SettingsCycleOptionRow(
                    icon = Icons.Default.Usb,
                    title = "Auto-Scan When USB Drive Plugged / Unplugged",
                    subtitle = "Automatically refresh catalog when a USB drive is connected",
                    valueLabel = if (settings.autoScanOnUsbMount) "Enabled" else "Disabled",
                    isAccent = settings.autoScanOnUsbMount,
                    onClick = {
                        onUpdateSettings { it.copy(autoScanOnUsbMount = !it.autoScanOnUsbMount) }
                    }
                )

                SettingsCycleOptionRow(
                    icon = Icons.Default.Usb,
                    title = "Hide Boot / EFI USB Partitions (e.g., VTOYEFI)",
                    subtitle = "Ignores non-media system boot partitions on multi-partition USB drives",
                    valueLabel = if (settings.hideBootUsbPartitions) "Hidden" else "Shown",
                    isAccent = settings.hideBootUsbPartitions,
                    onClick = {
                        onUpdateSettings { it.copy(hideBootUsbPartitions = !it.hideBootUsbPartitions) }
                        onRescanStorage()
                    }
                )

                SettingsCycleOptionRow(
                    icon = Icons.Default.FolderOpen,
                    title = "Add Video Folder (TV & USB Folder Browser)",
                    subtitle = "Browse internal storage or USB drives and add a specific folder",
                    valueLabel = "Browse Folders",
                    isAccent = true,
                    onClick = onAddFolder
                )

                if (safFolders.isNotEmpty()) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    Text(
                        text = "Saved Custom Folders (${safFolders.size})",
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
                            valueLabel = "Remove",
                            onClick = { onRemoveSafFolder(folder.uriString) }
                        )
                    }
                }
            }
        }

        // Section 5: YouTube Playlists, History & Data Reset
        item(key = "settings_data") {
            SettingsCategoryCard(
                title = "5. Saved YouTube Playlists, History & Reset",
                subtitle = "Manage saved YouTube playlists, watch progress, favorites, and app defaults"
            ) {
                if (savedChannels.isNotEmpty()) {
                    Text(
                        text = "Saved YouTube Channels (${savedChannels.size})",
                        color = SmartTubeTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    savedChannels.forEach { chan ->
                        SettingsCycleOptionRow(
                            icon = Icons.Default.AddLink,
                            title = "Channel: ${chan.title}",
                            subtitle = "${chan.handle} • Uploads: ${chan.uploadsPlaylistId}",
                            valueLabel = "Delete Channel",
                            onClick = {
                                pendingDestructiveAction = DestructiveAction(
                                    title = "Delete Channel",
                                    message = "Are you sure you want to delete channel \"${chan.title}\"? All its uploads and playlists will be removed.",
                                    confirmLabel = "Delete",
                                    onConfirm = { onDeleteChannel(chan.channelId) }
                                )
                            }
                        )
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                }

                if (savedPlaylists.isNotEmpty()) {
                    Text(
                        text = "Saved YouTube Playlists (${savedPlaylists.size})",
                        color = SmartTubeTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    savedPlaylists.forEach { pl ->
                        SettingsCycleOptionRow(
                            icon = Icons.Default.AddLink,
                            title = "Playlist: ${pl.title}",
                            subtitle = "${pl.videoCount} videos • ${pl.sourceUrl}",
                            valueLabel = "Delete Playlist",
                            onClick = {
                                pendingDestructiveAction = DestructiveAction(
                                    title = "Delete Playlist",
                                    message = "Are you sure you want to delete playlist \"${pl.title}\"? Its ${pl.videoCount} videos will be removed from your library.",
                                    confirmLabel = "Delete",
                                    onConfirm = { onDeletePlaylist(pl.playlistId) }
                                )
                            }
                        )
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                }

                SettingsCycleOptionRow(
                    icon = Icons.Default.History,
                    title = "Clear Watch History & Playback Progress",
                    subtitle = "Resets Continue Watching positions for all videos",
                    valueLabel = "Clear History",
                    onClick = {
                        pendingDestructiveAction = DestructiveAction(
                            title = "Clear Watch History",
                            message = "Are you sure you want to clear your watch history? Continue Watching progress will be reset for all videos.",
                            confirmLabel = "Clear History",
                            onConfirm = onClearWatchHistory
                        )
                    }
                )

                SettingsCycleOptionRow(
                    icon = Icons.Default.Delete,
                    title = "Clear All Favorites",
                    subtitle = "Removes all videos from your Favorites list",
                    valueLabel = "Clear Favorites",
                    onClick = {
                        pendingDestructiveAction = DestructiveAction(
                            title = "Clear All Favorites",
                            message = "Are you sure you want to remove all saved favorite videos?",
                            confirmLabel = "Clear Favorites",
                            onConfirm = onClearFavorites
                        )
                    }
                )

                SettingsCycleOptionRow(
                    icon = Icons.Default.Delete,
                    title = "Remove All Saved YouTube Links & Playlists",
                    subtitle = "Deletes imported YouTube links while keeping all local & USB videos untouched",
                    valueLabel = "Clear YouTube",
                    onClick = {
                        pendingDestructiveAction = DestructiveAction(
                            title = "Clear YouTube Library",
                            message = "Are you sure you want to delete all saved YouTube playlists, channels, and video links? Local and USB files will remain safe.",
                            confirmLabel = "Clear All",
                            onConfirm = onClearYouTubeLibrary
                        )
                    }
                )

                SettingsCycleOptionRow(
                    icon = Icons.Default.Restore,
                    title = "Reset All Settings to Default",
                    subtitle = "Restores all player, thumbnail, and layout settings to factory defaults",
                    valueLabel = "Reset Defaults",
                    onClick = {
                        pendingDestructiveAction = DestructiveAction(
                            title = "Reset All Settings",
                            message = "Are you sure you want to restore all settings to factory default values?",
                            confirmLabel = "Reset Defaults",
                            onConfirm = onResetSettings
                        )
                    }
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

            SettingsCategoryCard(
                title = "6. App Updates & GitHub Sync",
                subtitle = "Check for newer releases published on GitHub repository (mihm88/JoudJawadTube) and install directly"
            ) {
                var isFocusedCheck by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isFocusedCheck) SmartTubeCardFocusedBg else Color(0xFF191919))
                        .border(
                            width = if (isFocusedCheck) 2.dp else 1.dp,
                            color = if (isFocusedCheck) SmartTubeFocusBorder else Color.White.copy(alpha = 0.06f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = if (isFocusedCheck) SmartTubeRed else SmartTubeUsbCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Installed Version: v$versionName",
                                color = SmartTubeTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Repository: github.com/mihm88/JoudJawadTube",
                                color = SmartTubeTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    ActionPillButton(
                        icon = Icons.Default.Refresh,
                        text = if (updateState is UpdateState_Antigravity.Checking) "Checking..." else "Check for Updates",
                        isPrimary = false,
                        onClick = onCheckForUpdates
                    )
                }

                when (updateState) {
                    is UpdateState_Antigravity.Idle -> { /* Idle */ }
                    is UpdateState_Antigravity.Checking -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF202020))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Connecting to GitHub Releases API to check for updates...",
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
                                .background(Color(0xFF16321F))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "You are on the latest version (${updateState.currentVersion}). No new updates found.",
                                color = Color(0xFFE8F5E9),
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
                                .background(Color(0xFF261D10))
                                .border(1.dp, Color(0xFFFF9800).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "New Update Available: ${info.tagName} (${info.releaseTitle})",
                                        color = Color.White,
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
                                    text = "Download & Install Update",
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
                                .background(Color(0xFF202020))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val downloadedMb = updateState.downloadedBytes / (1024f * 1024f)
                            val totalMb = updateState.totalBytes / (1024f * 1024f)
                            Text(
                                text = "Downloading update: ${updateState.progressPercent}% (${String.format(java.util.Locale.US, "%.1f", downloadedMb)} MB / ${String.format(java.util.Locale.US, "%.1f", totalMb)} MB)",
                                color = SmartTubeTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            LinearProgressIndicator(
                                progress = { updateState.progressPercent / 100f },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = SmartTubeRed,
                                trackColor = Color.DarkGray
                            )
                        }
                    }
                    is UpdateState_Antigravity.ReadyToInstall -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF16321F))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Download complete! Opening package installer...",
                                color = Color(0xFFE8F5E9),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            ActionPillButton(
                                icon = Icons.Default.SystemUpdate,
                                text = "Launch Installer",
                                isPrimary = true,
                                onClick = { onDownloadAndInstallUpdate(updateState.info) }
                            )
                        }
                    }
                    is UpdateState_Antigravity.Error -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF331414))
                                .border(1.dp, SmartTubeRed.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Update Error: ${updateState.message}",
                                color = Color(0xFFFFCDD2),
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            ActionPillButton(
                                icon = Icons.Default.Refresh,
                                text = "Dismiss",
                                isPrimary = false,
                                onClick = onDismissUpdateStatus
                            )
                        }
                    }
                }
            }
        }
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
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
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
        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
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
        else -> Color(0xFF191919)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) SmartTubeFocusBorder else Color.White.copy(alpha = 0.06f),
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
                Text(
                    text = title,
                    color = SmartTubeTextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
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
                        else -> Color(0xFF2C2C2C)
                    }
                )
                .border(
                    width = 1.dp,
                    color = if (isAccent && !isFocused) SmartTubeUsbCyan.copy(alpha = 0.5f) else Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = valueLabel,
                color = if (isAccent && !isFocused) SmartTubeUsbCyan else Color.White,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
