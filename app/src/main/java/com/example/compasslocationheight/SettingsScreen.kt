package com.example.compasslocationheight

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(
    // Wir übergeben die Farben, damit der Screen zum Theme passt
    backgroundColor: Color,
    textColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor), // Hintergrundfarbe setzen
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Einstellungen (wird noch gebaut)",
            color = textColor, // Textfarbe setzen
            fontSize = 20.sp
        )
    }
}