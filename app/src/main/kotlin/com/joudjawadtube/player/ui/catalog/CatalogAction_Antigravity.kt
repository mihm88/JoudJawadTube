package com.joudjawadtube.player.ui.catalog

import com.joudjawadtube.player.data.local.VideoItem
import com.joudjawadtube.player.data.repository.KidsChannelItem
import com.joudjawadtube.player.data.settings.AppSettingsState
import com.joudjawadtube.player.data.updater.UpdateInfo_Antigravity
import com.joudjawadtube.player.ui.components.SidebarSection
import java.io.File

/**
 * Unidirectional user and UI intent actions dispatched from catalog, kids, and settings screens.
 */
sealed interface CatalogAction {
    data class SelectSection(val section: SidebarSection) : CatalogAction
    data class SelectTopCategory(val category: String) : CatalogAction
    data class UpdateSearchQuery(val query: String) : CatalogAction
    data class SelectFolderFilter(val folderName: String?) : CatalogAction
    data class OpenVideo(val video: VideoItem, val playlist: List<VideoItem> = emptyList()) : CatalogAction
    data class ToggleFavorite(val video: VideoItem) : CatalogAction
    data class OpenVideoOptions(val video: VideoItem) : CatalogAction
    data object OpenAddYouTubeDialog : CatalogAction
    data class DeletePlaylist(val playlistId: String) : CatalogAction
    data class DeleteChannel(val channelId: String) : CatalogAction
    data class ChangeChannelCategory(val channelId: String, val newCategory: String) : CatalogAction
    data class ChangePlaylistCategory(val playlistId: String, val newCategory: String) : CatalogAction
    data class OpenKidsPlaylist(val playlistId: String, val playImmediately: Boolean) : CatalogAction
    data class AddBuiltInChannel(val channel: KidsChannelItem) : CatalogAction
    data object RequestReadPermission : CatalogAction
    data object RequestAllFilesAccess : CatalogAction
    data object PickSafFolder : CatalogAction
    data object RescanStorage : CatalogAction
    data object ToggleTabletGridMode : CatalogAction
    data class UpdateSettings(val transform: (AppSettingsState) -> AppSettingsState) : CatalogAction
    data object RegenerateAllThumbnails : CatalogAction
    data object ClearThumbnailCache : CatalogAction
    data class RemoveSafFolder(val uriString: String) : CatalogAction
    data object ClearWatchHistory : CatalogAction
    data object ClearFavorites : CatalogAction
    data object ClearYouTubeLibrary : CatalogAction
    data object ResetSettings : CatalogAction
    data object CheckForUpdates : CatalogAction
    data class DownloadAndInstallUpdate(val info: UpdateInfo_Antigravity) : CatalogAction
    data class InstallDownloadedApk(val apkFile: File) : CatalogAction
    data object DismissUpdateStatus : CatalogAction
    data object DismissHomeUpdateBanner : CatalogAction
    data class ResolveVideoThumbnail(val video: VideoItem) : CatalogAction
}
