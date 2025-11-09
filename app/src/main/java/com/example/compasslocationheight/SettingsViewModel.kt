package com.example.compasslocationheight

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

enum class TemperatureUnit {
    Celsius, Fahrenheit
}

class SettingsViewModel(private val dataStore: SettingsDataStore) : ViewModel() {

    var themeMode by mutableStateOf(ThemeMode.Dark)
        private set

    var tempUnit by mutableStateOf(TemperatureUnit.Celsius)
        private set

    // HIER DIE ÄNDERUNG: "private set" wurde entfernt.
    var language by mutableStateOf("system")

    init {
        viewModelScope.launch {
            dataStore.themeModeFlow.collect { loadedTheme ->
                themeMode = loadedTheme
            }
        }
        viewModelScope.launch {
            dataStore.tempUnitFlow.collect { loadedUnit ->
                tempUnit = loadedUnit
            }
        }
    }

    fun setTheme(newTheme: ThemeMode) {
        themeMode = newTheme
        viewModelScope.launch {
            dataStore.saveThemeMode(newTheme)
        }
    }

    fun setTemperatureUnit(newUnit: TemperatureUnit) {
        tempUnit = newUnit
        viewModelScope.launch {
            dataStore.saveTemperatureUnit(newUnit)
        }
    }
}
