package com.kartik.mealtime.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data Transfer Objects (DTOs) that map directly to the Spoonacular API JSON responses.
 * These are strictly data-layer constructs — they never leak into the domain or UI layer.
 *
 * Deserialised by kotlinx.serialization through Retrofit's converter (see
 * [com.kartik.mealtime.di.NetworkModule]). `@SerialName` is only needed where the
 * Kotlin field name diverges from the wire key.
 */

// Response for GET /recipes/random
@Serializable
data class SpoonacularGetRecipesResponse(
    val recipes: List<SpoonacularRecipeDto>
)

// Response for GET /recipes/complexSearch
@Serializable
data class SpoonacularSearchResponse(
    val results: List<SpoonacularRecipeDto>,
    val totalResults: Int
)

// Response for GET /food/videos/search — recipe videos sourced from YouTube
@Serializable
data class SpoonacularVideoSearchResponse(
    val videos: List<SpoonacularVideoDto>? = emptyList()
)

@Serializable
data class SpoonacularVideoDto(
    val title: String? = null,
    // YouTube video id — build a watch URL as https://www.youtube.com/watch?v=<id>
    val youTubeId: String? = null
)

// The main recipe DTO — used in both list and detail responses
@Serializable
data class SpoonacularRecipeDto(
    val id: Int,
    val title: String,
    val image: String? = null,
    val servings: Int = 0,
    val readyInMinutes: Int = 0,
    val healthScore: Float = 0f,
    val spoonacularScore: Float = 0f,
    val summary: String? = null,   // nullable — not guaranteed by API
    val dishTypes: List<String>? = null,
    val cuisines: List<String>? = null,
    @SerialName("vegetarian") val isVegetarian: Boolean = false,
    @SerialName("vegan") val isVegan: Boolean = false,
    @SerialName("glutenFree") val isGlutenFree: Boolean = false,
    @SerialName("dairyFree") val isDairyFree: Boolean = false,
    @SerialName("extendedIngredients") val ingredients: List<SpoonacularIngredientDto>? = emptyList(),
    @SerialName("analyzedInstructions") val instructions: List<SpoonacularInstructionDto>? = emptyList(),
    // Nutrition data — populated when includeNutrition=true is passed to getRecipeDetails
    val nutrition: SpoonacularNutritionDto? = null,
    // YouTube video URL if Spoonacular has an associated video for this recipe
    val videoUrl: String? = null
)

@Serializable
data class SpoonacularIngredientDto(
    val id: Int = 0,
    @SerialName("nameClean") val name: String? = null,
    val amount: Float = 0f,
    val unit: String = "",
    val original: String = ""
)

@Serializable
data class SpoonacularInstructionDto(
    val name: String = "",
    val steps: List<SpoonacularStepDto>? = null
)

@Serializable
data class SpoonacularStepDto(
    val number: Int = 0,
    val step: String = ""
)

// ── Nutrition DTOs ────────────────────────────────────────────────────────────

/**
 * Top-level nutrition object returned by Spoonacular when includeNutrition=true.
 * The API returns a flat list of named nutrients rather than named fields.
 */
@Serializable
data class SpoonacularNutritionDto(
    val nutrients: List<SpoonacularNutrientDto>? = null
)

@Serializable
data class SpoonacularNutrientDto(
    val name: String = "",
    val amount: Float = 0f,
    val unit: String = ""
)
