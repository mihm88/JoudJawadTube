package com.joudjawadtube.player

import com.joudjawadtube.player.data.repository.KidsCatalogRepository_Antigravity
import com.joudjawadtube.player.data.youtube.ParsedYouTubeInput
import com.joudjawadtube.player.data.youtube.QualitySelectorHelper_Antigravity
import com.joudjawadtube.player.data.youtube.YouTubeQualityVariant
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

    @Test
    fun bilingualBuiltIn21ChannelsAssets_parseAndMatchAcrossLocales() {
        val enFile = File("src/main/assets/kids_channels_en_Antigravity.json")
        val arFile = File("src/main/assets/kids_channels_ar_Antigravity.json")
        assertTrue("English 21-channel asset must exist", enFile.exists())
        assertTrue("Arabic 21-channel asset must exist", arFile.exists())

        val enChannels = KidsCatalogRepository_Antigravity.parseKidsJson(enFile.readText())
        val arChannels = KidsCatalogRepository_Antigravity.parseKidsJson(arFile.readText())

        assertEquals(21, enChannels.size)
        assertEquals(21, arChannels.size)
        assertEquals(10, enChannels.count { it.lang == "ar" })
        assertEquals(11, enChannels.count { it.lang == "en" })
        assertEquals(enChannels.map { it.channelId }, arChannels.map { it.channelId })

        for (i in enChannels.indices) {
            val en = enChannels[i]
            val ar = arChannels[i]
            assertTrue("Channel ID must start with UC: ${en.channelId}", en.channelId.startsWith("UC") && en.channelId.length >= 24)
            assertTrue("Uploads playlist must start with UU: ${en.uploadsPlaylistId}", en.uploadsPlaylistId.startsWith("UU"))
            assertTrue("English channel name must not be blank", en.channelName.isNotBlank())
            assertTrue("Arabic channel name must not be blank", ar.channelName.isNotBlank())
            assertTrue("Arabic description must not be blank", ar.description.isNotBlank())
            assertTrue("Playlists must not be empty for ${en.channelName}", en.playlists.isNotEmpty() && ar.playlists.isNotEmpty())
        }
    }

    @Test
    fun qualitySelectorHelper_showsOnlyActualAvailableVideoQualities() {
        // Video with max 720p must ONLY offer Auto, 720p, 480p, 360p (no 1080p, 2K, or 4K)
        val variants720 = listOf(
            YouTubeQualityVariant("720p", 720, 1280, "u720"),
            YouTubeQualityVariant("480p", 480, 854, "u480"),
            YouTubeQualityVariant("360p", 360, 640, "u360")
        )
        assertEquals(
            listOf("Auto", "720p", "480p", "360p"),
            QualitySelectorHelper_Antigravity.buildAvailableQualities(variants720)
        )

        // Video with 1080p must include 1080p up to max, hiding 2K/4K
        val variants1080 = variants720 + YouTubeQualityVariant("1080p", 1080, 1920, "u1080")
        assertEquals(
            listOf("Auto", "1080p", "720p", "480p", "360p"),
            QualitySelectorHelper_Antigravity.buildAvailableQualities(variants1080)
        )

        // Video with 4K (including widescreen 3840x1600) must include 4K (2160p)
        val variants4k = variants1080 +
            YouTubeQualityVariant("2K (1440p)", 1440, 2560, "u1440") +
            YouTubeQualityVariant("4K (2160p)", 1600, 3840, "u2160")
        assertEquals(
            listOf("Auto", "4K (2160p)", "2K (1440p)", "1080p", "720p", "480p", "360p"),
            QualitySelectorHelper_Antigravity.buildAvailableQualities(variants4k)
        )

        // Local file without YouTube variants uses observed ExoPlayer track resolutions only
        assertEquals(
            listOf("Auto", "1080p"),
            QualitySelectorHelper_Antigravity.buildAvailableQualities(
                variants = emptyList(),
                observedTrackResolutions = listOf(1920 to 1080)
            )
        )
    }
}

