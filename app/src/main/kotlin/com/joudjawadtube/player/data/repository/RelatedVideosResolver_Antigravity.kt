package com.joudjawadtube.player.data.repository

import com.joudjawadtube.player.data.local.SavedChannelEntity
import com.joudjawadtube.player.data.local.SavedPlaylistEntity
import com.joudjawadtube.player.data.local.VideoItem

enum class RelatedTabMode {
    SOURCE,
    CATEGORY
}

data class RelatedVideos(
    val title: String,
    val isCategory: Boolean,
    val videos: List<VideoItem>,
    val sourceTitle: String = "",
    val sourceVideos: List<VideoItem> = emptyList()
)

/**
 * Resolves both:
 * 1. The specific Channel / Playlist (or Folder) video list (`sourceTitle`, `sourceVideos`)
 * 2. The broader Category (or Folder fallback) video list (`title`, `isCategory`, `videos`)
 */
object RelatedVideosResolver_Antigravity {

    fun resolve(
        current: VideoItem,
        allVideos: List<VideoItem>,
        savedPlaylists: List<SavedPlaylistEntity>,
        savedChannels: List<SavedChannelEntity>,
        maxItems: Int = 40
    ): RelatedVideos? {
        val sourcePair = resolveSourceCollection(
            current = current,
            allVideos = allVideos,
            savedPlaylists = savedPlaylists,
            savedChannels = savedChannels
        )
        val windowedSourceVideos = sourcePair?.second?.let { list ->
            if (list.size > 1) windowAround(current, list, maxItems) else emptyList()
        }.orEmpty()
        val sourceTitle = if (windowedSourceVideos.size > 1) sourcePair?.first.orEmpty() else ""

        if (current.isYoutube) {
            val category = findCategoryForYouTubeVideo(current, savedPlaylists, savedChannels)
            if (!category.isNullOrBlank()) {
                val playlistIdsInCategory = buildSet {
                    savedPlaylists.filter { it.category.equals(category, ignoreCase = true) }
                        .forEach { add(it.playlistId) }
                    savedChannels.filter { it.category.equals(category, ignoreCase = true) }
                        .forEach { if (it.uploadsPlaylistId.isNotBlank()) add(it.uploadsPlaylistId) }
                }
                val channelFolderPathsInCategory = savedChannels
                    .filter { it.category.equals(category, ignoreCase = true) }
                    .map { "youtube_channel_${it.channelId}" }
                    .toSet()

                val rawCategoryVideos = allVideos.filter { item ->
                    item.isYoutube && (
                        item.playlistId in playlistIdsInCategory ||
                            item.entity.folderPath in channelFolderPathsInCategory
                        )
                }
                val deduped = dedupeEnsuringCurrent(current, rawCategoryVideos)
                if (deduped.size > 1) {
                    return RelatedVideos(
                        title = category,
                        isCategory = true,
                        videos = windowAround(current, deduped, maxItems),
                        sourceTitle = sourceTitle,
                        sourceVideos = windowedSourceVideos
                    )
                }
            }
            if (windowedSourceVideos.size > 1 && sourceTitle.isNotBlank()) {
                return RelatedVideos(
                    title = sourceTitle,
                    isCategory = false,
                    videos = windowedSourceVideos,
                    sourceTitle = sourceTitle,
                    sourceVideos = windowedSourceVideos
                )
            }
        }

        // Local / USB fallback (or YouTube fallback if no category peers exist): same folder
        val folderPath = current.entity.folderPath
        if (folderPath.isBlank()) return null
        val folderVideos = dedupeEnsuringCurrent(
            current,
            allVideos.filter { it.entity.folderPath == folderPath }
        )
        if (folderVideos.size <= 1) return null

        val displayFolder = current.folderName.ifBlank { current.volumeLabel }
        if (displayFolder.isBlank()) return null

        val windowedFolder = windowAround(current, folderVideos, maxItems)
        return RelatedVideos(
            title = displayFolder,
            isCategory = false,
            videos = windowedFolder,
            sourceTitle = displayFolder,
            sourceVideos = windowedFolder
        )
    }

