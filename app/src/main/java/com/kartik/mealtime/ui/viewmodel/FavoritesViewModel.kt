package com.kartik.mealtime.ui.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kartik.mealtime.data.repository.FavoritesRepository
import com.kartik.mealtime.domain.model.Recipe
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val favoritesRepository: FavoritesRepository
) : ViewModel() {

    private val _favoriteRecipes = mutableStateOf<List<Recipe>>(emptyList())
    val favoriteRecipes: State<List<Recipe>> = _favoriteRecipes

    val favoriteIds: State<Set<String>> = derivedStateOf {
        _favoriteRecipes.value.mapTo(mutableSetOf()) { it.id }
    }

    /** Persisted grid/list toggle for the Favorites screen. */
    private val _favoritesGridMode = mutableStateOf(false)
    val favoritesGridMode: State<Boolean> = _favoritesGridMode

    fun toggleFavoritesGridMode() {
        _favoritesGridMode.value = !_favoritesGridMode.value
    }

    enum class FavoritesSortOrder(val label: String) {
        RECENTLY_ADDED("Recent"),
        NAME_AZ("A → Z"),
        NAME_ZA("Z → A"),
        RATING("Top Rated"),
        COOK_TIME("Quickest"),
        DIFFICULTY("Easiest")
    }

    private val _favoritesSortOrder = mutableStateOf(FavoritesSortOrder.RECENTLY_ADDED)
    val favoritesSortOrder: State<FavoritesSortOrder> = _favoritesSortOrder

    val sortedFavoriteRecipes: State<List<Recipe>> = derivedStateOf {
        val recipes = _favoriteRecipes.value
        when (_favoritesSortOrder.value) {
            FavoritesSortOrder.RECENTLY_ADDED -> recipes
            FavoritesSortOrder.NAME_AZ -> recipes.sortedBy { it.name.lowercase() }
            FavoritesSortOrder.NAME_ZA -> recipes.sortedByDescending { it.name.lowercase() }
            FavoritesSortOrder.RATING -> recipes.sortedByDescending { it.rating }
            FavoritesSortOrder.COOK_TIME -> recipes.sortedBy { it.prepTime + it.cookTime }
            FavoritesSortOrder.DIFFICULTY -> recipes.sortedBy { it.difficulty.ordinal }
        }
    }

    fun setFavoritesSortOrder(order: FavoritesSortOrder) {
        _favoritesSortOrder.value = order
    }

    init {
        viewModelScope.launch {
            favoritesRepository.favorites.collect { _favoriteRecipes.value = it }
        }
    }

    fun toggleFavorite(recipe: Recipe) {
        val isFav = _favoriteRecipes.value.any { it.id == recipe.id }
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

