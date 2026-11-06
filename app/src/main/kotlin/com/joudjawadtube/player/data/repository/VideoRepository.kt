package com.joudjawadtube.player.data.repository

import android.content.Context
import com.joudjawadtube.player.data.local.PlaybackProgressEntity
import com.joudjawadtube.player.data.local.SavedPlaylistEntity
import com.joudjawadtube.player.data.local.SavedSafFolderEntity
import com.joudjawadtube.player.data.local.VideoDao
import com.joudjawadtube.player.data.local.VideoEntity
import com.joudjawadtube.player.data.local.VideoItem
import com.joudjawadtube.player.data.scanner.LocalVideoScanner
import com.joudjawadtube.player.data.scanner.MountedVolumeInfo
import com.joudjawadtube.player.data.scanner.StorageVolumeHelper
import com.joudjawadtube.player.data.youtube.ParsedYouTubeInput
import com.joudjawadtube.player.data.youtube.YouTubeService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn

data class CategoryRow(
    val id: String,
    val title: String,
    val subtitle: String,
    val isUsbCategory: Boolean = false,
    val isYoutubeCategory: Boolean = false,
    val playlistId: String? = null,
    val videos: List<VideoItem>
)

data class CatalogSnapshot(
    val allVideos: List<VideoItem> = emptyList(),
    val continueWatching: List<VideoItem> = emptyList(),
    val usbVideos: List<VideoItem> = emptyList(),
    val youtubeVideos: List<VideoItem> = emptyList(),
    val fourKVideos: List<VideoItem> = emptyList(),
    val favoriteVideos: List<VideoItem> = emptyList(),
    val categoryRows: List<CategoryRow> = emptyList(),
    val folderNames: List<String> = emptyList(),
    val safFolders: List<SavedSafFolderEntity> = emptyList(),
    val savedPlaylists: List<SavedPlaylistEntity> = emptyList()
)

data class ImportedYouTubeResult(
    val firstVideo: VideoItem,
    val playlist: List<VideoItem>,
    val summaryMessage: String
)

