package com.csejudge.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "csejudge_theme_prefs")

/**
 * Repository interface for managing application theme preferences.
 */
interface ThemeRepository {
    val isDarkMode: Flow<Boolean>
    suspend fun setDarkMode(enabled: Boolean)
    suspend fun getInitialDarkMode(): Boolean
}

/**
 * DataStore-backed implementation of [ThemeRepository].
 * Defaulting to false (Light Mode) on fresh install.
 */
class ThemeRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : ThemeRepository {

    override val isDarkMode: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_DARK_MODE] ?: false
        }

    override suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_DARK_MODE] = enabled
        }
    }

    override suspend fun getInitialDarkMode(): Boolean {
        return try {
            isDarkMode.first()
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        val KEY_DARK_MODE = booleanPreferencesKey("key_dark_mode_enabled")

        fun create(context: Context): ThemeRepository {
            return ThemeRepositoryImpl(context.themeDataStore)
        }
    }
}
