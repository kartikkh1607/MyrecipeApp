package com.example.myrecipeapp.data.remote.dto

import android.text.Html
import com.example.myrecipeapp.domain.model.*
import java.util.Locale

/**
 * Extension functions that map Spoonacular DTOs → domain models.
 * These live in the data layer — domain models know nothing about DTOs.
 */

fun SpoonacularRecipeDto.toDomain(): Recipe {
    val rating = (this.healthScore / 20.0f).coerceIn(0.0f, 5.0f)
    val prepTime = (this.readyInMinutes * 0.4).toInt()
    val cookTime = (this.readyInMinutes * 0.6).toInt()
    val cleanSummary = Html.fromHtml(
        this.summary.take(500),  // cap before parsing — Spoonacular summaries can be 2000+ chars
        Html.FROM_HTML_MODE_LEGACY
    ).toString().trim()
    val difficulty = when {
        this.readyInMinutes < 20 -> RecipeDifficulty.EASY
        this.readyInMinutes < 45 -> RecipeDifficulty.MEDIUM
        else -> RecipeDifficulty.HARD
    }
    return Recipe(
        id = this.id.toString(),
        name = this.title,
        description = cleanSummary,
        imageUrl = this.image ?: "https://placehold.co/600x400?text=No+Image",
        category = this.dishTypes?.firstOrNull() ?: "General",
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
        tags = this.dishTypes ?: emptyList(),
        isVegetarian = this.isVegetarian,
        isVegan = this.isVegan,
        isGlutenFree = this.isGlutenFree,
        isDairyFree = this.isDairyFree
    )
}

fun SpoonacularIngredientDto.toDomain(): Ingredient = Ingredient(
    id = this.id.toString(),
    name = this.name ?: this.original,
    amount = String.format(Locale.ROOT, "%.1f", this.amount),  // Locale.ROOT prevents "1,50" in DE/FR locales
    unit = this.unit,
    notes = this.original
)

fun SpoonacularStepDto.toDomain(): RecipeStep = RecipeStep(
    stepNumber = this.number,
    instruction = this.step
)
