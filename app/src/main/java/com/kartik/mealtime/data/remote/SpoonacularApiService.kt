package com.kartik.mealtime.data.remote

import com.kartik.mealtime.data.remote.dto.SpoonacularGetRecipesResponse
import com.kartik.mealtime.data.remote.dto.SpoonacularRecipeDto
import com.kartik.mealtime.data.remote.dto.SpoonacularSearchResponse
import com.kartik.mealtime.data.remote.dto.SpoonacularVideoSearchResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface for the Spoonacular Food API.
 * This lives in the data layer — the domain layer never sees it.
 *
 * NOTE: The API key is injected server-side by the Cloudflare Worker proxy
 * (requests are routed through it), so no key is passed here.
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
        @Query("diet") diet: String? = null,
        @Query("number") number: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("addRecipeInformation") addRecipeInformation: Boolean = true,
        // Nutrition data ~triples response size. Off for plain search; callers that
        // render calories (e.g. category browse) must opt in.
        @Query("addRecipeNutrition") addRecipeNutrition: Boolean = false,
        @Query("instructionsRequired") instructionsRequired: Boolean = true
    ): SpoonacularSearchResponse

    /**
     * GET /recipes/{id}/information
     * Fetch detailed information for a single recipe.
     */
    @GET("recipes/{id}/information")
    suspend fun getRecipeDetails(
        @Path("id") recipeId: Int,
        // Nutrition breakdown triples the JSON size — only fetch if you render it
        @Query("includeNutrition") includeNutrition: Boolean = false,
        @Query("fillIngredients") fillIngredients: Boolean = true
    ): SpoonacularRecipeDto

    /**
     * GET /recipes/random
     * Fetch a number of random recipes, used for the featured carousel.
     */
    @GET("recipes/random")
    suspend fun getRandomRecipes(
        @Query("number") number: Int = 5,
        @Query("tags") tags: String? = null,
        // Only return recipes that have cooking steps — prevents empty CookingModeScreen
        @Query("instructionsRequired") instructionsRequired: Boolean = true
    ): SpoonacularGetRecipesResponse

    /**
     * GET /food/videos/search
     * Find recipe videos (sourced from YouTube) matching [query]. The response's
     * youTubeId builds a watch URL. Costs ~1 API point — call only on user action.
     */
    @GET("food/videos/search")
    suspend fun searchFoodVideos(
        @Query("query") query: String,
        @Query("number") number: Int = 1
    ): SpoonacularVideoSearchResponse
}
