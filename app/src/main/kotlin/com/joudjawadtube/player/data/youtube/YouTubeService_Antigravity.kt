package com.joudjawadtube.player.data.youtube

import android.content.Context
import com.joudjawadtube.player.data.local.SidecarSubtitle
import com.joudjawadtube.player.data.local.YouTubeIdExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URLEncoder

data class YouTubeVideoMetadata(
    val videoId: String,
    val title: String,
    val author: String,
    val durationMs: Long,
    val thumbnailUrl: String,
    val width: Int = 1920,
    val height: Int = 1080
)

data class YouTubePlaylistMetadata(
    val playlistId: String,
    val title: String,
    val author: String,
    val thumbnailUrl: String,
    val videos: List<YouTubeVideoMetadata>
)

data class YouTubeChannelMetadata(
    val channelId: String,
    val title: String,
    val handle: String,
    val avatarUrl: String,
    val uploadsPlaylistId: String,
    val uploadsVideos: List<YouTubeVideoMetadata>,
    val subPlaylists: List<YouTubePlaylistMetadata> = emptyList()
)

data class YouTubeQualityVariant(
    val label: String,
    val height: Int,
    val width: Int,
    val videoUrl: String,
    val isProgressive: Boolean = false,
    val isAvc1: Boolean = false
)

object QualitySelectorHelper_Antigravity {
    fun effectiveVerticalResolution(height: Int, width: Int): Int {
        if (height <= 0 && width <= 0) return 0
        if (width <= 0) return height
        if (height <= 0) return (width * 9) / 16
        return if (width >= height) {
            maxOf(height, (width * 9) / 16)
        } else {
            maxOf(width, (height * 9) / 16)
        }
    }

    fun formatQualityLabel(height: Int, width: Int = 0): String {
        val eff = effectiveVerticalResolution(height, width)
        return when {
            eff >= 2160 -> "4K (2160p)"
            eff >= 1440 -> "2K (1440p)"
            eff >= 1080 -> "1080p"
            eff >= 720 -> "720p"
            eff >= 480 -> "480p"
            eff >= 360 -> "360p"
            eff >= 240 -> "240p"
            eff > 0 -> "${eff}p"
            else -> "Auto"
        }
    }

    fun qualityRank(label: String): Int {
        return when {
            label.contains("2160") || label.contains("4K", ignoreCase = true) -> 2160
            label.contains("1440") || label.contains("2K", ignoreCase = true) -> 1440
            label.contains("1080") -> 1080
            label.contains("720") -> 720
            label.contains("480") -> 480
            label.contains("360") -> 360
            label.contains("240") -> 240
            else -> label.filter { it.isDigit() }.toIntOrNull() ?: 0
        }
    }

    fun buildAvailableQualities(
        variants: List<YouTubeQualityVariant>,
        observedTrackResolutions: List<Pair<Int, Int>> = emptyList(),
        fallbackHeight: Int = 0,
        fallbackWidth: Int = 0
    ): List<String> {
        val labelsWithRank = linkedMapOf<String, Int>()
        variants.forEach { v ->
            val label = v.label.ifBlank { formatQualityLabel(v.height, v.width) }
            val rank = qualityRank(label).coerceAtLeast(effectiveVerticalResolution(v.height, v.width))
            if (rank >= 144 && label != "Auto") {
                labelsWithRank[label] = maxOf(labelsWithRank[label] ?: 0, rank)
            }
        }
        observedTrackResolutions.forEach { (w, h) ->
            if (h > 0 || w > 0) {
                val label = formatQualityLabel(h, w)
                val rank = qualityRank(label).coerceAtLeast(effectiveVerticalResolution(h, w))
                if (rank >= 144 && label != "Auto") {
                    labelsWithRank[label] = maxOf(labelsWithRank[label] ?: 0, rank)
                }
            }
        }
        if (labelsWithRank.isEmpty() && (fallbackHeight > 0 || fallbackWidth > 0)) {
            val label = formatQualityLabel(fallbackHeight, fallbackWidth)
            val rank = qualityRank(label)
            if (rank >= 144 && label != "Auto") {
                labelsWithRank[label] = rank
            }
        }
        val sortedTiers = labelsWithRank.entries
            .sortedByDescending { it.value }
            .map { it.key }
        return listOf("Auto") + sortedTiers
    }
}

data class ResolvedYouTubeStream(
    val videoId: String,
    val hlsManifestUrl: String? = null,
    val progressiveUrl: String? = null,
    val videoStreamUrl: String? = null,
    val audioStreamUrl: String? = null,
    val width: Int = 1920,
    val height: Int = 1080,
    val durationMs: Long = 0L,
    val qualityVariants: List<YouTubeQualityVariant> = emptyList(),
    val subtitles: List<SidecarSubtitle> = emptyList(),
    val userAgent: String = "com.google.ios.youtubekids/9.40.0 (iPad14,1; U; CPU iOS 18_1_0 like Mac OS X;)",
    val restrictionReason: String? = null
) {
    val hasDirectStream: Boolean
        get() = !hlsManifestUrl.isNullOrBlank() ||
            !progressiveUrl.isNullOrBlank() ||
            (!videoStreamUrl.isNullOrBlank() && !audioStreamUrl.isNullOrBlank())

    fun primaryPlayableUrl(): String? = videoStreamUrl ?: hlsManifestUrl ?: progressiveUrl

    fun isExpired(
        safetyMarginSec: Long = 120L,
        nowEpochSec: Long = System.currentTimeMillis() / 1000L
    ): Boolean {
        val urlsToCheck = listOfNotNull(
            primaryPlayableUrl(),
            audioStreamUrl?.takeIf { !videoStreamUrl.isNullOrBlank() }
        )
        return urlsToCheck.any { url ->
            YouTubeService.isStreamUrlExpired(url, safetyMarginSec, nowEpochSec)
        }
    }
}

