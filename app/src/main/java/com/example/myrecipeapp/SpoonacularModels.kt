package com.example.myrecipeapp

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Objects (DTOs) that directly map to the JSON structure
 * returned by the Spoonacular API.
 */

// Response model for the GET /recipes/random endpoint
data class SpoonacularGetRecipesResponse(
    @SerializedName("recipes") val recipes: List<SpoonacularRecipe>
)

// The main recipe model from Spoonacular, used in both list and detail responses
data class SpoonacularRecipe(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("image") val image: String?,
    @SerializedName("servings") val servings: Int,
    @SerializedName("readyInMinutes") val readyInMinutes: Int,
    @SerializedName("healthScore") val healthScore: Float,
    @SerializedName("spoonacularScore") val spoonacularScore: Float,
    @SerializedName("summary") val summary: String,
    @SerializedName("dishTypes") val dishTypes: List<String>,
    @SerializedName("cuisines") val cuisines: List<String>,
    @SerializedName("vegetarian") val isVegetarian: Boolean,
    @SerializedName("vegan") val isVegan: Boolean,
    @SerializedName("glutenFree") val isGlutenFree: Boolean,
    @SerializedName("dairyFree") val isDairyFree: Boolean,
    @SerializedName("extendedIngredients") val ingredients: List<SpoonacularIngredient> = emptyList(),
    @SerializedName("analyzedInstructions") val instructions: List<SpoonacularInstruction> = emptyList()
)

data class SpoonacularIngredient(
    @SerializedName("id") val id: Int,
    @SerializedName("nameClean") val name: String?,
    @SerializedName("amount") val amount: Float,
    @SerializedName("unit") val unit: String,
    // --- THIS LINE IS NOW CORRECTED ---
    @SerializedName("original") val original: String // e.g., "2 cups all-purpose flour"
)

data class SpoonacularInstruction(
    @SerializedName("name") val name: String,
    @SerializedName("steps") val steps: List<SpoonacularStep>
)

data class SpoonacularStep(
    @SerializedName("number") val number: Int,
    @SerializedName("step") val step: String
)

