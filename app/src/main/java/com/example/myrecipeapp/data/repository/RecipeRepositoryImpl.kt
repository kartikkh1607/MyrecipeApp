package com.example.myrecipeapp.data.repository

import android.util.Log
import com.example.myrecipeapp.BuildConfig
import com.example.myrecipeapp.data.remote.SpoonacularApiService
import com.example.myrecipeapp.data.remote.dto.SpoonacularRecipeDto
import com.example.myrecipeapp.data.remote.dto.toDomain
import com.example.myrecipeapp.data.source.CategoryDataSource
import com.example.myrecipeapp.data.source.SampleDataSource
import com.example.myrecipeapp.domain.model.*
import com.example.myrecipeapp.domain.repository.RecipeRepository

/**
 * Concrete implementation of [RecipeRepository].
 *
 * All "try API → fallback to sample data" logic that previously lived
 * inside MainViewModel now lives here — in the data layer where it belongs.
 */
class RecipeRepositoryImpl(
    private val apiService: SpoonacularApiService
) : RecipeRepository {

    // Only needed to decide whether to call the API or fall back to sample data.
    // The actual key injection is handled by NetworkModule.ApiKeyInterceptor.
    private val apiKey: String = BuildConfig.SPOONACULAR_API_KEY
    private val apiConfigured: Boolean = apiKey.isNotEmpty() && apiKey != "null"

    // Cached once — maps from the SampleDataSource.featuredRecipes val (also built once)
    private val sampleRecipes: List<Recipe> by lazy {
        SampleDataSource.featuredRecipes.map { it.recipe }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Featured Recipes
    // ─────────────────────────────────────────────────────────────────────────

    override suspend fun getFeaturedRecipes(): List<FeaturedRecipe> {
        if (!apiConfigured) {
            Log.d(TAG, "API not configured — using sample featured recipes")
            return SampleDataSource.featuredRecipes
        }
        return try {
            Log.d(TAG, "Fetching featured recipes from Spoonacular")
            val response = apiService.getRandomRecipes(number = 10)
            response.recipes.mapIndexed { index, dto ->
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
        } catch (e: Exception) {
            Log.w(TAG, "Featured recipes API failed, using sample data: ${e.message}")
            SampleDataSource.featuredRecipes
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Search
    // ─────────────────────────────────────────────────────────────────────────

    override suspend fun searchRecipes(query: String, offset: Int, limit: Int): SearchResult {
        if (!apiConfigured || query.isBlank()) {
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
    // Recipe Details
    // ─────────────────────────────────────────────────────────────────────────

    override suspend fun getRecipeDetails(recipeId: String): Recipe? {
        if (!apiConfigured) return sampleRecipeById(recipeId)
        return try {
            val dto = apiService.getRecipeDetails(recipeId = recipeId.toInt())
            dto.toDomain()
        } catch (e: Exception) {
            Log.w(TAG, "Recipe details API failed, using sample: ${e.message}")
            sampleRecipeById(recipeId)
        }
    }

    private fun sampleRecipeById(id: String): Recipe? = sampleRecipes.find { it.id == id }

    // ─────────────────────────────────────────────────────────────────────────
    // Categories
    // ─────────────────────────────────────────────────────────────────────────

    override suspend fun getCategories(): List<RecipeCategory> =
        CategoryDataSource.categories

    // ─────────────────────────────────────────────────────────────────────────
    // Recipes By Category
    // ─────────────────────────────────────────────────────────────────────────

    override suspend fun getRecipesByCategory(categoryId: String, limit: Int): List<Recipe> {
        val category = CategoryDataSource.getCategoryById(categoryId)
            ?: run {
                Log.w(TAG, "Category not found: $categoryId")
                return emptyList()
            }

        if (!apiConfigured) return sampleRecipesForCategory(category, limit)

        return try {
            val aggregated = mutableListOf<SpoonacularRecipeDto>()
            var offset = 0
            var total = Int.MAX_VALUE

            while (aggregated.size < total && aggregated.size < limit) {
                val resp = if (category.cuisineType == CuisineType.INTERNATIONAL)
                    apiService.searchRecipes(query = "", type = category.spoonacularTag, number = PAGE_SIZE, offset = offset)
                else
                    apiService.searchRecipes(query = "", cuisine = category.spoonacularTag, number = PAGE_SIZE, offset = offset)

                if (total == Int.MAX_VALUE) total = resp.totalResults
                if (resp.results.isEmpty()) break
                aggregated += resp.results
                offset += PAGE_SIZE
            }

            val recipes = aggregated.take(minOf(total, limit)).map { it.toDomain() }
            Log.d(TAG, "Fetched ${recipes.size} recipes for category: ${category.name}")
            recipes
        } catch (e: Exception) {
            Log.e(TAG, "Category recipes API failed for $categoryId: ${e.message}")
            sampleRecipesForCategory(category, limit)
        }
    }

    private fun sampleRecipesForCategory(category: RecipeCategory, limit: Int): List<Recipe> =
        sampleRecipes
            .filter {
                it.category.equals(category.name, ignoreCase = true) ||
                it.cuisine.equals(category.name, ignoreCase = true)
            }
            .take(limit)

    companion object {
        private const val TAG = "RecipeRepositoryImpl"
        private const val PAGE_SIZE = 100
    }
}