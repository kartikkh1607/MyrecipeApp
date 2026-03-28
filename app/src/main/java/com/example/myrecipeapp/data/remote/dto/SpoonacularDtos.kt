package com.example.myrecipeapp.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Objects (DTOs) that map directly to the Spoonacular API JSON responses.
 * These are strictly data-layer constructs — they never leak into the domain or UI layer.
 */

// Response for GET /recipes/random
data class SpoonacularGetRecipesResponse(
    @SerializedName("recipes") val recipes: List<SpoonacularRecipeDto>
)

// Response for GET /recipes/complexSearch
data class SpoonacularSearchResponse(
    @SerializedName("results") val results: List<SpoonacularRecipeDto>,
    @SerializedName("totalResults") val totalResults: Int
)

// The main recipe DTO — used in both list and detail responses
data class SpoonacularRecipeDto(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("image") val image: String?,
    @SerializedName("servings") val servings: Int,
    @SerializedName("readyInMinutes") val readyInMinutes: Int,
    @SerializedName("healthScore") val healthScore: Float,
    @SerializedName("spoonacularScore") val spoonacularScore: Float,
    @SerializedName("summary") val summary: String,
    @SerializedName("dishTypes") val dishTypes: List<String>?,
    @SerializedName("cuisines") val cuisines: List<String>?,
    @SerializedName("vegetarian") val isVegetarian: Boolean,
    @SerializedName("vegan") val isVegan: Boolean,
    @SerializedName("glutenFree") val isGlutenFree: Boolean,
    @SerializedName("dairyFree") val isDairyFree: Boolean,
    @SerializedName("extendedIngredients") val ingredients: List<SpoonacularIngredientDto>? = emptyList(),
    @SerializedName("analyzedInstructions") val instructions: List<SpoonacularInstructionDto>? = emptyList()
)

data class SpoonacularIngredientDto(
    @SerializedName("id") val id: Int,
    @SerializedName("nameClean") val name: String?,
    @SerializedName("amount") val amount: Float,
    @SerializedName("unit") val unit: String,
    @SerializedName("original") val original: String
)

data class SpoonacularInstructionDto(
    @SerializedName("name") val name: String,
    @SerializedName("steps") val steps: List<SpoonacularStepDto>?
)

data class SpoonacularStepDto(
    @SerializedName("number") val number: Int,
    @SerializedName("step") val step: String
)
