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
import com.example.myrecipeapp.data.local.ThemePreferences
import com.example.myrecipeapp.data.local.toCachedEntity
import com.example.myrecipeapp.data.local.toFavoriteEntity
import com.example.myrecipeapp.data.local.toRecipe
import com.example.myrecipeapp.data.local.toShoppingListItem
import com.example.myrecipeapp.di.AppContainer
import com.example.myrecipeapp.domain.model.FeaturedRecipe
import com.example.myrecipeapp.domain.model.Recipe
import com.example.myrecipeapp.domain.model.RecipeCategory
import com.example.myrecipeapp.domain.model.ShoppingListItem
import com.example.myrecipeapp.domain.model.ThemeMode
import com.example.myrecipeapp.domain.usecase.GetCategoriesUseCase
import com.example.myrecipeapp.domain.usecase.GetFeaturedRecipesUseCase
import com.example.myrecipeapp.domain.usecase.GetRecipeDetailsUseCase
import com.example.myrecipeapp.domain.usecase.GetRecipesByCategoryUseCase
import com.example.myrecipeapp.domain.usecase.SearchRecipesUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Presentation-layer ViewModel.
 *
 * Responsibilities:
 *  - Hold and expose UI state
 *  - Delegate ALL data operations to use cases (no Retrofit, no sample data, no mapping here)
 *  - React to UI events (search query, refresh, category selection, etc.)
 *
 * Issue #4 fix: exposes [themeMode] from DataStore and [setThemeMode] to update it.
 * Issue #6 fix: [searchRecipes] now delays 300 ms before firing so rapid keystrokes
 *               cancel the coroutine before any network call is made.
 */
