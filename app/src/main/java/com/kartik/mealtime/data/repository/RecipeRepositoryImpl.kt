package com.kartik.mealtime.data.repository

import android.util.Log
import com.kartik.mealtime.BuildConfig
import com.kartik.mealtime.data.local.CachedRecipeDao
import com.kartik.mealtime.data.local.CachedRecipeEntity
import com.kartik.mealtime.data.local.FeaturedCacheDao
import com.kartik.mealtime.data.local.FeaturedCacheEntity
import com.kartik.mealtime.data.local.toCachedEntity
import com.kartik.mealtime.data.local.toRecipe
import com.kartik.mealtime.data.remote.SpoonacularApiService
import com.kartik.mealtime.data.remote.dto.toDomain
import com.kartik.mealtime.data.source.CategoryDataSource
import com.kartik.mealtime.data.source.SampleDataSource
import com.kartik.mealtime.domain.model.CategoryKind
import com.kartik.mealtime.domain.model.FeaturedRecipe
import com.kartik.mealtime.domain.model.FeaturedType
import com.kartik.mealtime.domain.model.Recipe
import com.kartik.mealtime.domain.model.RecipeCategory
import com.kartik.mealtime.domain.model.SearchResult
import com.kartik.mealtime.domain.repository.RecipeRepository
import javax.inject.Inject

/**
 * Concrete implementation of [RecipeRepository].
 *
 * All "try API → fallback to sample data" logic that previously lived
 * inside MainViewModel now lives here — in the data layer where it belongs.
 *
 * Issue #7 fix: [getRecipeDetails] now caches successful API responses in Room
 * and serves them from cache when offline, so the detail screen never shows up empty.
 */
