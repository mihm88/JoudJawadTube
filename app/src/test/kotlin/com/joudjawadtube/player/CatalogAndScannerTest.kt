package com.joudjawadtube.player

import androidx.media3.common.MimeTypes
import com.joudjawadtube.player.data.local.FormatUtils
import com.joudjawadtube.player.data.local.SidecarSubtitle
import com.joudjawadtube.player.data.local.SubtitleCodec
import com.joudjawadtube.player.data.scanner.LocalVideoScanner
import com.joudjawadtube.player.data.scanner.MountedVolumeInfo
import com.joudjawadtube.player.data.scanner.StorageVolumeHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogAndScannerTest {

    @Test
    fun formatDuration_formatsMinutesAndHoursCorrectly() {
        assertEquals("00:00", FormatUtils.formatDuration(0L))
        assertEquals("00:00", FormatUtils.formatDuration(-500L))
        assertEquals("00:09", FormatUtils.formatDuration(9_000L))
        assertEquals("04:15", FormatUtils.formatDuration(255_000L))
        assertEquals("1:02:05", FormatUtils.formatDuration(3_725_000L))
    }

    @Test
    fun computeProgressFraction_clampsBetweenZeroAndOne() {
        assertEquals(0f, FormatUtils.computeProgressFraction(0L, 100_000L), 0.001f)
        assertEquals(0f, FormatUtils.computeProgressFraction(5_000L, 0L), 0.001f)
        assertEquals(0.25f, FormatUtils.computeProgressFraction(25_000L, 100_000L), 0.001f)
        assertEquals(1.0f, FormatUtils.computeProgressFraction(150_000L, 100_000L), 0.001f)
    }

    @Test
    fun resolutionBadge_identifiesUhdAndHdCorrectly() {
        assertEquals("4K", FormatUtils.resolutionBadge(3840, 2160))
        assertEquals("2K", FormatUtils.resolutionBadge(2560, 1440))
        assertEquals("1080p", FormatUtils.resolutionBadge(1920, 1080))
        assertEquals("720p", FormatUtils.resolutionBadge(1280, 720))
        assertEquals("SD", FormatUtils.resolutionBadge(854, 480))
        assertNull(FormatUtils.resolutionBadge(0, 0))
    }

    @Test
    fun subtitleCodec_encodesAndDecodesLosslessly() {
        val original = listOf(
            SidecarSubtitle(
                uriOrPath = "file:///storage/USB1/Movies/Cartoon.en.srt",
                mimeType = MimeTypes.APPLICATION_SUBRIP,
                language = "en",
                label = "External - EN (SRT)"
            ),
            SidecarSubtitle(
                uriOrPath = "file:///storage/USB1/Movies/Cartoon.ar.ass",
                mimeType = MimeTypes.TEXT_SSA,
                language = "ar",
                label = "External - AR (ASS)"
            )
        )

        val encoded = SubtitleCodec.encode(original)
        val decoded = SubtitleCodec.decode(encoded)

        assertEquals(2, decoded.size)
        assertEquals(original[0], decoded[0])
        assertEquals(original[1], decoded[1])
    }

    @Test
    fun matchSidecarSubtitles_matchesExactAndLanguageVariants() {
        val siblings = listOf(
            "Joud_Episode_01.srt" to "file:///usb/Joud_Episode_01.srt",
            "Joud_Episode_01.en.vtt" to "file:///usb/Joud_Episode_01.en.vtt",
            "Joud_Episode_01_ar.ass" to "file:///usb/Joud_Episode_01_ar.ass",
            "Joud_Episode_02.srt" to "file:///usb/Joud_Episode_02.srt"
        )

        val matched = LocalVideoScanner.matchSidecarSubtitles("Joud_Episode_01.mkv", siblings)
        assertEquals(3, matched.size)

        assertEquals("und", matched[0].language)
        assertEquals(MimeTypes.APPLICATION_SUBRIP, matched[0].mimeType)

        assertEquals("en", matched[1].language)
        assertEquals(MimeTypes.TEXT_VTT, matched[1].mimeType)

        assertEquals("ar", matched[2].language)
        assertEquals(MimeTypes.TEXT_SSA, matched[2].mimeType)
    }

    @Test
    fun classifyFilePath_distinguishesInternalAndUsbVolumes() {
        val volumes = listOf(
            MountedVolumeInfo("primary", "Internal Storage", "/storage/emulated/0", false),
            MountedVolumeInfo("1A2B-3C4D", "USB: SanDisk 64GB", "/storage/1A2B-3C4D", true)
        )

        val internalClassification = StorageVolumeHelper.classifyFilePath(
            "/storage/emulated/0/Movies/Family_Clip.mp4",
            volumes
        )
        assertFalse(internalClassification.isUsbOrRemovable)
        assertEquals("Internal Storage", internalClassification.label)

        val usbClassification = StorageVolumeHelper.classifyFilePath(
            "/storage/1A2B-3C4D/KidsShows/Episode1.mkv",
            volumes
        )
        assertTrue(usbClassification.isUsbOrRemovable)
        assertEquals("USB: SanDisk 64GB", usbClassification.label)
    }

    @Test
    fun prettifyVideoTitle_cleansDotsAndUnderscores() {
        val cleaned = LocalVideoScanner.prettifyVideoTitle("Joud.and.Jawad_4K_Adventure.2026")
        assertEquals("Joud and Jawad 4K Adventure 2026", cleaned)
        val cleanedWithYtId = LocalVideoScanner.prettifyVideoTitle("Omar & Hana Arabic [O5VoZkNQH_s]")
        assertEquals("Omar & Hana Arabic", cleanedWithYtId)
    }

    @Test
    fun youtubeIdExtractor_extractsFromFilenamesAndUrls() {
        val id1 = com.joudjawadtube.player.data.local.YouTubeIdExtractor.extractFromFileNameOrUri(
            "Omar & Hana Arabic ｜ أنا خائفة جدًّا [O5VoZkNQH_s].webm"
        )
        assertEquals("O5VoZkNQH_s", id1)

        val id2 = com.joudjawadtube.player.data.local.YouTubeIdExtractor.extractFromFileNameOrUri(
            "video.mp4",
            "https://www.youtube.com/watch?v=IJT0Hk7d808"
        )
        assertEquals("IJT0Hk7d808", id2)
    }

    @Test
    fun youtubeService_parsesSingleVideoAndPlaylistLinks() {
        val single = com.joudjawadtube.player.data.youtube.YouTubeService.parseInput(
            "https://youtu.be/XxhJziwo998?si=123"
        )
        assertTrue(single is com.joudjawadtube.player.data.youtube.ParsedYouTubeInput.SingleVideo)
        assertEquals(
            "XxhJziwo998",
            (single as com.joudjawadtube.player.data.youtube.ParsedYouTubeInput.SingleVideo).videoId
        )

        val playlist = com.joudjawadtube.player.data.youtube.YouTubeService.parseInput(
            "https://www.youtube.com/playlist?list=PL1234567890ABCDEF"
        )
        assertTrue(playlist is com.joudjawadtube.player.data.youtube.ParsedYouTubeInput.Playlist)
        assertEquals(
            "PL1234567890ABCDEF",
            (playlist as com.joudjawadtube.player.data.youtube.ParsedYouTubeInput.Playlist).playlistId
        )
    }
}
