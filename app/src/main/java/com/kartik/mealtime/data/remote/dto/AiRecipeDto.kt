package com.kartik.mealtime.data.remote.dto

import com.kartik.mealtime.domain.model.Ingredient
import com.kartik.mealtime.domain.model.NutritionInfo
import com.kartik.mealtime.domain.model.Recipe
import com.kartik.mealtime.domain.model.RecipeDifficulty
import com.kartik.mealtime.domain.model.RecipeStep
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Wire shape the AI is instructed to emit for a generated/transformed recipe.
 *
 * Every field is nullable because we cannot trust a language model to populate
 * them all — [toRecipeOrNull] applies sane defaults and rejects responses that
 * are missing the essentials (a name plus either ingredients or steps).
 *
 * Kept separate from [Recipe] so the model's JSON contract can drift without
 * touching the domain model. The shape mirrors what [com.kartik.mealtime.data.remote.AiPrompts]
 * asks the model to produce.
 */
@Serializable
data class AiRecipeDto(
    val name: String? = null,
    val description: String? = null,
    val category: String? = null,
    val cuisine: String? = null,
    val difficulty: String? = null,                    // EASY | MEDIUM | HARD
    val prepTimeMinutes: Int? = null,
    val cookTimeMinutes: Int? = null,
    val servings: Int? = null,
    val calories: Int? = null,
    val ingredients: List<AiIngredientDto>? = null,
    val instructions: List<AiStepDto>? = null,
    val tags: List<String>? = null,
    val nutrition: AiNutritionDto? = null,
    val isVegetarian: Boolean? = null,
    val isVegan: Boolean? = null,
    val isGlutenFree: Boolean? = null,
    val isDairyFree: Boolean? = null,
    val isKeto: Boolean? = null,
    val isLowCarb: Boolean? = null,
)

@Serializable
data class AiIngredientDto(
    val name: String? = null,
    val amount: String? = null,
    val unit: String? = null,
)

@Serializable
data class AiStepDto(
    val step: Int? = null,
    val instruction: String? = null,
    val tip: String? = null,
)

@Serializable
data class AiNutritionDto(
    val calories: Int? = null,
    val protein: Float? = null,
    val carbs: Float? = null,
    val fat: Float? = null,
    val fiber: Float? = null,
    val sugar: Float? = null,
    val sodium: Float? = null,
)

/**
 * Maps the model output into a domain [Recipe], or returns null when the response
 * is too incomplete to be useful. AI recipes get an `ai-` id so the rest of the
 * app can recognise and route them locally (no API/network lookup).
 *
 * [imageUrl] is intentionally blank — AI recipes have no photo; the UI renders a
 * generated placeholder for blank image URLs.
 */
fun AiRecipeDto.toRecipeOrNull(idPrefix: String = "ai-"): Recipe? {
    val cleanName = name?.trim().orEmpty()
    val mappedIngredients = ingredients.orEmpty().mapNotNull { it.toIngredientOrNull() }
    // Renumber sequentially — the model's own step numbers are unreliable.
    val mappedSteps = instructions.orEmpty()
        .mapNotNull { it.toStepOrNull() }
        .mapIndexed { index, step -> step.copy(stepNumber = index + 1) }

    // Reject junk: we need a name and at least some substance to it.
    if (cleanName.isBlank()) return null
    if (mappedIngredients.isEmpty() && mappedSteps.isEmpty()) return null

    return Recipe(
        id = "$idPrefix${UUID.randomUUID()}",
        name = cleanName,
        description = description?.trim().orEmpty(),
        imageUrl = "",
        category = category?.trim().orEmpty().ifBlank { "AI" },
        cuisine = cuisine?.trim().orEmpty(),
        difficulty = parseDifficulty(difficulty),
        prepTime = prepTimeMinutes?.coerceAtLeast(0) ?: 0,
        cookTime = cookTimeMinutes?.coerceAtLeast(0) ?: 0,
        servings = servings?.coerceIn(1, 50) ?: 4,
        calories = calories?.takeIf { it > 0 } ?: nutrition?.calories?.takeIf { it > 0 },
        ingredients = mappedIngredients,
        instructions = mappedSteps,
        tags = tags.orEmpty().mapNotNull { it.trim().ifBlank { null } },
        nutritionInfo = nutrition?.toNutritionInfoOrNull(),
        isVegetarian = isVegetarian ?: false,
        isVegan = isVegan ?: false,
        isGlutenFree = isGlutenFree ?: false,
        isDairyFree = isDairyFree ?: false,
        isKeto = isKeto ?: false,
        isLowCarb = isLowCarb ?: false,
    )
}

private fun AiIngredientDto.toIngredientOrNull(): Ingredient? {
    val n = name?.trim().orEmpty()
    if (n.isBlank()) return null
    return Ingredient(
        id = "",
        name = n,
        amount = amount?.trim().orEmpty(),
        unit = unit?.trim().orEmpty(),
    )
}

private fun AiStepDto.toStepOrNull(): RecipeStep? {
    val text = instruction?.trim().orEmpty()
    if (text.isBlank()) return null
    return RecipeStep(
        stepNumber = step?.takeIf { it > 0 } ?: 0,  // renumbered below
        instruction = text,
        tips = tip?.trim()?.ifBlank { null },
    )
}

private fun AiNutritionDto.toNutritionInfoOrNull(): NutritionInfo? {
    // Require at least calories to consider the block meaningful.
    val cals = calories?.takeIf { it > 0 } ?: return null
    return NutritionInfo(
        calories = cals,
        protein = protein ?: 0f,
        carbs = carbs ?: 0f,
        fat = fat ?: 0f,
        fiber = fiber ?: 0f,
        sugar = sugar ?: 0f,
        sodium = sodium ?: 0f,
    )
}

private fun parseDifficulty(raw: String?): RecipeDifficulty =
    runCatching { RecipeDifficulty.valueOf(raw!!.trim().uppercase()) }
        .getOrDefault(RecipeDifficulty.MEDIUM)