sealed class ParsedYouTubeInput {
    data class SingleVideo(val videoId: String, val originalUrl: String) : ParsedYouTubeInput()
    data class Playlist(val playlistId: String, val initialVideoId: String?, val originalUrl: String) : ParsedYouTubeInput()
    data class Channel(val channelQuery: String, val originalUrl: String) : ParsedYouTubeInput()
    data class Invalid(val reason: String) : ParsedYouTubeInput()
}

object YouTubeService {

    val SAFE_VIDEO_ID_REGEX: Regex
        get() = YouTubeUrlParser_Antigravity.SAFE_VIDEO_ID_REGEX

    private const val INNERTUBE_PLAYER_URL = "https://www.youtube.com/youtubei/v1/player?prettyPrint=false"
    private const val INNERTUBE_BROWSE_URL = "https://www.youtube.com/youtubei/v1/browse?prettyPrint=false"
    private const val INNERTUBE_NEXT_URL = "https://www.youtube.com/youtubei/v1/next?prettyPrint=false"
    private const val INNERTUBE_SEARCH_URL = "https://www.youtube.com/youtubei/v1/search?prettyPrint=false"
    private const val INNERTUBE_RESOLVE_URL = "https://www.youtube.com/youtubei/v1/navigation/resolve_url?prettyPrint=false"
    private const val INNERTUBE_VISITOR_URL = "https://www.youtube.com/youtubei/v1/visitor_id?prettyPrint=false"

    internal const val MAX_STREAM_CACHE_SIZE = 50
    private const val STREAM_CACHE_TTL_MS = 15 * 60 * 1000L // 15 minutes
    private const val VISITOR_DATA_TTL_MS = 30 * 60 * 1000L // 30 minutes

    private data class CachedStreamEntry(
        val stream: ResolvedYouTubeStream,
        val cachedAtMs: Long,
        val fromGeneralFallback: Boolean = false
    )

