package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "webnest_settings")

enum class ThemeMode(val value: Int) {
    SYSTEM(0),
    LIGHT(1),
    DARK(2);

    companion object {
        fun fromInt(value: Int): ThemeMode = entries.firstOrNull { it.value == value } ?: SYSTEM
    }
}

enum class BookmarkLayoutMode(val value: Int) {
    ADAPTIVE_GRID(0),
    COMPACT_LIST(1);

    companion object {
        fun fromInt(value: Int): BookmarkLayoutMode =
            entries.firstOrNull { it.value == value } ?: ADAPTIVE_GRID
    }
}

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val layoutMode: BookmarkLayoutMode = BookmarkLayoutMode.ADAPTIVE_GRID,
    val preferChrome: Boolean = true
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = intPreferencesKey("theme_mode")
        val LAYOUT_MODE = intPreferencesKey("layout_mode")
        val PREFER_CHROME = booleanPreferencesKey("prefer_chrome")
    }

    val userSettingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            themeMode = ThemeMode.fromInt(prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.value),
            layoutMode = BookmarkLayoutMode.fromInt(
                prefs[Keys.LAYOUT_MODE] ?: BookmarkLayoutMode.ADAPTIVE_GRID.value
            ),
            preferChrome = prefs[Keys.PREFER_CHROME] ?: true
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.value
        }
    }

    suspend fun setLayoutMode(mode: BookmarkLayoutMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAYOUT_MODE] = mode.value
        }
    }

    suspend fun setPreferChrome(prefer: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PREFER_CHROME] = prefer
        }
    }
}
