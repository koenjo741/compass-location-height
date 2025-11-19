package com.example.compasslocationheight

import android.content.Context
import java.util.Locale

object LocaleHelper {
    fun setLocale(context: Context, languageCode: String) {
        val locale = if (languageCode == "system") {
            Locale.getDefault()
        } else {
            // Handle BCP 47 tags format used in Android resource folders (e.g., "b+cnr")
            if (languageCode.startsWith("b+")) {
                val code = languageCode.substring(2) // Remove "b+" prefix
                Locale(code)
            }
            // Check if the code contains a region separator (e.g., "zh-CN" or "zh-rCN" or "zh_CN")
            else if (languageCode.contains("-r")) {
                 // Handle legacy Android folder names like "zh-rCN" if passed directly
                 val parts = languageCode.split("-r")
                 if (parts.size == 2) {
                     Locale(parts[0], parts[1])
                 } else {
                     Locale(languageCode)
                 }
            } else if (languageCode.contains("-")) {
                val parts = languageCode.split("-")
                if (parts.size == 2) {
                    Locale(parts[0], parts[1])
                } else {
                    Locale(languageCode)
                }
            } else if (languageCode.contains("_")) {
                val parts = languageCode.split("_")
                if (parts.size == 2) {
                    Locale(parts[0], parts[1])
                } else {
                    Locale(languageCode)
                }
            } else {
                Locale(languageCode)
            }
        }
        Locale.setDefault(locale)
        val resources = context.resources
        val configuration = resources.configuration
        configuration.setLocale(locale)
        resources.updateConfiguration(configuration, resources.displayMetrics)
    }
}