class MainViewModel(
    private val getFeaturedRecipes: GetFeaturedRecipesUseCase,
    private val searchUseCase: SearchRecipesUseCase,
    private val getRecipeDetails: GetRecipeDetailsUseCase,
    private val getCategories: GetCategoriesUseCase,
    private val getByCategoryUseCase: GetRecipesByCategoryUseCase,
    private val favoriteDao: FavoriteDao,
    private val shoppingDao: ShoppingDao,
    private val cachedRecipeDao: com.example.myrecipeapp.data.local.CachedRecipeDao
) : ViewModel() {

    // ── Theme State (Issue #4) ────────────────────────────────────────────────

    /**
     * Emits the user's persisted theme choice from DataStore.
     * Defaults to [ThemeMode.SYSTEM] until DataStore emits its first value.
     */
    private val appContext = com.example.myrecipeapp.MyRecipeApplication.instance

    /**
     * Emits the user's persisted theme choice.
     * Collected from DataStore via [ThemePreferences]; defaults to [ThemeMode.SYSTEM].
     */
    val themeMode: StateFlow<ThemeMode> = ThemePreferences
        .themeMode(appContext)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ThemeMode.SYSTEM
        )

    /** Persists the chosen [mode] to DataStore. Change is immediately reflected in [themeMode]. */
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            ThemePreferences.setThemeMode(context = appContext, mode = mode)
        }
    }

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
        val loadingMore: Boolean = false,
        val recipes: List<Recipe> = emptyList(),
        val totalResults: Int = 0,
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
    )

    // ── Favorites State (Room-backed) ─────────────────────────────────────────
    private val _favoriteIds = mutableStateOf<Set<String>>(emptySet())
    val favoriteIds: State<Set<String>> = _favoriteIds

    private val _favoriteRecipes = mutableStateOf<List<Recipe>>(emptyList())
    val favoriteRecipes: State<List<Recipe>> = _favoriteRecipes

    /** Persisted grid/list toggle — survives navigation (lives in ViewModel, not screen). */
    private val _favoritesGridMode = mutableStateOf(false)
    val favoritesGridMode: State<Boolean> = _favoritesGridMode

    fun toggleFavoritesGridMode() {
        _favoritesGridMode.value = !_favoritesGridMode.value
    }

    fun removeFavorite(recipeId: String) {
        viewModelScope.launch { favoriteDao.delete(recipeId) }
    }

    fun toggleFavorite(recipe: Recipe) {
        viewModelScope.launch {
            if (recipe.id in _favoriteIds.value) {
                favoriteDao.delete(recipe.id)
            } else {
                favoriteDao.insert(recipe.toFavoriteEntity())
                // Also cache the full recipe so it's available offline from Favorites
                cachedRecipeDao.upsert(recipe.toCachedEntity())
            }
        }
    }

    private val _shoppingList = mutableStateOf<List<ShoppingListItem>>(emptyList())
    val shoppingList: State<List<ShoppingListItem>> = _shoppingList

    /** The last recipe name passed to [addToShoppingList]. Used by ShoppingListScreen to auto-focus that section. */
    private val _lastAddedRecipeName = mutableStateOf<String?>(null)
    val lastAddedRecipeName: State<String?> = _lastAddedRecipeName

    /** Replaces the shopping list with [recipe]'s ingredients.
     *  Previous items are cleared first so the user always sees exactly
     *  what they just selected — no accumulation of old recipes. */
    fun addToShoppingList(recipe: Recipe) {
        _lastAddedRecipeName.value = recipe.name
        viewModelScope.launch {
            shoppingDao.deleteAll()   // clear previous recipe's items first
            recipe.ingredients.forEach { ing ->
                val key = "${recipe.id}_${ing.id.ifEmpty { ing.name }}"
                shoppingDao.insert(
                    ShoppingItemEntity(
                        key = key,
                        ingredientName = ing.name,
                        amount = ing.amount,
                        unit = ing.unit,
                        recipeName = recipe.name
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

    // ── Selected category (hoisted from HomeScreen so it survives navigation) ──
    private val _selectedCategoryId = mutableStateOf<String?>(null)
    val selectedCategoryId: State<String?> = _selectedCategoryId

    fun selectCategory(id: String?) {
        _selectedCategoryId.value = id
    }

    // ── Category cache — keyed by categoryId, value = (recipes, cachedAt millis) ─
    // Max 20 entries, entries expire after 24 h so stale data doesn't persist all day.
    private val categoryCache = mutableMapOf<String, Pair<List<Recipe>, Long>>()
    private val CACHE_TTL_MS = 24 * 60 * 60 * 1000L  // 24 hours
    private val CACHE_MAX_SIZE = 20

    // ─────────────────────────────────────────────────────────────────────────
    init {
        // Single Room query feeds both favorites state objects — no duplicate DB round-trip
        viewModelScope.launch {
            favoriteDao.getAllFlow().collect { entities ->
                _favoriteRecipes.value = entities.map { it.toRecipe() }
                _favoriteIds.value = entities.map { it.id }.toSet()
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
        // Skip re-fetch only when the EXACT same recipe is already fully loaded.
        // For any other ID, reset state immediately so the old recipe never flashes
        // on screen while the new one is loading (the core stale-recipe bug).
        if (_recipeDetailState.value.recipe?.id == recipeId &&
            !_recipeDetailState.value.loading
        ) return

        // Clear previous recipe right away — UI shows a spinner, not stale content
        _recipeDetailState.value = RecipeDetailState(loading = true, recipe = null, error = null)

        launchOp(
            onStart = { /* state already set above */ },
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

    /**
     * Issue #6 fix: 300 ms debounce added before the API call.
     * If the user types another character within 300 ms, the coroutine is cancelled
     * and the previous call never reaches the network layer.
     */
    fun searchRecipes(query: String) {
        searchJob?.cancel()   // cancel previous — prevents stale results overwriting fresh ones
        searchJob = viewModelScope.launch {
            _searchState.value =
                _searchState.value.copy(
                    loading = true,
                    error = null,
                    query = query,
                    recipes = emptyList(),
                    totalResults = 0,
                    loadingMore = false
                )
            delay(300)  // ✅ Issue #6: debounce — rapid keystrokes cancel before this point
            searchUseCase(query, offset = 0)
                .fold(
                    onSuccess = {
                        _searchState.value =
                            SearchState(
                                loading = false,
                                loadingMore = false,
                                recipes = it.recipes,
                                totalResults = it.totalResults,
                                query = query
                            )
                    },
                    onFailure = {
                        Log.w(TAG, "searchRecipes: ${it.message}")
                        _searchState.value =
                            _searchState.value.copy(
                                loading = false,
                                error = "Failed to search recipes"
                            )
                    }
                )
        }
    }

    fun loadMoreSearchResults() {
        val currentState = _searchState.value
        // Prevent concurrent loads or loading beyond total available
        if (currentState.loading || currentState.loadingMore || currentState.query.isBlank() ||
            (currentState.recipes.isNotEmpty() && currentState.recipes.size >= currentState.totalResults)
        ) {
            return
        }

        _searchState.value = currentState.copy(loadingMore = true, error = null)

        viewModelScope.launch {
            searchUseCase(currentState.query, offset = currentState.recipes.size)
                .fold(
                    onSuccess = { result ->
                        _searchState.value = _searchState.value.copy(
                            loadingMore = false,
                            recipes = currentState.recipes + result.recipes, // append
                            totalResults = result.totalResults
                        )
                    },
                    onFailure = {
                        Log.w(TAG, "loadMoreSearchResults: ${it.message}")
                        _searchState.value = _searchState.value.copy(
                            loadingMore = false,
                            error = "Failed to load more recipes"
                        )
                    }
                )
        }
    }

    fun getRecipesByCategory(categoryId: String) {
        // Serve from cache if present and not expired
        val cached = categoryCache[categoryId]
        if (cached != null && (System.currentTimeMillis() - cached.second) < CACHE_TTL_MS) {
            _categoryRecipesState.value =
                CategoryRecipesState(loading = false, recipes = cached.first)
            return
        }
        launchOp(
            onStart = {
                _categoryRecipesState.value =
                    _categoryRecipesState.value.copy(loading = true, error = null)
            },
            call = { getByCategoryUseCase(categoryId) },
            onSuccess = {
                // Evict oldest entry if at capacity
                if (categoryCache.size >= CACHE_MAX_SIZE) {
                    categoryCache.keys.firstOrNull()?.let { categoryCache.remove(it) }
                }
                categoryCache[categoryId] = Pair(it, System.currentTimeMillis())
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
                getFeaturedRecipes = AppContainer.getFeaturedRecipesUseCase,
                searchUseCase = AppContainer.searchRecipesUseCase,
                getRecipeDetails = AppContainer.getRecipeDetailsUseCase,
                getCategories = AppContainer.getCategoriesUseCase,
                getByCategoryUseCase = AppContainer.getRecipesByCategoryUseCase,
                favoriteDao = AppContainer.favoriteDao,
                shoppingDao = AppContainer.shoppingDao,
                cachedRecipeDao = AppContainer.cachedRecipeDao
            ) as T
        }
    }
}
