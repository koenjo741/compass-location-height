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

    val hasSeenConsent: StateFlow<Boolean> = dataStore.hasSeenConsentFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true // Default true prevents flickering if loading, will update quickly
    )

    val consentGiven: StateFlow<Boolean> = dataStore.consentFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
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

    fun setConsent(agreed: Boolean) {
        viewModelScope.launch {
            dataStore.saveConsent(agreed)
            // Hier würde man den Ländercode senden, wenn agreed == true
            if (agreed) {
                // z.B. logCountry(Locale.getDefault().country)
            }
        }
    }
}