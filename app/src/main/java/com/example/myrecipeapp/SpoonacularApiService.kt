package com.example.myrecipeapp

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Spoonacular API Service
 * 
 * NOTE: You'll need to replace "YOUR_API_KEY" with your actual Spoonacular API key
 * Get your free API key at: https://spoonacular.com/food-api
 * 
 * Free tier allows 150 requests per day
 */

private const val SPOONACULAR_API_KEY = "YOUR_API_KEY_HERE" // Replace with your actual API key
private const val BASE_URL = "https://api.spoonacular.com/"

interface SpoonacularApiService {
    
    /**
     * Get random recipes
     * @param number Number of recipes to fetch (max 100)
     * @param tags Optional tags to filter recipes (e.g., "vegetarian,dessert")
     * @return List of random recipes
     */
    @GET("recipes/random")
    suspend fun getRandomRecipes(
        @Query("number") number: Int = 5,
        @Query("tags") tags: String? = null,
        @Query("apiKey") apiKey: String = SPOONACULAR_API_KEY
    ): SpoonacularGetRecipesResponse

    /**
     * Get detailed recipe information by ID
     * @param id Recipe ID
     * @param includeNutrition Whether to include nutrition information
     * @return Detailed recipe information
     */
    @GET("recipes/{id}/information")
    suspend fun getRecipeInformation(
        @Path("id") id: Int,
        @Query("includeNutrition") includeNutrition: Boolean = true,
        @Query("apiKey") apiKey: String = SPOONACULAR_API_KEY
    ): SpoonacularRecipe

    /**
     * Search recipes
     * @param query Search query
     * @param number Number of results (max 100)
     * @param cuisine Cuisine type filter
     * @param diet Diet filter (e.g., "vegetarian", "vegan")
     * @param intolerances Intolerances filter (e.g., "gluten", "dairy")
     * @param type Meal type (e.g., "main course", "dessert")
     * @param maxReadyTime Maximum ready time in minutes
     * @return Search results
     */
    @GET("recipes/complexSearch")
    suspend fun searchRecipes(
        @Query("query") query: String,
        @Query("number") number: Int = 10,
        @Query("cuisine") cuisine: String? = null,
        @Query("diet") diet: String? = null,
        @Query("intolerances") intolerances: String? = null,
        @Query("type") type: String? = null,
        @Query("maxReadyTime") maxReadyTime: Int? = null,
        @Query("addRecipeInformation") addRecipeInformation: Boolean = true,
        @Query("fillIngredients") fillIngredients: Boolean = true,
        @Query("apiKey") apiKey: String = SPOONACULAR_API_KEY
    ): SpoonacularSearchResponse
}

/**
 * Search response model
 */
data class SpoonacularSearchResponse(
    val results: List<SpoonacularRecipe>,
    val offset: Int,
    val number: Int,
    val totalResults: Int
)

// Retrofit instance for Spoonacular API
private val spoonacularRetrofit = Retrofit.Builder()
    .baseUrl(BASE_URL)
    .addConverterFactory(GsonConverterFactory.create())
    .build()

// Service instance
val spoonacularApiService = spoonacularRetrofit.create(SpoonacularApiService::class.java)
