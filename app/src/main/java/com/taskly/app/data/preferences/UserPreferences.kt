package com.taskly.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "taskly_preferences")

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

data class AppPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val notificationsEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val defaultSnoozeMinutes: Int = 10,
    val defaultPriority: String = "MEDIUM"
)

class UserPreferences(private val context: Context) {

    companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val KEY_VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val KEY_DEFAULT_SNOOZE = intPreferencesKey("default_snooze")
        val KEY_DEFAULT_PRIORITY = stringPreferencesKey("default_priority")
    }

    val preferencesFlow: Flow<AppPreferences> = context.dataStore.data.map { preferences ->
        val themeStr = preferences[KEY_THEME_MODE] ?: ThemeMode.SYSTEM.name
        val themeMode = try {
            ThemeMode.valueOf(themeStr)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }

        AppPreferences(
            themeMode = themeMode,
            notificationsEnabled = preferences[KEY_NOTIFICATIONS_ENABLED] ?: true,
            vibrationEnabled = preferences[KEY_VIBRATION_ENABLED] ?: true,
            defaultSnoozeMinutes = preferences[KEY_DEFAULT_SNOOZE] ?: 10,
            defaultPriority = preferences[KEY_DEFAULT_PRIORITY] ?: "MEDIUM"
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_VIBRATION_ENABLED] = enabled }
    }

    suspend fun setDefaultSnooze(minutes: Int) {
        context.dataStore.edit { it[KEY_DEFAULT_SNOOZE] = minutes }
    }

    suspend fun setDefaultPriority(priority: String) {
        context.dataStore.edit { it[KEY_DEFAULT_PRIORITY] = priority }
    }
}

