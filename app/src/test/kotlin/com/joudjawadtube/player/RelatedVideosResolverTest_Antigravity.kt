package com.joudjawadtube.player

import com.joudjawadtube.player.data.local.SavedChannelEntity
import com.joudjawadtube.player.data.local.SavedPlaylistEntity
import com.joudjawadtube.player.data.local.VideoEntity
import com.joudjawadtube.player.data.local.VideoItem
import com.joudjawadtube.player.data.repository.RelatedVideosResolver_Antigravity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RelatedVideosResolverTest_Antigravity {

    private fun makeYoutubeVideo(
        id: String,
        youtubeVideoId: String,
        playlistId: String,
        playlistTitle: String = "Playlist",
        title: String = "Video $id"
    ): VideoItem = VideoItem(
        entity = VideoEntity(
            id = id,
            videoKey = "yt_$youtubeVideoId",
            uri = "https://www.youtube.com/watch?v=$youtubeVideoId",
            filePath = "",
            title = title,
            fileName = "$youtubeVideoId.mp4",
            folderName = playlistTitle,
            folderPath = playlistId,
            volumeLabel = "YouTube",
            isUsb = false,
            durationMs = 120_000L,
            sizeBytes = 0L,
            width = 1920,
            height = 1080,
            mimeType = "video/mp4",
            dateModified = 1_000L,
            isYoutube = true,
            youtubeVideoId = youtubeVideoId,
            playlistId = playlistId,
            playlistTitle = playlistTitle
        ),
        progress = null
    )

    private fun makeLocalVideo(
        id: String,
        folderName: String,
        folderPath: String,
        title: String = "Local $id"
    ): VideoItem = VideoItem(
        entity = VideoEntity(
            id = id,
            videoKey = "local_$id",
            uri = "file://$folderPath/$id.mp4",
            filePath = "$folderPath/$id.mp4",
            title = title,
            fileName = "$id.mp4",
            folderName = folderName,
            folderPath = folderPath,
            volumeLabel = "Internal",
            isUsb = false,
            durationMs = 60_000L,
            sizeBytes = 10_000L,
            width = 1280,
            height = 720,
            mimeType = "video/mp4",
            dateModified = 1_000L,
            isYoutube = false
        ),
        progress = null
    )

    @Test
    fun resolve_youtubeCategory_returnsAllVideosFromPlaylistsAndChannelsInSameCategory() {
        val playlists = listOf(
            SavedPlaylistEntity(
                playlistId = "PL_KIDS_1",
                title = "Kids Stories",
                sourceUrl = "https://youtube.com/playlist?list=PL_KIDS_1",
                category = "Islamic Kids"
            ),
            SavedPlaylistEntity(
                playlistId = "PL_SCIENCE",
                title = "Physics",
                sourceUrl = "https://youtube.com/playlist?list=PL_SCIENCE",
                category = "Science"
            )
        )
        val channels = listOf(
            SavedChannelEntity(
                channelId = "UC_KIDS_CH",
                handle = "@kids",
                title = "Kids Channel",
                uploadsPlaylistId = "UU_KIDS_CH",
                category = "Islamic Kids"
            )
        )

        val current = makeYoutubeVideo("v1", "yt1", "PL_KIDS_1")
        val sameCatPlaylist = makeYoutubeVideo("v2", "yt2", "PL_KIDS_1")
        val sameCatChannelUploads = makeYoutubeVideo("v3", "yt3", "UU_KIDS_CH")
        val sameCatChannelSynthetic = makeYoutubeVideo("v4", "yt4", "youtube_channel_UC_KIDS_CH")
        val otherCat = makeYoutubeVideo("v5", "yt5", "PL_SCIENCE")

        val result = RelatedVideosResolver_Antigravity.resolve(
            current = current,
            allVideos = listOf(current, sameCatPlaylist, sameCatChannelUploads, sameCatChannelSynthetic, otherCat),
            savedPlaylists = playlists,
            savedChannels = channels
        )

        assertNotNull(result)
        assertEquals("Islamic Kids", result!!.title)
        assertTrue(result.isCategory)
        assertEquals(listOf("v1", "v2", "v3", "v4"), result.videos.map { it.entity.id })
    }

    @Test
    fun resolve_youtubeCategory_deduplicatesByYoutubeVideoIdKeepingCurrent() {
        val playlists = listOf(
            SavedPlaylistEntity(
                playlistId = "PL_A",
                title = "Playlist A",
                sourceUrl = "https://youtube.com/playlist?list=PL_A",
                category = "Islamic Kids"
            ),
            SavedPlaylistEntity(
                playlistId = "PL_B",
                title = "Playlist B",
                sourceUrl = "https://youtube.com/playlist?list=PL_B",
                category = "Islamic Kids"
            )
        )

        val duplicateOther = makeYoutubeVideo("v_other", "dup_yt", "PL_A")
        val current = makeYoutubeVideo("v_current", "dup_yt", "PL_B")
        val another = makeYoutubeVideo("v_another", "unique_yt", "PL_A")

        val result = RelatedVideosResolver_Antigravity.resolve(
            current = current,
            allVideos = listOf(duplicateOther, another, current),
            savedPlaylists = playlists,
            savedChannels = emptyList()
        )

        assertNotNull(result)
        val ids = result!!.videos.map { it.entity.id }
        assertEquals(2, ids.size)
        assertTrue(ids.contains("v_current"))
        assertFalse(ids.contains("v_other"))
        assertTrue(ids.contains("v_another"))
    }

    @Test
    fun resolve_capsAtMaxItemsAndAlwaysContainsCurrent() {
        val playlists = listOf(
            SavedPlaylistEntity(
                playlistId = "PL_BIG",
                title = "Big Playlist",
                sourceUrl = "https://youtube.com/playlist?list=PL_BIG",
                category = "Stories"
            )
        )
        val videos = (1..75).map { idx ->
            makeYoutubeVideo("v_$idx", "yt_$idx", "PL_BIG")
        }
        val current = videos[60]

        val result = RelatedVideosResolver_Antigravity.resolve(
            current = current,
            allVideos = videos,
            savedPlaylists = playlists,
            savedChannels = emptyList(),
            maxItems = 40
        )

        assertNotNull(result)
        assertEquals(40, result!!.videos.size)
        assertTrue(result.videos.any { it.entity.id == current.entity.id })
    }

    @Test
    fun resolve_localVideo_fallsBackToSameFolderWithIsCategoryFalse() {
        val v1 = makeLocalVideo("l1", "Cartoons", "/storage/emulated/0/Movies/Cartoons")
        val v2 = makeLocalVideo("l2", "Cartoons", "/storage/emulated/0/Movies/Cartoons")
        val otherFolder = makeLocalVideo("l3", "Docs", "/storage/emulated/0/Movies/Docs")

        val result = RelatedVideosResolver_Antigravity.resolve(
            current = v1,
            allVideos = listOf(v1, v2, otherFolder),
            savedPlaylists = emptyList(),
            savedChannels = emptyList()
        )

        assertNotNull(result)
        assertEquals("Cartoons", result!!.title)
        assertFalse(result.isCategory)
        assertEquals(listOf("l1", "l2"), result.videos.map { it.entity.id })
    }

    @Test
    fun resolve_singleVideoInFolderOrCategory_returnsNull() {
        val singleLocal = makeLocalVideo("only1", "SoloFolder", "/storage/emulated/0/SoloFolder")
        val otherLocal = makeLocalVideo("other1", "OtherFolder", "/storage/emulated/0/OtherFolder")

        val localResult = RelatedVideosResolver_Antigravity.resolve(
            current = singleLocal,
            allVideos = listOf(singleLocal, otherLocal),
            savedPlaylists = emptyList(),
            savedChannels = emptyList()
        )
        assertNull(localResult)

        val singleYt = makeYoutubeVideo("yt_only", "yt_only", "PL_SOLO")
        val ytResult = RelatedVideosResolver_Antigravity.resolve(
            current = singleYt,
            allVideos = listOf(singleYt),
            savedPlaylists = listOf(
                SavedPlaylistEntity(
                    playlistId = "PL_SOLO",
                    title = "Solo",
                    sourceUrl = "https://youtube.com/playlist?list=PL_SOLO",
                    category = "General"
                )
            ),
            savedChannels = emptyList()
        )
        assertNull(ytResult)
    }

    @Test
    fun resolve_populatesSourcePlaylistSeparatelyFromCategory() {
        val playlists = listOf(
            SavedPlaylistEntity(
                playlistId = "PL_QURAN",
                title = "Quran Stories",
                sourceUrl = "https://youtube.com/playlist?list=PL_QURAN",
                category = "Islamic Kids"
            ),
            SavedPlaylistEntity(
                playlistId = "PL_NASHEED",
                title = "Kids Nasheeds",
                sourceUrl = "https://youtube.com/playlist?list=PL_NASHEED",
                category = "Islamic Kids"
            )
        )
        val v1 = makeYoutubeVideo("q1", "yt_q1", "PL_QURAN", "Quran Stories")
        val v2 = makeYoutubeVideo("q2", "yt_q2", "PL_QURAN", "Quran Stories")
        val v3 = makeYoutubeVideo("n1", "yt_n1", "PL_NASHEED", "Kids Nasheeds")

        val result = RelatedVideosResolver_Antigravity.resolve(
            current = v1,
            allVideos = listOf(v1, v2, v3),
            savedPlaylists = playlists,
            savedChannels = emptyList()
        )

        assertNotNull(result)
        assertEquals("Islamic Kids", result!!.title)
        assertEquals(listOf("q1", "q2", "n1"), result.videos.map { it.entity.id })
        assertEquals("Quran Stories", result.sourceTitle)
        assertEquals(listOf("q1", "q2"), result.sourceVideos.map { it.entity.id })
    }
}
