package com.example.compasslocationheight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val dataStore: SettingsDataStore) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = dataStore.themeModeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ThemeMode.Dark
    )

    val tempUnit: StateFlow<TemperatureUnit> = dataStore.tempUnitFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TemperatureUnit.Celsius
    )

    val language: StateFlow<String> = dataStore.languageFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "system"
    )

    val coordinateFormat: StateFlow<CoordinateFormat> = dataStore.coordinateFormatFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CoordinateFormat.Decimal
    )

    fun setTheme(newTheme: ThemeMode) {
        viewModelScope.launch {
            dataStore.saveThemeMode(newTheme)
        }
    }

    fun setTemperatureUnit(newUnit: TemperatureUnit) {
        viewModelScope.launch {
            dataStore.saveTemperatureUnit(newUnit)
        }
    }

    fun setLanguage(newLanguage: String) {
        viewModelScope.launch {
            dataStore.saveLanguage(newLanguage)
        }
    }

    fun setCoordinateFormat(newFormat: CoordinateFormat) {
        viewModelScope.launch {
            dataStore.saveCoordinateFormat(newFormat)
        }
    }
}
