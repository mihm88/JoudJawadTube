package com.joudjawadtube.player.data.youtube

import com.joudjawadtube.player.data.local.YouTubeIdExtractor
import org.json.JSONArray
import org.json.JSONObject

/**
 * Extracts playlist metadata, video entries, durations, continuation tokens,
 * and channel sub-playlists from InnerTube browse/next JSON payloads.
 */
object YouTubePlaylistParser_Antigravity {

    fun extractPlaylistTitle(root: JSONObject): String? {
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

    fun extractPlaylistAuthor(root: JSONObject): String? {
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

    fun collectPlaylistVideosRecursive(
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

                // 3. Modern YouTube 2025/2026 lockupViewModel
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

    fun extractDurationFromLockup(lvm: JSONObject): Long {
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

    fun parseDurationTextToMs(raw: String?): Long {
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

    fun extractContinuationToken(node: Any?): String? {
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

    fun collectChannelPlaylistsRecursive(
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
}
