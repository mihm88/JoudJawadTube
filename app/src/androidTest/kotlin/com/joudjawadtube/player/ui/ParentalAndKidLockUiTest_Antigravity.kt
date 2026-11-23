package com.joudjawadtube.player.ui

import android.content.Context
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.joudjawadtube.player.R
import com.joudjawadtube.player.data.local.VideoEntity
import com.joudjawadtube.player.data.local.VideoItem
import com.joudjawadtube.player.ui.components.CompactBottomNav_Antigravity
import com.joudjawadtube.player.ui.components.ParentalGateDialog_Antigravity
import com.joudjawadtube.player.ui.components.SidebarSection
import com.joudjawadtube.player.ui.playback.SmartTubePlayerHud
import com.joudjawadtube.player.ui.theme.JoudJawadTubeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ParentalAndKidLockUiTest_Antigravity {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private fun extractChallengeProduct(): Int {
        val promptPrefix = context.getString(R.string.parental_gate_prompt, 0, 0)
            .substringBefore("0")
            .trim()
        val promptNode = composeTestRule.onNodeWithText(promptPrefix, substring = true).fetchSemanticsNode()
        val promptText = promptNode.config[SemanticsProperties.Text].joinToString("") { it.text }
        val factors = Regex("""\d+""").findAll(promptText).map { it.value.toInt() }.toList()
        assertEquals("Expected two factors in parental gate prompt: $promptText", 2, factors.size)
        return factors[0] * factors[1]
    }

    @Test
    fun parentalGateDialog_wrongAnswerShowsFeedback_andCorrectAnswerCallsOnSuccess() {
        var successCount = 0
        var dismissCount = 0

        composeTestRule.setContent {
            JoudJawadTubeTheme {
                ParentalGateDialog_Antigravity(
                    onSuccess = { successCount++ },
                    onDismiss = { dismissCount++ }
                )
            }
        }

        val initialProduct = extractChallengeProduct()
        val numericOptionNodes = composeTestRule.onAllNodes(
            SemanticsMatcher("Numeric option button") { node ->
                val text = node.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text }
                text != null && text.matches(Regex("""^\d+$"""))
            }
        ).fetchSemanticsNodes()

        val wrongText = numericOptionNodes
            .map { it.config[SemanticsProperties.Text].joinToString("") { s -> s.text } }
            .first { it != initialProduct.toString() }

        composeTestRule.onNodeWithText(wrongText).performClick()
        composeTestRule.waitForIdle()

        assertEquals(0, successCount)
        composeTestRule.onNodeWithText(context.getString(R.string.parental_gate_wrong)).assertIsDisplayed()

        val refreshedProduct = extractChallengeProduct()
        composeTestRule.onNodeWithText(refreshedProduct.toString()).performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, successCount)
        assertEquals(0, dismissCount)
    }

    @Test
    fun parentalGateDialog_cancelInvokesOnDismiss() {
        var dismissCount = 0

        composeTestRule.setContent {
            JoudJawadTubeTheme {
                ParentalGateDialog_Antigravity(
                    onSuccess = {},
                    onDismiss = { dismissCount++ }
                )
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.action_cancel)).performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, dismissCount)
    }

    @Test
    fun compactBottomNav_rendersFourTabsAndOpensMoreParentsSheet() {
        var selected: SidebarSection = SidebarSection.HOME

        composeTestRule.setContent {
            JoudJawadTubeTheme {
                CompactBottomNav_Antigravity(
                    selectedSection = selected,
                    onSelectSection = { selected = it }
                )
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.nav_home)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.nav_channels)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.nav_favorites)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.nav_more_parents)).assertIsDisplayed()

        composeTestRule.onNodeWithText(context.getString(R.string.nav_more_parents)).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(context.getString(R.string.nav_kids_picks)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.nav_settings)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.nav_settings)).performClick()
        composeTestRule.waitForIdle()

        assertEquals(SidebarSection.SETTINGS, selected)
    }

    @Test
    fun smartTubePlayerHud_rendersKidLockButtonAndInvokesCallback() {
        var lockInvoked = false
        val sampleVideo = VideoItem(
            entity = VideoEntity(
                id = "yt_dQw4w9WgXcQ",
                videoKey = "yt_dQw4w9WgXcQ",
                uri = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                filePath = "",
                title = "Kids Sample Story",
                fileName = "Kids Sample Story [dQw4w9WgXcQ].mp4",
                folderName = "Kids Stories",
                folderPath = "kids_stories",
                volumeLabel = "YouTube",
                isUsb = false,
                durationMs = 120_000L,
                sizeBytes = 0L,
                width = 1920,
                height = 1080,
                mimeType = "video/mp4",
                dateModified = 0L,
                isYoutube = true,
                youtubeVideoId = "dQw4w9WgXcQ"
            ),
            progress = null
        )

        composeTestRule.setContent {
            JoudJawadTubeTheme {
                SmartTubePlayerHud(
                    visible = true,
                    video = sampleVideo,
                    isPlaying = true,
                    positionProvider = { 10_000L },
                    durationMs = 120_000L,
                    videoStreamBadge = "1080p",
                    hasPrevious = false,
                    hasNext = false,
                    onPlayPauseToggle = {},
                    onSeekTo = {},
                    onSeekRelative = {},
                    onPreviousVideo = {},
                    onNextVideo = {},
                    onToggleFavorite = {},
                    onLockScreen = { lockInvoked = true },
                    onBack = {},
                    onUserInteraction = {}
                )
            }
        }

        val lockNode = composeTestRule.onNodeWithText(context.getString(R.string.kid_lock_label))
        lockNode.performScrollTo()
        lockNode.assertIsDisplayed()
        lockNode.performClick()
        composeTestRule.waitForIdle()

        assertTrue(lockInvoked)
    }
}
