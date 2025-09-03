package com.example.myrecipeapp

import android.text.Html

/**
 * The crucial adapter layer. These extension functions convert the raw Spoonacular API models
 * into the clean, consistent domain models that our app's UI uses.
 * This decouples the UI from the API's specific data structure.
 */

/**
 * Converts a raw [SpoonacularRecipe] from the API into our app's clean [Recipe] model.
 * This is where we apply business logic, like cleaning text and calculating fields.
 */
fun SpoonacularRecipe.toRecipe(): Recipe {
    // Convert Spoonacular's health score (0-100) to our app's rating (0-5)
    val rating = (this.healthScore / 20.0f).coerceIn(0.0f, 5.0f)

    // A simple heuristic to split the total time into prep and cook time
    val prepTime = (this.readyInMinutes * 0.4).toInt()
    val cookTime = (this.readyInMinutes * 0.6).toInt()

    // The summary from the API often contains HTML tags. We need to remove them.
    val cleanSummary = Html.fromHtml(this.summary, Html.FROM_HTML_MODE_LEGACY).toString()

    // Determine the difficulty based on the total time
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
        category = this.dishTypes.firstOrNull() ?: "General",
        cuisine = this.cuisines.firstOrNull() ?: "",
        difficulty = difficulty,
        prepTime = prepTime,
        cookTime = cookTime,
        servings = this.servings,
        rating = rating,
        reviewCount = (this.spoonacularScore * 2.5).toInt(), // A rough approximation for demo purposes
        ingredients = this.ingredients.map { it.toIngredient() },
        instructions = this.instructions.flatMap { it.steps }.map { it.toRecipeStep() },
        tags = this.dishTypes,
        isVegetarian = this.isVegetarian,
        isVegan = this.isVegan,
        isGlutenFree = this.isGlutenFree,
        isDairyFree = this.isDairyFree
        // Other fields like nutritionInfo can be mapped here if needed
    )
}

/**
 * Converts a [SpoonacularIngredient] into our app's [Ingredient] model.
 */
fun SpoonacularIngredient.toIngredient(): Ingredient {
    return Ingredient(
        id = this.id.toString(),
        // Use the 'nameClean' field if available, otherwise fallback to the original string
        name = this.name ?: this.original,
        amount = String.format("%.2f", this.amount), // Format amount nicely
        unit = this.unit,
        notes = this.original // The original string is a good note, e.g., "finely chopped"
    )
}

/**
 * Converts a [SpoonacularStep] into our app's [RecipeStep] model.
 */
fun SpoonacularStep.toRecipeStep(): RecipeStep {
    return RecipeStep(
        stepNumber = this.number,
        instruction = this.step
    )
}

