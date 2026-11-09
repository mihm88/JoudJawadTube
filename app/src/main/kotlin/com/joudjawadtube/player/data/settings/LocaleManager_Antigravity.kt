package com.joudjawadtube.player.data.settings

import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.joudjawadtube.player.R

enum class AppLanguage(val tag: String?, @StringRes val labelRes: Int) {
    FOLLOW_SYSTEM(null, R.string.settings_language_system),
    ENGLISH("en", R.string.settings_language_english),
    ARABIC("ar", R.string.settings_language_arabic)
}

enum class ThemeMode(@StringRes val labelRes: Int) {
    FOLLOW_SYSTEM(R.string.settings_theme_system),
    DARK(R.string.settings_theme_dark),
    LIGHT(R.string.settings_theme_light)
}

object LocaleManager_Antigravity {

    fun apply(language: AppLanguage) {
        val localeList = if (language.tag.isNullOrBlank()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(language.tag)
        }

        if (AppCompatDelegate.getApplicationLocales() != localeList) {
            AppCompatDelegate.setApplicationLocales(localeList)
        }
    }

    fun applyTheme(themeMode: ThemeMode) {
        val mode = when (themeMode) {
            ThemeMode.FOLLOW_SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
        }
        if (AppCompatDelegate.getDefaultNightMode() != mode) {
            AppCompatDelegate.setDefaultNightMode(mode)
        }
    }
}
