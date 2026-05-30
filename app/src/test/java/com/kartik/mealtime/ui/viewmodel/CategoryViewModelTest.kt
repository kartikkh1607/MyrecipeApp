package com.kartik.mealtime.ui.viewmodel

import android.app.Application
import com.kartik.mealtime.data.analytics.AnalyticsHelper
import com.kartik.mealtime.domain.model.DietaryFilter
import com.kartik.mealtime.domain.model.FeaturedRecipe
import com.kartik.mealtime.domain.model.Recipe
import com.kartik.mealtime.domain.model.RecipeCategory
import com.kartik.mealtime.domain.model.SearchResult
import com.kartik.mealtime.domain.repository.RecipeRepository
import com.kartik.mealtime.domain.usecase.GetCategoriesUseCase
import com.kartik.mealtime.domain.usecase.GetRecipesByCategoryUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [CategoryViewModel] — categories list, per-category recipe pages
 * (with LRU cache + dietary filter memory), and the soft selection state.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class CategoryViewModelTest {

    private class FakeRecipeRepository : RecipeRepository {
        var categories: List<RecipeCategory> = emptyList()
        var categoryRecipesResult: List<Recipe> = emptyList()

        var failCategories = false
        var failCategoryRecipes = false

        var byCategoryCallCount = 0
        val byCategoryArgs = mutableListOf<Triple<String, Int, Boolean>>()

        override suspend fun getFeaturedRecipes(forceRefresh: Boolean): List<FeaturedRecipe> =
            emptyList()

        override suspend fun searchRecipes(query: String, offset: Int, limit: Int): SearchResult =
            SearchResult(emptyList(), 0)

        override suspend fun getRecipeDetails(recipeId: String): Recipe? = null

        override suspend fun getCategories(): List<RecipeCategory> {
            if (failCategories) throw RuntimeException("categories failed")
            return categories
        }

        override suspend fun getRecipesByCategory(
            categoryId: String, limit: Int, offset: Int, append: Boolean
        ): List<Recipe> {
            byCategoryCallCount++
            byCategoryArgs += Triple(categoryId, offset, append)
            if (failCategoryRecipes) throw RuntimeException("category recipes failed")
            return categoryRecipesResult
        }

        override suspend fun findRecipeVideoId(recipeName: String): String? = null
    }

    private fun recipe(id: String) =
        Recipe(id = id, name = "Recipe $id", description = "", imageUrl = "", category = "")

    private fun category(id: String) =
        RecipeCategory(id = id, name = "Cat $id", description = "", imageUrl = "")

    private fun recipes(idRange: IntRange) = idRange.map { recipe("r$it") }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildVm(repo: FakeRecipeRepository = FakeRecipeRepository()): CategoryViewModel =
        CategoryViewModel(
            getCategories = GetCategoriesUseCase(repo),
            getByCategoryUseCase = GetRecipesByCategoryUseCase(repo),
            analytics = mock(AnalyticsHelper::class.java),
        )

    @Test
    fun `init loads categories into state`() {
        val repo = FakeRecipeRepository().apply { categories = listOf(category("c1")) }
        val vm = buildVm(repo)

        assertFalse(vm.recipeCategoriesState.value.loading)
        assertEquals(listOf("c1"), vm.recipeCategoriesState.value.categories.map { it.id })
    }

    @Test
    fun `init surfaces the error when categories fail`() {
        val repo = FakeRecipeRepository().apply { failCategories = true }
        val vm = buildVm(repo)

        assertFalse(vm.recipeCategoriesState.value.loading)
        assertEquals("categories failed", vm.recipeCategoriesState.value.error)
    }

    @Test
    fun `getRecipesByCategory loads a full page and flags hasMore`() {
        val repo = FakeRecipeRepository().apply { categoryRecipesResult = recipes(0..19) }
        val vm = buildVm(repo)

        vm.getRecipesByCategory("cat1")

        val state = vm.categoryRecipesState.value
        assertFalse(state.loading)
        assertEquals(20, state.recipes.size)
        assertEquals(20, state.totalLoaded)
        assertTrue(state.hasMore)
    }

    @Test
    fun `getRecipesByCategory serves a second call from cache`() {
        val repo = FakeRecipeRepository().apply { categoryRecipesResult = recipes(0..4) }
        val vm = buildVm(repo)

        vm.getRecipesByCategory("cat1")
        vm.getRecipesByCategory("cat1")

        assertEquals(1, repo.byCategoryCallCount)
        assertEquals(5, vm.categoryRecipesState.value.recipes.size)
    }

    @Test
    fun `refreshCategoryRecipes evicts the cache and re-fetches`() {
        val repo = FakeRecipeRepository().apply { categoryRecipesResult = recipes(0..4) }
        val vm = buildVm(repo)

        vm.getRecipesByCategory("cat1")
        vm.refreshCategoryRecipes("cat1")

        assertEquals(2, repo.byCategoryCallCount)
    }

    @Test
    fun `getRecipesByCategory shows a friendly error message on failure`() {
        val repo = FakeRecipeRepository().apply { failCategoryRecipes = true }
        val vm = buildVm(repo)

        vm.getRecipesByCategory("cat1")

        val state = vm.categoryRecipesState.value
        assertFalse(state.loading)
        assertTrue(state.recipes.isEmpty())
        assertEquals("API temporarily unavailable. Please try again later.", state.error)
    }

    @Test
    fun `loadMoreCategoryRecipes appends the next page with the right offset`() {
        val repo = FakeRecipeRepository().apply { categoryRecipesResult = recipes(0..19) }
        val vm = buildVm(repo)
        vm.getRecipesByCategory("cat1")          // 20 loaded, hasMore = true

        repo.categoryRecipesResult = recipes(20..24)  // next page = 5
        vm.loadMoreCategoryRecipes("cat1")

        val state = vm.categoryRecipesState.value
        assertEquals(25, state.recipes.size)
        assertEquals(25, state.totalLoaded)
        assertFalse(state.hasMore)               // 5 < 20 → no more pages
        assertFalse(state.isLoadingMore)
        assertEquals(Triple("cat1", 20, true), repo.byCategoryArgs.last())
    }

    @Test
    fun `category filter defaults to ALL and persists per category`() {
        val vm = buildVm()

        assertEquals(DietaryFilter.ALL, vm.getCategoryFilter("cat1"))
        vm.setCategoryFilter("cat1", DietaryFilter.VEGAN)
        assertEquals(DietaryFilter.VEGAN, vm.getCategoryFilter("cat1"))
        assertEquals(DietaryFilter.ALL, vm.getCategoryFilter("cat2"))
    }

    @Test
    fun `selectCategory updates the soft selection state`() {
        val vm = buildVm()

        vm.selectCategory("cat9")

        assertEquals("cat9", vm.selectedCategoryId.value)
    }
}
