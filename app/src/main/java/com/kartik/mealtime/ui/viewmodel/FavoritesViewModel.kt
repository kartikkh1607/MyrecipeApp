package com.kartik.mealtime.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kartik.mealtime.data.preferences.UserPreferencesRepository
import com.kartik.mealtime.data.repository.FavoritesRepository
import com.kartik.mealtime.domain.model.Recipe
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Thin per-screen ViewModel over [FavoritesRepository] (extracted from MainViewModel).
 *
 * Obtained via hiltViewModel() in each screen/component that shows favorite
 * toggles. Multiple instances are fine: favorite data is the repository's
 * Room-backed Flow, so every instance stays in sync. Only the grid/sort UI
 * prefs are instance-local (they matter solely on the Favorites screen).
 */
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoritesRepository: FavoritesRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    val favoriteRecipes: StateFlow<List<Recipe>> = favoritesRepository.favorites
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val favoriteIds: StateFlow<Set<String>> = favoriteRecipes
        .map { recipes -> recipes.mapTo(mutableSetOf()) { it.id } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    /**
     * Persisted grid/list toggle for the Favorites screen. Backed by DataStore so it
     * survives tab navigation (which pops this VM's NavBackStackEntry) and process
     * death — earlier the in-memory state reset to false every time the user left the
     * tab.
     */
    val favoritesGridMode: StateFlow<Boolean> = userPreferencesRepository.favoritesGridMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun toggleFavoritesGridMode() {
        viewModelScope.launch {
            userPreferencesRepository.setFavoritesGridMode(!favoritesGridMode.value)
        }
    }

    enum class FavoritesSortOrder(val label: String) {
        RECENTLY_ADDED("Recent"),
        NAME_AZ("A → Z"),
        NAME_ZA("Z → A"),
        RATING("Top Rated"),
        COOK_TIME("Quickest"),
        DIFFICULTY("Easiest")
    }

    private val _favoritesSortOrder = MutableStateFlow(FavoritesSortOrder.RECENTLY_ADDED)
    val favoritesSortOrder: StateFlow<FavoritesSortOrder> = _favoritesSortOrder.asStateFlow()

    val sortedFavoriteRecipes: StateFlow<List<Recipe>> =
        combine(favoriteRecipes, _favoritesSortOrder) { recipes, order ->
            when (order) {
                FavoritesSortOrder.RECENTLY_ADDED -> recipes
                FavoritesSortOrder.NAME_AZ -> recipes.sortedBy { it.name.lowercase() }
                FavoritesSortOrder.NAME_ZA -> recipes.sortedByDescending { it.name.lowercase() }
                FavoritesSortOrder.RATING -> recipes.sortedByDescending { it.rating }
                FavoritesSortOrder.COOK_TIME -> recipes.sortedBy { it.prepTime + it.cookTime }
                FavoritesSortOrder.DIFFICULTY -> recipes.sortedBy { it.difficulty.ordinal }
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setFavoritesSortOrder(order: FavoritesSortOrder) {
        _favoritesSortOrder.value = order
    }

    fun toggleFavorite(recipe: Recipe) {
        val isFav = favoriteRecipes.value.any { it.id == recipe.id }
        viewModelScope.launch { favoritesRepository.toggleFavorite(recipe, isFav) }
    }

    fun removeFavorite(recipeId: String) {
        viewModelScope.launch { favoritesRepository.removeFavorite(recipeId) }
    }

    /** Restores a previously removed favorite (used by undo-snackbar). */
    fun addFavorite(recipe: Recipe) {
        viewModelScope.launch { favoritesRepository.addFavorite(recipe) }
    }
}
