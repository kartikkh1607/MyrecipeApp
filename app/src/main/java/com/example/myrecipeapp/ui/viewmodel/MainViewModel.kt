package com.example.myrecipeapp.ui.viewmodel

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myrecipeapp.data.local.FavoriteDao
import com.example.myrecipeapp.data.local.ShoppingDao
import com.example.myrecipeapp.data.local.ShoppingItemEntity
import com.example.myrecipeapp.data.local.toFavoriteEntity
import com.example.myrecipeapp.data.local.toRecipe
import com.example.myrecipeapp.data.local.toShoppingListItem
import com.example.myrecipeapp.di.AppContainer
import com.example.myrecipeapp.domain.model.FeaturedRecipe
import com.example.myrecipeapp.domain.model.Recipe
import com.example.myrecipeapp.domain.model.RecipeCategory
import com.example.myrecipeapp.domain.usecase.GetCategoriesUseCase
import com.example.myrecipeapp.domain.usecase.GetFeaturedRecipesUseCase
import com.example.myrecipeapp.domain.usecase.GetRecipeDetailsUseCase
import com.example.myrecipeapp.domain.usecase.GetRecipesByCategoryUseCase
import com.example.myrecipeapp.domain.usecase.SearchRecipesUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Presentation-layer ViewModel.
 *
 * Responsibilities:
 *  - Hold and expose UI state
 *  - Delegate ALL data operations to use cases (no Retrofit, no sample data, no mapping here)
 *  - React to UI events (search query, refresh, category selection, etc.)
 */
