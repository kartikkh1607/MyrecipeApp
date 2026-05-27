package com.kartik.mealtime.data.repository

import com.kartik.mealtime.data.local.AiRecipeDao
import com.kartik.mealtime.data.local.AiRecipeEntity
import com.kartik.mealtime.data.local.AiRecipeSource
import com.kartik.mealtime.data.local.CachedRecipeDao
import com.kartik.mealtime.data.local.CachedRecipeEntity
import com.kartik.mealtime.data.local.FeaturedCacheDao
import com.kartik.mealtime.data.local.FeaturedCacheEntity
import com.kartik.mealtime.data.local.toAiRecipeEntity
import com.kartik.mealtime.data.remote.SpoonacularApiService
import com.kartik.mealtime.data.remote.dto.SpoonacularGetRecipesResponse
import com.kartik.mealtime.data.remote.dto.SpoonacularRecipeDto
import com.kartik.mealtime.data.remote.dto.SpoonacularSearchResponse
import com.kartik.mealtime.data.remote.dto.SpoonacularVideoSearchResponse
import com.kartik.mealtime.domain.model.Recipe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions

/**
 * Unit tests for [RecipeRepositoryImpl]'s cache -> API -> fallback decision logic.
 *
 * The repo is built through its test constructor so [apiConfigured] / [sampleFallbackEnabled]
 * are explicit instead of read from BuildConfig. Sample fallback is disabled in every test
 * so assertions don't depend on the hardcoded SampleDataSource. DAOs are simple fakes; the
 * API is either a fake that always fails or a Mockito mock used to prove it was never called.
 * `android.util.Log` is no-op'd by testOptions.unitTests.isReturnDefaultValues.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RecipeRepositoryImplTest {

    // ── Fakes ─────────────────────────────────────────────────────────────────────

    private class FakeCachedRecipeDao(private var stored: CachedRecipeEntity? = null) : CachedRecipeDao {
        override suspend fun upsert(entity: CachedRecipeEntity) { stored = entity }
        override suspend fun getById(id: String): CachedRecipeEntity? = stored?.takeIf { it.id == id }
        override suspend fun count(): Int = if (stored == null) 0 else 1
        override suspend fun evictOldest(count: Int) {}
        override suspend fun deleteAll() { stored = null }
    }

    private class FakeFeaturedCacheDao(private var stored: FeaturedCacheEntity? = null) : FeaturedCacheDao {
        override suspend fun upsert(entity: FeaturedCacheEntity) { stored = entity }
        override suspend fun get(): FeaturedCacheEntity? = stored
    }

    private class FakeAiRecipeDao(private val stored: MutableList<AiRecipeEntity> = mutableListOf()) : AiRecipeDao {
        override fun getAllFlow() = kotlinx.coroutines.flow.flowOf(stored.toList())
        override suspend fun getById(id: String): AiRecipeEntity? = stored.find { it.id == id }
        override suspend fun upsert(entity: AiRecipeEntity) { stored.add(entity) }
        override suspend fun delete(id: String) { stored.removeAll { it.id == id } }
        override suspend fun deleteAll() { stored.clear() }
    }

    /** Every endpoint throws — models a network/API failure. */
    private class FailingApiService : SpoonacularApiService {
        override suspend fun searchRecipes(
            query: String, type: String?, cuisine: String?, diet: String?, number: Int,
            offset: Int, addRecipeInformation: Boolean, addRecipeNutrition: Boolean,
            instructionsRequired: Boolean
        ): SpoonacularSearchResponse = throw RuntimeException("API down")

        override suspend fun getRecipeDetails(
            recipeId: Int, includeNutrition: Boolean, fillIngredients: Boolean
        ): SpoonacularRecipeDto = throw RuntimeException("API down")

        override suspend fun getRandomRecipes(
            number: Int, tags: String?, instructionsRequired: Boolean
        ): SpoonacularGetRecipesResponse = throw RuntimeException("API down")

        override suspend fun searchFoodVideos(
            query: String, number: Int
        ): SpoonacularVideoSearchResponse = throw RuntimeException("API down")
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    private fun recipe(id: String) =
        Recipe(id = id, name = "Recipe $id", description = "", imageUrl = "", category = "")

    private fun freshCacheOf(recipe: Recipe) =
        CachedRecipeEntity(id = recipe.id, recipe = recipe, cachedAt = System.currentTimeMillis())

    private fun staleCacheOf(recipe: Recipe) = CachedRecipeEntity(
        id = recipe.id,
        recipe = recipe,
        cachedAt = System.currentTimeMillis() - CachedRecipeEntity.CACHE_MAX_AGE_MS - 1_000L
    )

    private fun repo(
        api: SpoonacularApiService,
        cached: CachedRecipeDao = FakeCachedRecipeDao(),
        featured: FeaturedCacheDao = FakeFeaturedCacheDao(),
        aiRecipes: AiRecipeDao = FakeAiRecipeDao(),
        apiConfigured: Boolean = true,
        sampleEnabled: Boolean = false
    ) = RecipeRepositoryImpl(api, cached, featured, aiRecipes, apiConfigured, sampleEnabled)

    // ── getRecipeDetails ──────────────────────────────────────────────────────────

    @Test
    fun `getRecipeDetails serves fresh cache without calling the API`() = runTest {
        val recipe = recipe("123")
        val api = mock(SpoonacularApiService::class.java)
        val r = repo(api, cached = FakeCachedRecipeDao(freshCacheOf(recipe)))

        assertEquals(recipe, r.getRecipeDetails("123"))
        verifyNoInteractions(api)
    }

    @Test
    fun `getRecipeDetails falls back to stale cache when the API fails`() = runTest {
        val recipe = recipe("123")
        val r = repo(FailingApiService(), cached = FakeCachedRecipeDao(staleCacheOf(recipe)))

        assertEquals(recipe, r.getRecipeDetails("123"))
    }

    @Test
    fun `getRecipeDetails returns null when API fails with no cache and sample disabled`() = runTest {
        val r = repo(FailingApiService(), cached = FakeCachedRecipeDao(null))
        assertNull(r.getRecipeDetails("123"))
    }

    @Test
    fun `getRecipeDetails serves ai- ids from the local store without touching the API`() = runTest {
        val aiRecipe = recipe("ai-abc123")
        val api = mock(SpoonacularApiService::class.java)
        val aiDao = FakeAiRecipeDao(
            mutableListOf(aiRecipe.toAiRecipeEntity(AiRecipeSource.GENERATED))
        )
        val r = repo(api, aiRecipes = aiDao)

        assertEquals(aiRecipe, r.getRecipeDetails("ai-abc123"))
        verifyNoInteractions(api)
    }

    @Test
    fun `getRecipeDetails returns null for a non-integer id when sample disabled`() = runTest {
        val api = mock(SpoonacularApiService::class.java)
        val r = repo(api)
        assertNull(r.getRecipeDetails("not-a-number"))
        verifyNoInteractions(api)
    }

    @Test
    fun `getRecipeDetails returns null when API not configured and sample disabled`() = runTest {
        val api = mock(SpoonacularApiService::class.java)
        val r = repo(api, apiConfigured = false)
        assertNull(r.getRecipeDetails("123"))
        verifyNoInteractions(api)
    }

    // ── searchRecipes ──────────────────────────────────────────────────────────────

    @Test
    fun `searchRecipes returns empty for a blank query when sample disabled`() = runTest {
        val api = mock(SpoonacularApiService::class.java)
        val r = repo(api)
        val result = r.searchRecipes("   ", offset = 0, limit = 20)
        assertTrue(result.recipes.isEmpty())
        assertEquals(0, result.totalResults)
        verifyNoInteractions(api)
    }

    @Test
    fun `searchRecipes returns empty when the API fails`() = runTest {
        val r = repo(FailingApiService())
        val result = r.searchRecipes("pasta", offset = 0, limit = 20)
        assertTrue(result.recipes.isEmpty())
        assertEquals(0, result.totalResults)
    }

    // ── getFeaturedRecipes ───────────────────────────────────────────────────────

    @Test
    fun `getFeaturedRecipes serves fresh cache without calling the API`() = runTest {
        val api = mock(SpoonacularApiService::class.java)
        val freshFeatured = FakeFeaturedCacheDao(FeaturedCacheEntity.fromList(emptyList()))
        val r = repo(api, featured = freshFeatured)

        assertTrue(r.getFeaturedRecipes(forceRefresh = false).isEmpty())
        verifyNoInteractions(api)
    }

    @Test
    fun `getFeaturedRecipes returns empty when API fails with no cache and sample disabled`() = runTest {
        val r = repo(FailingApiService(), featured = FakeFeaturedCacheDao(null))
        assertTrue(r.getFeaturedRecipes(forceRefresh = true).isEmpty())
    }

    @Test
    fun `getFeaturedRecipes returns empty when API not configured and sample disabled`() = runTest {
        val api = mock(SpoonacularApiService::class.java)
        val r = repo(api, apiConfigured = false)
        assertTrue(r.getFeaturedRecipes(forceRefresh = false).isEmpty())
    }
}
