package com.example.myrecipeapp.data.remote

import com.example.myrecipeapp.data.remote.dto.SpoonacularGetRecipesResponse
import com.example.myrecipeapp.data.remote.dto.SpoonacularRecipeDto
import com.example.myrecipeapp.data.remote.dto.SpoonacularSearchResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface for the Spoonacular Food API.
 * This lives in the data layer — the domain layer never sees it.
 *
 * NOTE: The API key is injected automatically by [NetworkModule.ApiKeyInterceptor]
 * on every request — no need to pass it here.
 */
interface SpoonacularApiService {

    /**
     * GET /recipes/complexSearch
     * Search recipes by keyword, type, and/or cuisine.
     */
    @GET("recipes/complexSearch")
    suspend fun searchRecipes(
        @Query("query") query: String,
        @Query("type") type: String? = null,
        @Query("cuisine") cuisine: String? = null,
        @Query("number") number: Int = 50,
        @Query("offset") offset: Int = 0,
        @Query("addRecipeInformation") addRecipeInformation: Boolean = true
    ): SpoonacularSearchResponse

    /**
     * GET /recipes/{id}/information
     * Fetch detailed information for a single recipe.
     */
    @GET("recipes/{id}/information")
    suspend fun getRecipeDetails(
        @Path("id") recipeId: Int,
        @Query("includeNutrition") includeNutrition: Boolean = true
    ): SpoonacularRecipeDto

    /**
     * GET /recipes/random
     * Fetch a number of random recipes, used for the featured carousel.
     */
    @GET("recipes/random")
    suspend fun getRandomRecipes(
        @Query("number") number: Int = 5,
        @Query("tags") tags: String? = null
    ): SpoonacularGetRecipesResponse
}
