package com.example.compasslocationheight

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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

    val activeGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF1A2980), Color(0xFF26D0CE))
    )
    
    val actionGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFFFF512F), Color(0xFFDD2476))
    )

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
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(R.string.theme_section_title), 
                fontSize = 20.sp, 
                color = textColor, 
                // Reduziert von Bold auf SemiBold (ca. -20% Gewicht)
                fontWeight = FontWeight.SemiBold
            )
            // Abstand erhöht von 16.dp auf 24.dp
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    ThemeButton(
                        text = stringResource(R.string.theme_dark), 
                        onClick = { settingsViewModel.setTheme(ThemeMode.Dark) }, 
                        isSelected = currentTheme == ThemeMode.Dark, 
                        activeGradient = activeGradient
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    ThemeButton(
                        text = stringResource(R.string.theme_night), 
                        onClick = { settingsViewModel.setTheme(ThemeMode.Night) }, 
                        isSelected = currentTheme == ThemeMode.Night, 
                        activeGradient = activeGradient
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    ThemeButton(
                        text = stringResource(R.string.theme_light), 
                        onClick = { settingsViewModel.setTheme(ThemeMode.Light) }, 
                        isSelected = currentTheme == ThemeMode.Light, 
                        activeGradient = activeGradient
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = stringResource(R.string.temp_unit_section_title), 
                fontSize = 20.sp, 
                color = textColor, 
                // Reduziert von Bold auf SemiBold
                fontWeight = FontWeight.SemiBold
            )
            // Abstand erhöht von 16.dp auf 24.dp
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    ThemeButton(
                        text = stringResource(R.string.unit_celsius), 
                        onClick = { settingsViewModel.setTemperatureUnit(TemperatureUnit.Celsius) }, 
                        isSelected = currentTempUnit == TemperatureUnit.Celsius, 
                        activeGradient = activeGradient
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    ThemeButton(
                        text = stringResource(R.string.unit_fahrenheit), 
                        onClick = { settingsViewModel.setTemperatureUnit(TemperatureUnit.Fahrenheit) }, 
                        isSelected = currentTempUnit == TemperatureUnit.Fahrenheit, 
                        activeGradient = activeGradient
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(R.string.language_section_title), 
                fontSize = 20.sp, 
                color = textColor, 
                // Reduziert von Bold auf SemiBold
                fontWeight = FontWeight.SemiBold
            )
            // Abstand erhöht von 16.dp auf 24.dp
            Spacer(modifier = Modifier.height(24.dp))

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
            
            GradientButton(
                text = stringResource(R.string.button_back),
                onClick = { navController.popBackStack() },
                gradient = actionGradient,
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
            )
        }
    }
}

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    gradient: Brush,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(),
        modifier = modifier
            .height(48.dp)
            .shadow(5.dp, CircleShape)
            .background(gradient, CircleShape)
            .clip(CircleShape)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = text,
                color = textColor,
                fontSize = 18.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ThemeButton(
    text: String, 
    onClick: () -> Unit, 
    isSelected: Boolean, 
    activeGradient: Brush
) {
    if (isSelected) {
        GradientButton(
            text = text,
            onClick = onClick,
            gradient = activeGradient,
            modifier = Modifier.fillMaxWidth()
        )
    } else {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF424242), 
                contentColor = Color.LightGray
            ),
            modifier = Modifier
                .height(48.dp)
                .fillMaxWidth() 
                .clip(CircleShape)
        ) {
            Text(
                text = text, 
                fontSize = 18.5.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}