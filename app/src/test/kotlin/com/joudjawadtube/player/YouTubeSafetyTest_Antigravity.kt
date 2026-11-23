package com.joudjawadtube.player

import com.joudjawadtube.player.data.youtube.ResolvedYouTubeStream
import com.joudjawadtube.player.data.youtube.YouTubeService
import com.joudjawadtube.player.ui.components.ParentalGateChallenge_Antigravity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class YouTubeSafetyTest_Antigravity {

    @Test
    fun parentalGateChallenge_generatesValidRangeAndFourDistinctOptions() {
        val random = Random(42)
        repeat(100) {
            val challenge = ParentalGateChallenge_Antigravity.generate(random)
            assertTrue("a=${challenge.a} out of 6..9", challenge.a in 6..9)
            assertTrue("b=${challenge.b} out of 4..8", challenge.b in 4..8)
            assertEquals(challenge.a * challenge.b, challenge.correctAnswer)
            assertEquals(4, challenge.options.size)
            assertEquals(4, challenge.options.distinct().size)
            assertTrue(challenge.options.contains(challenge.correctAnswer))
            assertTrue(challenge.options.all { it > 0 })
        }
    }

    @Test
    fun safeVideoIdRegex_acceptsValidElevenCharIds() {
        val validIds = listOf(
            "dQw4w9WgXcQ",
            "IJT0Hk7d808",
            "a_b-c_D-123",
            "0123456789_"
        )
        validIds.forEach { id ->
            assertTrue("Expected valid video ID: $id", YouTubeService.SAFE_VIDEO_ID_REGEX.matches(id))
        }
    }

    @Test
    fun safeVideoIdRegex_rejectsMaliciousOrMalformedIds() {
        val invalidIds = listOf(
            "",
            "short",
            "1234567890",
            "123456789012",
            "abc\"onClick",
            "dQw4w9WgX'Q",
            "<script>ale",
            "dQw4w9WgX/Q",
            "dQw4w9WgX Q",
            "dQw4w9WgX\nQ",
            "dQw4w9WgX?Q"
        )
        invalidIds.forEach { id ->
            assertFalse("Expected rejected video ID: $id", YouTubeService.SAFE_VIDEO_ID_REGEX.matches(id))
        }
    }

    @Test
    fun resolvedYouTubeStream_exposesRestrictionReason() {
        val unrestricted = ResolvedYouTubeStream(videoId = "IJT0Hk7d808")
        assertNull(unrestricted.restrictionReason)
        assertFalse(unrestricted.hasDirectStream)

        val restricted = ResolvedYouTubeStream(
            videoId = "IJT0Hk7d808",
            restrictionReason = "KIDS_RESTRICTED"
        )
        assertEquals("KIDS_RESTRICTED", restricted.restrictionReason)
        assertFalse(restricted.hasDirectStream)
    }
}
