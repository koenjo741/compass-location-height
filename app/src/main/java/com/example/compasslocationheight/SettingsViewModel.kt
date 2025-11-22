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

    val coordFormat: StateFlow<CoordinateFormat> = dataStore.coordFormatFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CoordinateFormat.Decimal
    )

    val language: StateFlow<String> = dataStore.languageFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "system"
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

    fun setCoordinateFormat(newFormat: CoordinateFormat) {
        viewModelScope.launch {
            dataStore.saveCoordinateFormat(newFormat)
        }
    }

    fun setLanguage(newLanguage: String) {
        viewModelScope.launch {
            dataStore.saveLanguage(newLanguage)
        }
    }
}