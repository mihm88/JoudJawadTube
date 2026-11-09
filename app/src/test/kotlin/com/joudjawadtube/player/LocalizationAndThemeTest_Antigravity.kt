package com.joudjawadtube.player

import com.joudjawadtube.player.data.settings.AppLanguage
import com.joudjawadtube.player.data.settings.AppSettingsState
import com.joudjawadtube.player.data.settings.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocalizationAndThemeTest_Antigravity {

    @Test
    fun testAppLanguageTags() {
        assertNull(AppLanguage.FOLLOW_SYSTEM.tag)
        assertEquals("en", AppLanguage.ENGLISH.tag)
        assertEquals("ar", AppLanguage.ARABIC.tag)
    }

    @Test
    fun testThemeModes() {
        val modes = ThemeMode.entries
        assertEquals(3, modes.size)
        assertEquals(ThemeMode.FOLLOW_SYSTEM, modes[0])
        assertEquals(ThemeMode.DARK, modes[1])
        assertEquals(ThemeMode.LIGHT, modes[2])
    }

    @Test
    fun testDefaultSettingsContainSystemLanguageAndTheme() {
        val state = AppSettingsState()
        assertEquals(AppLanguage.FOLLOW_SYSTEM, state.appLanguage)
        assertEquals(ThemeMode.FOLLOW_SYSTEM, state.themeMode)
    }
}