class MainViewModel(
    private val getFeaturedRecipes: GetFeaturedRecipesUseCase,
    private val searchUseCase: SearchRecipesUseCase,
    private val getRecipeDetails: GetRecipeDetailsUseCase,
    private val getCategories: GetCategoriesUseCase,
    private val getByCategoryUseCase: GetRecipesByCategoryUseCase,
    private val favoriteDao: FavoriteDao,
    private val shoppingDao: ShoppingDao
) : ViewModel() {

    // ── Home Screen State ─────────────────────────────────────────────────────
    data class HomeRecipeState(
        val loading: Boolean = true,
        val featuredRecipes: List<FeaturedRecipe> = emptyList(),
        val error: String? = null
    )

    private val _homeRecipeState = mutableStateOf(HomeRecipeState())
    val homeRecipeState: State<HomeRecipeState> = _homeRecipeState

    // ── Recipe Detail State ───────────────────────────────────────────────────
    data class RecipeDetailState(
        val loading: Boolean = true,
        val recipe: Recipe? = null,
        val error: String? = null
    )

    private val _recipeDetailState = mutableStateOf(RecipeDetailState())
    val recipeDetailState: State<RecipeDetailState> = _recipeDetailState

    // ── Categories State ──────────────────────────────────────────────────────
    data class RecipeCategoryState(
        val loading: Boolean = true,
        val categories: List<RecipeCategory> = emptyList(),
        val error: String? = null
    )

    private val _recipeCategoriesState = mutableStateOf(RecipeCategoryState())
    val recipeCategoriesState: State<RecipeCategoryState> = _recipeCategoriesState

    // ── Search State ──────────────────────────────────────────────────────────
    data class SearchState(
        val loading: Boolean = false,
        val recipes: List<Recipe> = emptyList(),
        val error: String? = null,
        val query: String = ""
    )

    private val _searchState = mutableStateOf(SearchState())
    val searchState: State<SearchState> = _searchState

    // ── Category Recipes State ────────────────────────────────────────────────
    data class CategoryRecipesState(
        val loading: Boolean = false,
        val recipes: List<Recipe> = emptyList(),
        val error: String? = null
        // categoryId removed: it was stored but never consumed by the UI
    )

    // ── Favorites State (Room-backed) ─────────────────────────────────────────
    private val _favoriteIds = mutableStateOf<Set<String>>(emptySet())
    val favoriteIds: State<Set<String>> = _favoriteIds

    private val _favoriteRecipes = mutableStateOf<List<Recipe>>(emptyList())
    val favoriteRecipes: State<List<Recipe>> = _favoriteRecipes

    fun toggleFavorite(recipe: Recipe) {
        viewModelScope.launch {
            if (recipe.id in _favoriteIds.value) {
                favoriteDao.delete(recipe.id)
            } else {
                favoriteDao.insert(recipe.toFavoriteEntity())
            }
        }
    }

    // ── Shopping List State (Room-backed) ─────────────────────────────────────
    data class ShoppingListItem(
        val key: String,
        val ingredientName: String,
        val amount: String,
        val unit: String,
        val recipeName: String,
        val isChecked: Boolean = false
    )

    private val _shoppingList = mutableStateOf<List<ShoppingListItem>>(emptyList())
    val shoppingList: State<List<ShoppingListItem>> = _shoppingList

    /** Adds all ingredients from [recipe], skipping duplicates (IGNORE conflict strategy). */
    fun addToShoppingList(recipe: Recipe) {
        viewModelScope.launch {
            recipe.ingredients.forEach { ing ->
                val key = "${recipe.id}_${ing.id.ifEmpty { ing.name }}"
                shoppingDao.insert(
                    ShoppingItemEntity(
                        key            = key,
                        ingredientName = ing.name,
                        amount         = ing.amount,
                        unit           = ing.unit,
                        recipeName     = recipe.name
                    )
                )
            }
        }
    }

    fun toggleShoppingItem(key: String) {
        viewModelScope.launch {
            val current = _shoppingList.value.find { it.key == key } ?: return@launch
            shoppingDao.setChecked(key, !current.isChecked)
        }
    }

    fun removeCheckedItems() {
        viewModelScope.launch { shoppingDao.deleteChecked() }
    }

    fun removeItem(key: String) {
        viewModelScope.launch { shoppingDao.deleteByKey(key) }
    }

    fun clearShoppingList() {
        viewModelScope.launch { shoppingDao.deleteAll() }
    }


    // ── Category Recipes State ────────────────────────────────────────────────
    private val _categoryRecipesState = mutableStateOf(CategoryRecipesState())
    val categoryRecipesState: State<CategoryRecipesState> = _categoryRecipesState

    // ── Category cache ────────────────────────────────────────────────────────
    private val categoryCache = mutableMapOf<String, List<Recipe>>()

    // ─────────────────────────────────────────────────────────────────────────
    init {
        // Collect Room Flows → update mutableStateOf so all existing UI code works unchanged
        viewModelScope.launch {
            favoriteDao.getAllFlow().collect { entities ->
                _favoriteRecipes.value = entities.map { it.toRecipe() }
            }
        }
        viewModelScope.launch {
            favoriteDao.getIdsFlow().collect { ids ->
                _favoriteIds.value = ids.toSet()
            }
        }
        viewModelScope.launch {
            shoppingDao.getAllFlow().collect { entities ->
                _shoppingList.value = entities.map { it.toShoppingListItem() }
            }
        }
        loadFeaturedRecipes()
        loadCategories()
    }

    // ── Public API ────────────────────────────────────────────────────────────

    fun refreshFeaturedRecipes() = loadFeaturedRecipes()
    fun refreshRecipeCategories() = loadCategories()

    fun fetchRecipeDetails(recipeId: String) {
        // Skip re-fetch if this recipe is already loaded
        if (_recipeDetailState.value.recipe?.id == recipeId) return
        launchOp(
            onStart = {
                _recipeDetailState.value =
                    _recipeDetailState.value.copy(loading = true, error = null)
            },
            call = { getRecipeDetails(recipeId) },
            onSuccess = {
                _recipeDetailState.value = RecipeDetailState(loading = false, recipe = it)
            },
            onFailure = {
                Log.w(TAG, "fetchRecipeDetails: ${it.message}")
                _recipeDetailState.value = RecipeDetailState(loading = false, error = it.message)
            }
        )
    }

    // Job reference so we can cancel a stale search when a new one starts
    private var searchJob: Job? = null

    fun searchRecipes(query: String) {
        searchJob?.cancel()   // cancel previous — prevents stale results overwriting fresh ones
        searchJob = launchOp(
            onStart = {
                _searchState.value =
                    _searchState.value.copy(loading = true, error = null, query = query)
            },
            call = { searchUseCase(query) },
            onSuccess = {
                _searchState.value =
                    SearchState(loading = false, recipes = it.recipes, query = query)
            },
            onFailure = {
                Log.w(TAG, "searchRecipes: ${it.message}")
                _searchState.value =
                    _searchState.value.copy(loading = false, error = "Failed to search recipes")
            }
        )
    }

    fun getRecipesByCategory(categoryId: String) {
        // Serve from cache instantly — avoids re-fetching when the user navigates back
        val cached = categoryCache[categoryId]
        if (cached != null) {
            _categoryRecipesState.value = CategoryRecipesState(loading = false, recipes = cached)
            return
        }
        launchOp(
            onStart = {
                _categoryRecipesState.value =
                    _categoryRecipesState.value.copy(loading = true, error = null)
            },
            call = { getByCategoryUseCase(categoryId) },
            onSuccess = {
                categoryCache[categoryId] = it   // store for instant next access
                _categoryRecipesState.value = CategoryRecipesState(loading = false, recipes = it)
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

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun loadFeaturedRecipes() = launchOp(
        onStart = {
            _homeRecipeState.value = _homeRecipeState.value.copy(loading = true, error = null)
        },
        call = { getFeaturedRecipes() },
        onSuccess = {
            _homeRecipeState.value = HomeRecipeState(loading = false, featuredRecipes = it)
        },
        onFailure = {
            Log.w(TAG, "loadFeaturedRecipes: ${it.message}")
            _homeRecipeState.value = HomeRecipeState(loading = false, error = it.message)
        }
    )

    private fun loadCategories() = launchOp(
        onStart = {
            _recipeCategoriesState.value =
                _recipeCategoriesState.value.copy(loading = true, error = null)
        },
        call = { getCategories() },
        onSuccess = {
            _recipeCategoriesState.value = RecipeCategoryState(loading = false, categories = it)
            Log.d(TAG, "Loaded ${it.size} categories")
        },
        onFailure = {
            Log.e(TAG, "loadCategories: ${it.message}")
            _recipeCategoriesState.value = RecipeCategoryState(loading = false, error = it.message)
        }
    )

    /**
     * Generic coroutine launcher that follows the standard:
     * setLoading → call use case → handle success/failure.
     * Eliminates the repeated viewModelScope.launch boilerplate across all operations.
     */
    private fun <T> launchOp(
        onStart: () -> Unit,
        call: suspend () -> Result<T>,
        onSuccess: (T) -> Unit,
        onFailure: (Throwable) -> Unit
    ): Job = viewModelScope.launch {
        onStart()
        call().fold(onSuccess, onFailure)
    }

    companion object {
        private const val TAG = "MainViewModel"
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(
                getFeaturedRecipes    = AppContainer.getFeaturedRecipesUseCase,
                searchUseCase         = AppContainer.searchRecipesUseCase,
                getRecipeDetails      = AppContainer.getRecipeDetailsUseCase,
                getCategories         = AppContainer.getCategoriesUseCase,
                getByCategoryUseCase  = AppContainer.getRecipesByCategoryUseCase,
                favoriteDao           = AppContainer.favoriteDao,
                shoppingDao           = AppContainer.shoppingDao
            ) as T
        }
    }
}
