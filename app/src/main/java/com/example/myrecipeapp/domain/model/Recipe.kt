package com.example.myrecipeapp.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Recipe(
    val id: String,
    val name: String,
    val description: String,
    val imageUrl: String,
    val category: String,
    val cuisine: String = "",
    val difficulty: RecipeDifficulty = RecipeDifficulty.MEDIUM,
    val prepTime: Int = 0, // in minutes
    val cookTime: Int = 0, // in minutes
    val servings: Int = 4,
    val rating: Float = 0f,
    val reviewCount: Int = 0,
    val calories: Int? = null,
    val ingredients: List<Ingredient> = emptyList(),
    val instructions: List<RecipeStep> = emptyList(),
    val tags: List<String> = emptyList(),
    val nutritionInfo: NutritionInfo? = null,
    val videoUrl: String? = null,
    val isVegetarian: Boolean = false,
    val isVegan: Boolean = false,
    val isGlutenFree: Boolean = false,
    val isDairyFree: Boolean = false,
    val isKeto: Boolean = false,
    val isLowCarb: Boolean = false
) : Parcelable

@Parcelize
data class Ingredient(
    val id: String = "",
    val name: String,
    val amount: String,
    val unit: String = "",
    val isOptional: Boolean = false
) : Parcelable

@Parcelize
data class RecipeStep(
    val stepNumber: Int,
    val instruction: String,
    val duration: Int? = null,
    val tips: String? = null
) : Parcelable

@Parcelize
data class NutritionInfo(
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val fiber: Float,
    val sugar: Float,
    val sodium: Float
) : Parcelable

enum class RecipeDifficulty {
    EASY, MEDIUM, HARD;

    fun displayName(): String = when (this) {
        EASY -> "Easy"
        MEDIUM -> "Medium"
        HARD -> "Hard"
    }

    fun emoji(): String = when (this) {
        EASY -> "😊"
        MEDIUM -> "🤔"
        HARD -> "😤"
    }
}

// Featured recipe types for carousel
enum class FeaturedType {
    RECIPE_OF_THE_DAY,
    POPULAR_THIS_WEEK,
    QUICK_MEALS
}

@Parcelize
data class FeaturedRecipe(
    val recipe: Recipe,
    val type: FeaturedType,
    val subtitle: String = "",
    val badgeText: String = "",
    val gradientColors: List<String> = emptyList()
) : Parcelable

/** Wraps paged search results from the API */
data class SearchResult(
    val recipes: List<Recipe>,
    val totalResults: Int
)

