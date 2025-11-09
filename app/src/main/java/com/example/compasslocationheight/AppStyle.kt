// --- AppStyle.kt ---
package com.example.compasslocationheight

import androidx.compose.ui.graphics.Color

object AppColors {
    // Dark Mode (unser bisheriger Standard)
    val DarkBackground = Color.Black
    val DarkText = Color(0xFFFFFAF0) // FloralWhite
    val DarkHeading = Color(0xFF1E90FF) // Blau
    val DarkAccent = Color(0xFFFE0000)  // Rot
    val DarkSubtle = Color.Gray

    // Light Mode
    val LightBackground = Color.White
    val LightText = Color.Black
    val LightHeading = Color(0xFF0000DD) // Dunkleres Blau
    val LightAccent = Color(0xFFCC0000)  // Dunkleres Rot
    val LightSubtle = Color.DarkGray

    // Night Mode (Rotlicht)
    val NightBackground = Color.Black
    val NightText = Color(0xFFB71C1C) // Dunkles Rot
    val NightHeading = Color(0xFF0045F5).copy(alpha = 0.7f) // Abgedunkeltes Blau -> Violettstich
    val NightAccent = Color(0xFFF44336)   // Helles Rot
    val NightSubtle = Color(0xFF4E342E)   // Sehr dunkles Rotbraun

    // Fadenkreuz & Wasserwaage (bleiben meist gleich)
    val CrosshairGreen = Color(0xFF33FF33)
    val BubbleOrange = Color(0xFFFF9933)
}

enum class ThemeMode {
    Dark, Light, Night
}