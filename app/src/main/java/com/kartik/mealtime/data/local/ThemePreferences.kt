package com.kartik.mealtime.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kartik.mealtime.domain.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Top-level extension creates a single DataStore instance per Context.
private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_prefs")

/**
 * Thin wrapper around DataStore that persists the user's [ThemeMode] choice.
 *
 * - Reads: [themeMode] — a [Flow] that emits the current preference.
 * - Writes: [setThemeMode] — suspending function, safe to call from a coroutine.
 *
 * Defaults to [ThemeMode.SYSTEM] so first-run behavior is unchanged.
 */
@Singleton
class ThemePreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Emits the current [ThemeMode] whenever it changes.
     * Falls back to [ThemeMode.SYSTEM] if the key doesn't exist yet.
     */
    fun themeMode(): Flow<ThemeMode> =
        context.themeDataStore.data.map { prefs ->
            val ordinal = prefs[THEME_KEY] ?: ThemeMode.SYSTEM.ordinal
            ThemeMode.entries.getOrElse(ordinal) { ThemeMode.SYSTEM }
        }

    /** Persists [mode] to DataStore. */
    suspend fun setThemeMode(mode: ThemeMode) {
        context.themeDataStore.edit { prefs ->
            prefs[THEME_KEY] = mode.ordinal
        }
    }

    private companion object {
        val THEME_KEY = intPreferencesKey("theme_mode_ordinal")
    }
}