class VideoRepository(
    private val appContext: Context,
    val videoDao: VideoDao
) {
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _mountedVolumes = MutableStateFlow<List<MountedVolumeInfo>>(emptyList())
    val mountedVolumes: StateFlow<List<MountedVolumeInfo>> = _mountedVolumes.asStateFlow()

    val catalogFlow: Flow<CatalogSnapshot> = combine(
        videoDao.observeAllVideos(),
        videoDao.observeAllProgress(),
        videoDao.observeSafFolders(),
        videoDao.observeSavedPlaylists()
    ) { videos, progressList, safFolders, savedPlaylists ->
        val progressMap = progressList.associateBy { it.videoKey }
        val items = videos.map { entity ->
            VideoItem(entity = entity, progress = progressMap[entity.videoKey])
        }

        val continueWatching = items
            .filter { it.positionMs > 3_000L && it.progressFraction < 0.96f }
            .sortedByDescending { it.lastPlayedAt }

        val usbVideos = items.filter { it.isUsb && !it.isYoutube }
        val youtubeVideos = items.filter { it.isYoutube }
        val fourKVideos = items.filter { it.resolutionBadge == "4K" || it.resolutionBadge == "2K" }
        val favorites = items.filter { it.isFavorite }.sortedByDescending { it.lastPlayedAt }

        val rows = mutableListOf<CategoryRow>()

        if (continueWatching.isNotEmpty()) {
            rows.add(
                CategoryRow(
                    id = "continue_watching",
                    title = "Continue Watching",
                    subtitle = "${continueWatching.size} videos in progress",
                    videos = continueWatching
                )
            )
        }

        // Saved YouTube Playlists rows
        val ytByPlaylist = youtubeVideos.filter { it.playlistId.isNotBlank() }.groupBy { it.playlistId }
        for ((plId, plVideos) in ytByPlaylist) {
            val plMeta = savedPlaylists.firstOrNull { it.playlistId == plId }
            val plTitle = plMeta?.title ?: plVideos.firstOrNull()?.playlistTitle?.ifBlank { "YouTube Playlist" } ?: "YouTube Playlist"
            rows.add(
                CategoryRow(
                    id = "yt_playlist_$plId",
                    title = plTitle,
                    subtitle = "${plVideos.size} videos • YouTube Playlist",
                    isYoutubeCategory = true,
                    playlistId = plId,
                    videos = plVideos
                )
            )
        }

        // Standalone saved YouTube videos
        val standaloneYt = youtubeVideos.filter { it.playlistId.isBlank() }
        if (standaloneYt.isNotEmpty()) {
            rows.add(
                CategoryRow(
                    id = "yt_saved_videos",
                    title = "Saved YouTube Videos",
                    subtitle = "${standaloneYt.size} saved YouTube links",
                    isYoutubeCategory = true,
                    videos = standaloneYt
                )
            )
        }

        if (favorites.isNotEmpty()) {
            rows.add(
                CategoryRow(
                    id = "favorites",
                    title = "Favorites",
                    subtitle = "${favorites.size} saved videos",
                    videos = favorites
                )
            )
        }

        // Group local & USB videos by folder name (without duplicating them under a redundant full-USB row)
        val localAndUsbItems = items.filter { !it.isYoutube }
        val byFolder = localAndUsbItems.groupBy { it.folderName }
        for ((folderName, folderVideos) in byFolder) {
            val isUsbFolder = folderVideos.any { it.isUsb }
            val volLabel = folderVideos.firstOrNull()?.volumeLabel.orEmpty()
            val subtitleText = buildString {
                append("${folderVideos.size} videos")
                if (isUsbFolder && volLabel.isNotBlank()) {
                    append(" • $volLabel")
                } else if (isUsbFolder) {
                    append(" • USB")
                }
            }
            rows.add(
                CategoryRow(
                    id = "folder_$folderName",
                    title = folderName,
                    subtitle = subtitleText,
                    isUsbCategory = isUsbFolder,
                    videos = folderVideos
                )
            )
        }

        CatalogSnapshot(
            allVideos = items,
            continueWatching = continueWatching,
            usbVideos = usbVideos,
            youtubeVideos = youtubeVideos,
            fourKVideos = fourKVideos,
            favoriteVideos = favorites,
            categoryRows = rows,
            folderNames = byFolder.keys.sorted(),
            safFolders = safFolders,
            savedPlaylists = savedPlaylists
        )
    }.flowOn(Dispatchers.Default)

    suspend fun refreshVolumesOnly(hideBootPartitions: Boolean = true) {
        _mountedVolumes.value = StorageVolumeHelper.getMountedVolumes(appContext, hideBootPartitions)
    }

    suspend fun refreshStorageAndScan(hideBootPartitions: Boolean = true) {
        if (_isScanning.value) return
        _isScanning.value = true
        try {
            _mountedVolumes.value = StorageVolumeHelper.getMountedVolumes(appContext, hideBootPartitions)
            val safFolders = videoDao.getSafFolders()
            val scanned = LocalVideoScanner.scanAllSources(appContext, safFolders, hideBootPartitions)
            videoDao.replaceCatalog(scanned)
        } finally {
            _isScanning.value = false
        }
    }

    suspend fun importYouTubeUrl(rawUrl: String): Result<ImportedYouTubeResult> {
        return when (val parsed = YouTubeService.parseInput(rawUrl)) {
            is ParsedYouTubeInput.Invalid -> Result.failure(IllegalArgumentException(parsed.reason))

            is ParsedYouTubeInput.SingleVideo -> {
                val meta = YouTubeService.fetchVideoDetails(parsed.videoId)
                    ?: return Result.failure(IllegalStateException("Could not load YouTube video details."))
                val entity = VideoEntity(
                    id = "yt_${meta.videoId}",
                    videoKey = "yt_${meta.videoId}",
                    uri = "https://www.youtube.com/watch?v=${meta.videoId}",
                    filePath = "",
                    title = meta.title,
                    fileName = "${meta.title} [${meta.videoId}].mp4",
                    folderName = meta.author.ifBlank { "YouTube" },
                    folderPath = "youtube_single_${meta.author}",
                    volumeLabel = "YouTube",
                    isUsb = false,
                    durationMs = meta.durationMs,
                    sizeBytes = 0L,
                    width = meta.width,
                    height = meta.height,
                    mimeType = "video/mp4",
                    dateModified = System.currentTimeMillis(),
                    sidecarSubtitles = "",
                    isYoutube = true,
                    youtubeVideoId = meta.videoId,
                    playlistId = "",
                    playlistTitle = "",
                    thumbnailUrl = meta.thumbnailUrl
                )
                videoDao.insertVideo(entity)
                val progress = videoDao.getProgress(entity.videoKey)
                val item = VideoItem(entity, progress)
                Result.success(
                    ImportedYouTubeResult(
                        firstVideo = item,
                        playlist = listOf(item),
                        summaryMessage = "Saved YouTube video: ${meta.title}"
                    )
                )
            }

            is ParsedYouTubeInput.Playlist -> {
                val playlistMeta = YouTubeService.fetchPlaylist(parsed.playlistId)
                if (playlistMeta == null || playlistMeta.videos.isEmpty()) {
                    // If playlist fetch failed but an initialVideoId was present in the URL, fall back to single video
                    val fallbackVid = parsed.initialVideoId
                    if (!fallbackVid.isNullOrBlank()) {
                        return importYouTubeUrl("https://www.youtube.com/watch?v=$fallbackVid")
                    }
                    return Result.failure(
                        IllegalStateException("Could not fetch videos from playlist (${parsed.playlistId}). Check connection or make sure playlist is public.")
                    )
                }

                val savedPlaylist = SavedPlaylistEntity(
                    playlistId = playlistMeta.playlistId,
                    title = playlistMeta.title,
                    author = playlistMeta.author,
                    sourceUrl = parsed.originalUrl,
                    thumbnailUrl = playlistMeta.thumbnailUrl,
                    videoCount = playlistMeta.videos.size,
                    addedAt = System.currentTimeMillis()
                )
                videoDao.insertPlaylist(savedPlaylist)

                val baseTime = System.currentTimeMillis()
                val entities = playlistMeta.videos.mapIndexed { index, vid ->
                    VideoEntity(
                        id = "yt_${playlistMeta.playlistId}_${vid.videoId}",
                        videoKey = "yt_${vid.videoId}",
                        uri = "https://www.youtube.com/watch?v=${vid.videoId}",
                        filePath = "",
                        title = vid.title,
                        fileName = "${vid.title} [${vid.videoId}].mp4",
                        folderName = playlistMeta.title,
                        folderPath = "youtube_playlist_${playlistMeta.playlistId}",
                        volumeLabel = "YouTube Playlist",
                        isUsb = false,
                        durationMs = vid.durationMs,
                        sizeBytes = 0L,
                        width = vid.width,
                        height = vid.height,
                        mimeType = "video/mp4",
                        dateModified = baseTime + index,
                        sidecarSubtitles = "",
                        isYoutube = true,
                        youtubeVideoId = vid.videoId,
                        playlistId = playlistMeta.playlistId,
                        playlistTitle = playlistMeta.title,
                        thumbnailUrl = vid.thumbnailUrl
                    )
                }
                videoDao.insertVideos(entities)

                val items = entities.map { ent ->
                    VideoItem(ent, videoDao.getProgress(ent.videoKey))
                }
                val startItem = parsed.initialVideoId?.let { initId ->
                    items.firstOrNull { it.youtubeVideoId == initId }
                } ?: items.first()

                Result.success(
                    ImportedYouTubeResult(
                        firstVideo = startItem,
                        playlist = items,
                        summaryMessage = "Saved playlist \"${playlistMeta.title}\" (${items.size} videos)"
                    )
                )
            }
        }
    }

    suspend fun deleteYouTubeVideo(videoId: String) {
        videoDao.deleteVideoById(videoId)
    }

    suspend fun deleteYouTubePlaylist(playlistId: String) {
        videoDao.deletePlaylist(playlistId)
        videoDao.deleteVideosByPlaylistId(playlistId)
    }

    suspend fun clearAllYouTubeContent() {
        videoDao.clearAllPlaylists()
        videoDao.clearYoutubeVideos()
    }

    suspend fun clearWatchHistory() {
        videoDao.clearWatchHistory()
    }

    suspend fun clearAllFavorites() {
        videoDao.clearAllFavorites()
    }

    suspend fun getVideoItemById(videoId: String): VideoItem? {
        val entity = videoDao.getVideoById(videoId) ?: return null
        val progress = videoDao.getProgress(entity.videoKey)
        return VideoItem(entity, progress)
    }

    suspend fun getFolderPlaylist(folderPath: String): List<VideoItem> {
        val entities = videoDao.getVideosByFolder(folderPath)
        return entities.map { entity ->
            VideoItem(entity, videoDao.getProgress(entity.videoKey))
        }
    }

    suspend fun savePlaybackProgress(
        videoKey: String,
        positionMs: Long,
        durationMs: Long
    ) {
        val current = videoDao.getProgress(videoKey)
        val updated = (current ?: PlaybackProgressEntity(videoKey = videoKey)).copy(
            positionMs = positionMs.coerceAtLeast(0L),
            durationMs = maxOf(durationMs, current?.durationMs ?: 0L),
            lastPlayedAt = System.currentTimeMillis(),
            playCount = (current?.playCount ?: 0) + if (current == null) 1 else 0
        )
        videoDao.upsertProgress(updated)
    }

    suspend fun toggleFavorite(videoKey: String) {
        val current = videoDao.getProgress(videoKey)
        val updated = (current ?: PlaybackProgressEntity(videoKey = videoKey)).copy(
            isFavorite = !(current?.isFavorite ?: false),
            lastPlayedAt = maxOf(current?.lastPlayedAt ?: 0L, System.currentTimeMillis())
        )
        videoDao.upsertProgress(updated)
    }

    suspend fun addSafFolder(uriString: String, displayName: String, hideBootPartitions: Boolean = true) {
        videoDao.insertSafFolder(
            SavedSafFolderEntity(
                uriString = uriString,
                displayName = displayName
            )
        )
        refreshStorageAndScan(hideBootPartitions)
    }

    suspend fun removeSafFolder(uriString: String, hideBootPartitions: Boolean = true) {
        videoDao.deleteSafFolder(uriString)
        refreshStorageAndScan(hideBootPartitions)
    }
}
