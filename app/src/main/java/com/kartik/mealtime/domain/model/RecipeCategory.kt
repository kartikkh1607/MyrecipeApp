package com.kartik.mealtime.domain.model

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize

enum class DietaryFilter {
    ALL, VEGETARIAN, VEGAN, NON_VEG, GLUTEN_FREE, DAIRY_FREE, KETO, LOW_CARB
}

/**
 * Controls how [RecipeCategory.spoonacularTag] is used when calling the API:
 * - CUISINE  → passed as `cuisine` param  (e.g. "indian", "italian")
 * - DISH_TYPE → passed as `type` param    (e.g. "breakfast", "dessert", "soup")
 * - DIETARY  → passed as `diet` param     (e.g. "vegan", "vegetarian", "ketogenic")
 */
enum class CategoryKind { CUISINE, DISH_TYPE, DIETARY }

enum class CuisineType {
    INTERNATIONAL, INDIAN, ITALIAN, CONTINENTAL, CHINESE, MEXICAN, THAI, FRENCH, AMERICAN, MEDITERRANEAN
}

@Immutable
@Parcelize
data class RecipeCategory(
    val id: String,
    val name: String,
    val description: String,
    val imageUrl: String,
    val recipeCount: Int = 0,
    val cuisineType: CuisineType = CuisineType.INTERNATIONAL,
    val dietaryTags: List<DietaryFilter> = emptyList(),
    val spoonacularTag: String = "",
    /**
     * Drawable resource id for a bundled category image; when > 0 the UI loads it
     * locally (instant, offline) and falls back to [imageUrl] otherwise. Kept as a
     * plain Int (no @DrawableRes) so the domain model carries no Android-resource
     * annotation; the UI layer validates/uses it as a drawable id.
     */
    val imageResId: Int = 0,
    /** Determines which Spoonacular query param receives [spoonacularTag]. */
    val kind: CategoryKind = CategoryKind.DISH_TYPE,
    /**
     * Optional keyword passed as `query=` alongside the type/cuisine/diet param.
     * Use this to narrow results when the type alone returns irrelevant dishes
     * (e.g. Dinner and Lunch both map to type="main course" but need separate keywords).
     */
    val apiQuery: String = ""
) : Parcelable
