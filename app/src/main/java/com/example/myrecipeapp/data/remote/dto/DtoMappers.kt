package com.example.myrecipeapp.data.remote.dto

import com.example.myrecipeapp.domain.model.*
import java.util.Locale

/**
 * Extension functions that map Spoonacular DTOs → domain models.
 * These live in the data layer — domain models know nothing about DTOs.
 */

// Strips HTML tags without creating a Spanned object — faster than Html.fromHtml
// and avoids importing android.text.Html into the data layer.
private val HTML_TAG_REGEX = Regex("<[^>]*>")

fun SpoonacularRecipeDto.toDomain(): Recipe {
    val rating = (this.healthScore / 20.0f).coerceIn(0.0f, 5.0f)
    val prepTime = (this.readyInMinutes * 0.4).toInt()
    val cookTime = (this.readyInMinutes * 0.6).toInt()

    // Strip HTML tags with regex — avoids android.text.Html allocation overhead
    val cleanSummary = this.summary.orEmpty().take(500).replace(HTML_TAG_REGEX, "").trim()

    val difficulty = when {
        this.readyInMinutes < 20 -> RecipeDifficulty.EASY
        this.readyInMinutes < 45 -> RecipeDifficulty.MEDIUM
        else -> RecipeDifficulty.HARD
    }

    // Cache dishTypes once — used for both category and tags below
    val dishTypes = this.dishTypes.orEmpty()

    // Map nutrition once — reused for both nutritionInfo and calories fields below
    val nutrition = this.nutrition?.toDomain()

    return Recipe(
        id = this.id.toString(),
        name = this.title,
        description = cleanSummary,
        imageUrl = this.image ?: "https://placehold.co/600x400?text=No+Image",
        category = dishTypes.firstOrNull() ?: "General",
        cuisine = this.cuisines?.firstOrNull() ?: "",
        difficulty = difficulty,
        prepTime = prepTime,
        cookTime = cookTime,
        servings = this.servings,
        rating = rating,
        reviewCount = (this.spoonacularScore * 2.5).toInt(),
        ingredients = this.ingredients?.map { it.toDomain() } ?: emptyList(),
        instructions = this.instructions
            ?.flatMap { it.steps ?: emptyList() }
            ?.map { it.toDomain() } ?: emptyList(),
        tags = dishTypes,
        isVegetarian = this.isVegetarian,
        isVegan = this.isVegan,
        isGlutenFree = this.isGlutenFree,
        isDairyFree = this.isDairyFree,
        nutritionInfo = nutrition,
        calories = nutrition?.calories
    )
}

fun SpoonacularIngredientDto.toDomain(): Ingredient = Ingredient(
    id = this.id.toString(),
    name = this.name ?: this.original,
    amount = String.format(Locale.ROOT, "%.1f", this.amount),  // Locale.ROOT avoids "1,50" in DE/FR
    unit = this.unit
)

fun SpoonacularStepDto.toDomain(): RecipeStep = RecipeStep(
    stepNumber = this.number,
    instruction = this.step
)

// ── Nutrition mapping ─────────────────────────────────────────────────────────

/**
 * Maps the Spoonacular flat nutrient list → [NutritionInfo].
 *
 * The API returns a list like:
 *   [{ name: "Calories", amount: 320, unit: "kcal" }, { name: "Protein", ... }, ...]
 * We look up each nutrient by name (case-insensitive) and default to 0 if absent.
 */
fun SpoonacularNutritionDto.toDomain(): NutritionInfo {
    val map = this.nutrients
        ?.associate { it.name.lowercase() to it.amount }
        ?: emptyMap()

    return NutritionInfo(
        calories = map["calories"]?.toInt() ?: 0,
        protein  = map["protein"] ?: 0f,
        carbs    = map["carbohydrates"] ?: 0f,
        fat      = map["fat"] ?: 0f,
        fiber    = map["fiber"] ?: 0f,
        sugar    = map["sugar"] ?: 0f,
        sodium   = map["sodium"] ?: 0f
    )
}
