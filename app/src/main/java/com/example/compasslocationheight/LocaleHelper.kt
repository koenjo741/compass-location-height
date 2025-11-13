package com.example.compasslocationheight

import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object LocaleHelper {
    fun setLocale(context: Context, languageCode: String) {
        Toast.makeText(context, "setLocale called with: $languageCode", Toast.LENGTH_SHORT).show()
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