    /**
     * Resolves all videos belonging to the same Channel, Playlist, or Folder as [current].
     * Used by both the Player HUD ("Channel / Playlist") and Video Card Options ("Go to Channel / Playlist").
     */
    fun resolveSourceCollection(
        current: VideoItem,
        allVideos: List<VideoItem>,
        savedPlaylists: List<SavedPlaylistEntity>,
        savedChannels: List<SavedChannelEntity>
    ): Pair<String, List<VideoItem>>? {
        if (current.isYoutube) {
            if (current.playlistId.isNotBlank()) {
                val matchedPlaylist = savedPlaylists.firstOrNull { it.playlistId == current.playlistId }
                val matchedChannel = savedChannels.firstOrNull {
                    (it.uploadsPlaylistId.isNotBlank() && it.uploadsPlaylistId == current.playlistId) ||
                        it.channelId == matchedPlaylist?.channelId ||
                        current.entity.folderPath == "youtube_channel_${it.channelId}"
                }
                val samePlaylistVideos = dedupeEnsuringCurrent(
                    current,
                    allVideos.filter { it.isYoutube && it.playlistId == current.playlistId }
                )
                if (samePlaylistVideos.isNotEmpty()) {
                    val label = matchedPlaylist?.title?.takeIf { it.isNotBlank() }
                        ?: matchedChannel?.title?.takeIf { it.isNotBlank() }
                        ?: current.playlistTitle.ifBlank { current.folderName }
                    if (label.isNotBlank()) {
                        return label to samePlaylistVideos
                    }
                }
            }

            val channelByFolder = savedChannels.firstOrNull {
                current.entity.folderPath == "youtube_channel_${it.channelId}"
            }
            if (channelByFolder != null) {
                val chanVideos = dedupeEnsuringCurrent(
                    current,
                    allVideos.filter {
                        it.isYoutube && (
                            it.entity.folderPath == "youtube_channel_${channelByFolder.channelId}" ||
                                (channelByFolder.uploadsPlaylistId.isNotBlank() && it.playlistId == channelByFolder.uploadsPlaylistId)
                            )
                    }
                )
                if (chanVideos.isNotEmpty()) {
                    return channelByFolder.title to chanVideos
                }
            }
        }

        val folderPath = current.entity.folderPath
        if (folderPath.isNotBlank()) {
            val folderVideos = dedupeEnsuringCurrent(
                current,
                allVideos.filter { it.entity.folderPath == folderPath }
            )
            val displayFolder = current.folderName.ifBlank { current.volumeLabel }
            if (folderVideos.isNotEmpty() && displayFolder.isNotBlank()) {
                return displayFolder to folderVideos
            }
        }
        return null
    }

    private fun findCategoryForYouTubeVideo(
        current: VideoItem,
        savedPlaylists: List<SavedPlaylistEntity>,
        savedChannels: List<SavedChannelEntity>
    ): String? {
        savedPlaylists.firstOrNull { it.playlistId == current.playlistId }
            ?.category?.takeIf { it.isNotBlank() }?.let { return it }

        savedChannels.firstOrNull {
            (it.uploadsPlaylistId.isNotBlank() && it.uploadsPlaylistId == current.playlistId) ||
                current.entity.folderPath == "youtube_channel_${it.channelId}"
        }?.category?.takeIf { it.isNotBlank() }?.let { return it }

        return null
    }

    private fun dedupeEnsuringCurrent(current: VideoItem, list: List<VideoItem>): List<VideoItem> {
        val seen = HashSet<String>(list.size + 1)
        val result = ArrayList<VideoItem>(list.size)
        var currentIncluded = false
        val currentKey = dedupeKey(current)

        for (item in list) {
            val key = dedupeKey(item)
            if (key == currentKey) {
                if (!currentIncluded) {
                    result.add(current)
                    seen.add(key)
                    currentIncluded = true
                }
            } else if (seen.add(key)) {
                result.add(item)
            }
        }
        if (!currentIncluded) {
            result.add(0, current)
        }
        return result
    }

    private fun dedupeKey(item: VideoItem): String {
        return if (item.isYoutube && item.youtubeVideoId.isNotBlank()) {
            "yt:${item.youtubeVideoId}"
        } else {
            "id:${item.id}"
        }
    }

    internal fun windowAround(
        current: VideoItem,
        videos: List<VideoItem>,
        maxItems: Int
    ): List<VideoItem> {
        if (maxItems <= 0 || videos.size <= maxItems) return videos
        val currentKey = dedupeKey(current)
        val index = videos.indexOfFirst { dedupeKey(it) == currentKey }.coerceAtLeast(0)
        val half = maxItems / 2
        val maxStart = (videos.size - maxItems).coerceAtLeast(0)
        val start = (index - half).coerceIn(0, maxStart)
        val end = (start + maxItems).coerceAtMost(videos.size)
        return videos.subList(start, end)
    }
}