    private val streamCacheLock = Any()
    private val streamCache = object : LinkedHashMap<String, CachedStreamEntry>(
        MAX_STREAM_CACHE_SIZE,
        0.75f,
        true
    ) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CachedStreamEntry>?): Boolean {
            return size > MAX_STREAM_CACHE_SIZE
        }
    }

    @Volatile
    private var cachedVisitorData: String? = null

    @Volatile
    private var visitorDataFetchedAtMs: Long = 0L

    fun initialize(context: Context) {
        InnerTubeClientConfig_Antigravity.initialize(context)
    }

    fun parseInput(rawInput: String): ParsedYouTubeInput =
        YouTubeUrlParser_Antigravity.parseInput(rawInput)

    fun extractExpireEpochSec(url: String?): Long? =
        YouTubeUrlParser_Antigravity.extractExpireEpochSec(url)

    fun isStreamUrlExpired(
        url: String?,
        safetyMarginSec: Long = 120L,
        nowEpochSec: Long = System.currentTimeMillis() / 1000L
    ): Boolean = YouTubeUrlParser_Antigravity.isStreamUrlExpired(url, safetyMarginSec, nowEpochSec)

    fun cleanTitleForYouTubeSearch(rawTitle: String): String =
        YouTubeUrlParser_Antigravity.cleanTitleForYouTubeSearch(rawTitle)

    /**
     * Fetches single video metadata (title, channel, duration, HD thumbnail).
     */
    suspend fun fetchVideoDetails(videoId: String): YouTubeVideoMetadata? = withContext(Dispatchers.IO) {
        if (!SAFE_VIDEO_ID_REGEX.matches(videoId)) return@withContext null

        val webProfile = InnerTubeClientConfig_Antigravity.getClient("WEB")
        // 1. Try InnerTube player for full duration & dimensions
        val playerJson = callInnerTubePlayer(
            videoId = videoId,
            profile = webProfile
        )
        val videoDetails = playerJson?.optJSONObject("videoDetails")
        if (videoDetails != null) {
            val title = videoDetails.optString("title").takeIf { it.isNotBlank() }
            val author = videoDetails.optString("author").ifBlank { "YouTube" }
            val lengthSec = videoDetails.optString("lengthSeconds").toLongOrNull() ?: 0L
            if (!title.isNullOrBlank()) {
                return@withContext YouTubeVideoMetadata(
                    videoId = videoId,
                    title = title,
                    author = author,
                    durationMs = lengthSec * 1000L,
                    thumbnailUrl = YouTubeIdExtractor.buildYouTubeThumbnailUrl(videoId)
                )
            }
        }

        // 2. Fallback to YouTube oEmbed endpoint
        runCatching {
            val oembedUrl = "https://www.youtube.com/oembed?url=" +
                URLEncoder.encode("https://www.youtube.com/watch?v=$videoId", "UTF-8") +
                "&format=json"
            val raw = InnerTubeHttpClient_Antigravity.getString(oembedUrl)
            if (!raw.isNullOrBlank()) {
                val obj = JSONObject(raw)
                val title = obj.optString("title").ifBlank { "YouTube Video ($videoId)" }
                val author = obj.optString("author_name").ifBlank { "YouTube" }
                return@withContext YouTubeVideoMetadata(
                    videoId = videoId,
                    title = title,
                    author = author,
                    durationMs = 0L,
                    thumbnailUrl = YouTubeIdExtractor.buildYouTubeThumbnailUrl(videoId)
                )
            }
        }

        YouTubeVideoMetadata(
            videoId = videoId,
            title = "YouTube Video ($videoId)",
            author = "YouTube",
            durationMs = 0L,
            thumbnailUrl = YouTubeIdExtractor.buildYouTubeThumbnailUrl(videoId)
        )
    }

    /**
     * Fetches a full YouTube playlist and all its individual videos via InnerTube browse/next APIs.
     * Supports modern lockupViewModel playlists, classic playlistVideoRenderer, and playlistPanelVideoRenderer.
     */
    suspend fun fetchPlaylist(playlistId: String, maxVideos: Int = 200): YouTubePlaylistMetadata? = withContext(Dispatchers.IO) {
        val webProfile = InnerTubeClientConfig_Antigravity.getClient("WEB")
        val browseId = if (playlistId.startsWith("VL")) playlistId else "VL$playlistId"
        val webBody = buildWebContextBody(webProfile) {
            put("browseId", browseId)
        }

        val responseStr = InnerTubeHttpClient_Antigravity.postJson(
            url = INNERTUBE_BROWSE_URL,
            jsonBody = webBody.toString(),
            userAgent = webProfile.userAgent
        )

        var playlistTitle: String? = null
        var playlistAuthor: String? = null
        val videos = mutableListOf<YouTubeVideoMetadata>()
        var continuationToken: String? = null

        if (!responseStr.isNullOrBlank()) {
            runCatching {
                val root = JSONObject(responseStr)
                playlistTitle = YouTubePlaylistParser_Antigravity.extractPlaylistTitle(root)
                playlistAuthor = YouTubePlaylistParser_Antigravity.extractPlaylistAuthor(root)
                YouTubePlaylistParser_Antigravity.collectPlaylistVideosRecursive(
                    node = root,
                    defaultAuthor = playlistAuthor ?: "YouTube Playlist",
                    sink = videos
                )
                continuationToken = YouTubePlaylistParser_Antigravity.extractContinuationToken(root)
            }
        }

        // Page through continuation tokens up to maxVideos (e.g. for channel uploads)
        var pagesCount = 0
        while (!continuationToken.isNullOrBlank() && videos.size < maxVideos && pagesCount < 4) {
            pagesCount++
            val contBody = buildWebContextBody(webProfile) {
                put("continuation", continuationToken)
            }
            val contResponse = InnerTubeHttpClient_Antigravity.postJson(
                url = INNERTUBE_BROWSE_URL,
                jsonBody = contBody.toString(),
                userAgent = webProfile.userAgent
            )
            if (!contResponse.isNullOrBlank()) {
                runCatching {
                    val contRoot = JSONObject(contResponse)
                    YouTubePlaylistParser_Antigravity.collectPlaylistVideosRecursive(
                        node = contRoot,
                        defaultAuthor = playlistAuthor ?: "YouTube Playlist",
                        sink = videos
                    )
                    continuationToken = YouTubePlaylistParser_Antigravity.extractContinuationToken(contRoot)
                }
            } else {
                break
            }
        }

        // Fallback to /youtubei/v1/next (watch playlist panel) if browse returned no items
        if (videos.isEmpty()) {
            val nextBody = buildWebContextBody(webProfile) {
                put("playlistId", playlistId)
            }
            val nextStr = InnerTubeHttpClient_Antigravity.postJson(
                url = INNERTUBE_NEXT_URL,
                jsonBody = nextBody.toString(),
                userAgent = webProfile.userAgent
            )
            if (!nextStr.isNullOrBlank()) {
                runCatching {
                    val nextRoot = JSONObject(nextStr)
                    if (playlistTitle == null) playlistTitle = YouTubePlaylistParser_Antigravity.extractPlaylistTitle(nextRoot)
                    if (playlistAuthor == null) playlistAuthor = YouTubePlaylistParser_Antigravity.extractPlaylistAuthor(nextRoot)
                    YouTubePlaylistParser_Antigravity.collectPlaylistVideosRecursive(
                        node = nextRoot,
                        defaultAuthor = playlistAuthor ?: "YouTube Playlist",
                        sink = videos
                    )
                }
            }
        }

        // Fallback to YouTube public XML/Atom RSS feed if InnerTube browse/next returned empty (e.g., hidden UU tab)
        if (videos.isEmpty()) {
            val rssUrl = if (playlistId.startsWith("UU") && playlistId.length >= 24) {
                val channelId = "UC" + playlistId.substring(2)
                "https://www.youtube.com/feeds/videos.xml?channel_id=$channelId"
            } else {
                "https://www.youtube.com/feeds/videos.xml?playlist_id=$playlistId"
            }
            val xml = InnerTubeHttpClient_Antigravity.getString(rssUrl)
            if (!xml.isNullOrBlank()) {
                val feedTitle = Regex("""<feed[^>]*>[\s\S]*?<title>([^<]+)</title>""").find(xml)?.groupValues?.getOrNull(1)?.trim()
                if (playlistTitle == null && !feedTitle.isNullOrBlank()) playlistTitle = feedTitle
                if (playlistAuthor == null && !feedTitle.isNullOrBlank()) playlistAuthor = feedTitle
                val entryRegex = Regex("""<entry>([\s\S]*?)</entry>""")
                val vidRegex = Regex("""<yt:videoId>([a-zA-Z0-9_-]{11})</yt:videoId>""")
                val titleRegex = Regex("""<title>([\s\S]*?)</title>""")
                val authorRegex = Regex("""<author>\s*<name>([\s\S]*?)</name>""")
                entryRegex.findAll(xml).forEach { match ->
                    val block = match.groupValues[1]
                    val vid = vidRegex.find(block)?.groupValues?.getOrNull(1) ?: return@forEach
                    val rawTitle = titleRegex.find(block)?.groupValues?.getOrNull(1)?.trim().orEmpty()
                    val cleanTitle = rawTitle
                        .replace("&amp;", "&")
                        .replace("&lt;", "<")
                        .replace("&gt;", ">")
                        .replace("&quot;", "\"")
                        .replace("&#39;", "'")
                        .ifBlank { "Video $vid" }
                    val author = authorRegex.find(block)?.groupValues?.getOrNull(1)?.trim()
                        ?.replace("&amp;", "&")
                        ?: playlistAuthor
                        ?: "YouTube Channel"
                    videos.add(
                        YouTubeVideoMetadata(
                            videoId = vid,
                            title = cleanTitle,
                            author = author,
                            durationMs = 0L,
                            thumbnailUrl = YouTubeIdExtractor.buildYouTubeThumbnailUrl(vid)
                        )
                    )
                }
            }
        }

        val uniqueVideos = videos.distinctBy { it.videoId }.take(maxVideos)
        if (uniqueVideos.isEmpty()) return@withContext null

        val finalTitle = playlistTitle ?: "YouTube Playlist ($playlistId)"
        val finalAuthor = playlistAuthor ?: uniqueVideos.first().author.ifBlank { "YouTube Playlist" }
        val thumb = uniqueVideos.firstOrNull()?.thumbnailUrl
            ?: YouTubeIdExtractor.buildYouTubeThumbnailUrl(uniqueVideos.first().videoId)

        YouTubePlaylistMetadata(
            playlistId = playlistId,
            title = finalTitle,
            author = finalAuthor,
            thumbnailUrl = thumb,
            videos = uniqueVideos
        )
    }

    suspend fun resolveChannel(channelQuery: String): Pair<String, String>? = withContext(Dispatchers.IO) {
        val trimmed = channelQuery.trim()
        if (trimmed.startsWith("UC") && trimmed.length >= 24 && !trimmed.contains("/")) {
            return@withContext Pair(trimmed, trimmed)
        }

        val cleanUrl = when {
            trimmed.startsWith("http") -> trimmed
            trimmed.startsWith("@") -> "https://www.youtube.com/$trimmed"
            trimmed.startsWith("channel/") -> "https://www.youtube.com/$trimmed"
            trimmed.startsWith("c/") -> "https://www.youtube.com/$trimmed"
            trimmed.startsWith("user/") -> "https://www.youtube.com/$trimmed"
            else -> "https://www.youtube.com/@$trimmed"
        }

        val webProfile = InnerTubeClientConfig_Antigravity.getClient("WEB")
        try {
            val payload = JSONObject().apply {
                put(
                    "context",
                    JSONObject().apply {
                        put(
                            "client",
                            JSONObject().apply {
                                put("clientName", webProfile.clientName)
                                put("clientVersion", "2.20240410.01.00")
                                put("hl", "en")
                                put("gl", "US")
                            }
                        )
                    }
                )
                put("url", cleanUrl)
            }
            val response = InnerTubeHttpClient_Antigravity.postJson(
                url = INNERTUBE_RESOLVE_URL,
                jsonBody = payload.toString(),
                userAgent = webProfile.userAgent
            ) ?: return@withContext null

            val root = JSONObject(response)
            val browseEndpoint = root.optJSONObject("endpoint")?.optJSONObject("browseEndpoint")
            val browseId = browseEndpoint?.optString("browseId")
            val canon = browseEndpoint?.optString("canonicalBaseUrl").orEmpty()
            if (!browseId.isNullOrBlank()) {
                Pair(browseId, if (canon.isNotBlank()) canon else cleanUrl)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun fetchChannel(
        channelQuery: String,
        videoCap: Int = 200
    ): YouTubeChannelMetadata? = withContext(Dispatchers.IO) {
        val resolved = resolveChannel(channelQuery) ?: return@withContext null
        val channelId = resolved.first
        val handle = resolved.second

        val uploadsPlaylistId = if (channelId.startsWith("UC")) "UU" + channelId.substring(2) else channelId
        val uploadsPl = fetchPlaylist(uploadsPlaylistId, maxVideos = videoCap)

        val channelTitle = uploadsPl?.author?.takeIf { it.isNotBlank() && it != "YouTube Playlist" }
            ?: handle.removePrefix("/@").removePrefix("@").ifBlank { "YouTube Channel" }

        val subPlaylists = fetchChannelPublicPlaylists(channelId)

        YouTubeChannelMetadata(
            channelId = channelId,
            title = channelTitle,
            handle = handle,
            avatarUrl = uploadsPl?.thumbnailUrl.orEmpty(),
            uploadsPlaylistId = uploadsPlaylistId,
            uploadsVideos = uploadsPl?.videos ?: emptyList(),
            subPlaylists = subPlaylists
        )
    }

    suspend fun fetchChannelPublicPlaylists(channelId: String): List<YouTubePlaylistMetadata> = withContext(Dispatchers.IO) {
        val webProfile = InnerTubeClientConfig_Antigravity.getClient("WEB")
        val playlists = mutableListOf<YouTubePlaylistMetadata>()
        val browseId = if (channelId.startsWith("UC")) channelId else "UC$channelId"
        val payload = buildWebContextBody(webProfile) {
            put("browseId", browseId)
            put("params", "EglwbGF5bGlzdHPyBgQKAkIA")
        }
        val response = InnerTubeHttpClient_Antigravity.postJson(
            url = INNERTUBE_BROWSE_URL,
            jsonBody = payload.toString(),
            userAgent = webProfile.userAgent
        )
        if (!response.isNullOrBlank()) {
            runCatching {
                val root = JSONObject(response)
                YouTubePlaylistParser_Antigravity.collectChannelPlaylistsRecursive(root, playlists)
            }
        }
        playlists.distinctBy { it.playlistId }
    }

    fun invalidateStreamCache(videoId: String) {
        if (videoId.isNotBlank()) {
            synchronized(streamCacheLock) {
                streamCache.remove(videoId)
            }
        }
    }

    private fun getValidCachedStream(
        videoId: String,
        allowGeneralClientFallback: Boolean
    ): ResolvedYouTubeStream? {
        val now = System.currentTimeMillis()
        val cached = synchronized(streamCacheLock) { streamCache[videoId] } ?: return null
        val isFresh = (now - cached.cachedAtMs) < STREAM_CACHE_TTL_MS &&
            (!cached.fromGeneralFallback || allowGeneralClientFallback) &&
            cached.stream.hasDirectStream &&
            !cached.stream.isExpired()
        return if (isFresh) cached.stream else null
    }

    private fun putCachedStream(
        videoId: String,
        stream: ResolvedYouTubeStream,
        fromGeneralFallback: Boolean
    ) {
        synchronized(streamCacheLock) {
            streamCache[videoId] = CachedStreamEntry(
                stream = stream,
                cachedAtMs = System.currentTimeMillis(),
                fromGeneralFallback = fromGeneralFallback
            )
        }
    }

    internal fun cacheSizeForTesting(): Int = synchronized(streamCacheLock) { streamCache.size }

    internal fun containsCachedVideoForTesting(videoId: String): Boolean =
        synchronized(streamCacheLock) { streamCache.containsKey(videoId) }

    internal fun clearCacheForTesting() {
        synchronized(streamCacheLock) { streamCache.clear() }
    }

    internal fun putCacheEntryForTesting(
        videoId: String,
        stream: ResolvedYouTubeStream,
        fromGeneralFallback: Boolean = false
    ) {
        putCachedStream(videoId, stream, fromGeneralFallback)
    }

    suspend fun refreshStreamForPlayback(
        videoId: String,
        allowGeneralClientFallback: Boolean = false
    ): ResolvedYouTubeStream {
        invalidateStreamCache(videoId)
        return resolveStreamForPlayback(videoId, allowGeneralClientFallback = allowGeneralClientFallback)
    }

    private suspend fun getOrFetchVisitorData(): String? {
        val now = System.currentTimeMillis()
        cachedVisitorData?.takeIf { it.isNotBlank() && (now - visitorDataFetchedAtMs) < VISITOR_DATA_TTL_MS }?.let {
            return it
        }
        val body = JSONObject().apply {
            put(
                "context",
                JSONObject().apply {
                    put(
                        "client",
                        JSONObject().apply {
                            put("clientName", "WEB")
                            put("clientVersion", "2.20250101.00.00")
                        }
                    )
                }
            )
        }
        val webProfile = InnerTubeClientConfig_Antigravity.getClient("WEB")
        val resp = InnerTubeHttpClient_Antigravity.postJson(
            url = INNERTUBE_VISITOR_URL,
            jsonBody = body.toString(),
            userAgent = webProfile.userAgent
        ) ?: return cachedVisitorData

        val parsed = runCatching {
            JSONObject(resp).optJSONObject("responseContext")?.optString("visitorData")
        }.getOrNull()?.takeIf { it.isNotBlank() }

        if (parsed != null) {
            cachedVisitorData = parsed
            visitorDataFetchedAtMs = now
        }
        return parsed ?: cachedVisitorData
    }

    /**
     * Pre-warms the stream cache in the background (e.g., for the next video in a playlist).
     */
    suspend fun preResolveStream(
        videoId: String,
        allowGeneralClientFallback: Boolean = false
    ) {
        if (!SAFE_VIDEO_ID_REGEX.matches(videoId)) return
        if (getValidCachedStream(videoId, allowGeneralClientFallback) != null) return
        runCatching {
            resolveStreamForPlayback(videoId, allowGeneralClientFallback = allowGeneralClientFallback)
        }
    }

    /**
     * Resolves direct playable video/audio/HLS streams for Media3 ExoPlayer.
     * Uses cancellable OkHttp parallel client racing + bounded LRU stream caching.
     */
    suspend fun resolveStreamForPlayback(
        videoId: String,
        allowGeneralClientFallback: Boolean = false
    ): ResolvedYouTubeStream = withContext(Dispatchers.IO) {
        if (!SAFE_VIDEO_ID_REGEX.matches(videoId)) {
            return@withContext ResolvedYouTubeStream(videoId = videoId)
        }

        getValidCachedStream(videoId, allowGeneralClientFallback)?.let { return@withContext it }

        // Wave 1: True first-to-finish parallel race between IOS_KIDS and ANDROID_KIDS (~300ms).
        val kidsProfiles = listOf(
            InnerTubeClientConfig_Antigravity.getClient("IOS_KIDS"),
            InnerTubeClientConfig_Antigravity.getClient("ANDROID_KIDS")
        )
        val wave1Winner = coroutineScope {
            val resultChannel = Channel<ResolvedYouTubeStream?>(capacity = kidsProfiles.size)
            val jobs = kidsProfiles.map { profile ->
                launch {
                    val resolved = resolveWithClientProfile(videoId, profile)
                    resultChannel.trySend(resolved)
                }
            }

            var winner: ResolvedYouTubeStream? = null
            repeat(jobs.size) {
                val candidate = resultChannel.receive()
                if (candidate != null) {
                    winner = candidate
                    jobs.forEach { it.cancel() }
                    return@coroutineScope winner
                }
            }
            jobs.forEach { it.cancel() }
            null
        }

        if (wave1Winner != null) {
            putCachedStream(videoId, wave1Winner, fromGeneralFallback = false)
            return@withContext wave1Winner
        }

        if (!allowGeneralClientFallback) {
            return@withContext ResolvedYouTubeStream(
                videoId = videoId,
                restrictionReason = "KIDS_RESTRICTED"
            )
        }

        // Wave 2 Fallback (for general non-Kids videos): Race ANDROID, IOS, and ANDROID_VR in parallel.
        val visitorData = getOrFetchVisitorData()
        val generalProfiles = listOf(
            InnerTubeClientConfig_Antigravity.getClient("ANDROID"),
            InnerTubeClientConfig_Antigravity.getClient("IOS"),
            InnerTubeClientConfig_Antigravity.getClient("ANDROID_VR")
        )
        val wave2Winner = coroutineScope {
            val resultChannel = Channel<ResolvedYouTubeStream?>(capacity = generalProfiles.size)
            val jobs = generalProfiles.map { profile ->
                launch {
                    val resolved = resolveWithClientProfile(
                        videoId = videoId,
                        profile = profile,
                        visitorData = visitorData,
                        validateAdaptiveProbe = true
                    )
                    resultChannel.trySend(resolved)
                }
            }

            var bestFallback: ResolvedYouTubeStream? = null
            var adaptiveWinner: ResolvedYouTubeStream? = null
            var sawAgeOrContentRestriction = false
            repeat(jobs.size) {
                val candidate = resultChannel.receive()
                if (candidate != null) {
                    if (candidate.restrictionReason == "KIDS_RESTRICTED") {
                        sawAgeOrContentRestriction = true
                    } else if (!candidate.videoStreamUrl.isNullOrBlank() && !candidate.audioStreamUrl.isNullOrBlank()) {
                        adaptiveWinner = candidate
                        jobs.forEach { it.cancel() }
                        return@coroutineScope candidate
                    } else if (bestFallback == null) {
                        bestFallback = candidate
                    }
                }
            }
            jobs.forEach { it.cancel() }
            adaptiveWinner ?: bestFallback ?: if (sawAgeOrContentRestriction) {
                ResolvedYouTubeStream(videoId = videoId, restrictionReason = "KIDS_RESTRICTED")
            } else {
                null
            }
        }

        if (wave2Winner != null) {
            if (wave2Winner.hasDirectStream) {
                putCachedStream(videoId, wave2Winner, fromGeneralFallback = true)
            }
            return@withContext wave2Winner
        }

        ResolvedYouTubeStream(videoId = videoId)
    }

    fun isAgeOrContentRestricted(playerJson: JSONObject?): Boolean {
        val playability = playerJson?.optJSONObject("playabilityStatus") ?: return false
        val status = playability.optString("status", "").uppercase(java.util.Locale.ROOT)
        if (status == "LOGIN_REQUIRED" ||
            status == "AGE_CHECK_REQUIRED" ||
            status == "CONTENT_CHECK_REQUIRED"
        ) {
            return true
        }
        val reason = playability.optString("reason", "").lowercase(java.util.Locale.ROOT)
        return reason.contains("sign in to confirm your age") ||
            reason.contains("age-restricted") ||
            reason.contains("inappropriate") ||
            reason.contains("offensive")
    }

    private suspend fun resolveWithClientProfile(
        videoId: String,
        profile: InnerTubeClientProfile_Antigravity,
        visitorData: String? = null,
        validateAdaptiveProbe: Boolean = false
    ): ResolvedYouTubeStream? {
        val json = callInnerTubePlayer(
            videoId = videoId,
            profile = profile,
            visitorData = visitorData
        )
        if (isAgeOrContentRestricted(json)) {
            return ResolvedYouTubeStream(videoId = videoId, restrictionReason = "KIDS_RESTRICTED")
        }
        val parsed = parseStreamingData(videoId, json, profile.userAgent)?.takeIf { it.hasDirectStream }
            ?: return null
        return if (validateAdaptiveProbe) {
            sanitizeAdaptiveStreamIfForbidden(parsed).takeIf { it.hasDirectStream }
        } else {
            parsed
        }
    }

    /**
     * Checks whether a Wave-2 (ANDROID/IOS) adaptive video URL responds with HTTP 200/206.
     * If Google CDN returns 403 Forbidden (due to missing poToken on non-Kids clients),
     * strips the dead adaptive URLs so ExoPlayer plays the working progressive or HLS stream immediately.
     */
    private suspend fun sanitizeAdaptiveStreamIfForbidden(stream: ResolvedYouTubeStream): ResolvedYouTubeStream {
        val adaptiveUrl = stream.videoStreamUrl
        if (adaptiveUrl.isNullOrBlank()) return stream
        val probeOk = InnerTubeHttpClient_Antigravity.probeRangeOk(adaptiveUrl, stream.userAgent)
        return if (probeOk) {
            stream
        } else {
            stream.copy(
                videoStreamUrl = null,
                audioStreamUrl = null,
                qualityVariants = stream.qualityVariants.filter { it.isProgressive }
            )
        }
    }

    /**
     * Searches YouTube by video title to find the official YouTube videoId.
     */
    suspend fun searchVideoIdByTitle(rawTitle: String): String? = withContext(Dispatchers.IO) {
        val cleanedQuery = cleanTitleForYouTubeSearch(rawTitle)
        if (cleanedQuery.length < 3) return@withContext null

        val webProfile = InnerTubeClientConfig_Antigravity.getClient("WEB")
        val body = buildWebContextBody(webProfile) {
            put("query", cleanedQuery)
        }

        val response = InnerTubeHttpClient_Antigravity.postJson(
            url = INNERTUBE_SEARCH_URL,
            jsonBody = body.toString(),
            userAgent = webProfile.userAgent
        ) ?: return@withContext null

        val match = Regex(""""videoRenderer"\s*:\s*\{\s*"videoId"\s*:\s*"([a-zA-Z0-9_-]{11})"""").find(response)
        match?.groupValues?.getOrNull(1)
    }

    private fun parseStreamingData(
        videoId: String,
        playerJson: JSONObject?,
        userAgent: String
    ): ResolvedYouTubeStream? {
        if (playerJson == null) return null
        val streamingData = playerJson.optJSONObject("streamingData") ?: return null
        val videoDetails = playerJson.optJSONObject("videoDetails")
        val durationMs = (videoDetails?.optString("lengthSeconds")?.toLongOrNull() ?: 0L) * 1000L

        val hlsUrl = streamingData.optString("hlsManifestUrl").takeIf { it.isNotBlank() }

        var bestProgressiveUrl: String? = null
        var bestProgressiveHeight = 0

        val formats = streamingData.optJSONArray("formats")
        if (formats != null) {
            for (i in 0 until formats.length()) {
                val fmt = formats.optJSONObject(i) ?: continue
                val url = fmt.optString("url").takeIf { it.isNotBlank() } ?: continue
                val h = fmt.optInt("height", 360)
                if (h >= bestProgressiveHeight) {
                    bestProgressiveHeight = h
                    bestProgressiveUrl = url
                }
            }
        }

        var bestVideoUrl: String? = null
        var bestVideoHeight = 0
        var bestVideoWidth = 1920
        var bestVideoIsAvc1 = false
        var bestAudioUrl: String? = null
        var bestAudioBitrate = 0

        val adaptive = streamingData.optJSONArray("adaptiveFormats")
        if (adaptive != null) {
            for (i in 0 until adaptive.length()) {
                val fmt = adaptive.optJSONObject(i) ?: continue
                val url = fmt.optString("url").takeIf { it.isNotBlank() } ?: continue
                val mime = fmt.optString("mimeType")
                if (mime.startsWith("video/")) {
                    // Skip AV1 (av01) as many TV hardware decoders fail or drop frames on AV1
                    if (mime.contains("av01")) continue
                    val h = fmt.optInt("height", 0)
                    val w = fmt.optInt("width", 0)
                    val isAvc1 = mime.contains("avc1")
                    // Prefer H.264 (avc1) up to 1080p for guaranteed hardware decoding on all TVs
                    val isBetter = when {
                        bestVideoUrl == null -> h in 240..1080
                        isAvc1 && !bestVideoIsAvc1 && h in 480..1080 -> true
                        !isAvc1 && bestVideoIsAvc1 -> false
                        else -> h in 360..1080 && (h > bestVideoHeight || (h == bestVideoHeight && isAvc1))
                    }
                    if (isBetter) {
                        bestVideoHeight = h
                        bestVideoWidth = if (w > 0) w else 1920
                        bestVideoUrl = url
                        bestVideoIsAvc1 = isAvc1
                    }
                } else if (mime.startsWith("audio/")) {
                    val br = fmt.optInt("bitrate", 0)
                    val isMp4a = mime.contains("mp4a") || mime.contains("audio/mp4")
                    if (isMp4a && (bestAudioUrl == null || br > bestAudioBitrate)) {
                        bestAudioBitrate = br
                        bestAudioUrl = url
                    } else if (bestAudioUrl == null) {
                        bestAudioBitrate = br
                        bestAudioUrl = url
                    }
                }
            }
        }

        val variants = mutableListOf<YouTubeQualityVariant>()
        if (formats != null) {
            for (i in 0 until formats.length()) {
                val fmt = formats.optJSONObject(i) ?: continue
                val url = fmt.optString("url").takeIf { it.isNotBlank() } ?: continue
                val h = fmt.optInt("height", 360)
                val w = fmt.optInt("width", (h * 16) / 9)
                val label = QualitySelectorHelper_Antigravity.formatQualityLabel(h, w)
                val mime = fmt.optString("mimeType")
                variants.add(
                    YouTubeQualityVariant(
                        label = label,
                        height = h,
                        width = w,
                        videoUrl = url,
                        isProgressive = true,
                        isAvc1 = mime.contains("avc1")
                    )
                )
            }
        }
        if (adaptive != null) {
            for (i in 0 until adaptive.length()) {
                val fmt = adaptive.optJSONObject(i) ?: continue
                val url = fmt.optString("url").takeIf { it.isNotBlank() } ?: continue
                val mime = fmt.optString("mimeType")
                if (mime.startsWith("video/")) {
                    if (mime.contains("av01")) continue
                    val h = fmt.optInt("height", 0)
                    val w = fmt.optInt("width", (h * 16) / 9)
                    if (QualitySelectorHelper_Antigravity.effectiveVerticalResolution(h, w) >= 240) {
                        val label = QualitySelectorHelper_Antigravity.formatQualityLabel(h, w)
                        variants.add(
                            YouTubeQualityVariant(
                                label = label,
                                height = h,
                                width = w,
                                videoUrl = url,
                                isProgressive = false,
                                isAvc1 = mime.contains("avc1")
                            )
                        )
                    }
                }
            }
        }
        val distinctVariants = variants
            .sortedWith(
                compareByDescending<YouTubeQualityVariant> {
                    QualitySelectorHelper_Antigravity.effectiveVerticalResolution(it.height, it.width)
                }
                    .thenBy { it.isProgressive }
                    .thenByDescending { it.isAvc1 }
            )
            .distinctBy { it.label }

        val subtitles = extractSubtitles(playerJson)

        return ResolvedYouTubeStream(
            videoId = videoId,
            hlsManifestUrl = hlsUrl,
            progressiveUrl = bestProgressiveUrl,
            videoStreamUrl = bestVideoUrl,
            audioStreamUrl = bestAudioUrl,
            width = bestVideoWidth,
            height = if (bestVideoHeight > 0) bestVideoHeight else bestProgressiveHeight.coerceAtLeast(720),
            durationMs = durationMs,
            qualityVariants = distinctVariants,
            subtitles = subtitles,
            userAgent = userAgent
        )
    }

    private fun extractSubtitles(playerJson: JSONObject): List<SidecarSubtitle> {
        val list = mutableListOf<SidecarSubtitle>()
        val captionTracks = playerJson
            .optJSONObject("captions")
            ?.optJSONObject("playerCaptionsTracklistRenderer")
            ?.optJSONArray("captionTracks") ?: return emptyList()

        for (i in 0 until captionTracks.length()) {
            val track = captionTracks.optJSONObject(i) ?: continue
            val baseUrl = track.optString("baseUrl").takeIf { it.isNotBlank() } ?: continue
            val lang = track.optString("languageCode", "en")
            val nameObj = track.optJSONObject("name")
            val label = nameObj?.optString("simpleText")
                ?: nameObj?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                ?: lang.uppercase()
            val vttUrl = if (baseUrl.contains("fmt=")) baseUrl else "$baseUrl&fmt=vtt"
            list.add(
                SidecarSubtitle(
                    uriOrPath = vttUrl,
                    mimeType = "text/vtt",
                    language = lang,
                    label = label
                )
            )
        }
        return list
    }

    private suspend fun callInnerTubePlayer(
        videoId: String,
        profile: InnerTubeClientProfile_Antigravity,
        visitorData: String? = null
    ): JSONObject? {
        val clientObj = JSONObject().apply {
            put("clientName", profile.clientName)
            put("clientVersion", profile.clientVersion)
            put("hl", "en")
            put("gl", "US")
            if (!visitorData.isNullOrBlank()) {
                put("visitorData", visitorData)
            }
            for ((k, v) in profile.extraClientFields) {
                put(k, v)
            }
        }
        val body = JSONObject().apply {
            put("videoId", videoId)
            put("context", JSONObject().put("client", clientObj))
        }

        val extraHeaders = mutableMapOf<String, String>()
        if (!profile.clientNameHeaderId.isNullOrBlank()) {
            extraHeaders["X-YouTube-Client-Name"] = profile.clientNameHeaderId
            extraHeaders["X-YouTube-Client-Version"] = profile.clientVersion
        }
        if (!visitorData.isNullOrBlank()) {
            extraHeaders["X-Goog-Visitor-Id"] = visitorData
        }

        val response = InnerTubeHttpClient_Antigravity.postJson(
            url = INNERTUBE_PLAYER_URL,
            jsonBody = body.toString(),
            userAgent = profile.userAgent,
            extraHeaders = extraHeaders
        ) ?: return null

        return runCatching { JSONObject(response) }.getOrNull()
    }

    private inline fun buildWebContextBody(
        webProfile: InnerTubeClientProfile_Antigravity,
        configure: JSONObject.() -> Unit
    ): JSONObject {
        return JSONObject().apply {
            configure()
            put(
                "context",
                JSONObject().apply {
                    put(
                        "client",
                        JSONObject().apply {
                            put("clientName", webProfile.clientName)
                            put("clientVersion", webProfile.clientVersion)
                            put("hl", "en")
                            put("gl", "US")
                        }
                    )
                }
            )
        }
    }

    fun httpGet(url: String): String? = InnerTubeHttpClient_Antigravity.getStringBlocking(url)
}
