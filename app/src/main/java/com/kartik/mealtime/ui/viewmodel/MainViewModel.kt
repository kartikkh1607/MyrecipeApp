package com.kartik.mealtime.ui.viewmodel

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kartik.mealtime.data.analytics.AnalyticsHelper
import com.kartik.mealtime.data.local.ThemePreferences
import com.kartik.mealtime.data.preferences.RecentRecipesRepository
import com.kartik.mealtime.data.preferences.UserPreferencesRepository
import com.kartik.mealtime.domain.model.FeaturedRecipe
import com.kartik.mealtime.domain.model.Recipe
import com.kartik.mealtime.domain.model.ThemeMode
import com.kartik.mealtime.domain.usecase.FindRecipeVideoUseCase
import com.kartik.mealtime.domain.usecase.GetFeaturedRecipesUseCase
import com.kartik.mealtime.domain.usecase.GetRecipeDetailsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val getRecipeDetails: GetRecipeDetailsUseCase,
    private val themePreferences: ThemePreferences,
    private val analytics: AnalyticsHelper,
    private val userPrefsRepo: UserPreferencesRepository,
    private val recentRecipesRepo: RecentRecipesRepository,
    private val findRecipeVideo: FindRecipeVideoUseCase
) : ViewModel() {

    // ── Recipe Video (YouTube) ────────────────────────────────────────────────

    /**
     * Resolves the best YouTube URL for [recipeName]: a specific video when
     * Spoonacular has one, otherwise a plain search-results page. The video
     * lookup costs ~1 API point, so callers must invoke this only on an explicit
     * user action (the "Watch Video" tap).
     */
    suspend fun resolveYoutubeUrl(recipeName: String): String {
        val videoId = findRecipeVideo(recipeName).getOrNull()
        return if (!videoId.isNullOrBlank()) {
            "https://www.youtube.com/watch?v=$videoId"
        } else {
            val encoded = java.net.URLEncoder.encode(recipeName, "UTF-8")
            "https://www.youtube.com/results?search_query=$encoded"
        }
    }

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
    val themeMode: StateFlow<ThemeMode> = themePreferences
        .themeMode()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ThemeMode.SYSTEM
        )

    /** Persists the chosen [mode] to DataStore. Change is immediately reflected in [themeMode]. */
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            themePreferences.setThemeMode(mode)
        }
    }

    // ── Premium (dev unlock) ──────────────────────────────────────────────────
    // Local placeholder until Play Billing exists. The Settings dev toggle flips
    // this so premium AI features can be exercised without a real purchase. Real
    // entitlement is read app-wide via EntitlementRepository, which is backed by
    // the same DataStore flag today.

    // Lazy so construction never touches userPrefsRepo (keeps ViewModel tests that
    // pass a bare mock working); initialized on first observation by the UI.
    val isPremium: StateFlow<Boolean> by lazy {
        userPrefsRepo.isPremium.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false
        )
    }

    fun setPremiumDevOverride(enabled: Boolean) {
        viewModelScope.launch { userPrefsRepo.setPremium(enabled) }
    }

    // ── Home Screen State ─────────────────────────────────────────────────────
    @Immutable
    data class HomeRecipeState(
        val loading: Boolean = true,
        val featuredRecipes: List<FeaturedRecipe> = emptyList(),
        val error: String? = null
    )

    private val _homeRecipeState = mutableStateOf(HomeRecipeState())
    val homeRecipeState: State<HomeRecipeState> = _homeRecipeState

    // ── Recipe Detail State ───────────────────────────────────────────────────
    @Immutable
    data class RecipeDetailState(
        val loading: Boolean = true,
        val recipe: Recipe? = null,
        val error: String? = null
    )

    private val _recipeDetailState = mutableStateOf(RecipeDetailState())
    val recipeDetailState: State<RecipeDetailState> = _recipeDetailState

    // ─────────────────────────────────────────────────────────────────────────
    init {
        loadFeaturedRecipes()
    }

    // ── Public API ────────────────────────────────────────────────────────────

    fun refreshFeaturedRecipes() = loadFeaturedRecipes(forceRefresh = true)

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
}
