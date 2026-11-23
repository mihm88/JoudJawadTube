package com.joudjawadtube.player.data.youtube

/**
 * Parses YouTube video, playlist, and channel inputs, validates video IDs,
 * and checks stream URL expiration timestamps.
 */
object YouTubeUrlParser_Antigravity {

    val SAFE_VIDEO_ID_REGEX = Regex("""^[a-zA-Z0-9_-]{11}$""")

    private val VIDEO_ID_REGEX = Regex("""(?:v=|youtu\.be/|shorts/|embed/|/v/)([a-zA-Z0-9_-]{11})""")
    private val PLAYLIST_ID_REGEX = Regex("""[?&]list=([a-zA-Z0-9_-]+)""")
    private val RAW_PLAYLIST_ID_REGEX = Regex("""^(?:PL|UU|FL|OLAK5uy_|RD)[a-zA-Z0-9_-]{10,}$""")
    private val EXPIRE_PARAM_REGEX = Regex("""(?:[?&]expire=|/expire/)(\d+)""")
    private val CHANNEL_REGEX = Regex("""(?:https?://(?:www\.)?youtube\.com/)?(@[a-zA-Z0-9_.-]+|channel/[a-zA-Z0-9_-]+|c/[a-zA-Z0-9_.-]+|user/[a-zA-Z0-9_.-]+)""")

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

        if (SAFE_VIDEO_ID_REGEX.matches(urlToken)) {
            return ParsedYouTubeInput.SingleVideo(
                videoId = urlToken,
                originalUrl = "https://www.youtube.com/watch?v=$urlToken"
            )
        }

        val channelMatch = CHANNEL_REGEX.find(urlToken)
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

    fun extractExpireEpochSec(url: String?): Long? {
        if (url.isNullOrBlank()) return null
        return EXPIRE_PARAM_REGEX.find(url)?.groupValues?.getOrNull(1)?.toLongOrNull()
    }

    fun isStreamUrlExpired(
        url: String?,
        safetyMarginSec: Long = 120L,
        nowEpochSec: Long = System.currentTimeMillis() / 1000L
    ): Boolean {
        val expireSec = extractExpireEpochSec(url) ?: return false
        return (nowEpochSec + safetyMarginSec) >= expireSec
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
}
