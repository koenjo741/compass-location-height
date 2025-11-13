package com.example.compasslocationheight

import android.content.Context
import java.util.Locale

object LocaleHelper {
    fun setLocale(context: Context, languageCode: String) {
        val locale = if (languageCode == "system") {
            Locale.getDefault()
        } else if (languageCode.contains("+")) {
            val parts = languageCode.split("+")
            Locale(parts[0], parts[1].uppercase(Locale.ROOT))
        } else {
            Locale(languageCode)
        }
        Locale.setDefault(locale)
        val resources = context.resources
        val configuration = resources.configuration
        configuration.setLocale(locale)
        resources.updateConfiguration(configuration, resources.displayMetrics)
    }
}