package com.example.myrecipeapp.domain.repository

import com.example.myrecipeapp.domain.model.FeaturedRecipe
import com.example.myrecipeapp.domain.model.Recipe
import com.example.myrecipeapp.domain.model.RecipeCategory
import com.example.myrecipeapp.domain.model.SearchResult

/**
 * Pure interface describing all data operations available to the domain layer.
 * The presentation layer depends only on this contract — never on the implementation.
 */
interface RecipeRepository {

    /**
     * Returns featured recipes for the home carousel.
     *
     * Served from the Room cache when fresh; falls back to sample data when the
     * API is unavailable. Pass [forceRefresh] = true (e.g. pull-to-refresh) to
     * bypass the cache and always hit the network.
     */
    suspend fun getFeaturedRecipes(forceRefresh: Boolean = false): List<FeaturedRecipe>

    /**
     * Returns a page of search results for [query].
     * Falls back to filtered sample data when the API is unavailable.
     */
    suspend fun searchRecipes(query: String, offset: Int = 0, limit: Int = 20): SearchResult

    /**
     * Returns detailed information for a single recipe identified by [recipeId].
     * Returns null when neither the API nor fallback data can supply the recipe.
     */
    suspend fun getRecipeDetails(recipeId: String): Recipe?

    /**
     * Returns the curated list of recipe categories.
     */
    suspend fun getCategories(): List<RecipeCategory>

    /**
     * Returns recipes belonging to [categoryId] up to [limit] results.
     */
    suspend fun getRecipesByCategory(categoryId: String, limit: Int = 20): List<Recipe>
}
