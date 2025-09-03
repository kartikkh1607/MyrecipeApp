package com.example.myrecipeapp

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// Base URL for the Spoonacular API
private const val SPOONACULAR_BASE_URL = "https://api.spoonacular.com/"

// API Key is now securely stored in BuildConfig from local.properties
// The API key is read from local.properties file which is NOT committed to version control
// Get your free API key at: https://spoonacular.com/food-api
private val API_KEY = BuildConfig.SPOONACULAR_API_KEY

// Retrofit instance for the Spoonacular API
private val retrofit = Retrofit.Builder()
    .baseUrl(SPOONACULAR_BASE_URL)
    .addConverterFactory(GsonConverterFactory.create())
    .build()

// Service instance that the ViewModel will use
val spoonacularApiService: SpoonacularApiService = retrofit.create(SpoonacularApiService::class.java)

interface SpoonacularApiService {

    /**
     * Searches for recipes based on various criteria.
     * Corresponds to Spoonacular's /recipes/complexSearch endpoint.
     */
    @GET("recipes/complexSearch")
    suspend fun searchRecipes(
        @Query("query") query: String,
        @Query("type") type: String? = null, // e.g., "main course", maps to our category
        @Query("number") number: Int = 20,
        @Query("addRecipeInformation") addRecipeInformation: Boolean = true, // To get detailed info
        @Query("apiKey") apiKey: String = API_KEY
    ): SpoonacularSearchResponse

    /**
     * Gets detailed information for a single recipe by its ID.
     * Corresponds to Spoonacular's /recipes/{id}/information endpoint.
     */
    @GET("recipes/{id}/information")
    suspend fun getRecipeDetails(
        @Path("id") recipeId: Int,
        @Query("includeNutrition") includeNutrition: Boolean = true,
        @Query("apiKey") apiKey: String = API_KEY
    ): SpoonacularRecipe // This will return a single recipe object

    /**
     * Gets a number of random recipes, often used for a "featured" section.
     * Corresponds to Spoonacular's /recipes/random endpoint.
     */
    @GET("recipes/random")
    suspend fun getRandomRecipes(
        @Query("number") number: Int = 5,
        @Query("tags") tags: String? = null, // e.g., "vegetarian,dessert"
        @Query("apiKey") apiKey: String = API_KEY
    ): SpoonacularGetRecipesResponse
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
