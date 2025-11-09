package com.example.compasslocationheight

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

enum class TemperatureUnit {
    Celsius, Fahrenheit
}

class SettingsViewModel(private val dataStore: SettingsDataStore) : ViewModel() {

    // State for Theme
    private val _themeMode = mutableStateOf(ThemeMode.Dark)
    val themeMode: State<ThemeMode> = _themeMode

    // State for Temperature Unit
    private val _tempUnit = mutableStateOf(TemperatureUnit.Celsius)
    val tempUnit: State<TemperatureUnit> = _tempUnit

    // State for Language
    private val _language = mutableStateOf("system")
    val language: State<String> = _language

    init {
        viewModelScope.launch {
            dataStore.themeModeFlow.collect { _themeMode.value = it }
        }
        viewModelScope.launch {
            dataStore.tempUnitFlow.collect { _tempUnit.value = it }
        }
        viewModelScope.launch {
            dataStore.languageFlow.collect { _language.value = it }
        }
    }

    fun setTheme(newTheme: ThemeMode) {
        _themeMode.value = newTheme
        viewModelScope.launch {
            dataStore.saveThemeMode(newTheme)
        }
    }

    fun setTemperatureUnit(newUnit: TemperatureUnit) {
        _tempUnit.value = newUnit
        viewModelScope.launch {
            dataStore.saveTemperatureUnit(newUnit)
        }
    }

    fun setLanguage(newLanguage: String) {
        _language.value = newLanguage
        viewModelScope.launch {
            dataStore.saveLanguage(newLanguage)
        }
    }
}