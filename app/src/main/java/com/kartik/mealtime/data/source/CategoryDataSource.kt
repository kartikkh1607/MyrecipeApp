package com.kartik.mealtime.data.source

import com.kartik.mealtime.R
import com.kartik.mealtime.domain.model.CategoryKind
import com.kartik.mealtime.domain.model.CuisineType
import com.kartik.mealtime.domain.model.DietaryFilter
import com.kartik.mealtime.domain.model.RecipeCategory

/**
 * Provides the curated list of recipe categories.
 * Images are loaded from local drawables (instant, offline) via [RecipeCategory.imageResId].
 * [RecipeCategory.imageUrl] remains as a remote fallback.
 */
object CategoryDataSource {

    val categories: List<RecipeCategory> = listOf(
        // ── Cuisine-based ──────────────────────────────────────────────────────
        RecipeCategory(
            id = "indian", name = "Indian Cuisine",
            description = "Authentic Indian flavors with aromatic spices and rich curries",
            imageUrl = "https://images.unsplash.com/photo-1631452180519-c014fe946bc7?w=500&h=400&fit=crop",
            imageResId = R.drawable.cat_indian,
            recipeCount = 2850, cuisineType = CuisineType.INDIAN,
            dietaryTags = listOf(
                DietaryFilter.VEGETARIAN,
                DietaryFilter.VEGAN,
                DietaryFilter.NON_VEG
            ),
            spoonacularTag = "indian", kind = CategoryKind.CUISINE
        ),
        RecipeCategory(
            id = "italian", name = "Italian Cuisine",
            description = "Classic Italian dishes from pasta to pizza and risotto",
            imageUrl = "https://images.unsplash.com/photo-1498579397066-22750a3cb424?w=500&h=400&fit=crop",
            imageResId = R.drawable.cat_italian,
            recipeCount = 3240, cuisineType = CuisineType.ITALIAN,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN, DietaryFilter.NON_VEG),
            spoonacularTag = "italian", kind = CategoryKind.CUISINE
        ),
        RecipeCategory(
            id = "continental", name = "Continental",
            description = "European-style dishes with refined flavors and techniques",
            imageUrl = "https://images.unsplash.com/photo-1414235077428-338989a2e8c0?w=500&h=400&fit=crop",
            imageResId = R.drawable.cat_continental,
            recipeCount = 2220, cuisineType = CuisineType.CONTINENTAL,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN, DietaryFilter.NON_VEG),
            spoonacularTag = "european", kind = CategoryKind.CUISINE
        ),
        RecipeCategory(
            id = "chinese", name = "Chinese Cuisine",
            description = "Traditional Chinese dishes with bold flavors and wok cooking",
            imageUrl = "https://images.unsplash.com/photo-1582878826629-29b7ad1cdc43?w=500&h=400&fit=crop",
            imageResId = R.drawable.cat_chinese,
            recipeCount = 1980, cuisineType = CuisineType.CHINESE,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN, DietaryFilter.NON_VEG),
            spoonacularTag = "chinese", kind = CategoryKind.CUISINE
        ),
        RecipeCategory(
            id = "mexican", name = "Mexican Cuisine",
            description = "Vibrant Mexican flavors with fresh ingredients and bold spices",
            imageUrl = "https://images.unsplash.com/photo-1555939594-58d7cb561ad1?w=500&h=400&fit=crop",
            imageResId = R.drawable.cat_mexican,
            recipeCount = 1850, cuisineType = CuisineType.MEXICAN,
            dietaryTags = listOf(
                DietaryFilter.VEGETARIAN,
                DietaryFilter.VEGAN,
                DietaryFilter.NON_VEG
            ),
            spoonacularTag = "mexican", kind = CategoryKind.CUISINE
        ),
        RecipeCategory(
            id = "mediterranean", name = "Mediterranean",
            description = "Healthy Mediterranean diet with olive oil, fresh herbs, and seafood",
            imageUrl = "https://images.unsplash.com/photo-1544982503-9f984c14501a?w=500&h=400&fit=crop",
            imageResId = R.drawable.cat_mediterranean,
            recipeCount = 1760, cuisineType = CuisineType.MEDITERRANEAN,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN, DietaryFilter.NON_VEG),
            spoonacularTag = "mediterranean", kind = CategoryKind.CUISINE
        ),
        // ── Meal type ──────────────────────────────────────────────────────────
        RecipeCategory(
            id = "breakfast", name = "Breakfast",
            description = "Start your day right with these energizing morning meals",
            imageUrl = "https://images.unsplash.com/photo-1493770348161-369560ae357d?w=400",
            imageResId = R.drawable.cat_breakfast,
            recipeCount = 1180, cuisineType = CuisineType.INTERNATIONAL,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN, DietaryFilter.VEGAN),
            spoonacularTag = "breakfast", kind = CategoryKind.DISH_TYPE
        ),
        RecipeCategory(
            id = "lunch", name = "Lunch",
            description = "Satisfying midday meals to fuel your afternoon",
            imageUrl = "https://images.unsplash.com/photo-1567620905732-2d1ec7ab7445?w=400",
            imageResId = R.drawable.cat_lunch,
            recipeCount = 1450, cuisineType = CuisineType.INTERNATIONAL,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN, DietaryFilter.NON_VEG),
            spoonacularTag = "main course", kind = CategoryKind.DISH_TYPE,
            apiQuery = "lunch"
        ),
        RecipeCategory(
            id = "dinner", name = "Dinner",
            description = "Hearty evening meals perfect for sharing with family",
            imageUrl = "https://images.unsplash.com/photo-1484723091739-30a097e8f929?w=400",
            imageResId = R.drawable.cat_dinner,
            recipeCount = 1680, cuisineType = CuisineType.INTERNATIONAL,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN, DietaryFilter.NON_VEG),
            // Both Lunch and Dinner use type="main course" — apiQuery differentiates results.
            spoonacularTag = "main course", kind = CategoryKind.DISH_TYPE,
            apiQuery = "dinner"
        ),
        // ── Dish type ──────────────────────────────────────────────────────────
        RecipeCategory(
            id = "appetizers", name = "Appetizers",
            description = "Delicious starters and small bites to begin your meal",
            imageUrl = "https://images.unsplash.com/photo-1541529086526-db283c563270?w=400",
            imageResId = R.drawable.cat_appetizers,
            recipeCount = 890, cuisineType = CuisineType.INTERNATIONAL,
            dietaryTags = listOf(
                DietaryFilter.VEGETARIAN,
                DietaryFilter.VEGAN,
                DietaryFilter.NON_VEG
            ),
            // "fingerfood" is a valid Spoonacular type alongside "appetizer"
            spoonacularTag = "appetizer,fingerfood", kind = CategoryKind.DISH_TYPE
        ),
        RecipeCategory(
            id = "desserts", name = "Desserts",
            description = "Sweet treats and decadent desserts for any occasion",
            imageUrl = "https://images.unsplash.com/photo-1551024506-0bccd828d307?w=400",
            imageResId = R.drawable.cat_desserts,
            recipeCount = 1120, cuisineType = CuisineType.INTERNATIONAL,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN, DietaryFilter.VEGAN),
            spoonacularTag = "dessert", kind = CategoryKind.DISH_TYPE
        ),
        RecipeCategory(
            id = "snacks", name = "Snacks",
            description = "Quick bites and healthy snack options for any time",
            imageUrl = "https://images.unsplash.com/photo-1621939514649-280e2ee25f60?w=400",
            imageResId = R.drawable.cat_snacks,
            recipeCount = 640, cuisineType = CuisineType.INTERNATIONAL,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN, DietaryFilter.VEGAN),
            // "snack,fingerfood" covers both light snacks and finger foods.
            // apiQuery="snacks" anchors results so irrelevant dishes don't slip through.
            spoonacularTag = "snack,fingerfood", kind = CategoryKind.DISH_TYPE,
            apiQuery = "snacks"
        ),
        // ── Dietary ───────────────────────────────────────────────────────────
        RecipeCategory(
            id = "vegetarian", name = "Vegetarian",
            description = "Delicious plant-based meals with dairy and eggs",
            imageUrl = "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=400",
            imageResId = R.drawable.cat_vegetarian,
            recipeCount = 2200, cuisineType = CuisineType.INTERNATIONAL,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN),
            spoonacularTag = "vegetarian", kind = CategoryKind.DIETARY
        ),
        RecipeCategory(
            id = "vegan", name = "Vegan",
            description = "Completely plant-based recipes with no animal products",
            imageUrl = "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=400",
            imageResId = R.drawable.cat_vegan,
            recipeCount = 1580, cuisineType = CuisineType.INTERNATIONAL,
            dietaryTags = listOf(DietaryFilter.VEGAN),
            spoonacularTag = "vegan", kind = CategoryKind.DIETARY
        ),
        RecipeCategory(
            id = "keto", name = "Keto",
            description = "Low-carb, high-fat recipes perfect for ketogenic diet",
            imageUrl = "https://images.unsplash.com/photo-1490645935967-10de6ba17061?w=400",
            imageResId = R.drawable.cat_keto,
            recipeCount = 750, cuisineType = CuisineType.INTERNATIONAL,
            dietaryTags = listOf(DietaryFilter.KETO, DietaryFilter.LOW_CARB),
            spoonacularTag = "ketogenic", kind = CategoryKind.DIETARY
        ),
        // ── Specialty ─────────────────────────────────────────────────────────
        RecipeCategory(
            id = "soups", name = "Soups & Stews",
            description = "Warming and comforting soups for every season",
            imageUrl = "https://images.unsplash.com/photo-1547592166-23ac45744acd?w=400",
            imageResId = R.drawable.cat_soups,
            recipeCount = 520, cuisineType = CuisineType.INTERNATIONAL,
            dietaryTags = listOf(
                DietaryFilter.VEGETARIAN,
                DietaryFilter.VEGAN,
                DietaryFilter.NON_VEG
            ),
            spoonacularTag = "soup", kind = CategoryKind.DISH_TYPE
        ),
        RecipeCategory(
            id = "beverages", name = "Beverages",
            description = "Refreshing drinks, smoothies, and specialty beverages",
            imageUrl = "https://images.unsplash.com/photo-1544145945-f90425340c7e?w=400",
            imageResId = R.drawable.cat_beverages,
            recipeCount = 420, cuisineType = CuisineType.INTERNATIONAL,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN, DietaryFilter.VEGAN),
            spoonacularTag = "drink", kind = CategoryKind.DISH_TYPE
        )
    )

    fun getCategoryById(id: String): RecipeCategory? = categories.find { it.id == id }
}

