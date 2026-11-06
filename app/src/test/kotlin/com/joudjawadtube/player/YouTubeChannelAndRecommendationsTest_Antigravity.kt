package com.joudjawadtube.player

import com.joudjawadtube.player.data.repository.KidsCatalogRepository_Antigravity
import com.joudjawadtube.player.data.youtube.ParsedYouTubeInput
import com.joudjawadtube.player.data.youtube.YouTubeService
import com.joudjawadtube.player.ui.components.WindowWidthClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class YouTubeChannelAndRecommendationsTest_Antigravity {

    @Test
    fun parseInput_correctlyIdentifiesHandlesAndChannelUrls() {
        val handle1 = YouTubeService.parseInput("@LearnWithZakaria")
        assertTrue(handle1 is ParsedYouTubeInput.Channel)
        assertEquals("@LearnWithZakaria", (handle1 as ParsedYouTubeInput.Channel).channelQuery)

        val handleUrl = YouTubeService.parseInput("https://www.youtube.com/@AhlanSimsim")
        assertTrue(handleUrl is ParsedYouTubeInput.Channel)
        assertEquals("@AhlanSimsim", (handleUrl as ParsedYouTubeInput.Channel).channelQuery)

        val channelIdUrl = YouTubeService.parseInput("https://www.youtube.com/channel/UCfEOy302XpjN00cQ2GW4aZA")
        assertTrue(channelIdUrl is ParsedYouTubeInput.Channel)
        assertEquals("channel/UCfEOy302XpjN00cQ2GW4aZA", (channelIdUrl as ParsedYouTubeInput.Channel).channelQuery)

        val playlistUrl = YouTubeService.parseInput("https://www.youtube.com/playlist?list=PLix29vxQ1_WoyPFUYURRjcqoU8I0HAx0s")
        assertTrue(playlistUrl is ParsedYouTubeInput.Playlist)
        assertEquals("PLix29vxQ1_WoyPFUYURRjcqoU8I0HAx0s", (playlistUrl as ParsedYouTubeInput.Playlist).playlistId)

        val videoUrl = YouTubeService.parseInput("https://youtu.be/dQw4w9WgXcQ")
        assertTrue(videoUrl is ParsedYouTubeInput.SingleVideo)
        assertEquals("dQw4w9WgXcQ", (videoUrl as ParsedYouTubeInput.SingleVideo).videoId)
    }

    @Test
    fun kidsCatalogJson_containsAll14VerifiedChannels() {
        val assetFile = File("src/main/assets/kids_catalog_Antigravity.json")
        assertTrue("kids_catalog_Antigravity.json must exist in assets", assetFile.exists())

        val jsonString = assetFile.readText()
        val channels = KidsCatalogRepository_Antigravity.parseKidsJson(jsonString)
        assertNotNull("Channels must parse successfully", channels)
        assertEquals("Catalog should contain exactly 14 curated channels", 14, channels.size)

        val arabicChannels = channels.filter { it.lang.equals("ar", ignoreCase = true) }
        val englishChannels = channels.filter { it.lang.equals("en", ignoreCase = true) }

        assertEquals(7, arabicChannels.size)
        assertEquals(7, englishChannels.size)

        for (channel in channels) {
            assertTrue("Channel ID must not be empty", channel.channelId.isNotBlank())
            assertTrue("Channel Name must not be empty", channel.channelName.isNotBlank())
            assertTrue("Playlists list must not be empty for ${channel.channelName}", channel.playlists.isNotEmpty())
            for (pl in channel.playlists) {
                assertTrue("Playlist ID must not be empty for ${pl.title}", pl.id.isNotBlank())
                assertTrue("Playlist Title must not be empty", pl.title.isNotBlank())
            }
        }
    }

    @Test
    fun windowWidthClass_classifiesCorrectly() {
        assertEquals(WindowWidthClass.Compact, WindowWidthClass.fromWidthDp(400f))
        assertEquals(WindowWidthClass.Compact, WindowWidthClass.fromWidthDp(599f))
        assertEquals(WindowWidthClass.Medium, WindowWidthClass.fromWidthDp(600f))
        assertEquals(WindowWidthClass.Medium, WindowWidthClass.fromWidthDp(839f))
        assertEquals(WindowWidthClass.Expanded, WindowWidthClass.fromWidthDp(840f))
        assertEquals(WindowWidthClass.Expanded, WindowWidthClass.fromWidthDp(1920f))
    }
}

