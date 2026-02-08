package com.example.compasslocationheight

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    private val THEME_KEY = stringPreferencesKey("theme_mode")
    private val TEMP_UNIT_KEY = stringPreferencesKey("temp_unit")
    private val COORD_FORMAT_KEY = stringPreferencesKey("coord_format")
    private val LANGUAGE_KEY = stringPreferencesKey("language")
    private val CONSENT_KEY = booleanPreferencesKey("country_consent")

    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data
        .map { preferences ->
            val themeName = preferences[THEME_KEY] ?: ThemeMode.Dark.name
            ThemeMode.valueOf(themeName)
        }

    val tempUnitFlow: Flow<TemperatureUnit> = context.dataStore.data
        .map { preferences ->
            val unitName = preferences[TEMP_UNIT_KEY] ?: TemperatureUnit.Celsius.name
            TemperatureUnit.valueOf(unitName)
        }

    val coordFormatFlow: Flow<CoordinateFormat> = context.dataStore.data
        .map { preferences ->
            val formatName = preferences[COORD_FORMAT_KEY] ?: CoordinateFormat.Decimal.name
            CoordinateFormat.valueOf(formatName)
        }

    val languageFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[LANGUAGE_KEY] ?: "system"
        }

    val consentFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[CONSENT_KEY] ?: false
        }
    
    val hasSeenConsentFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences.contains(CONSENT_KEY)
        }

    suspend fun saveThemeMode(themeMode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = themeMode.name
        }
    }

    suspend fun saveTemperatureUnit(unit: TemperatureUnit) {
        context.dataStore.edit { preferences ->
            preferences[TEMP_UNIT_KEY] = unit.name
        }
    }

    suspend fun saveCoordinateFormat(format: CoordinateFormat) {
        context.dataStore.edit { preferences ->
            preferences[COORD_FORMAT_KEY] = format.name
        }
    }

    suspend fun saveLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = language
        }
    }

    suspend fun saveConsent(agreed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[CONSENT_KEY] = agreed
        }
    }
}