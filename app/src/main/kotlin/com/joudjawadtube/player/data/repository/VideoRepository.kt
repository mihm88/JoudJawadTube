package com.joudjawadtube.player.data.repository

import android.content.Context
import com.joudjawadtube.player.data.local.FormatUtils
import com.joudjawadtube.player.data.local.PlaybackProgressEntity
import com.joudjawadtube.player.data.local.SavedChannelEntity
import com.joudjawadtube.player.data.local.SavedPlaylistEntity
import com.joudjawadtube.player.data.local.SavedSafFolderEntity
import com.joudjawadtube.player.data.local.VideoDao
import com.joudjawadtube.player.data.local.VideoEntity
import com.joudjawadtube.player.data.local.VideoItem
import com.joudjawadtube.player.data.scanner.LocalVideoScanner
import com.joudjawadtube.player.data.scanner.MountedVolumeInfo
import com.joudjawadtube.player.data.scanner.StorageVolumeHelper
import com.joudjawadtube.player.data.settings.AppLanguage
import com.joudjawadtube.player.data.youtube.ParsedYouTubeInput
import com.joudjawadtube.player.data.youtube.YouTubeService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.Random
import java.util.UUID

data class CategoryRow(
    val id: String,
    val title: String,
    val subtitle: String,
    val isUsbCategory: Boolean = false,
    val isYoutubeCategory: Boolean = false,
    val playlistId: String? = null,
    val channelId: String? = null,
    val category: String = "General",
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
    val savedPlaylists: List<SavedPlaylistEntity> = emptyList(),
    val savedChannels: List<SavedChannelEntity> = emptyList()
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

    // Session-stable PRNG seed created once per app launch so recommendations stay stable while navigating
    // but reshuffle freshly each time the app process starts.
    private val sessionSeed: Long = System.currentTimeMillis() xor UUID.randomUUID().mostSignificantBits

    val catalogFlow: Flow<CatalogSnapshot> = combine(
        videoDao.observeAllVideos(),
        videoDao.observeAllProgress(),
        videoDao.observeSafFolders(),
        videoDao.observeSavedPlaylists(),
        videoDao.observeSavedChannels()
    ) { videos, progressList, safFolders, savedPlaylists, savedChannels ->
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

        // Build Row 2: "Recommended for You"
        // Excludes videos already in Continue Watching or finished (> 95% watched)
        val inProgressKeys = continueWatching.map { it.videoKey }.toSet()
        val candidatePool = items.filter { candidate ->
            candidate.videoKey !in inProgressKeys && candidate.progressFraction < 0.95f
        }

        // Group candidate videos by source: YouTube playlist or local folder
        val groupedBySource = candidatePool.groupBy { video ->
            when {
                video.isYoutube && video.playlistId.isNotBlank() -> "yt_pl_${video.playlistId}"
                video.isYoutube -> "yt_singles"
                else -> "folder_${video.folderName}"
            }
        }

        val sourcePrng = Random(sessionSeed)
        val candidateList = mutableListOf<VideoItem>()
        for ((_, sourceVideos) in groupedBySource) {
            val shuffledSource = sourceVideos.shuffled(sourcePrng)
            candidateList.addAll(shuffledSource.take(3)) // Max 3 per source playlist/folder
        }
        val recommended = candidateList.shuffled(sourcePrng).take(20) // Capped at 20 videos

        val rows = mutableListOf<CategoryRow>()

        // Row 1: Continue Watching
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

        // Row 2: Recommended for You
        if (recommended.isNotEmpty()) {
            rows.add(
                CategoryRow(
                    id = "recommended_for_you",
                    title = "Recommended for You",
                    subtitle = "${recommended.size} picks • Refreshed each app launch",
                    videos = recommended
                )
            )
        }

        // Saved YouTube Playlists rows
        val ytByPlaylist = youtubeVideos.filter { it.playlistId.isNotBlank() }.groupBy { it.playlistId }
        for ((plId, plVideos) in ytByPlaylist) {
            val plMeta = savedPlaylists.firstOrNull { it.playlistId == plId }
            val plTitle = plMeta?.title ?: plVideos.firstOrNull()?.playlistTitle?.ifBlank { "YouTube Playlist" } ?: "YouTube Playlist"
            val plCat = plMeta?.category ?: "General"
            val chId = plMeta?.channelId
            rows.add(
                CategoryRow(
                    id = "yt_playlist_$plId",
                    title = plTitle,
                    subtitle = "${plVideos.size} videos • $plCat",
                    isYoutubeCategory = true,
                    playlistId = plId,
                    channelId = chId,
                    category = plCat,
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

        // Group local & USB videos by folder name
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
            savedPlaylists = savedPlaylists,
            savedChannels = savedChannels
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

            // Auto-sync saved YouTube channels if unsynced or stale
            syncSavedChannels(onlyUnsyncedOrStale = true)
        } finally {
            _isScanning.value = false
        }
    }

    suspend fun seedAndSyncBuiltInChannels(
        removedChannelIds: Set<String>,
        appLanguage: AppLanguage = AppLanguage.FOLLOW_SYSTEM
    ) {
        val builtInChannels = KidsCatalogRepository_Antigravity.loadKidsChannels(appContext, appLanguage)
        if (builtInChannels.isEmpty()) return

        val existingChannels = videoDao.getSavedChannels().associateBy { it.channelId }
        val existingPlaylists = videoDao.getSavedPlaylists().associateBy { it.playlistId }
        val baseTime = System.currentTimeMillis()
        val playlistsToInsert = mutableListOf<SavedPlaylistEntity>()

        builtInChannels.forEachIndexed { idx, item ->
            if (item.channelId in removedChannelIds) return@forEachIndexed
            val existing = existingChannels[item.channelId]
            val targetCategory = existing?.category?.takeIf { it.isNotBlank() && it != "General" } ?: item.category

            if (existing == null) {
                videoDao.insertChannel(
                    SavedChannelEntity(
                        channelId = item.channelId,
                        handle = item.handle,
                        title = item.channelName,
                        avatarUrl = "",
                        uploadsPlaylistId = item.uploadsPlaylistId,
                        category = targetCategory,
                        importedAt = baseTime + idx,
                        lastSyncedAt = 0L,
                        videoCap = 30
                    )
                )
            } else if (existing.title != item.channelName || existing.handle != item.handle) {
                videoDao.insertChannel(
                    existing.copy(
                        title = item.channelName,
                        handle = item.handle.ifBlank { existing.handle }
                    )
                )
            }

            item.playlists.forEachIndexed { pIdx, pl ->
                val existingPl = existingPlaylists[pl.id]
                if (existingPl == null) {
                    playlistsToInsert.add(
                        SavedPlaylistEntity(
                            playlistId = pl.id,
                            title = pl.title,
                            author = item.channelName,
                            sourceUrl = if (pl.id == item.uploadsPlaylistId) {
                                item.preferredUrl
                            } else {
                                "https://www.youtube.com/playlist?list=${pl.id}"
                            },
                            thumbnailUrl = "",
                            videoCount = 0,
                            category = targetCategory,
                            channelId = item.channelId,
                            isAutoCreatedFromChannel = true,
                            addedAt = baseTime + (idx * 10L) + pIdx
                        )
                    )
                } else if (existingPl.isAutoCreatedFromChannel && existingPl.author != item.channelName) {
                    playlistsToInsert.add(
                        existingPl.copy(
                            title = pl.title,
                            author = item.channelName
                        )
                    )
                }
            }
        }

        if (playlistsToInsert.isNotEmpty()) {
            videoDao.insertPlaylists(playlistsToInsert)
        }

        syncSavedChannels(onlyUnsyncedOrStale = true)
    }

    suspend fun addBuiltInChannelToLibrary(
        channelItem: KidsChannelItem
    ) {
        val baseTime = System.currentTimeMillis()
        videoDao.insertChannel(
            SavedChannelEntity(
                channelId = channelItem.channelId,
                handle = channelItem.handle,
                title = channelItem.channelName,
                avatarUrl = "",
                uploadsPlaylistId = channelItem.uploadsPlaylistId,
                category = channelItem.category.ifBlank { "General" },
                importedAt = baseTime,
                lastSyncedAt = 0L,
                videoCap = 30
            )
        )
        val playlists = channelItem.playlists.mapIndexed { pIdx, pl ->
            SavedPlaylistEntity(
                playlistId = pl.id,
                title = pl.title,
                author = channelItem.channelName,
                sourceUrl = if (pl.id == channelItem.uploadsPlaylistId) {
                    channelItem.preferredUrl
                } else {
                    "https://www.youtube.com/playlist?list=${pl.id}"
                },
                thumbnailUrl = "",
                videoCount = 0,
                category = channelItem.category.ifBlank { "General" },
                channelId = channelItem.channelId,
                isAutoCreatedFromChannel = true,
                addedAt = baseTime + pIdx
            )
        }
        if (playlists.isNotEmpty()) {
            videoDao.insertPlaylists(playlists)
        }
        syncChannel(
            SavedChannelEntity(
                channelId = channelItem.channelId,
                handle = channelItem.handle,
                title = channelItem.channelName,
                avatarUrl = "",
                uploadsPlaylistId = channelItem.uploadsPlaylistId,
                category = channelItem.category.ifBlank { "General" },
                importedAt = baseTime,
                lastSyncedAt = 0L,
                videoCap = 30
            )
        )
    }

    suspend fun syncSavedChannels(onlyUnsyncedOrStale: Boolean = false) = coroutineScope {
        val channels = videoDao.getSavedChannels()
        if (channels.isEmpty()) return@coroutineScope
        val now = System.currentTimeMillis()
        val staleThresholdMs = 30 * 60 * 1000L
        val semaphore = Semaphore(4)

        channels.map { channel ->
            async(Dispatchers.IO) {
                val hasVideos = videoDao.getVideosByPlaylistId(channel.uploadsPlaylistId).isNotEmpty()
                val isStale = (now - channel.lastSyncedAt) > staleThresholdMs || channel.lastSyncedAt == 0L
                if (!onlyUnsyncedOrStale || !hasVideos || isStale) {
                    semaphore.withPermit {
                        syncChannel(channel)
                    }
                }
            }
        }.awaitAll()
    }

    private suspend fun syncChannel(channel: SavedChannelEntity) {
        runCatching {
            val uploadsPl = YouTubeService.fetchPlaylist(channel.uploadsPlaylistId, maxVideos = channel.videoCap)
            if (uploadsPl != null && uploadsPl.videos.isNotEmpty()) {
                val baseTime = System.currentTimeMillis()
                val entities = uploadsPl.videos.mapIndexed { index, vid ->
                    VideoEntity(
                        id = "yt_${channel.uploadsPlaylistId}_${vid.videoId}",
                        videoKey = "yt_${vid.videoId}",
                        uri = "https://www.youtube.com/watch?v=${vid.videoId}",
                        filePath = "",
                        title = vid.title,
                        fileName = "${vid.title} [${vid.videoId}].mp4",
                        folderName = channel.title,
                        folderPath = "youtube_channel_${channel.channelId}",
                        volumeLabel = "YouTube Channel",
                        isUsb = false,
                        durationMs = vid.durationMs,
                        sizeBytes = 0L,
                        width = vid.width,
                        height = vid.height,
                        mimeType = "video/mp4",
                        dateModified = baseTime - (index * 1000L),
                        sidecarSubtitles = "",
                        isYoutube = true,
                        youtubeVideoId = vid.videoId,
                        playlistId = channel.uploadsPlaylistId,
                        playlistTitle = "${channel.title} - Uploads",
                        thumbnailUrl = vid.thumbnailUrl
                    )
                }
                videoDao.insertVideos(entities)
                val firstThumb = uploadsPl.videos.firstOrNull()?.thumbnailUrl.orEmpty()
                videoDao.insertChannel(
                    channel.copy(
                        avatarUrl = channel.avatarUrl.ifBlank { firstThumb },
                        lastSyncedAt = System.currentTimeMillis()
                    )
                )
                val existingPl = videoDao.getSavedPlaylists().firstOrNull { it.playlistId == channel.uploadsPlaylistId }
                if (existingPl != null && (existingPl.videoCount != entities.size || existingPl.thumbnailUrl.isBlank())) {
                    videoDao.insertPlaylist(
                        existingPl.copy(
                            videoCount = entities.size,
                            thumbnailUrl = existingPl.thumbnailUrl.ifBlank { firstThumb }
                        )
                    )
                }
            }
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

            is ParsedYouTubeInput.Channel -> {
                importYouTubeChannel(parsed.originalUrl)
            }
        }
    }

    suspend fun importYouTubeChannel(rawUrl: String, videoCap: Int = 200): Result<ImportedYouTubeResult> {
        val channelMeta = YouTubeService.fetchChannel(rawUrl, videoCap)
            ?: return Result.failure(IllegalStateException("Could not resolve YouTube channel from link: $rawUrl"))

        val kidsChannels = KidsCatalogRepository_Antigravity.loadKidsChannels(appContext)
        val matchedKidsChannel = kidsChannels.firstOrNull { it.channelId == channelMeta.channelId }
        val category = matchedKidsChannel?.category?.takeIf { it.isNotBlank() } ?: "General"

        val entity = SavedChannelEntity(
            channelId = channelMeta.channelId,
            handle = channelMeta.handle,
            title = channelMeta.title,
            avatarUrl = channelMeta.avatarUrl,
            uploadsPlaylistId = channelMeta.uploadsPlaylistId,
            category = category,
            importedAt = System.currentTimeMillis(),
            lastSyncedAt = System.currentTimeMillis(),
            videoCap = videoCap
        )
        videoDao.insertChannel(entity)

        if (channelMeta.uploadsVideos.isEmpty()) {
            return Result.failure(IllegalStateException("Channel \"${channelMeta.title}\" has no public uploads or could not be loaded."))
        }

        // Save uploads playlist as a playlist entry in the DB
        val savedPlaylist = SavedPlaylistEntity(
            playlistId = channelMeta.uploadsPlaylistId,
            title = "${channelMeta.title} - Uploads",
            author = channelMeta.title,
            sourceUrl = rawUrl,
            thumbnailUrl = channelMeta.avatarUrl,
            videoCount = channelMeta.uploadsVideos.size,
            category = category,
            channelId = channelMeta.channelId,
            isAutoCreatedFromChannel = true,
            addedAt = System.currentTimeMillis()
        )
        videoDao.insertPlaylist(savedPlaylist)

        // Also save discovered sub-playlists from the channel or kids catalog
        val extraPlaylists = mutableListOf<SavedPlaylistEntity>()
        matchedKidsChannel?.playlists?.forEach { kp ->
            if (kp.id != channelMeta.uploadsPlaylistId) {
                extraPlaylists.add(
                    SavedPlaylistEntity(
                        playlistId = kp.id,
                        title = kp.title,
                        author = channelMeta.title,
                        sourceUrl = "https://www.youtube.com/playlist?list=${kp.id}",
                        thumbnailUrl = channelMeta.avatarUrl,
                        videoCount = 0,
                        category = category,
                        channelId = channelMeta.channelId,
                        isAutoCreatedFromChannel = true,
                        addedAt = System.currentTimeMillis()
                    )
                )
            }
        }
        channelMeta.subPlaylists.forEach { sp ->
            if (sp.playlistId != channelMeta.uploadsPlaylistId && extraPlaylists.none { it.playlistId == sp.playlistId }) {
                extraPlaylists.add(
                    SavedPlaylistEntity(
                        playlistId = sp.playlistId,
                        title = sp.title,
                        author = channelMeta.title,
                        sourceUrl = "https://www.youtube.com/playlist?list=${sp.playlistId}",
                        thumbnailUrl = sp.thumbnailUrl.ifBlank { channelMeta.avatarUrl },
                        videoCount = 0,
                        category = category,
                        channelId = channelMeta.channelId,
                        isAutoCreatedFromChannel = true,
                        addedAt = System.currentTimeMillis()
                    )
                )
            }
        }
        if (extraPlaylists.isNotEmpty()) {
            videoDao.insertPlaylists(extraPlaylists)
        }

        val baseTime = System.currentTimeMillis()
        val entities = channelMeta.uploadsVideos.mapIndexed { index, vid ->
            VideoEntity(
                id = "yt_${channelMeta.uploadsPlaylistId}_${vid.videoId}",
                videoKey = "yt_${vid.videoId}",
                uri = "https://www.youtube.com/watch?v=${vid.videoId}",
                filePath = "",
                title = vid.title,
                fileName = "${vid.title} [${vid.videoId}].mp4",
                folderName = channelMeta.title,
                folderPath = "youtube_channel_${channelMeta.channelId}",
                volumeLabel = "YouTube Channel",
                isUsb = false,
                durationMs = vid.durationMs,
                sizeBytes = 0L,
                width = vid.width,
                height = vid.height,
                mimeType = "video/mp4",
                dateModified = baseTime - (index * 1000L),
                sidecarSubtitles = "",
                isYoutube = true,
                youtubeVideoId = vid.videoId,
                playlistId = channelMeta.uploadsPlaylistId,
                playlistTitle = "${channelMeta.title} - Uploads",
                thumbnailUrl = vid.thumbnailUrl
            )
        }
        videoDao.insertVideos(entities)

        val items = entities.map { ent ->
            VideoItem(ent, videoDao.getProgress(ent.videoKey))
        }

        return Result.success(
            ImportedYouTubeResult(
                firstVideo = items.first(),
                playlist = items,
                summaryMessage = "Imported channel \"${channelMeta.title}\" (${items.size} videos, ${extraPlaylists.size + 1} playlists)"
            )
        )
    }

    suspend fun updateChannelCategory(channelId: String, category: String) {
        videoDao.updateChannelCategory(channelId, category)
        videoDao.getSavedPlaylists().filter { it.channelId == channelId }.forEach {
            videoDao.updatePlaylistCategory(it.playlistId, category)
        }
    }

    suspend fun updatePlaylistCategory(playlistId: String, category: String) {
        videoDao.updatePlaylistCategory(playlistId, category)
    }

    suspend fun deleteYouTubeVideo(videoId: String) {
        videoDao.deleteVideoById(videoId)
    }

    suspend fun deleteYouTubePlaylist(playlistId: String) {
        videoDao.deletePlaylist(playlistId)
        videoDao.deleteVideosByPlaylistId(playlistId)
    }

    suspend fun deleteYouTubeChannel(channelId: String) {
        val uploadsId = if (channelId.startsWith("UC")) "UU" + channelId.substring(2) else channelId
        val channelPlaylists = videoDao.getSavedPlaylists().filter { it.channelId == channelId }
        channelPlaylists.forEach { videoDao.deleteVideosByPlaylistId(it.playlistId) }
        videoDao.deleteChannel(channelId)
        videoDao.deletePlaylist(uploadsId)
        videoDao.deletePlaylistsByChannelId(channelId)
        videoDao.deleteVideosByPlaylistId(uploadsId)
    }

    suspend fun clearAllYouTubeContent() {
        videoDao.clearAllChannels()
        videoDao.clearAllPlaylists()
        videoDao.clearYoutubeVideos()
    }

    suspend fun clearWatchHistory() {
        videoDao.clearWatchHistory()
    }

    suspend fun clearAllFavorites() {
        videoDao.clearAllFavorites()
    }

    suspend fun clearFavorites() {
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
