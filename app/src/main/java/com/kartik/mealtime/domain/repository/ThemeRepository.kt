package com.kartik.mealtime.domain.repository

import com.kartik.mealtime.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * Persists the user's theme preference.
 *
 * Lives in the domain layer so [com.kartik.mealtime.ui.viewmodel.MainViewModel]
 * can depend on this abstraction instead of the DataStore-backed
 * [com.kartik.mealtime.data.local.ThemePreferences] directly — letting unit tests
 * inject a pure-Kotlin fake and skip Robolectric.
 */
interface ThemeRepository {
    /** Emits the current theme; defaults to [ThemeMode.SYSTEM] when unset. */
    fun themeMode(): Flow<ThemeMode>

    /** Persists [mode] so it survives process death. */
    suspend fun setThemeMode(mode: ThemeMode)
}
