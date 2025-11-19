package com.example.compasslocationheight

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
    val currentTheme by settingsViewModel.themeMode.collectAsState()
    val currentTempUnit by settingsViewModel.tempUnit.collectAsState()
    val currentLanguage by settingsViewModel.language.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = backgroundColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(text = stringResource(R.string.theme_section_title), fontSize = 20.sp, color = textColor)
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ThemeButton(text = stringResource(R.string.theme_dark), onClick = { settingsViewModel.setTheme(ThemeMode.Dark) }, isSelected = currentTheme == ThemeMode.Dark, selectedColor = headingColor)
                ThemeButton(text = stringResource(R.string.theme_light), onClick = { settingsViewModel.setTheme(ThemeMode.Light) }, isSelected = currentTheme == ThemeMode.Light, selectedColor = headingColor)
                ThemeButton(text = stringResource(R.string.theme_night), onClick = { settingsViewModel.setTheme(ThemeMode.Night) }, isSelected = currentTheme == ThemeMode.Night, selectedColor = headingColor)
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text(text = stringResource(R.string.temp_unit_section_title), fontSize = 20.sp, color = textColor)
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ThemeButton(text = stringResource(R.string.unit_celsius), onClick = { settingsViewModel.setTemperatureUnit(TemperatureUnit.Celsius) }, isSelected = currentTempUnit == TemperatureUnit.Celsius, selectedColor = headingColor)
                ThemeButton(text = stringResource(R.string.unit_fahrenheit), onClick = { settingsViewModel.setTemperatureUnit(TemperatureUnit.Fahrenheit) }, isSelected = currentTempUnit == TemperatureUnit.Fahrenheit, selectedColor = headingColor)
            }
            Spacer(modifier = Modifier.height(32.dp))

            Text(text = "Sprache / Language", fontSize = 20.sp, color = textColor)
            Spacer(modifier = Modifier.height(16.dp))

            var expanded by remember { mutableStateOf(false) }
            val context = LocalContext.current
            val languageNames = context.resources.getStringArray(R.array.language_names)
            val languageCodes = context.resources.getStringArray(R.array.language_codes)
            val languageOptions = remember(languageNames, languageCodes) {
                val systemLocale = java.util.Locale.getDefault().displayLanguage
                val systemLanguageString = "System ($systemLocale)"
                mapOf("system" to systemLanguageString) + languageCodes.zip(languageNames).toMap()
            }
            val selectedOptionText = languageOptions[currentLanguage] ?: languageOptions["system"]!!

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                TextField(
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
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

            Spacer(Modifier.weight(1f))
            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = headingColor)
            ) {
                Text(text = stringResource(R.string.button_back), fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun ThemeButton(text: String, onClick: () -> Unit, isSelected: Boolean, selectedColor: Color) {
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