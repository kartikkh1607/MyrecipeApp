package com.kartik.mealtime.ui.viewmodel

import android.app.Application
import com.kartik.mealtime.data.analytics.AnalyticsHelper
import com.kartik.mealtime.data.preferences.RecentRecipesRepository
import com.kartik.mealtime.data.preferences.UserPreferencesRepository
import com.kartik.mealtime.domain.model.DietaryFilter
import com.kartik.mealtime.domain.model.FeaturedRecipe
import com.kartik.mealtime.domain.model.FeaturedType
import com.kartik.mealtime.domain.model.Recipe
import com.kartik.mealtime.domain.model.RecipeCategory
import com.kartik.mealtime.domain.model.SearchResult
import com.kartik.mealtime.domain.repository.RecipeRepository
import com.kartik.mealtime.domain.usecase.FindRecipeVideoUseCase
import com.kartik.mealtime.domain.usecase.GetCategoriesUseCase
import com.kartik.mealtime.domain.usecase.GetFeaturedRecipesUseCase
import com.kartik.mealtime.domain.usecase.GetRecipeDetailsUseCase
import com.kartik.mealtime.domain.usecase.GetRecipesByCategoryUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Unit tests for [MainViewModel] — the app's largest ViewModel.
 *
 * The four recipe use cases are *real* (they only wrap repository calls in
 * `runCatching`); a single [FakeRecipeRepository] drives every code path, which
 * keeps assertions about call-counts / caching honest without Mockito suspend
 * stubbing. The concrete analytics + preferences collaborators are mocked since
 * the VM only fires-and-forgets at them.
 *
 * Robolectric is required: the VM builds a DataStore-backed `themeMode` flow off
 * the injected [android.content.Context] in its field initializer. `sdk = 34`
 * because Robolectric 4.16 can't emulate the project's compileSdk 36, and the
 * stub `Application` keeps the real MyRecipeApplication.onCreate() (→ Firebase)
 * from booting. The `themeMode` flow is `WhileSubscribed`, so with no collector
 * it never actually reads the DataStore file.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class MainViewModelTest {

    // ── Fake repository ─────────────────────────────────────────────────────────

    private class FakeRecipeRepository : RecipeRepository {
        var featured: List<FeaturedRecipe> = emptyList()
        var categories: List<RecipeCategory> = emptyList()
        var detailsResult: Recipe? = null
        var categoryRecipesResult: List<Recipe> = emptyList()
        var videoId: String? = null

        var failFeatured = false
        var failCategories = false
        var failDetails = false
        var failCategoryRecipes = false

        var detailsCallCount = 0
        var byCategoryCallCount = 0
        val byCategoryArgs = mutableListOf<Triple<String, Int, Boolean>>()

        override suspend fun getFeaturedRecipes(forceRefresh: Boolean): List<FeaturedRecipe> {
            if (failFeatured) throw RuntimeException("featured failed")
            return featured
        }

        override suspend fun searchRecipes(query: String, offset: Int, limit: Int): SearchResult =
            SearchResult(emptyList(), 0)

        override suspend fun getRecipeDetails(recipeId: String): Recipe? {
            detailsCallCount++
            if (failDetails) throw RuntimeException("details failed")
            return detailsResult
        }

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

        override suspend fun findRecipeVideoId(recipeName: String): String? = videoId
    }

    // ── Fixtures ─────────────────────────────────────────────────────────────────

    private fun recipe(id: String) =
        Recipe(id = id, name = "Recipe $id", description = "", imageUrl = "", category = "")

    private fun featured(id: String) =
        FeaturedRecipe(recipe = recipe(id), type = FeaturedType.RECIPE_OF_THE_DAY)

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

    private fun buildVm(repo: FakeRecipeRepository = FakeRecipeRepository()): MainViewModel =
        MainViewModel(
            getFeaturedRecipes = GetFeaturedRecipesUseCase(repo),
            getRecipeDetails = GetRecipeDetailsUseCase(repo),
            getCategories = GetCategoriesUseCase(repo),
            getByCategoryUseCase = GetRecipesByCategoryUseCase(repo),
            appContext = RuntimeEnvironment.getApplication(),
            analytics = mock(AnalyticsHelper::class.java),
            userPrefsRepo = mock(UserPreferencesRepository::class.java),
            recentRecipesRepo = mock(RecentRecipesRepository::class.java),
            findRecipeVideo = FindRecipeVideoUseCase(repo)
        )

    // ── init ───────────────────────────────────────────────────────────────────

    @Test
    fun `init loads featured recipes and categories into state`() {
        val repo = FakeRecipeRepository().apply {
            featured = listOf(featured("1"), featured("2"))
            categories = listOf(category("c1"))
        }
        val vm = buildVm(repo)

        assertFalse(vm.homeRecipeState.value.loading)
        assertEquals(2, vm.homeRecipeState.value.featuredRecipes.size)
        assertFalse(vm.recipeCategoriesState.value.loading)
        assertEquals(listOf("c1"), vm.recipeCategoriesState.value.categories.map { it.id })
    }

    @Test
    fun `init surfaces the error when featured recipes fail`() {
        val repo = FakeRecipeRepository().apply { failFeatured = true }
        val vm = buildVm(repo)

        assertFalse(vm.homeRecipeState.value.loading)
        assertEquals("featured failed", vm.homeRecipeState.value.error)
        assertTrue(vm.homeRecipeState.value.featuredRecipes.isEmpty())
    }

    // ── fetchRecipeDetails ───────────────────────────────────────────────────────

    @Test
    fun `fetchRecipeDetails populates state and caches on success`() {
        val repo = FakeRecipeRepository().apply { detailsResult = recipe("123") }
        val vm = buildVm(repo)

        vm.fetchRecipeDetails("123")

        val state = vm.recipeDetailState.value
        assertFalse(state.loading)
        assertEquals("123", state.recipe?.id)
        assertNull(state.error)
        assertEquals(recipe("123"), vm.recipeDetailCache["123"])
    }

    @Test
    fun `fetchRecipeDetails records an error state on failure`() {
        val repo = FakeRecipeRepository().apply { failDetails = true }
        val vm = buildVm(repo)

        vm.fetchRecipeDetails("123")

        val state = vm.recipeDetailState.value
        assertFalse(state.loading)
        assertNull(state.recipe)
        assertEquals("details failed", state.error)
    }

    @Test
    fun `fetchRecipeDetails skips a re-fetch of the already-loaded recipe`() {
        val repo = FakeRecipeRepository().apply { detailsResult = recipe("123") }
        val vm = buildVm(repo)

        vm.fetchRecipeDetails("123")
        vm.fetchRecipeDetails("123")

        assertEquals(1, repo.detailsCallCount)
    }

    // ── getRecipesByCategory + caching ───────────────────────────────────────────

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

    // ── small state holders ───────────────────────────────────────────────────────

    @Test
    fun `category filter defaults to ALL and persists per category`() {
        val vm = buildVm()

        assertEquals(DietaryFilter.ALL, vm.getCategoryFilter("cat1"))
        vm.setCategoryFilter("cat1", DietaryFilter.VEGAN)
        assertEquals(DietaryFilter.VEGAN, vm.getCategoryFilter("cat1"))
        assertEquals(DietaryFilter.ALL, vm.getCategoryFilter("cat2"))
    }

    @Test
    fun `selectCategory and setRecipeSwipeList update their state`() {
        val vm = buildVm()

        vm.selectCategory("cat9")
        vm.setRecipeSwipeList(listOf("a", "b", "c"))

        assertEquals("cat9", vm.selectedCategoryId.value)
        assertEquals(listOf("a", "b", "c"), vm.recipeSwipeIds.value)
    }

    // ── resolveYoutubeUrl ─────────────────────────────────────────────────────────

    @Test
    fun `resolveYoutubeUrl returns a watch link when a video id is found`() = runTest {
        val repo = FakeRecipeRepository().apply { videoId = "abc123" }
        val vm = buildVm(repo)

        assertEquals("https://www.youtube.com/watch?v=abc123", vm.resolveYoutubeUrl("Pasta"))
    }

    @Test
    fun `resolveYoutubeUrl falls back to a search link when no video id is found`() = runTest {
        val repo = FakeRecipeRepository().apply { videoId = null }
        val vm = buildVm(repo)

        assertEquals(
            "https://www.youtube.com/results?search_query=Spaghetti+Carbonara",
            vm.resolveYoutubeUrl("Spaghetti Carbonara")
        )
    }
}
