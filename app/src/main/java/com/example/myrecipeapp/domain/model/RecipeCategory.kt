package com.example.myrecipeapp.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

enum class DietaryFilter {
    ALL, VEGETARIAN, VEGAN, NON_VEG, GLUTEN_FREE, DAIRY_FREE, KETO, LOW_CARB
}

enum class CuisineType {
    INTERNATIONAL, INDIAN, ITALIAN, CONTINENTAL, CHINESE, MEXICAN, THAI, FRENCH, AMERICAN, MEDITERRANEAN
}

@Parcelize
data class RecipeCategory(
    val id: String,
    val name: String,
    val description: String,
    val imageUrl: String,
    val recipeCount: Int = 0,
    val cuisineType: CuisineType = CuisineType.INTERNATIONAL,
    val dietaryTags: List<DietaryFilter> = emptyList(),
    val spoonacularTag: String = ""
) : Parcelable
