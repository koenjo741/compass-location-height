// --- SettingsScreen.kt (Version mit Zurück-Button unten) ---

package com.example.compasslocationheight

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun SettingsScreen(
    navController: NavController,
    settingsViewModel: SettingsViewModel,
    backgroundColor: Color,
    textColor: Color,
    headingColor: Color
) {
    val currentTheme = settingsViewModel.themeMode

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(16.dp)
    ) {
        // Die obere Leiste wurde komplett entfernt.

        Spacer(modifier = Modifier.height(16.dp))

        // Abschnitt für die Theme-Auswahl (bleibt gleich)
        Text(text = "App-Theme", fontSize = 20.sp, color = textColor)
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ThemeButton(
                text = "Dark",
                onClick = { settingsViewModel.setTheme(ThemeMode.Dark) },
                isSelected = currentTheme == ThemeMode.Dark,
                selectedColor = headingColor
            )
            ThemeButton(
                text = "Light",
                onClick = { settingsViewModel.setTheme(ThemeMode.Light) },
                isSelected = currentTheme == ThemeMode.Light,
                selectedColor = headingColor
            )
            ThemeButton(
                text = "Night",
                onClick = { settingsViewModel.setTheme(ThemeMode.Night) },
                isSelected = currentTheme == ThemeMode.Night,
                selectedColor = headingColor
            )
        }

        // Dieser Spacer nimmt allen übrigen Platz ein und schiebt den Button nach unten.
        Spacer(Modifier.weight(1f))

        // NEU: Der Zurück-Button am unteren Rand
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = headingColor)
        ) {
            Text(text = "Zurück", fontSize = 18.sp)
        }
    }
}

@Composable
private fun ThemeButton(
    text: String,
    onClick: () -> Unit,
    isSelected: Boolean,
    selectedColor: Color
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) selectedColor else Color.Gray,
            contentColor = Color.White
        )
    ) {
        Text(text)
    }
}