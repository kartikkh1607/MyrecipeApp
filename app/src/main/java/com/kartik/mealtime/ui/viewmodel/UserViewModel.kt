package com.kartik.mealtime.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kartik.mealtime.data.local.FavoriteDao
import com.kartik.mealtime.data.preferences.DietaryPref
import com.kartik.mealtime.data.preferences.RecentRecipe
import com.kartik.mealtime.data.preferences.RecentRecipesRepository
import com.kartik.mealtime.data.preferences.UserPreferences
import com.kartik.mealtime.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for personalization — the user's display name, avatar, dietary
 * preferences, and cooking stats.
 *
 * Cleanly separated from [MainViewModel] which already owns recipe state.
 * Both share the same singleton [UserPreferencesRepository], so any update
 * from this VM is immediately visible to HomeScreen's greeting, AI Chef's
 * prompt builder, and the Profile screen.
 */
@HiltViewModel
class UserViewModel @Inject constructor(
    private val userPrefsRepo: UserPreferencesRepository,
    recentRecipesRepo: RecentRecipesRepository,
    favoriteDao: FavoriteDao
) : ViewModel() {

    /** Current user preferences. Hot — survives screen rotations. */
    val preferences: StateFlow<UserPreferences> = userPrefsRepo.preferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserPreferences()
        )

    /** Recently-viewed recipes — drives the "Recently viewed" row on HomeScreen. */
    val recentRecipes: StateFlow<List<RecentRecipe>> = recentRecipesRepo.recents
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /** Live favorites count — used by Profile stats row. */
    val favoritesCount: StateFlow<Int> = favoriteDao.getAllFlow()
        .map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0
        )

    // ── Edit actions ─────────────────────────────────────────────────────────

    fun updateDisplayName(name: String) {
        viewModelScope.launch { userPrefsRepo.updateDisplayName(name) }
    }

    fun updateAvatar(emoji: String) {
        viewModelScope.launch { userPrefsRepo.updateAvatar(emoji) }
    }

    /** Toggles a single dietary pref in the user's selection set. */
    fun toggleDietaryPref(pref: DietaryPref) {
        val current = preferences.value.dietaryPrefs
        val updated = if (pref in current) current - pref else current + pref
        viewModelScope.launch { userPrefsRepo.updateDietaryPrefs(updated) }
    }

    /**
     * Atomic profile save — used by the EditProfileSheet's Save button. Delegates to a
     * single DataStore transaction in the repository, so observers see one emission
     * instead of three (no UI flicker through partially-updated state).
     */
    fun saveProfile(name: String, emoji: String, dietaryPrefs: Set<DietaryPref>) {
        viewModelScope.launch {
            userPrefsRepo.saveProfile(name, emoji, dietaryPrefs)
        }
    }
}
