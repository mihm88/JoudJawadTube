package com.joudjawadtube.player.data.youtube

import com.joudjawadtube.player.data.local.SidecarSubtitle
import com.joudjawadtube.player.data.local.YouTubeIdExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

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
    val isProgressive: Boolean = false
)

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
    val userAgent: String = "com.google.ios.youtubekids/9.40.0 (iPad14,1; U; CPU iOS 18_1_0 like Mac OS X;)"
) {
    val hasDirectStream: Boolean
        get() = !hlsManifestUrl.isNullOrBlank() ||
            !progressiveUrl.isNullOrBlank() ||
            (!videoStreamUrl.isNullOrBlank() && !audioStreamUrl.isNullOrBlank())
}

sealed class ParsedYouTubeInput {
    data class SingleVideo(val videoId: String, val originalUrl: String) : ParsedYouTubeInput()
    data class Playlist(val playlistId: String, val initialVideoId: String?, val originalUrl: String) : ParsedYouTubeInput()
    data class Channel(val channelQuery: String, val originalUrl: String) : ParsedYouTubeInput()
    data class Invalid(val reason: String) : ParsedYouTubeInput()
}

object YouTubeService {

    private val VIDEO_ID_REGEX = Regex("""(?:v=|youtu\.be/|shorts/|embed/|/v/)([a-zA-Z0-9_-]{11})""")
    private val PLAYLIST_ID_REGEX = Regex("""[?&]list=([a-zA-Z0-9_-]+)""")
    private val RAW_VIDEO_ID_REGEX = Regex("""^[a-zA-Z0-9_-]{11}$""")
    private val RAW_PLAYLIST_ID_REGEX = Regex("""^(?:PL|UU|FL|OLAK5uy_|RD)[a-zA-Z0-9_-]{10,}$""")

    fun parseInput(rawInput: String): ParsedYouTubeInput {
        val text = rawInput.trim()
        if (text.isBlank()) {
            return ParsedYouTubeInput.Invalid("Please enter or paste a YouTube video or playlist link.")
        }

        // Extract first URL if shared text has title + URL
        val urlToken = Regex("""https?://\S+""").find(text)?.value ?: text

        val listMatch = PLAYLIST_ID_REGEX.find(urlToken)?.groupValues?.getOrNull(1)
        val videoMatch = VIDEO_ID_REGEX.find(urlToken)?.groupValues?.getOrNull(1)

        if (!listMatch.isNullOrBlank() && (urlToken.contains("/playlist") || videoMatch == null)) {
            return ParsedYouTubeInput.Playlist(
                playlistId = listMatch,
                initialVideoId = videoMatch,
                originalUrl = "https://www.youtube.com/playlist?list=$listMatch"
            )
        }

        if (!listMatch.isNullOrBlank() && !videoMatch.isNullOrBlank()) {
            // Link has both v= and list= -> import playlist and also identify initial video
            return ParsedYouTubeInput.Playlist(
                playlistId = listMatch,
                initialVideoId = videoMatch,
                originalUrl = "https://www.youtube.com/playlist?list=$listMatch"
            )
        }

        if (!videoMatch.isNullOrBlank()) {
            return ParsedYouTubeInput.SingleVideo(
                videoId = videoMatch,
                originalUrl = "https://www.youtube.com/watch?v=$videoMatch"
            )
        }

        if (RAW_PLAYLIST_ID_REGEX.matches(urlToken)) {
            return ParsedYouTubeInput.Playlist(
                playlistId = urlToken,
                initialVideoId = null,
                originalUrl = "https://www.youtube.com/playlist?list=$urlToken"
            )
        }

        if (RAW_VIDEO_ID_REGEX.matches(urlToken)) {
            return ParsedYouTubeInput.SingleVideo(
                videoId = urlToken,
                originalUrl = "https://www.youtube.com/watch?v=$urlToken"
            )
        }

        val channelMatch = Regex("""(?:https?://(?:www\.)?youtube\.com/)?(@[a-zA-Z0-9_.-]+|channel/[a-zA-Z0-9_-]+|c/[a-zA-Z0-9_.-]+|user/[a-zA-Z0-9_.-]+)""").find(urlToken)
        if (channelMatch != null && !urlToken.contains("/watch") && !urlToken.contains("/playlist")) {
            val q = channelMatch.groupValues[1]
            val fullUrl = if (urlToken.startsWith("http")) urlToken else "https://www.youtube.com/$q"
            return ParsedYouTubeInput.Channel(
                channelQuery = q,
                originalUrl = fullUrl
            )
        }

        if (urlToken.startsWith("@")) {
            return ParsedYouTubeInput.Channel(
                channelQuery = urlToken,
                originalUrl = "https://www.youtube.com/$urlToken"
            )
        }

        if (urlToken.startsWith("UC") && urlToken.length >= 24 && !urlToken.contains("/")) {
            return ParsedYouTubeInput.Channel(
                channelQuery = urlToken,
                originalUrl = "https://www.youtube.com/channel/$urlToken"
            )
        }

        return ParsedYouTubeInput.Invalid("Could not recognize a valid YouTube video, playlist, or channel URL.")
    }

