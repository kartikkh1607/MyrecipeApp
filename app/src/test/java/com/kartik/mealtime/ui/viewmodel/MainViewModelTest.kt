package com.kartik.mealtime.ui.viewmodel

import android.app.Application
import com.kartik.mealtime.data.analytics.AnalyticsHelper
import com.kartik.mealtime.data.local.ThemePreferences
import com.kartik.mealtime.data.preferences.RecentRecipesRepository
import com.kartik.mealtime.data.preferences.UserPreferencesRepository
import com.kartik.mealtime.domain.model.FeaturedRecipe
import com.kartik.mealtime.domain.model.FeaturedType
import com.kartik.mealtime.domain.model.Recipe
import com.kartik.mealtime.domain.model.RecipeCategory
import com.kartik.mealtime.domain.model.SearchResult
import com.kartik.mealtime.domain.repository.RecipeRepository
import com.kartik.mealtime.domain.usecase.FindRecipeVideoUseCase
import com.kartik.mealtime.domain.usecase.GetFeaturedRecipesUseCase
import com.kartik.mealtime.domain.usecase.GetRecipeDetailsUseCase
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
 * Unit tests for [MainViewModel] — covers featured-recipes + recipe-detail state
 * (theme/premium flows + cross-screen swipe list). Category state has its own
 * VM and test class — see [CategoryViewModelTest].
 *
 * The recipe use cases are *real* (they only wrap repository calls in
 * `runCatching`); a single [FakeRecipeRepository] drives every code path, which
 * keeps assertions about call-counts honest without Mockito suspend stubbing.
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
        var detailsResult: Recipe? = null
        var videoId: String? = null

        var failFeatured = false
        var failDetails = false

        var detailsCallCount = 0

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

        override suspend fun getCategories(): List<RecipeCategory> = emptyList()

        override suspend fun getRecipesByCategory(
            categoryId: String, limit: Int, offset: Int, append: Boolean
        ): List<Recipe> = emptyList()

        override suspend fun findRecipeVideoId(recipeName: String): String? = videoId
    }

    // ── Fixtures ─────────────────────────────────────────────────────────────────

    private fun recipe(id: String) =
        Recipe(id = id, name = "Recipe $id", description = "", imageUrl = "", category = "")

    private fun featured(id: String) =
        FeaturedRecipe(recipe = recipe(id), type = FeaturedType.RECIPE_OF_THE_DAY)

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
            themePreferences = ThemePreferences(RuntimeEnvironment.getApplication()),
            analytics = mock(AnalyticsHelper::class.java),
            userPrefsRepo = mock(UserPreferencesRepository::class.java),
            recentRecipesRepo = mock(RecentRecipesRepository::class.java),
            findRecipeVideo = FindRecipeVideoUseCase(repo)
        )

    // ── init ───────────────────────────────────────────────────────────────────

    @Test
    fun `init loads featured recipes into state`() {
        val repo = FakeRecipeRepository().apply {
            featured = listOf(featured("1"), featured("2"))
        }
        val vm = buildVm(repo)

        assertFalse(vm.homeRecipeState.value.loading)
        assertEquals(2, vm.homeRecipeState.value.featuredRecipes.size)
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

    @Test
    fun `setRecipeSwipeList updates the cross-screen swipe state`() {
        val vm = buildVm()

        vm.setRecipeSwipeList(listOf("a", "b", "c"))

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
