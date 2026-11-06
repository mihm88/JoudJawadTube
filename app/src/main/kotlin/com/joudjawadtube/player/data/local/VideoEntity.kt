package com.joudjawadtube.player.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Locale

@Entity(
    tableName = "videos",
    indices = [
        Index(value = ["videoKey"]),
        Index(value = ["folderPath"]),
        Index(value = ["isUsb"]),
        Index(value = ["isYoutube"]),
        Index(value = ["playlistId"])
    ]
)
data class VideoEntity(
    @PrimaryKey
    val id: String,
    val videoKey: String,
    val uri: String,
    val filePath: String,
    val title: String,
    val fileName: String,
    val folderName: String,
    val folderPath: String,
    val volumeLabel: String,
    val isUsb: Boolean,
    val durationMs: Long,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val mimeType: String,
    val dateModified: Long,
    val sidecarSubtitles: String = "",
    val isYoutube: Boolean = false,
    val youtubeVideoId: String = "",
    val playlistId: String = "",
    val playlistTitle: String = "",
    val thumbnailUrl: String = "",
    val customThumbnailPath: String = ""
)

data class SidecarSubtitle(
    val uriOrPath: String,
    val mimeType: String,
    val language: String,
    val label: String
)

object SubtitleCodec {
    private const val ENTRY_SEP = "|||"
    private const val FIELD_SEP = ":::"

    fun encode(subtitles: List<SidecarSubtitle>): String {
        if (subtitles.isEmpty()) return ""
        return subtitles.joinToString(ENTRY_SEP) { sub ->
            listOf(sub.uriOrPath, sub.mimeType, sub.language, sub.label).joinToString(FIELD_SEP)
        }
    }

    fun decode(encoded: String): List<SidecarSubtitle> {
        if (encoded.isBlank()) return emptyList()
        return encoded.split(ENTRY_SEP).mapNotNull { entry ->
            val parts = entry.split(FIELD_SEP)
            if (parts.size >= 4) {
                SidecarSubtitle(
                    uriOrPath = parts[0],
                    mimeType = parts[1],
                    language = parts[2],
                    label = parts[3]
                )
            } else {
                null
            }
        }
    }
}

object YouTubeIdExtractor {
    // Matches [11-char-id] or (11-char-id) in yt-dlp / youtube-dl filenames
    private val BRACKET_ID_REGEX = Regex("""\[([a-zA-Z0-9_-]{11})\](?=\.[^.\[\]]+$|$)""")
    private val ANY_BRACKET_ID_REGEX = Regex("""\[([a-zA-Z0-9_-]{11})\]""")
    private val URL_ID_REGEX = Regex("""(?:v=|youtu\.be/|shorts/|embed/)([a-zA-Z0-9_-]{11})""")

    fun extractFromFileNameOrUri(fileName: String, uri: String = ""): String? {
        URL_ID_REGEX.find(uri)?.groupValues?.getOrNull(1)?.let { return it }
        BRACKET_ID_REGEX.find(fileName)?.groupValues?.getOrNull(1)?.let { return it }
        val allMatches = ANY_BRACKET_ID_REGEX.findAll(fileName).toList()
        if (allMatches.isNotEmpty()) {
            return allMatches.last().groupValues[1]
        }
        return null
    }

    fun buildYouTubeThumbnailUrl(videoId: String): String {
        return "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
    }
}

data class VideoItem(
    val entity: VideoEntity,
    val progress: PlaybackProgressEntity?
) {
    val id: String get() = entity.id
    val videoKey: String get() = entity.videoKey
    val title: String get() = entity.title
    val folderName: String get() = entity.folderName
    val volumeLabel: String get() = entity.volumeLabel
    val isUsb: Boolean get() = entity.isUsb
    val isYoutube: Boolean get() = entity.isYoutube
    val youtubeVideoId: String
        get() = entity.youtubeVideoId.ifBlank {
            YouTubeIdExtractor.extractFromFileNameOrUri(entity.fileName, entity.uri).orEmpty()
        }
    val playlistId: String get() = entity.playlistId
    val playlistTitle: String get() = entity.playlistTitle
    val durationMs: Long get() = maxOf(entity.durationMs, progress?.durationMs ?: 0L)
    val positionMs: Long get() = progress?.positionMs ?: 0L
    val isFavorite: Boolean get() = progress?.isFavorite ?: false
    val lastPlayedAt: Long get() = progress?.lastPlayedAt ?: 0L

    val hasSidecarSubtitles: Boolean = entity.sidecarSubtitles.isNotBlank()

    val progressFraction: Float = FormatUtils.computeProgressFraction(positionMs, durationMs)

    val formattedDuration: String = FormatUtils.formatDuration(durationMs)

    val formattedPosition: String
        get() = FormatUtils.formatDuration(positionMs)

    val resolutionBadge: String? = FormatUtils.resolutionBadge(entity.width, entity.height)

    val formattedSize: String = FormatUtils.formatFileSize(entity.sizeBytes)

    val sidecarSubtitleList: List<SidecarSubtitle>
        get() = SubtitleCodec.decode(entity.sidecarSubtitles)
}

object FormatUtils {
    fun computeProgressFraction(positionMs: Long, durationMs: Long): Float {
        if (durationMs <= 0L || positionMs <= 0L) return 0f
        return (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    }

    fun formatDuration(durationMs: Long): String {
        if (durationMs <= 0L) return "00:00"
        val totalSeconds = durationMs / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0L) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    fun resolutionBadge(width: Int, height: Int): String? {
        val maxDim = maxOf(width, height)
        val minDim = minOf(width, height)
        return when {
            maxDim >= 3800 || minDim >= 2100 -> "4K"
            maxDim >= 2500 || minDim >= 1400 -> "2K"
            maxDim >= 1900 || minDim >= 1060 -> "1080p"
            maxDim >= 1260 || minDim >= 700 -> "720p"
            maxDim > 0 -> "SD"
            else -> null
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0L) return ""
        val mb = bytes.toDouble() / (1024.0 * 1024.0)
        return if (mb >= 1024.0) {
            String.format(Locale.US, "%.1f GB", mb / 1024.0)
        } else {
            String.format(Locale.US, "%.0f MB", mb)
        }
    }
}
