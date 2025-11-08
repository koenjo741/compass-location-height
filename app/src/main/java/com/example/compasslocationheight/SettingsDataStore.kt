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

    // Schlüssel für das Theme (unverändert)
    private val THEME_KEY = stringPreferencesKey("theme_mode")
    // NEU: Eindeutiger Schlüssel für die Temperatureinheit
    private val TEMP_UNIT_KEY = stringPreferencesKey("temp_unit")

    // Flow für das Theme (unverändert)
    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data
        .map { preferences ->
            val themeName = preferences[THEME_KEY] ?: ThemeMode.Dark.name
            ThemeMode.valueOf(themeName)
        }

    // NEU: Flow zum Laden der Temperatureinheit
    val tempUnitFlow: Flow<TemperatureUnit> = context.dataStore.data
        .map { preferences ->
            // Lese den Wert. Wenn nichts da ist, nimm "Celsius" als Standard.
            val unitName = preferences[TEMP_UNIT_KEY] ?: TemperatureUnit.Celsius.name
            TemperatureUnit.valueOf(unitName)
        }

    // Speicherfunktion für das Theme (unverändert)
    suspend fun saveThemeMode(themeMode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = themeMode.name
        }
    }

    // NEU: Speicherfunktion für die Temperatureinheit
    suspend fun saveTemperatureUnit(unit: TemperatureUnit) {
        context.dataStore.edit { preferences ->
            preferences[TEMP_UNIT_KEY] = unit.name
        }
    }
}