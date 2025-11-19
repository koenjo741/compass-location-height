package com.example.compasslocationheight

import android.content.Context
import java.util.Locale

object LocaleHelper {
    fun setLocale(context: Context, languageCode: String) {
        val locale = if (languageCode == "system") {
            Locale.getDefault()
        } else {
            // Check if the code contains a region separator (e.g., "zh-CN" or "zh-rCN" or "zh_CN")
            // Also handle BCP 47 tags like "b+cnr" if necessary, but simple split usually works for region.
            // Note: "zh-rCN" is an Android resource folder name convention, not a standard locale tag.
            // We expect standard codes like "zh-CN" from arrays.xml.
            if (languageCode.contains("-r")) {
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