class RecipeRepositoryImpl(
    private val apiService: SpoonacularApiService,
    private val cachedRecipeDao: CachedRecipeDao,
    private val featuredCacheDao: FeaturedCacheDao,
    // Whether a Spoonacular key is present (decides API vs. sample fallback). The actual
    // key injection is handled by NetworkModule.ApiKeyInterceptor.
    private val apiConfigured: Boolean,
    // Sample data is a debug-only fallback so release builds don't quietly ship demo
    // recipes when the network is unavailable. In release, network failures propagate as
    // empty results and the UI shows its error state.
    private val sampleFallbackEnabled: Boolean
) : RecipeRepository {

    /** Production constructor — Hilt-injected; reads API config from [BuildConfig]. */
    @Inject
    constructor(
        apiService: SpoonacularApiService,
        cachedRecipeDao: CachedRecipeDao,
        featuredCacheDao: FeaturedCacheDao
    ) : this(
        apiService = apiService,
        cachedRecipeDao = cachedRecipeDao,
        featuredCacheDao = featuredCacheDao,
        apiConfigured = BuildConfig.SPOONACULAR_API_KEY.isNotEmpty() &&
                BuildConfig.SPOONACULAR_API_KEY != "null",
        sampleFallbackEnabled = BuildConfig.DEBUG
    )

    private val sampleRecipes: List<Recipe> by lazy {
        SampleDataSource.featuredRecipes.map { it.recipe }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Featured Recipes
    // ─────────────────────────────────────────────────────────────────────────

    override suspend fun getFeaturedRecipes(forceRefresh: Boolean): List<FeaturedRecipe> {
        if (!apiConfigured) {
            Log.d(TAG, "API not configured — using sample featured recipes")
            return if (sampleFallbackEnabled) SampleDataSource.featuredRecipes else emptyList()
        }

        // Cache-first: avoid the 5-pt random-recipes call when fresh data exists.
        if (!forceRefresh) {
            val cached = featuredCacheDao.get()
            if (cached != null && !cached.isExpired()) {
                Log.d(TAG, "Serving featured recipes from cache (no API call)")
                return FeaturedCacheEntity.toList(cached)
            }
        }

        return try {
            Log.d(TAG, "Fetching featured recipes from Spoonacular")
            val response = apiService.getRandomRecipes(number = 5)  // 5 pts vs 10 pts per open
            val featured = response.recipes.mapIndexed { index, dto ->
                val recipe = dto.toDomain()
                FeaturedRecipe(
                    recipe = recipe,
                    type = when (index) {
                        0 -> FeaturedType.RECIPE_OF_THE_DAY
                        1 -> FeaturedType.POPULAR_THIS_WEEK
                        else -> FeaturedType.QUICK_MEALS
                    },
                    subtitle = recipe.category.ifEmpty { "New Discovery" },
                    badgeText = recipe.category.uppercase().ifEmpty { "NEW" },
                    gradientColors = listOf("#FF6B6B", "#FF8E53")
                )
            }
            featuredCacheDao.upsert(FeaturedCacheEntity.fromList(featured))
            featured
        } catch (e: Exception) {
            Log.w(TAG, "Featured recipes API failed: ${e.message}")
            // Network failure — serve stale cache if any, then sample as last resort.
            val cached = featuredCacheDao.get()
            if (cached != null) {
                Log.w(TAG, "Serving featured recipes from STALE cache after API failure")
                return FeaturedCacheEntity.toList(cached)
            }
            if (sampleFallbackEnabled) SampleDataSource.featuredRecipes else emptyList()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Search
    // ─────────────────────────────────────────────────────────────────────────

    override suspend fun searchRecipes(query: String, offset: Int, limit: Int): SearchResult {
        if (!apiConfigured || query.isBlank()) {
            if (!sampleFallbackEnabled) return SearchResult(emptyList(), 0)
            val filtered = sampleRecipes.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.category.contains(query, ignoreCase = true) ||
                        it.cuisine.contains(query, ignoreCase = true)
            }
            return SearchResult(filtered, filtered.size)
        }
        return try {
            val response = apiService.searchRecipes(
                query = query,
                number = limit,
                offset = offset
            )
            SearchResult(
                recipes = response.results.map { it.toDomain() },
                totalResults = response.totalResults
            )
        } catch (e: Exception) {
            Log.w(TAG, "Search API failed: ${e.message}")
            SearchResult(emptyList(), 0)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Recipe Details  (Issue #7 — cache on success, serve cache on failure)
    // ─────────────────────────────────────────────────────────────────────────

    override suspend fun getRecipeDetails(recipeId: String): Recipe? {
        if (!apiConfigured) return sampleRecipeById(recipeId)

        val numericId = recipeId.toIntOrNull()
        if (numericId == null) {
            Log.w(TAG, "getRecipeDetails: non-integer ID \"$recipeId\", falling back to sample")
            return sampleRecipeById(recipeId)
        }

        // Cache-first: revisits within 24 h cost zero API points.
        val cached = cachedRecipeDao.getById(recipeId)
        if (cached != null && !cached.isExpired()) {
            Log.d(TAG, "Serving $recipeId from fresh cache (no API call)")
            return cached.toRecipe()
        }

        return try {
            val dto = apiService.getRecipeDetails(recipeId = numericId, includeNutrition = true)
            val recipe = dto.toDomain()
            cachedRecipeDao.upsert(recipe.toCachedEntity())
            Log.d(TAG, "Cached recipe detail for id=$recipeId")
            recipe
        } catch (e: Exception) {
            Log.w(TAG, "Recipe details API failed for id=$recipeId: ${e.message}")
            // API failed — serve stale cache if we have one, else fall through to sample.
            if (cached != null) {
                Log.w(
                    TAG,
                    "Serving $recipeId from STALE cache (>${CachedRecipeEntity.CACHE_MAX_AGE_MS / 3_600_000}h old)"
                )
                cached.toRecipe()
            } else {
                Log.w(TAG, "No offline cache for $recipeId — using sample fallback")
                sampleRecipeById(recipeId)
            }
        }
    }

    private fun sampleRecipeById(id: String): Recipe? =
        if (sampleFallbackEnabled) sampleRecipes.find { it.id == id } else null

    // ─────────────────────────────────────────────────────────────────────────
    // Recipe Video (YouTube)
    // ─────────────────────────────────────────────────────────────────────────

    override suspend fun findRecipeVideoId(recipeName: String): String? {
        // No key → no video; caller falls back to a plain YouTube search.
        if (!apiConfigured) return null
        return try {
            val response = apiService.searchFoodVideos(query = recipeName, number = 1)
            response.videos?.firstOrNull()?.youTubeId?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            // Soft-fail: a missing video should never surface an error to the user.
            Log.w(TAG, "Video search failed for \"$recipeName\": ${e.message}")
            null
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Categories
    // ─────────────────────────────────────────────────────────────────────────

    override suspend fun getCategories(): List<RecipeCategory> =
        CategoryDataSource.categories

    // ─────────────────────────────────────────────────────────────────────────
    // Recipes By Category
    // ─────────────────────────────────────────────────────────────────────────

    override suspend fun getRecipesByCategory(
        categoryId: String,
        limit: Int,
        offset: Int,
        append: Boolean
    ): List<Recipe> {
        val category = CategoryDataSource.getCategoryById(categoryId)
            ?: run {
                Log.w(TAG, "Category not found: $categoryId")
                return emptyList()
            }

        if (!apiConfigured) return sampleRecipesForCategory(category, limit)

        // Evict old cache entries when loading fresh data (first page or pull-to-refresh).
        // Keep the cache bounded to prevent unbounded storage growth.
        if (offset == 0 && !append) {
            try {
                if (cachedRecipeDao.count() > MAX_CACHED_RECIPES) {
                    cachedRecipeDao.evictOldest(EVICT_BATCH_SIZE)
                    Log.d(TAG, "Evicted oldest cache entries (limit: $MAX_CACHED_RECIPES)")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Cache eviction failed: ${e.message}")
            }
        }

        return try {
            val resp = when (category.kind) {
                CategoryKind.CUISINE -> apiService.searchRecipes(
                    query = category.apiQuery,
                    cuisine = category.spoonacularTag,
                    number = limit,
                    offset = offset,
                    addRecipeNutrition = true
                )

                CategoryKind.DISH_TYPE -> apiService.searchRecipes(
                    query = category.apiQuery,
                    type = category.spoonacularTag,
                    number = limit,
                    offset = offset,
                    addRecipeNutrition = true
                )

                CategoryKind.DIETARY -> apiService.searchRecipes(
                    query = category.apiQuery,
                    diet = category.spoonacularTag,
                    number = limit,
                    offset = offset,
                    addRecipeNutrition = true
                )
            }

            val recipes = resp.results.map { it.toDomain() }
            Log.d(
                TAG,
                "Fetched ${recipes.size} recipes for category: ${category.name} (offset=$offset)"
            )
            recipes
        } catch (e: Exception) {
            Log.e(TAG, "Category recipes API failed for $categoryId: ${e.message}")
            sampleRecipesForCategory(category, limit)
        }
    }

    private fun sampleRecipesForCategory(category: RecipeCategory, limit: Int): List<Recipe> {
        if (!sampleFallbackEnabled) return emptyList()
        return sampleRecipes.filter { recipe ->
            when (category.kind) {
                CategoryKind.CUISINE ->
                    recipe.cuisine.equals(category.spoonacularTag, ignoreCase = true) ||
                            recipe.tags.any {
                                it.equals(
                                    category.spoonacularTag,
                                    ignoreCase = true
                                )
                            }

                CategoryKind.DISH_TYPE ->
                    recipe.category.equals(category.spoonacularTag, ignoreCase = true) ||
                            recipe.tags.any {
                                it.equals(
                                    category.spoonacularTag,
                                    ignoreCase = true
                                )
                            } ||
                            recipe.category.equals(category.name, ignoreCase = true)

                CategoryKind.DIETARY -> {
                    when (category.spoonacularTag.lowercase()) {
                        "vegetarian" -> recipe.isVegetarian
                        "vegan" -> recipe.isVegan
                        "gluten free" -> recipe.isGlutenFree
                        "dairy free" -> recipe.isDairyFree
                        "ketogenic" -> recipe.isKeto
                        "low carb" -> recipe.isLowCarb
                        else -> false
                    }
                }
            }
        }.take(limit)
    }

    companion object {
        private const val TAG = "RecipeRepositoryImpl"

        /** Max cached recipes before we evict the oldest 20. */
        private const val MAX_CACHED_RECIPES = 100
        private const val EVICT_BATCH_SIZE = 20
    }
}