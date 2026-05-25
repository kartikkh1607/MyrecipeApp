package com.kartik.mealtime.data.repository

import com.kartik.mealtime.data.analytics.AnalyticsHelper
import com.kartik.mealtime.data.local.CachedRecipeDao
import com.kartik.mealtime.data.local.FavoriteDao
import com.kartik.mealtime.data.local.toCachedEntity
import com.kartik.mealtime.data.local.toFavoriteEntity
import com.kartik.mealtime.data.local.toRecipe
import com.kartik.mealtime.domain.model.Recipe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for favorites (extracted from MainViewModel).
 *
 * Favorites are app-wide shared state — every recipe card on every screen has a
 * toggle. Rather than thread one ViewModel through the whole nav tree, this Room-
 * backed repository is a @Singleton, so any number of small per-screen
 * [com.kartik.mealtime.ui.viewmodel.FavoritesViewModel] instances observing
 * [favorites] stay perfectly in sync.
 */
@Singleton
class FavoritesRepository @Inject constructor(
    private val favoriteDao: FavoriteDao,
    private val cachedRecipeDao: CachedRecipeDao,
    private val analytics: AnalyticsHelper,
    private val userRepository: UserRepository,
    private val syncRepository: SyncRepository
) {
    /** All favorited recipes as a Flow — the single source of truth. */
    val favorites: Flow<List<Recipe>> =
        favoriteDao.getAllFlow().map { entities -> entities.map { it.toRecipe() } }

    /** Adds or removes [recipe] depending on [isCurrentlyFavorite] (caller knows the current state). */
    suspend fun toggleFavorite(recipe: Recipe, isCurrentlyFavorite: Boolean) {
        val uid = userRepository.currentUser?.uid
        if (isCurrentlyFavorite) {
            favoriteDao.delete(recipe.id)
            uid?.let { runCatching { syncRepository.deleteFavorite(it, recipe.id) } }
        } else {
            val entity = recipe.toFavoriteEntity()
            favoriteDao.insert(entity)
            cachedRecipeDao.upsert(recipe.toCachedEntity())
            analytics.logRecipeFavorited(recipe.id, recipe.name)
            uid?.let { runCatching { syncRepository.uploadFavorite(it, entity) } }
        }
    }

    suspend fun removeFavorite(recipeId: String) {
        favoriteDao.delete(recipeId)
        userRepository.currentUser?.uid?.let { uid ->
            runCatching { syncRepository.deleteFavorite(uid, recipeId) }
        }
    }

    /** Restores a previously removed favorite (used by undo-snackbar). */
    suspend fun addFavorite(recipe: Recipe) {
        // Do NOT upsert into cachedRecipeDao here — a recipe sourced from FavoriteEntity
        // has servings=0, no ingredients, and no instructions, so it would corrupt the
        // detail cache and make the servings stepper show wrong values.
        favoriteDao.insert(recipe.toFavoriteEntity())
    }
}