    /**
     * Fetches single video metadata (title, channel, duration, HD thumbnail).
     */
    suspend fun fetchVideoDetails(videoId: String): YouTubeVideoMetadata? = withContext(Dispatchers.IO) {
        // 1. Try InnerTube player for full duration & dimensions
        val playerJson = callInnerTubePlayer(
            videoId = videoId,
            clientName = "WEB",
            clientVersion = "2.20240726.00.00",
            userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
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
            val raw = httpGet(oembedUrl)
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
        val browseId = if (playlistId.startsWith("VL")) playlistId else "VL$playlistId"
        val webBody = JSONObject().apply {
            put("browseId", browseId)
            put(
                "context",
                JSONObject().apply {
                    put(
                        "client",
                        JSONObject().apply {
                            put("clientName", "WEB")
                            put("clientVersion", "2.20240726.00.00")
                            put("hl", "en")
                            put("gl", "US")
                        }
                    )
                }
            )
        }

        val responseStr = httpPostJson(
            url = "https://www.youtube.com/youtubei/v1/browse?prettyPrint=false",
            jsonBody = webBody.toString(),
            userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
        )

        var playlistTitle: String? = null
        var playlistAuthor: String? = null
        val videos = mutableListOf<YouTubeVideoMetadata>()
        var continuationToken: String? = null

        if (!responseStr.isNullOrBlank()) {
            runCatching {
                val root = JSONObject(responseStr)
                playlistTitle = extractPlaylistTitle(root)
                playlistAuthor = extractPlaylistAuthor(root)
                collectPlaylistVideosRecursive(
                    node = root,
                    defaultAuthor = playlistAuthor ?: "YouTube Playlist",
                    sink = videos
                )
                continuationToken = extractContinuationToken(root)
            }
        }

        // Page through continuation tokens up to maxVideos (e.g. for channel uploads)
        var pagesCount = 0
        while (!continuationToken.isNullOrBlank() && videos.size < maxVideos && pagesCount < 4) {
            pagesCount++
            val contBody = JSONObject().apply {
                put("continuation", continuationToken)
                put(
                    "context",
                    JSONObject().apply {
                        put(
                            "client",
                            JSONObject().apply {
                                put("clientName", "WEB")
                                put("clientVersion", "2.20240726.00.00")
                                put("hl", "en")
                                put("gl", "US")
                            }
                        )
                    }
                )
            }
            val contResponse = httpPostJson(
                url = "https://www.youtube.com/youtubei/v1/browse?prettyPrint=false",
                jsonBody = contBody.toString(),
                userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
            )
            if (!contResponse.isNullOrBlank()) {
                runCatching {
                    val contRoot = JSONObject(contResponse)
                    collectPlaylistVideosRecursive(
                        node = contRoot,
                        defaultAuthor = playlistAuthor ?: "YouTube Playlist",
                        sink = videos
                    )
                    continuationToken = extractContinuationToken(contRoot)
                }
            } else {
                break
            }
        }

        // Fallback to /youtubei/v1/next (watch playlist panel) if browse returned no items
        if (videos.isEmpty()) {
            val nextBody = JSONObject().apply {
                put("playlistId", playlistId)
                put(
                    "context",
                    JSONObject().apply {
                        put(
                            "client",
                            JSONObject().apply {
                                put("clientName", "WEB")
                                put("clientVersion", "2.20240726.00.00")
                                put("hl", "en")
                                put("gl", "US")
                            }
                        )
                    }
                )
            }
            val nextStr = httpPostJson(
                url = "https://www.youtube.com/youtubei/v1/next?prettyPrint=false",
                jsonBody = nextBody.toString(),
                userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
            )
            if (!nextStr.isNullOrBlank()) {
                runCatching {
                    val nextRoot = JSONObject(nextStr)
                    if (playlistTitle == null) playlistTitle = extractPlaylistTitle(nextRoot)
                    if (playlistAuthor == null) playlistAuthor = extractPlaylistAuthor(nextRoot)
                    collectPlaylistVideosRecursive(
                        node = nextRoot,
                        defaultAuthor = playlistAuthor ?: "YouTube Playlist",
                        sink = videos
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

        try {
            val payload = JSONObject().apply {
                put(
                    "context",
                    JSONObject().apply {
                        put(
                            "client",
                            JSONObject().apply {
                                put("clientName", "WEB")
                                put("clientVersion", "2.20240410.01.00")
                                put("hl", "en")
                                put("gl", "US")
                            }
                        )
                    }
                )
                put("url", cleanUrl)
            }
            val response = httpPostJson(
                url = "https://www.youtube.com/youtubei/v1/navigation/resolve_url?prettyPrint=false",
                jsonBody = payload.toString(),
                userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
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
        val playlists = mutableListOf<YouTubePlaylistMetadata>()
        val browseId = if (channelId.startsWith("UC")) channelId else "UC$channelId"
        val payload = JSONObject().apply {
            put("browseId", browseId)
            put("params", "EglwbGF5bGlzdHPyBgQKAkIA")
            put(
                "context",
                JSONObject().apply {
                    put(
                        "client",
                        JSONObject().apply {
                            put("clientName", "WEB")
                            put("clientVersion", "2.20240726.00.00")
                            put("hl", "en")
                            put("gl", "US")
                        }
                    )
                }
            )
        }
        val response = httpPostJson(
            url = "https://www.youtube.com/youtubei/v1/browse?prettyPrint=false",
            jsonBody = payload.toString(),
            userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
        )
        if (!response.isNullOrBlank()) {
            runCatching {
                val root = JSONObject(response)
                collectChannelPlaylistsRecursive(root, playlists)
            }
        }
        playlists.distinctBy { it.playlistId }
    }

    private fun collectChannelPlaylistsRecursive(
        node: Any?,
        sink: MutableList<YouTubePlaylistMetadata>
    ) {
        when (node) {
            is JSONObject -> {
                val gpr = node.optJSONObject("gridPlaylistRenderer")
                    ?: node.optJSONObject("playlistRenderer")
                if (gpr != null) {
                    val plId = gpr.optString("playlistId")
                    if (plId.length >= 12) {
                        val title = gpr.optJSONObject("title")?.optString("simpleText")
                            ?: gpr.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                            ?: "Playlist $plId"
                        val thumb = gpr.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                            ?.optJSONObject(0)?.optString("url") ?: ""
                        sink.add(
                            YouTubePlaylistMetadata(
                                playlistId = plId,
                                title = title,
                                author = "",
                                thumbnailUrl = thumb,
                                videos = emptyList()
                            )
                        )
                    }
                }
                val keys = node.keys()
                while (keys.hasNext()) {
                    collectChannelPlaylistsRecursive(node.opt(keys.next()), sink)
                }
            }
            is JSONArray -> {
                for (i in 0 until node.length()) {
                    collectChannelPlaylistsRecursive(node.opt(i), sink)
                }
            }
        }
    }

    private data class CachedStreamEntry(
        val stream: ResolvedYouTubeStream,
        val cachedAtMs: Long
    )

    private val streamCache = ConcurrentHashMap<String, CachedStreamEntry>()
    private const val STREAM_CACHE_TTL_MS = 15 * 60 * 1000L // 15 minutes

    /**
     * Pre-warms the stream cache in the background (e.g., for the next video in a playlist).
     */
    suspend fun preResolveStream(videoId: String) {
        if (videoId.isBlank()) return
        val existing = streamCache[videoId]
        if (existing != null && (System.currentTimeMillis() - existing.cachedAtMs) < STREAM_CACHE_TTL_MS) {
            return
        }
        runCatching { resolveStreamForPlayback(videoId) }
    }

    /**
     * Resolves direct playable video/audio/HLS streams for Media3 ExoPlayer.
     * Uses parallel client racing + in-memory stream caching for sub-second startup.
     */
    suspend fun resolveStreamForPlayback(videoId: String): ResolvedYouTubeStream = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        streamCache[videoId]?.let { cached ->
            if ((now - cached.cachedAtMs) < STREAM_CACHE_TTL_MS && cached.stream.hasDirectStream) {
                return@withContext cached.stream
            }
        }

        // Wave 1: True first-to-finish parallel race between IOS_KIDS and ANDROID_KIDS (~300ms).
        // Neither Kids client requires a poToken, so their 1080p adaptiveFormats & HLS streams
        // always return HTTP 200/206 immediately without 403 Forbidden errors.
        val wave1Winner = coroutineScope {
            val resultChannel = Channel<ResolvedYouTubeStream?>(capacity = 2)

            val iosKidsJob = launch {
                val iosKidsUa = "com.google.ios.youtubekids/9.40.0 (iPad14,1; U; CPU iOS 18_1_0 like Mac OS X;)"
                val json = callInnerTubePlayer(
                    videoId = videoId,
                    clientName = "IOS_KIDS",
                    clientVersion = "9.40.0",
                    userAgent = iosKidsUa,
                    extraClientFields = mapOf(
                        "deviceMake" to "Apple",
                        "deviceModel" to "iPad14,1",
                        "osName" to "iOS",
                        "osVersion" to "18.1.0.22B83"
                    )
                )
                val parsed = parseStreamingData(videoId, json, iosKidsUa)?.takeIf { it.hasDirectStream }
                resultChannel.trySend(parsed)
            }

            val androidKidsJob = launch {
                val androidKidsUa = "com.google.android.apps.youtube.kids/9.40.00 (Linux; U; Android 14) gzip"
                val json = callInnerTubePlayer(
                    videoId = videoId,
                    clientName = "ANDROID_KIDS",
                    clientVersion = "9.40.00",
                    userAgent = androidKidsUa,
                    extraClientFields = mapOf(
                        "androidSdkVersion" to 34,
                        "osName" to "Android",
                        "osVersion" to "14"
                    )
                )
                val parsed = parseStreamingData(videoId, json, androidKidsUa)?.takeIf { it.hasDirectStream }
                resultChannel.trySend(parsed)
            }

            val first = resultChannel.receive()
            if (first != null) {
                iosKidsJob.cancel()
                androidKidsJob.cancel()
                return@coroutineScope first
            }

            val second = resultChannel.receive()
            iosKidsJob.cancel()
            androidKidsJob.cancel()
            second
        }

        if (wave1Winner != null) {
            streamCache[videoId] = CachedStreamEntry(wave1Winner, System.currentTimeMillis())
            return@withContext wave1Winner
        }

        // Wave 2 Fallback (for general non-Kids videos): Race ANDROID and IOS in parallel,
        // and sanitize any poToken-protected 403 adaptive URLs so ExoPlayer never stalls.
        val wave2Winner = coroutineScope {
            val androidDeferred = async {
                val androidUa = "com.google.android.youtube/20.10.38 (Linux; U; Android 14) gzip"
                val json = callInnerTubePlayer(
                    videoId = videoId,
                    clientName = "ANDROID",
                    clientVersion = "20.10.38",
                    userAgent = androidUa,
                    extraClientFields = mapOf(
                        "androidSdkVersion" to 34,
                        "osName" to "Android",
                        "osVersion" to "14"
                    )
                )
                val parsed = parseStreamingData(videoId, json, androidUa)?.takeIf { it.hasDirectStream }
                parsed?.let { sanitizeAdaptiveStreamIfForbidden(it) }?.takeIf { it.hasDirectStream }
            }

            val iosDeferred = async {
                val iosUa = "com.google.ios.youtube/20.10.4 (iPhone16,2; U; CPU iOS 18_3_1 like Mac OS X;)"
                val json = callInnerTubePlayer(
                    videoId = videoId,
                    clientName = "IOS",
                    clientVersion = "20.10.4",
                    userAgent = iosUa,
                    extraClientFields = mapOf(
                        "deviceMake" to "Apple",
                        "deviceModel" to "iPhone16,2",
                        "osName" to "iOS",
                        "osVersion" to "18.3.1.22D72"
                    )
                )
                val parsed = parseStreamingData(videoId, json, iosUa)?.takeIf { it.hasDirectStream }
                parsed?.let { sanitizeAdaptiveStreamIfForbidden(it) }?.takeIf { it.hasDirectStream }
            }

            val androidRes = androidDeferred.await()
            if (androidRes != null) {
                iosDeferred.cancel()
                return@coroutineScope androidRes
            }
            iosDeferred.await()
        }

        if (wave2Winner != null) {
            streamCache[videoId] = CachedStreamEntry(wave2Winner, System.currentTimeMillis())
            return@withContext wave2Winner
        }

        ResolvedYouTubeStream(videoId = videoId)
    }

    /**
     * Checks whether a Wave-2 (ANDROID/IOS) adaptive video URL responds with HTTP 200/206.
     * If Google CDN returns 403 Forbidden (due to missing poToken on non-Kids clients),
     * strips the dead adaptive URLs so ExoPlayer plays the working progressive or HLS stream immediately.
     */
    private fun sanitizeAdaptiveStreamIfForbidden(stream: ResolvedYouTubeStream): ResolvedYouTubeStream {
        val adaptiveUrl = stream.videoStreamUrl
        if (adaptiveUrl.isNullOrBlank()) return stream
        val probeOk = runCatching {
            val sep = if (adaptiveUrl.contains('?')) '&' else '?'
            val conn = (URL("${adaptiveUrl}${sep}range=0-1023").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 2_000
                readTimeout = 2_000
                setRequestProperty("User-Agent", stream.userAgent)
            }
            val code = conn.responseCode
            conn.disconnect()
            code in 200..299
        }.getOrDefault(false)

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
     * Searches YouTube by video title to find the official YouTube videoId for thumbnail generation.
     */
    suspend fun searchVideoIdByTitle(rawTitle: String): String? = withContext(Dispatchers.IO) {
        val cleanedQuery = cleanTitleForYouTubeSearch(rawTitle)
        if (cleanedQuery.length < 3) return@withContext null

        val body = JSONObject().apply {
            put("query", cleanedQuery)
            put(
                "context",
                JSONObject().apply {
                    put(
                        "client",
                        JSONObject().apply {
                            put("clientName", "WEB")
                            put("clientVersion", "2.20240726.00.00")
                        }
                    )
                }
            )
        }

        val response = httpPostJson(
            url = "https://www.youtube.com/youtubei/v1/search?prettyPrint=false",
            jsonBody = body.toString(),
            userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
        ) ?: return@withContext null

        // Fast regex scan for first videoRenderer -> videoId
        val match = Regex(""""videoRenderer"\s*:\s*\{\s*"videoId"\s*:\s*"([a-zA-Z0-9_-]{11})"""").find(response)
        match?.groupValues?.getOrNull(1)
    }

    fun cleanTitleForYouTubeSearch(rawTitle: String): String {
        return rawTitle
            .replace(Regex("""^\d{1,3}[\s._-]+"""), "") // Strip leading track numbers like "01-"
            .replace('｜', ' ')
            .replace('|', ' ')
            .replace(Regex("""#\S+"""), "") // Strip hashtags
            .replace(Regex("""\[[a-zA-Z0-9_-]{11}\]"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
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
                        isAvc1 && !bestVideoIsAvc1 && h >= 480 && h <= 1080 -> true
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
                val label = when {
                    h >= 2160 || w >= 3840 -> "4K (2160p)"
                    h >= 1440 || w >= 2560 -> "2K (1440p)"
                    h >= 1080 -> "1080p"
                    h >= 720 -> "720p"
                    h >= 480 -> "480p"
                    else -> "${h}p"
                }
                variants.add(YouTubeQualityVariant(label, h, w, url, isProgressive = true))
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
                    if (h >= 240) {
                        val label = when {
                            h >= 2160 || w >= 3840 -> "4K (2160p)"
                            h >= 1440 || w >= 2560 -> "2K (1440p)"
                            h >= 1080 -> "1080p"
                            h >= 720 -> "720p"
                            h >= 480 -> "480p"
                            else -> "${h}p"
                        }
                        variants.add(YouTubeQualityVariant(label, h, w, url, isProgressive = false))
                    }
                }
            }
        }
        val distinctVariants = variants
            .sortedByDescending { it.height }
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

    private fun callInnerTubePlayer(
        videoId: String,
        clientName: String,
        clientVersion: String,
        userAgent: String,
        extraClientFields: Map<String, Any> = emptyMap()
    ): JSONObject? {
        val clientObj = JSONObject().apply {
            put("clientName", clientName)
            put("clientVersion", clientVersion)
            put("hl", "en")
            put("gl", "US")
            for ((k, v) in extraClientFields) {
                put(k, v)
            }
        }
        val body = JSONObject().apply {
            put("videoId", videoId)
            put("context", JSONObject().put("client", clientObj))
            put("contentCheckOk", true)
            put("racyCheckOk", true)
        }

        val response = httpPostJson(
            url = "https://www.youtube.com/youtubei/v1/player?prettyPrint=false",
            jsonBody = body.toString(),
            userAgent = userAgent
        ) ?: return null

        return runCatching { JSONObject(response) }.getOrNull()
    }

    private fun extractPlaylistTitle(root: JSONObject): String? {
        root.optJSONObject("metadata")
            ?.optJSONObject("playlistMetadataRenderer")
            ?.optString("title")
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        root.optJSONObject("header")
            ?.optJSONObject("pageHeaderRenderer")
            ?.optString("pageTitle")
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        root.optJSONObject("header")
            ?.optJSONObject("playlistHeaderRenderer")
            ?.optJSONObject("title")
            ?.optString("simpleText")
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        root.optJSONObject("contents")
            ?.optJSONObject("twoColumnWatchNextResults")
            ?.optJSONObject("playlist")
            ?.optJSONObject("playlist")
            ?.optString("title")
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        return null
    }

    private fun extractPlaylistAuthor(root: JSONObject): String? {
        root.optJSONObject("header")
            ?.optJSONObject("playlistHeaderRenderer")
            ?.optJSONObject("ownerText")
            ?.optJSONArray("runs")
            ?.optJSONObject(0)
            ?.optString("text")
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        val sidebarItems = root.optJSONObject("sidebar")
            ?.optJSONObject("playlistSidebarRenderer")
            ?.optJSONArray("items")
        if (sidebarItems != null) {
            for (i in 0 until sidebarItems.length()) {
                val ownerText = sidebarItems.optJSONObject(i)
                    ?.optJSONObject("playlistSidebarSecondaryInfoRenderer")
                    ?.optJSONObject("videoOwner")
                    ?.optJSONObject("videoOwnerRenderer")
                    ?.optJSONObject("title")
                    ?.optJSONArray("runs")
                    ?.optJSONObject(0)
                    ?.optString("text")
                    ?.takeIf { it.isNotBlank() }
                if (ownerText != null) return ownerText
            }
        }

        return null
    }

    private fun collectPlaylistVideosRecursive(
        node: Any?,
        defaultAuthor: String,
        sink: MutableList<YouTubeVideoMetadata>
    ) {
        when (node) {
            is JSONObject -> {
                // 1. Classic playlistVideoRenderer
                val pvr = node.optJSONObject("playlistVideoRenderer")
                if (pvr != null) {
                    val vid = pvr.optString("videoId")
                    if (vid.length == 11) {
                        val title = pvr.optJSONObject("title")
                            ?.optJSONArray("runs")
                            ?.optJSONObject(0)
                            ?.optString("text")
                            ?: pvr.optJSONObject("title")?.optString("simpleText")
                            ?: "Video $vid"
                        val durationSec = pvr.optString("lengthSeconds").toLongOrNull()
                        val durationMs = if (durationSec != null && durationSec > 0L) {
                            durationSec * 1000L
                        } else {
                            parseDurationTextToMs(pvr.optJSONObject("lengthText")?.optString("simpleText"))
                        }
                        val author = pvr.optJSONObject("shortBylineText")
                            ?.optJSONArray("runs")
                            ?.optJSONObject(0)
                            ?.optString("text")
                            ?: defaultAuthor
                        sink.add(
                            YouTubeVideoMetadata(
                                videoId = vid,
                                title = title,
                                author = author,
                                durationMs = durationMs,
                                thumbnailUrl = YouTubeIdExtractor.buildYouTubeThumbnailUrl(vid)
                            )
                        )
                    }
                }

                // 2. Watch-next playlistPanelVideoRenderer
                val ppvr = node.optJSONObject("playlistPanelVideoRenderer")
                if (ppvr != null) {
                    val vid = ppvr.optString("videoId")
                    if (vid.length == 11) {
                        val title = ppvr.optJSONObject("title")?.optString("simpleText")
                            ?: ppvr.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                            ?: "Video $vid"
                        val durationMs = parseDurationTextToMs(
                            ppvr.optJSONObject("lengthText")?.optString("simpleText")
                                ?: ppvr.optJSONObject("lengthText")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                        )
                        val author = ppvr.optJSONObject("shortBylineText")
                            ?.optJSONArray("runs")
                            ?.optJSONObject(0)
                            ?.optString("text")
                            ?: defaultAuthor
                        sink.add(
                            YouTubeVideoMetadata(
                                videoId = vid,
                                title = title,
                                author = author,
                                durationMs = durationMs,
                                thumbnailUrl = YouTubeIdExtractor.buildYouTubeThumbnailUrl(vid)
                            )
                        )
                    }
                }

                // 3. Modern YouTube 2025/2026 lockupViewModel (used by courses, music & modern playlists)
                val lvm = node.optJSONObject("lockupViewModel")
                if (lvm != null) {
                    val vid = lvm.optString("contentId").takeIf { it.length == 11 }
                        ?: lvm.optJSONObject("rendererContext")
                            ?.optJSONObject("commandContext")
                            ?.optJSONObject("onTap")
                            ?.optJSONObject("innertubeCommand")
                            ?.optJSONObject("watchEndpoint")
                            ?.optString("videoId")
                            ?.takeIf { it.length == 11 }
                    if (vid != null) {
                        val lockupMeta = lvm.optJSONObject("metadata")
                            ?.optJSONObject("lockupMetadataViewModel")
                        val title = lockupMeta?.optJSONObject("title")
                            ?.optString("content")
                            ?.takeIf { it.isNotBlank() }
                            ?: "Video $vid"
                        val author = lockupMeta?.optJSONObject("metadata")
                            ?.optJSONObject("contentMetadataViewModel")
                            ?.optJSONArray("metadataRows")
                            ?.optJSONObject(0)
                            ?.optJSONArray("metadataParts")
                            ?.optJSONObject(0)
                            ?.optJSONObject("text")
                            ?.optString("content")
                            ?.takeIf { it.isNotBlank() }
                            ?: defaultAuthor
                        val durationMs = extractDurationFromLockup(lvm)
                        sink.add(
                            YouTubeVideoMetadata(
                                videoId = vid,
                                title = title,
                                author = author,
                                durationMs = durationMs,
                                thumbnailUrl = YouTubeIdExtractor.buildYouTubeThumbnailUrl(vid)
                            )
                        )
                    }
                }

                val keys = node.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    if (k != "playlistVideoRenderer" && k != "playlistPanelVideoRenderer" && k != "lockupViewModel") {
                        collectPlaylistVideosRecursive(node.opt(k), defaultAuthor, sink)
                    }
                }
            }
            is JSONArray -> {
                for (i in 0 until node.length()) {
                    collectPlaylistVideosRecursive(node.opt(i), defaultAuthor, sink)
                }
            }
        }
    }

    private fun extractDurationFromLockup(lvm: JSONObject): Long {
        val overlays = lvm.optJSONObject("contentImage")
            ?.optJSONObject("thumbnailViewModel")
            ?.optJSONArray("overlays") ?: return 0L
        for (i in 0 until overlays.length()) {
            val badges = overlays.optJSONObject(i)
                ?.optJSONObject("thumbnailBottomOverlayViewModel")
                ?.optJSONArray("badges") ?: continue
            for (j in 0 until badges.length()) {
                val text = badges.optJSONObject(j)
                    ?.optJSONObject("thumbnailBadgeViewModel")
                    ?.optString("text")
                    ?.takeIf { it.isNotBlank() }
                val parsed = parseDurationTextToMs(text)
                if (parsed > 0L) return parsed
            }
        }
        return 0L
    }

    private fun parseDurationTextToMs(raw: String?): Long {
        if (raw.isNullOrBlank()) return 0L
        val parts = raw.trim().split(":").mapNotNull { it.trim().toLongOrNull() }
        val seconds = when (parts.size) {
            3 -> parts[0] * 3600L + parts[1] * 60L + parts[2]
            2 -> parts[0] * 60L + parts[1]
            1 -> parts[0]
            else -> 0L
        }
        return seconds * 1000L
    }

    private fun extractContinuationToken(node: Any?): String? {
        if (node is JSONObject) {
            val contCmd = node.optJSONObject("continuationCommand")
            if (contCmd != null) {
                val token = contCmd.optString("token")
                if (token.isNotBlank()) return token
            }
            val keys = node.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val res = extractContinuationToken(node.opt(k))
                if (res != null) return res
            }
        } else if (node is JSONArray) {
            for (i in 0 until node.length()) {
                val res = extractContinuationToken(node.opt(i))
                if (res != null) return res
            }
        }
        return null
    }

    private fun httpPostJson(url: String, jsonBody: String, userAgent: String): String? {
        return runCatching {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 8_000
                readTimeout = 10_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", userAgent)
            }
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(jsonBody)
                writer.flush()
            }
            if (conn.responseCode in 200..299) {
                BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
            } else {
                null
            }
        }.getOrNull()
    }

    fun httpGet(url: String): String? {
        return runCatching {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 7_000
                readTimeout = 8_000
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            }
            if (conn.responseCode in 200..299) {
                BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
            } else {
                null
            }
        }.getOrNull()
    }
}
