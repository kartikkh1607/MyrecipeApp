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
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
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
    val recentRecipes: StateFlow<ImmutableList<RecentRecipe>> = recentRecipesRepo.recents
        .map { it.toImmutableList() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = persistentListOf()
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
     * instead of many (no UI flicker through partially-updated state). The sheet passes
     * a copy of the current prefs with the edited fields applied; stat fields on it are
     * ignored by the repository.
     */
    fun saveProfile(edited: UserPreferences) {
        viewModelScope.launch {
            userPrefsRepo.saveProfile(edited)
        }
    }
}
