package com.joudjawadtube.player

import com.joudjawadtube.player.data.local.SavedChannelEntity
import com.joudjawadtube.player.data.local.SavedPlaylistEntity
import com.joudjawadtube.player.data.settings.RepeatMode
import com.joudjawadtube.player.data.updater.AppUpdater_Antigravity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NewFeaturesTest_Antigravity {

    @Test
    fun appUpdater_versionComparison_detectsNewerVersions() {
        assertTrue(AppUpdater_Antigravity.compareVersions("v1.0.1", "1.0.0") > 0)
        assertTrue(AppUpdater_Antigravity.compareVersions("v2.0.0", "v1.9.9") > 0)
        assertTrue(AppUpdater_Antigravity.compareVersions("1.1.0", "1.0.9") > 0)
        assertEquals(0, AppUpdater_Antigravity.compareVersions("v1.0.0", "1.0.0"))
        assertEquals(0, AppUpdater_Antigravity.compareVersions("1.0.0-beta", "1.0.0"))
        assertTrue(AppUpdater_Antigravity.compareVersions("v1.0.0", "1.0.1") < 0)
    }

    @Test
    fun repeatMode_cyclesCorrectly() {
        assertEquals(3, RepeatMode.values().size)
        val modes = listOf(RepeatMode.OFF, RepeatMode.REPEAT_ONE, RepeatMode.REPEAT_ALL)
        assertEquals(RepeatMode.REPEAT_ONE, modes[(modes.indexOf(RepeatMode.OFF) + 1) % modes.size])
        assertEquals(RepeatMode.REPEAT_ALL, modes[(modes.indexOf(RepeatMode.REPEAT_ONE) + 1) % modes.size])
        assertEquals(RepeatMode.OFF, modes[(modes.indexOf(RepeatMode.REPEAT_ALL) + 1) % modes.size])
    }

    @Test
    fun inAppScopedSearch_matchesSavedChannelsAndPlaylistsStrictly() {
        val channels = listOf(
            SavedChannelEntity(
                channelId = "UC123",
                handle = "@islamickids",
                title = "Islamic Kids Stories",
                avatarUrl = "",
                uploadsPlaylistId = "PL1",
                category = "Islamic Kids"
            ),
            SavedChannelEntity(
                channelId = "UC456",
                handle = "@englishphonics",
                title = "English Phonics",
                avatarUrl = "",
                uploadsPlaylistId = "PL2",
                category = "English Learning"
            )
        )

        val playlists = listOf(
            SavedPlaylistEntity(
                playlistId = "PL1",
                title = "Prophet Stories for Children",
                author = "Islamic Kids Stories",
                sourceUrl = "https://youtube.com/playlist?list=PL1",
                thumbnailUrl = "",
                category = "Islamic Kids"
            ),
            SavedPlaylistEntity(
                playlistId = "PL2",
                title = "ABC Alphabet Rhymes",
                author = "English Phonics",
                sourceUrl = "https://youtube.com/playlist?list=PL2",
                thumbnailUrl = "",
                category = "English Learning"
            )
        )

        // Query "islamic"
        val queryIslamic = "islamic"
        val matchedChannels = channels.filter {
            it.title.contains(queryIslamic, ignoreCase = true) ||
                it.category.contains(queryIslamic, ignoreCase = true)
        }
        val matchedPlaylists = playlists.filter {
            it.title.contains(queryIslamic, ignoreCase = true) ||
                it.category.contains(queryIslamic, ignoreCase = true)
        }

        assertEquals(1, matchedChannels.size)
        assertEquals("Islamic Kids Stories", matchedChannels.first().title)
        assertEquals(1, matchedPlaylists.size)
        assertEquals("Prophet Stories for Children", matchedPlaylists.first().title)

        // Query "ABC"
        val queryAbc = "ABC"
        val matchedAbcPlaylists = playlists.filter {
            it.title.contains(queryAbc, ignoreCase = true)
        }
        assertEquals(1, matchedAbcPlaylists.size)
        assertEquals("ABC Alphabet Rhymes", matchedAbcPlaylists.first().title)
    }
}
