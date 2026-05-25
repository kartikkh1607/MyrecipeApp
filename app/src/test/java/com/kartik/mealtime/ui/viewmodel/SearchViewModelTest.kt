package com.kartik.mealtime.ui.viewmodel

import com.kartik.mealtime.data.analytics.AnalyticsHelper
import com.kartik.mealtime.domain.repository.RecipeRepository
import com.kartik.mealtime.domain.usecase.SearchRecipesUseCase
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock

/**
 * Unit tests for [SearchViewModel]'s pure presentation logic — recent-search
 * dedup/cap. No Android dependencies, so no Robolectric needed (the search use
 * case + analytics are never invoked by addRecentSearch). Relocated from
 * MainViewModelTest when search was split out of the god ViewModel.
 */
class SearchViewModelTest {

    private fun buildViewModel(): SearchViewModel = SearchViewModel(
        searchUseCase = SearchRecipesUseCase(mock(RecipeRepository::class.java)),
        analytics = mock(AnalyticsHelper::class.java)
    )

    @Test
    fun `recent search dedups case-insensitively and moves match to front`() {
        val vm = buildViewModel()
        vm.addRecentSearch("Pasta")
        vm.addRecentSearch("Salad")
        vm.addRecentSearch("pasta")  // same as "Pasta", ignoring case

        assertEquals(listOf("pasta", "Salad"), vm.recentSearches.value)
    }

    @Test
    fun `recent searches are capped at five most-recent`() {
        val vm = buildViewModel()
        listOf("a", "b", "c", "d", "e", "f").forEach { vm.addRecentSearch(it) }

        assertEquals(listOf("f", "e", "d", "c", "b"), vm.recentSearches.value)
    }

    @Test
    fun `blank recent search is ignored`() {
        val vm = buildViewModel()
        vm.addRecentSearch("   ")
        assertEquals(emptyList<String>(), vm.recentSearches.value)
    }

    @Test
    fun `clearRecentSearch removes only the matching entry`() {
        val vm = buildViewModel()
        vm.addRecentSearch("eggs")
        vm.addRecentSearch("milk")
        vm.clearRecentSearch("eggs")
        assertEquals(listOf("milk"), vm.recentSearches.value)
    }

    @Test
    fun `clearAllRecentSearches empties the list`() {
        val vm = buildViewModel()
        vm.addRecentSearch("eggs")
        vm.addRecentSearch("milk")
        vm.clearAllRecentSearches()
        assertEquals(emptyList<String>(), vm.recentSearches.value)
    }
}
