package com.kartik.mealtime.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kartik.mealtime.data.analytics.AnalyticsHelper
import com.kartik.mealtime.domain.model.DietaryFilter
import com.kartik.mealtime.domain.model.Recipe
import com.kartik.mealtime.domain.model.RecipeCategory
import com.kartik.mealtime.domain.usecase.GetCategoriesUseCase
import com.kartik.mealtime.domain.usecase.GetRecipesByCategoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Owns category-related state extracted from MainViewModel:
 *  - The list of categories shown on Home + the Categories tab
 *  - The per-category recipe list (with in-memory LRU cache + dietary-filter memory)
 *  - The currently selected category (a soft selection for nav callers)
 *
 * Shared across HomeScreen / RecipeScreen / CategoryDetailScreen so a refresh on one
 * surface is visible on the others (and the cache survives detail → back navigation).
 * Wired at Navigation composable scope so all three screens get the same instance.
 */
@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val getCategories: GetCategoriesUseCase,
    private val getByCategoryUseCase: GetRecipesByCategoryUseCase,
    private val analytics: AnalyticsHelper,
) : ViewModel() {

    data class RecipeCategoryState(
        val loading: Boolean = true,
        val categories: ImmutableList<RecipeCategory> = persistentListOf(),
        val error: String? = null
    )

    data class CategoryRecipesState(
        val loading: Boolean = false,
        val recipes: ImmutableList<Recipe> = persistentListOf(),
        val error: String? = null,
        val totalLoaded: Int = 0,
        val hasMore: Boolean = false,
        val isLoadingMore: Boolean = false
    )

    private val _recipeCategoriesState = MutableStateFlow(RecipeCategoryState())
    val recipeCategoriesState: StateFlow<RecipeCategoryState> = _recipeCategoriesState.asStateFlow()

    private val _categoryRecipesState = MutableStateFlow(CategoryRecipesState())
    val categoryRecipesState: StateFlow<CategoryRecipesState> = _categoryRecipesState.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId.asStateFlow()

    fun selectCategory(id: String?) {
        _selectedCategoryId.value = id
    }

    // Max 20 entries, entries expire after 24h. accessOrder=true makes this a real LRU:
    // reading via get() moves the entry to the most-recently-used end, so eviction picks
    // the entry whose last *access* (not insertion) was oldest.
    private val categoryCache = object : LinkedHashMap<String, Pair<List<Recipe>, Long>>(
        16, 0.75f, /* accessOrder = */ true
    ) {
        override fun removeEldestEntry(eldest: Map.Entry<String, Pair<List<Recipe>, Long>>): Boolean =
            size > CATEGORY_CACHE_MAX_SIZE
    }

    // Remembers the user's per-category dietary filter so it's restored on re-visit.
    private val categoryFilters = mutableMapOf<String, DietaryFilter>()

    fun getCategoryFilter(categoryId: String): DietaryFilter =
        categoryFilters[categoryId] ?: DietaryFilter.ALL

    fun setCategoryFilter(categoryId: String, filter: DietaryFilter) {
        categoryFilters[categoryId] = filter
    }

    init {
        loadCategories()
    }

    fun refreshRecipeCategories() = loadCategories()

    /** Force-refresh category recipes — bypasses the cache TTL (used by pull-to-refresh). */
    fun refreshCategoryRecipes(categoryId: String) {
        categoryCache.remove(categoryId)
        getRecipesByCategory(categoryId)
    }

    fun getRecipesByCategory(categoryId: String) {
        val cached = categoryCache[categoryId]
        if (cached != null && (System.currentTimeMillis() - cached.second) < CACHE_TTL_MS) {
            val cachedRecipes = cached.first.toImmutableList()
            _categoryRecipesState.value = CategoryRecipesState(
                loading = false,
                recipes = cachedRecipes,
                totalLoaded = cachedRecipes.size,
                hasMore = false
            )
            return
        }
        viewModelScope.launch {
            _categoryRecipesState.value =
                _categoryRecipesState.value.copy(loading = true, error = null)
            getByCategoryUseCase(categoryId).fold(
                onSuccess = { newRecipes ->
                    categoryCache[categoryId] = Pair(newRecipes, System.currentTimeMillis())
                    val categoryName = _recipeCategoriesState.value.categories
                        .find { it.id == categoryId }?.name ?: categoryId
                    analytics.logCategoryViewed(categoryId, categoryName)
                    _categoryRecipesState.value = CategoryRecipesState(
                        loading = false,
                        recipes = newRecipes.toImmutableList(),
                        totalLoaded = newRecipes.size,
                        hasMore = newRecipes.size >= 20
                    )
                },
                onFailure = {
                    Log.e(TAG, "getRecipesByCategory: ${it.message}")
                    _categoryRecipesState.value = CategoryRecipesState(
                        loading = false,
                        error = "API temporarily unavailable. Please try again later."
                    )
                }
            )
        }
    }

    fun loadMoreCategoryRecipes(categoryId: String) {
        val current = _categoryRecipesState.value
        if (current.loading || current.isLoadingMore || !current.hasMore) return

        _categoryRecipesState.value = current.copy(isLoadingMore = true, error = null)

        viewModelScope.launch {
            val offset = current.totalLoaded
            getByCategoryUseCase(categoryId, offset = offset, append = true).fold(
                onSuccess = { newRecipes ->
                    val merged = (current.recipes + newRecipes).toImmutableList()
                    val updated = current.copy(
                        isLoadingMore = false,
                        recipes = merged,
                        totalLoaded = current.totalLoaded + newRecipes.size,
                        hasMore = newRecipes.size >= 20
                    )
                    _categoryRecipesState.value = updated
                    categoryCache[categoryId] = Pair(merged, System.currentTimeMillis())
                },
                onFailure = {
                    Log.w(TAG, "loadMoreCategoryRecipes: ${it.message}")
                    _categoryRecipesState.value = current.copy(
                        isLoadingMore = false,
                        error = "Failed to load more recipes"
                    )
                }
            )
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            _recipeCategoriesState.value =
                _recipeCategoriesState.value.copy(loading = true, error = null)
            getCategories().fold(
                onSuccess = {
                    _recipeCategoriesState.value =
                        RecipeCategoryState(loading = false, categories = it.toImmutableList())
                    Log.d(TAG, "Loaded ${it.size} categories")
                },
                onFailure = {
                    Log.e(TAG, "loadCategories: ${it.message}")
                    _recipeCategoriesState.value =
                        RecipeCategoryState(loading = false, error = it.message)
                }
            )
        }
    }

    companion object {
        private const val TAG = "CategoryViewModel"
        private const val CACHE_TTL_MS = 24 * 60 * 60 * 1000L
        private const val CATEGORY_CACHE_MAX_SIZE = 20
    }
}
