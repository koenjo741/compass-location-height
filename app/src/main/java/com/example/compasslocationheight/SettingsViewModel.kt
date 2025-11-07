package com.example.compasslocationheight

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

// Die Temperatureinheit als eigener Datentyp
enum class TemperatureUnit {
    Celsius, Fahrenheit
}

// Das ViewModel. Es erbt von androidx.lifecycle.ViewModel
class SettingsViewModel : ViewModel() {

    // Hier speichern wir den Zustand der UI-Einstellungen.
    // Diese Variablen sind "beobachtbar", d.h. die UI merkt, wenn sie sich ändern.

    // Wir verschieben die Theme-Auswahl von der MainActivity hierher.
    var themeMode by mutableStateOf(ThemeMode.Dark)
        private set // 'private set' bedeutet, nur das ViewModel selbst kann den Wert ändern.

    // Das ist die neue Einstellung für die Temperatureinheit.
    var tempUnit by mutableStateOf(TemperatureUnit.Celsius)
        private set

    // --- Öffentliche Funktionen, die die UI aufrufen kann, um Änderungen anzufordern ---

    fun setTheme(newTheme: ThemeMode) {
        themeMode = newTheme
    }

    fun setTemperatureUnit(newUnit: TemperatureUnit) {
        tempUnit = newUnit
    }
}