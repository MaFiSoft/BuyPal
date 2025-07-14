// app/src/main/java/com/MaFiSoft/BuyPal/data/preferences/ThemeManager.kt
// Stand: 2025-07-09_12:28:00, Codezeilen: ~60 (Initialer ThemeManager mit DataStore)

package com.MaFiSoft.BuyPal.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// DataStore Instanz fuer die Theme-Einstellungen
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

/**
 * Verwaltet die Speicherung und das Abrufen des ausgewaehlten Themes.
 * Nutzt DataStore fuer die Persistenz.
 */
@Singleton
class ThemeManager @Inject constructor(@ApplicationContext private val context: Context) {

    // Key fuer die Speicherung des Theme-Namens
    private object PreferencesKeys {
        val SELECTED_THEME = stringPreferencesKey("selected_theme")
    }

    /**
     * Gibt den aktuell gespeicherten Theme-Namen als Flow zurueck.
     * Standardwert ist "Blau", falls noch kein Theme gespeichert wurde.
     */
    val selectedTheme: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.SELECTED_THEME] ?: "Blau" // Standard-Theme ist Blau
        }

    /**
     * Speichert den ausgewaehlten Theme-Namen.
     */
    suspend fun saveSelectedTheme(themeName: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_THEME] = themeName
        }
    }
}
