package com.example.compasslocationheight

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object LocaleHelper {
    fun setLocale(languageCode: String) {
        val appLocale: LocaleListCompat = when (languageCode) {
            "system" -> {
                LocaleListCompat.getEmptyLocaleList()
            }
            else -> {
                LocaleListCompat.forLanguageTags(languageCode)
            }
        }
        AppCompatDelegate.setApplicationLocales(appLocale)
    }
}