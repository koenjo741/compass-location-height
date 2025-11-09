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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    settingsViewModel: SettingsViewModel,
    backgroundColor: Color,
    textColor: Color,
    headingColor: Color
) {
    val currentTheme by settingsViewModel.themeMode
    val currentTempUnit by settingsViewModel.tempUnit
    val currentLanguage by settingsViewModel.language

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(16.dp)
    ) {
        // Theme Section
        Text(text = stringResource(R.string.theme_section_title), fontSize = 20.sp, color = textColor)
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ThemeButton(
                text = stringResource(R.string.theme_dark),
                onClick = { settingsViewModel.setTheme(ThemeMode.Dark) },
                isSelected = currentTheme == ThemeMode.Dark,
                selectedColor = headingColor
            )
            ThemeButton(
                text = stringResource(R.string.theme_light),
                onClick = { settingsViewModel.setTheme(ThemeMode.Light) },
                isSelected = currentTheme == ThemeMode.Light,
                selectedColor = headingColor
            )
            ThemeButton(
                text = stringResource(R.string.theme_night),
                onClick = { settingsViewModel.setTheme(ThemeMode.Night) },
                isSelected = currentTheme == ThemeMode.Night,
                selectedColor = headingColor
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Temperature Unit Section
        Text(text = stringResource(R.string.temp_unit_section_title), fontSize = 20.sp, color = textColor)
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ThemeButton(
                text = stringResource(R.string.unit_celsius),
                onClick = { settingsViewModel.setTemperatureUnit(TemperatureUnit.Celsius) },
                isSelected = currentTempUnit == TemperatureUnit.Celsius,
                selectedColor = headingColor
            )
            ThemeButton(
                text = stringResource(R.string.unit_fahrenheit),
                onClick = { settingsViewModel.setTemperatureUnit(TemperatureUnit.Fahrenheit) },
                isSelected = currentTempUnit == TemperatureUnit.Fahrenheit,
                selectedColor = headingColor
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Language Section
        Text(text = "Sprache / Language", fontSize = 20.sp, color = textColor)
        Spacer(modifier = Modifier.height(16.dp))

        var expanded by remember { mutableStateOf(false) }
        val systemLocale = java.util.Locale.getDefault().displayLanguage
        val systemLanguageString = "System ($systemLocale)"
        val languageOptions = mapOf(
            "system" to systemLanguageString,
            "de" to "Deutsch",
            "en" to "English"
        )
        val selectedOptionText = languageOptions[currentLanguage] ?: languageOptions["system"]!!

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            TextField(
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                readOnly = true,
                value = selectedOptionText,
                onValueChange = {},
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.textFieldColors(),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                languageOptions.forEach { (langCode, langName) ->
                    DropdownMenuItem(
                        text = { Text(text = langName) },
                        onClick = {
                            settingsViewModel.setLanguage(langCode)
                            expanded = false
                        }
                    )
                }
            }
        }

        // Back Button
        Spacer(Modifier.weight(1f))
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = headingColor)
        ) {
            Text(text = stringResource(R.string.button_back), fontSize = 18.sp)
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