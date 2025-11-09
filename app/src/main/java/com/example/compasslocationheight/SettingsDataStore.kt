package com.example.compasslocationheight

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    private val THEME_KEY = stringPreferencesKey("theme_mode")
    private val TEMP_UNIT_KEY = stringPreferencesKey("temp_unit")
    // NEU: Eindeutiger Schlüssel für die Sprache
    private val LANGUAGE_KEY = stringPreferencesKey("language")

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
}