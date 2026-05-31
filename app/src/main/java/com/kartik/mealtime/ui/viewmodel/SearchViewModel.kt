package com.kartik.mealtime.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kartik.mealtime.data.analytics.AnalyticsHelper
import com.kartik.mealtime.domain.model.Recipe
import com.kartik.mealtime.domain.model.SearchResult
import com.kartik.mealtime.domain.usecase.SearchRecipesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Owns recipe search (extracted from MainViewModel): query results, paging,
 * the in-memory result cache, and session recent-search history.
 *
 * Shared (activity-scoped) so recent searches persist for the whole session,
 * matching the previous behaviour when this lived in the single shared VM.
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchUseCase: SearchRecipesUseCase,
    private val analytics: AnalyticsHelper
) : ViewModel() {

    data class SearchState(
        val loading: Boolean = false,
        val loadingMore: Boolean = false,
        val recipes: ImmutableList<Recipe> = persistentListOf(),
        val totalResults: Int = 0,
        val error: String? = null,
        val query: String = ""
    )

    private val _searchState = MutableStateFlow(SearchState())
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    // ── Recent Searches ───────────────────────────────────────────────────────
    // In-memory only — session history, max 5 entries, most-recent first.
    private val _recentSearches = MutableStateFlow<ImmutableList<String>>(persistentListOf())
    val recentSearches: StateFlow<ImmutableList<String>> = _recentSearches.asStateFlow()

    /** Adds [query] to the top of the recent list (deduped, capped at 5). */
    fun addRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        val updated = (listOf(trimmed) + _recentSearches.value.filter {
            it.lowercase() != trimmed.lowercase()
        }).take(5).toImmutableList()
        _recentSearches.value = updated
    }

    fun clearRecentSearch(query: String) {
        _recentSearches.value = _recentSearches.value.filter { it != query }.toImmutableList()
    }

    fun clearAllRecentSearches() {
        _recentSearches.value = persistentListOf()
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
                recipes = cached.first.recipes.toImmutableList(),
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
                    recipes = persistentListOf(),
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
                                recipes = it.recipes.toImmutableList(),
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
                recipes = (currentState.recipes + cachedPage.first.recipes).toImmutableList(),
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
                            recipes = (currentState.recipes + result.recipes).toImmutableList(), // append
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

    companion object {
        private const val TAG = "SearchViewModel"
        private const val SEARCH_CACHE_TTL_MS = 30 * 60 * 1000L   // 30 minutes
        private const val SEARCH_CACHE_MAX_SIZE = 20
    }
}
