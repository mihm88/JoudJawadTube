package com.joudjawadtube.player

import com.joudjawadtube.player.data.youtube.InnerTubeClientConfig_Antigravity
import com.joudjawadtube.player.data.youtube.ParsedYouTubeInput
import com.joudjawadtube.player.data.youtube.ResolvedYouTubeStream
import com.joudjawadtube.player.data.youtube.YouTubePlaylistParser_Antigravity
import com.joudjawadtube.player.data.youtube.YouTubeService
import com.joudjawadtube.player.data.youtube.YouTubeUrlParser_Antigravity
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InnerTubeResilienceTest_Antigravity {

    @Before
    fun setUp() {
        YouTubeService.clearCacheForTesting()
    }

    @After
    fun tearDown() {
        YouTubeService.clearCacheForTesting()
    }

    @Test
    fun streamCache_evictsOldestEntryBeyondFiftyItems() {
        for (i in 1..60) {
            val videoId = "vid_%07d".format(i)
            YouTubeService.putCacheEntryForTesting(
                videoId = videoId,
                stream = ResolvedYouTubeStream(
                    videoId = videoId,
                    progressiveUrl = "https://example.com/$videoId.mp4"
                )
            )
        }

        assertEquals(YouTubeService.MAX_STREAM_CACHE_SIZE, YouTubeService.cacheSizeForTesting())
        assertFalse(YouTubeService.containsCachedVideoForTesting("vid_0000001"))
        assertFalse(YouTubeService.containsCachedVideoForTesting("vid_0000010"))
        assertTrue(YouTubeService.containsCachedVideoForTesting("vid_0000011"))
        assertTrue(YouTubeService.containsCachedVideoForTesting("vid_0000060"))
    }

    @Test
    fun youtubeUrlParser_parsesUrlsAndChecksExpiration() {
        val shortUrl = YouTubeUrlParser_Antigravity.parseInput("https://youtu.be/dQw4w9WgXcQ")
        assertTrue(shortUrl is ParsedYouTubeInput.SingleVideo)
        assertEquals("dQw4w9WgXcQ", (shortUrl as ParsedYouTubeInput.SingleVideo).videoId)

        val playlistUrl = YouTubeUrlParser_Antigravity.parseInput("https://www.youtube.com/playlist?list=PL1234567890abcdef")
        assertTrue(playlistUrl is ParsedYouTubeInput.Playlist)
        assertEquals("PL1234567890abcdef", (playlistUrl as ParsedYouTubeInput.Playlist).playlistId)

        val channelUrl = YouTubeUrlParser_Antigravity.parseInput("@KidsIslamicStories")
        assertTrue(channelUrl is ParsedYouTubeInput.Channel)

        assertEquals(1_800_000_000L, YouTubeUrlParser_Antigravity.extractExpireEpochSec("https://cdn.example/video?expire=1800000000&itag=18"))
        assertNull(YouTubeUrlParser_Antigravity.extractExpireEpochSec("https://cdn.example/video?itag=18"))

        assertTrue(YouTubeUrlParser_Antigravity.isStreamUrlExpired("https://cdn.example/video?expire=1000", safetyMarginSec = 120L, nowEpochSec = 900L))
        assertFalse(YouTubeUrlParser_Antigravity.isStreamUrlExpired("https://cdn.example/video?expire=2000", safetyMarginSec = 120L, nowEpochSec = 900L))

        assertEquals(
            "Alphabet Song",
            YouTubeUrlParser_Antigravity.cleanTitleForYouTubeSearch("01- Alphabet Song [dQw4w9WgXcQ] #kids")
        )
    }

    @Test
    fun innerTubeClientConfig_parsesJsonAndFallsBackToDefaults() {
        val sampleJson = """
            {
              "version": 2,
              "clients": {
                "IOS_KIDS": {
                  "clientName": "IOS_KIDS",
                  "clientVersion": "9.99.0",
                  "userAgent": "TestAgent/9.99",
                  "clientNameHeaderId": "6",
                  "extraClientFields": {
                    "deviceMake": "Apple",
                    "androidSdkVersion": 34
                  }
                },
                "ANDROID_KIDS": {
                  "clientName": "ANDROID_KIDS",
                  "clientVersion": "9.98.0",
                  "userAgent": "TestAndroidKids/9.98",
                  "clientNameHeaderId": null,
                  "extraClientFields": {}
                }
              }
            }
        """.trimIndent()

        val parsed = InnerTubeClientConfig_Antigravity.parseConfigJson(sampleJson)
        assertEquals(2, parsed.size)
        val iosKids = parsed.getValue("IOS_KIDS")
        assertEquals("9.99.0", iosKids.clientVersion)
        assertEquals("TestAgent/9.99", iosKids.userAgent)
        assertEquals("6", iosKids.clientNameHeaderId)
        assertEquals("Apple", iosKids.extraClientFields["deviceMake"])
        assertEquals(34, iosKids.extraClientFields["androidSdkVersion"])

        val androidKids = parsed.getValue("ANDROID_KIDS")
        assertNull(androidKids.clientNameHeaderId)

        // Hardcoded defaults work without Android Context
        val defaults = InnerTubeClientConfig_Antigravity.defaultClients()
        assertTrue(defaults.containsKey("IOS_KIDS"))
        assertTrue(defaults.containsKey("ANDROID_KIDS"))
        assertTrue(defaults.containsKey("ANDROID"))
        assertTrue(defaults.containsKey("IOS"))
        assertTrue(defaults.containsKey("ANDROID_VR"))
        assertTrue(defaults.containsKey("WEB"))
    }

    @Test
    fun playlistParser_parsesDurationsAndContinuationTokens() {
        assertEquals(65_000L, YouTubePlaylistParser_Antigravity.parseDurationTextToMs("1:05"))
        assertEquals(3_661_000L, YouTubePlaylistParser_Antigravity.parseDurationTextToMs("1:01:01"))
        assertEquals(0L, YouTubePlaylistParser_Antigravity.parseDurationTextToMs(null))

        val payload = JSONObject(
            """
            {
              "continuationEndpoint": {
                "continuationCommand": {
                  "token": "4qmFsgJPEhhVV..."
                }
              }
            }
            """.trimIndent()
        )
        assertEquals("4qmFsgJPEhhVV...", YouTubePlaylistParser_Antigravity.extractContinuationToken(payload))
    }
}
