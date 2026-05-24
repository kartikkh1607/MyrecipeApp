package com.kartik.mealtime.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kartik.mealtime.data.local.CachedRecipeDao
import com.kartik.mealtime.data.local.FavoriteDao
import com.kartik.mealtime.data.local.ShoppingDao
import com.kartik.mealtime.data.local.ShoppingItemEntity
import com.kartik.mealtime.data.local.ThemePreferences
import com.kartik.mealtime.data.local.toCachedEntity
import com.kartik.mealtime.data.local.toFavoriteEntity
import com.kartik.mealtime.data.local.toRecipe
import com.kartik.mealtime.data.local.toShoppingListItem
import com.kartik.mealtime.domain.model.FeaturedRecipe
import com.kartik.mealtime.domain.model.Recipe
import com.kartik.mealtime.domain.model.RecipeCategory
import com.kartik.mealtime.domain.model.SearchResult
import com.kartik.mealtime.domain.model.ShoppingListItem
import com.kartik.mealtime.domain.model.ThemeMode
import com.kartik.mealtime.domain.usecase.GetCategoriesUseCase
import com.kartik.mealtime.domain.usecase.GetFeaturedRecipesUseCase
import com.kartik.mealtime.domain.usecase.GetRecipeDetailsUseCase
import com.kartik.mealtime.domain.usecase.GetRecipesByCategoryUseCase
import com.kartik.mealtime.data.analytics.AnalyticsHelper
import com.kartik.mealtime.data.preferences.RecentRecipesRepository
import com.kartik.mealtime.data.preferences.UserPreferencesRepository
import com.kartik.mealtime.data.repository.SyncRepository
import com.kartik.mealtime.data.repository.UserRepository
import com.kartik.mealtime.domain.model.DietaryFilter
import com.kartik.mealtime.domain.usecase.SearchRecipesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

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
@HiltViewModel
class MainViewModel @Inject constructor(
    private val getFeaturedRecipes: GetFeaturedRecipesUseCase,
    private val searchUseCase: SearchRecipesUseCase,
    private val getRecipeDetails: GetRecipeDetailsUseCase,
    private val getCategories: GetCategoriesUseCase,
    private val getByCategoryUseCase: GetRecipesByCategoryUseCase,
    private val favoriteDao: FavoriteDao,
    private val shoppingDao: ShoppingDao,
    private val cachedRecipeDao: CachedRecipeDao,
    @ApplicationContext private val appContext: Context,
    private val analytics: AnalyticsHelper,
    private val userRepository: UserRepository,
    private val syncRepository: SyncRepository,
    private val userPrefsRepo: UserPreferencesRepository,
    private val recentRecipesRepo: RecentRecipesRepository
) : ViewModel() {

    // ── Recipe detail cache — keyed by recipe ID ─────────────────────────────
    // Populated on every successful fetchRecipeDetails call so that the pager
    // can show already-loaded recipes instantly without re-fetching.
    val recipeDetailCache = mutableStateMapOf<String, Recipe>()

    private val _recipeSwipeIds = mutableStateOf<List<String>>(emptyList())
    val recipeSwipeIds: State<List<String>> = _recipeSwipeIds

    fun setRecipeSwipeList(ids: List<String>) {
        _recipeSwipeIds.value = ids
    }

    // ── Theme State (Issue #4) ────────────────────────────────────────────────

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

    // ── Recent Searches ───────────────────────────────────────────────────────
    // In-memory only — session history, max 5 entries, most-recent first.
    private val _recentSearches = mutableStateOf<List<String>>(emptyList())
    val recentSearches: State<List<String>> = _recentSearches

    /** Adds [query] to the top of the recent list (deduped, capped at 5). */
    fun addRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        val updated = (listOf(trimmed) + _recentSearches.value.filter {
            it.lowercase() != trimmed.lowercase()
        }).take(5)
        _recentSearches.value = updated
    }

    fun clearRecentSearch(query: String) {
        _recentSearches.value = _recentSearches.value.filter { it != query }
    }

    fun clearAllRecentSearches() {
        _recentSearches.value = emptyList()
    }

    // ── Category Recipes State ────────────────────────────────────────────────
    data class CategoryRecipesState(
        val loading: Boolean = false,
        val recipes: List<Recipe> = emptyList(),
        val error: String? = null,
        val totalLoaded: Int = 0,
        val hasMore: Boolean = false,
        val isLoadingMore: Boolean = false
    )

    // ── Favorites State (Room-backed) ─────────────────────────────────────────
    // Single source of truth — _favoriteRecipes. favoriteIds is derived so the
    // two views can never disagree.
    private val _favoriteRecipes = mutableStateOf<List<Recipe>>(emptyList())
    val favoriteRecipes: State<List<Recipe>> = _favoriteRecipes

    val favoriteIds: State<Set<String>> = derivedStateOf {
        _favoriteRecipes.value.mapTo(mutableSetOf()) { it.id }
    }

    /** Persisted grid/list toggle — survives navigation (lives in ViewModel, not screen). */
    private val _favoritesGridMode = mutableStateOf(false)
    val favoritesGridMode: State<Boolean> = _favoritesGridMode

    fun toggleFavoritesGridMode() {
        _favoritesGridMode.value = !_favoritesGridMode.value
    }

    // ── Favorites Sort Order ─────────────────────────────────────────────────
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

    fun removeFavorite(recipeId: String) {
        viewModelScope.launch {
            favoriteDao.delete(recipeId)
            userRepository.currentUser?.uid?.let { uid ->
                runCatching { syncRepository.deleteFavorite(uid, recipeId) }
            }
        }
    }

    /** Restores a previously removed favorite (used by undo-snackbar). */
    fun addFavorite(recipe: Recipe) {
        viewModelScope.launch {
            favoriteDao.insert(recipe.toFavoriteEntity())
            // Do NOT upsert into cachedRecipeDao here — the recipe object sourced from
            // FavoriteEntity has servings=0, no ingredients, and no instructions, so it
            // would corrupt the detail cache and cause the servings stepper to show wrong values.
        }
    }


    fun toggleFavorite(recipe: Recipe) {
        viewModelScope.launch {
            val uid = userRepository.currentUser?.uid
            if (_favoriteRecipes.value.any { it.id == recipe.id }) {
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
    }

    private val _shoppingList = mutableStateOf<List<ShoppingListItem>>(emptyList())
    val shoppingList: State<List<ShoppingListItem>> = _shoppingList

    /** The last recipe name passed to [addToShoppingList]. Used by ShoppingListScreen to auto focus that section. */
    private val _lastAddedRecipeName = mutableStateOf<String?>(null)
    val lastAddedRecipeName: State<String?> = _lastAddedRecipeName

    /** Replaces all recipe items (keeps Custom items) with [recipe]'s ingredients. */
    fun addToShoppingList(recipe: Recipe) {
        _lastAddedRecipeName.value = recipe.name
        analytics.logShoppingListUpdated("add_recipe", recipe.name)
        viewModelScope.launch {
            val uid = userRepository.currentUser?.uid
            shoppingDao.deleteByRecipeExcluding()
            uid?.let { runCatching { syncRepository.clearShoppingList(it) } }
            recipe.ingredients.forEach { ing ->
                val key = "${recipe.id}_${ing.id.ifEmpty { ing.name }}"
                val entity = ShoppingItemEntity(
                    key = key,
                    ingredientName = ing.name,
                    amount = ing.amount,
                    unit = ing.unit,
                    recipeName = recipe.name
                )
                shoppingDao.insert(entity)
                uid?.let { runCatching { syncRepository.uploadShoppingItem(it, entity) } }
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
        viewModelScope.launch {
            shoppingDao.deleteByKey(key)
            userRepository.currentUser?.uid?.let { uid ->
                runCatching { syncRepository.deleteShoppingItem(uid, key) }
            }
        }
    }

    /** Restores a previously deleted shopping item (used by undo-snackbar). */
    fun restoreShoppingItem(item: ShoppingListItem) {
        viewModelScope.launch {
            shoppingDao.insert(
                ShoppingItemEntity(
                    key = item.key,
                    ingredientName = item.ingredientName,
                    amount = item.amount,
                    unit = item.unit,
                    recipeName = item.recipeName,
                    checked = item.isChecked
                )
            )
        }
    }


    fun clearShoppingList() {
        viewModelScope.launch {
            shoppingDao.deleteAll()
            userRepository.currentUser?.uid?.let { uid ->
                runCatching { syncRepository.clearShoppingList(uid) }
            }
        }
    }

    /**
     * Adds a free-text item (not from any recipe) to the shopping list.
     * Groups under the "Custom" section so it's visually distinct.
     */
    fun addCustomShoppingItem(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            val key =
                "custom_${System.currentTimeMillis()}_${trimmed.lowercase().replace(' ', '_')}"
            shoppingDao.insert(
                ShoppingItemEntity(
                    key = key,
                    ingredientName = trimmed,
                    amount = "",
                    unit = "",
                    recipeName = "Custom"
                )
            )
        }
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
    // Max 20 entries, entries expire after 24 h. accessOrder=true makes this a real LRU:
    // reading an entry via get() moves it to the most-recently-used end, so eviction
    // picks the entry whose last *access* (not insertion) was oldest.
    private val categoryCache = object : LinkedHashMap<String, Pair<List<Recipe>, Long>>(
        16, 0.75f, /* accessOrder = */ true
    ) {
        override fun removeEldestEntry(eldest: Map.Entry<String, Pair<List<Recipe>, Long>>): Boolean =
            size > CATEGORY_CACHE_MAX_SIZE
    }

    // ── Per-category dietary filter persistence ─────────────────────────────
    // Remembers user's filter choice per category so it's restored on re-visit.
    private val categoryFilters = mutableMapOf<String, DietaryFilter>()

    fun getCategoryFilter(categoryId: String): DietaryFilter =
        categoryFilters[categoryId] ?: DietaryFilter.ALL

    fun setCategoryFilter(categoryId: String, filter: DietaryFilter) {
        categoryFilters[categoryId] = filter
    }

    // ── Search-result cache — keyed by "query|offset", value = (result, cachedAt) ─
    // Saves API calls when the user types the same query twice (e.g. clears + retypes).
    // 30-minute TTL keeps results fresh enough; 20-entry LRU bounds memory.
    private val searchCache = object : LinkedHashMap<String, Pair<SearchResult, Long>>(
        16, 0.75f, /* accessOrder = */ true
    ) {
        override fun removeEldestEntry(eldest: Map.Entry<String, Pair<SearchResult, Long>>): Boolean =
            size > SEARCH_CACHE_MAX_SIZE
    }


    private fun searchCacheKey(query: String, offset: Int): String =
        "${query.lowercase().trim()}|$offset"

    // ─────────────────────────────────────────────────────────────────────────
    init {
        // Single Room query — favoriteIds is derived from _favoriteRecipes.
        viewModelScope.launch {
            favoriteDao.getAllFlow().collect { entities ->
                _favoriteRecipes.value = entities.map { it.toRecipe() }
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

    fun refreshFeaturedRecipes() = loadFeaturedRecipes(forceRefresh = true)
    fun refreshRecipeCategories() = loadCategories()

    /** Force-refresh category recipes — bypasses the cache TTL (used by pull-to-refresh). */
    fun refreshCategoryRecipes(categoryId: String) {
        categoryCache.remove(categoryId)   // evict so getRecipesByCategory() hits the network
        getRecipesByCategory(categoryId)
    }

    fun fetchRecipeDetails(recipeId: String) {
        // Skip re-fetch only when the EXACT same recipe is already fully loaded.
        // For any other ID, reset state immediately so the old recipe never flashes
        // on screen while the new one is loading (the core stale-recipe bug).
        if (_recipeDetailState.value.recipe?.id == recipeId &&
            !_recipeDetailState.value.loading
        ) return

        // Only reset state when there's something to clear. If the state is already
        // the default (loading=true, no recipe), skipping this write prevents a spurious
        // recompose of RecipeDetailScreen right as the navigation enter-animation begins.
        val current = _recipeDetailState.value
        if (current.recipe != null || current.error != null) {
            _recipeDetailState.value =
                RecipeDetailState(loading = true, recipe = null, error = null)
        }

        launchOp(
            onStart = { /* state already set above */ },
            call = { getRecipeDetails(recipeId) },
            onSuccess = {
                _recipeDetailState.value = RecipeDetailState(loading = false, recipe = it)
                it?.let { r ->
                    recipeDetailCache[r.id] = r
                    analytics.logRecipeViewed(r.id, r.name)
                }
            },
            onFailure = {
                Log.w(TAG, "fetchRecipeDetails: ${it.message}")
                _recipeDetailState.value = RecipeDetailState(loading = false, error = it.message)
            }
        )
    }

    /**
     * Records a recipe view for personalization — updates streak, increments
     * lifetime view count, pushes onto the recently-viewed list. Called from
     * [RecipeDetailPage] once the recipe is loaded (whether fresh or cached).
     */
    fun recordRecipeView(recipe: Recipe) {
        viewModelScope.launch {
            userPrefsRepo.recordRecipeView()
            recentRecipesRepo.add(recipe)
        }
    }

    // Job reference so we can cancel a stale search when a new one starts
    private var searchJob: Job? = null

    /**
     * Fires a search. Debouncing is the UI layer's job (SearchScreen uses
     * snapshotFlow + debounce(300)); we still cancel any in-flight search so
     * stale results can't overwrite fresh ones.
     */
    fun searchRecipes(query: String) {
        searchJob?.cancel()

        // Serve from in-memory LRU if a fresh result for this query exists.
        // Saves an API call when the user clears and retypes the same word.
        val key = searchCacheKey(query, 0)
        val cached = searchCache[key]
        if (cached != null && (System.currentTimeMillis() - cached.second) < SEARCH_CACHE_TTL_MS) {
            _searchState.value = SearchState(
                loading = false,
                loadingMore = false,
                recipes = cached.first.recipes,
                totalResults = cached.first.totalResults,
                query = query
            )
            return
        }

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
            searchUseCase(query, offset = 0)
                .fold(
                    onSuccess = {
                        searchCache[key] = it to System.currentTimeMillis()
                        addRecentSearch(query)
                        analytics.logSearch(query)
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

        val offset = currentState.recipes.size
        val pageKey = searchCacheKey(currentState.query, offset)
        val cachedPage = searchCache[pageKey]
        if (cachedPage != null && (System.currentTimeMillis() - cachedPage.second) < SEARCH_CACHE_TTL_MS) {
            _searchState.value = _searchState.value.copy(
                loadingMore = false,
                recipes = currentState.recipes + cachedPage.first.recipes,
                totalResults = cachedPage.first.totalResults
            )
            return
        }

        viewModelScope.launch {
            searchUseCase(currentState.query, offset = offset)
                .fold(
                    onSuccess = { result ->
                        searchCache[pageKey] = result to System.currentTimeMillis()
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
                CategoryRecipesState(
                    loading = false,
                    recipes = cached.first,
                    totalLoaded = cached.first.size,
                    hasMore = false
                )
            return
        }
        launchOp(
            onStart = {
                _categoryRecipesState.value =
                    _categoryRecipesState.value.copy(loading = true, error = null)
            },
            call = { getByCategoryUseCase(categoryId) },
            onSuccess = { newRecipes ->
                categoryCache[categoryId] = Pair(newRecipes, System.currentTimeMillis())
                val categoryName = _recipeCategoriesState.value.categories
                    .find { it.id == categoryId }?.name ?: categoryId
                analytics.logCategoryViewed(categoryId, categoryName)
                _categoryRecipesState.value = CategoryRecipesState(
                    loading = false,
                    recipes = newRecipes,
                    totalLoaded = newRecipes.size,
                    hasMore = newRecipes.size >= 20  // Spoonacular returns 20 per page
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

    fun loadMoreCategoryRecipes(categoryId: String) {
        val current = _categoryRecipesState.value
        if (current.loading || current.isLoadingMore || !current.hasMore) return

        _categoryRecipesState.value = current.copy(isLoadingMore = true, error = null)

        viewModelScope.launch {
            val offset = current.totalLoaded
            val result = getByCategoryUseCase(categoryId, offset = offset, append = true)
            result.fold(
                onSuccess = { newRecipes ->
                    val updated = current.copy(
                        isLoadingMore = false,
                        recipes = current.recipes + newRecipes,
                        totalLoaded = current.totalLoaded + newRecipes.size,
                        hasMore = newRecipes.size >= 20
                    )
                    _categoryRecipesState.value = updated
                    // Update cache with full list
                    categoryCache[categoryId] = Pair(updated.recipes, System.currentTimeMillis())
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

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun loadFeaturedRecipes(forceRefresh: Boolean = false) = launchOp(
        onStart = {
            _homeRecipeState.value = _homeRecipeState.value.copy(loading = true, error = null)
        },
        call = { getFeaturedRecipes(forceRefresh) },
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
        private const val CACHE_TTL_MS = 24 * 60 * 60 * 1000L     // 24 hours
        private const val CATEGORY_CACHE_MAX_SIZE = 20
        private const val SEARCH_CACHE_TTL_MS = 30 * 60 * 1000L   // 30 minutes
        private const val SEARCH_CACHE_MAX_SIZE = 20
    }
}
