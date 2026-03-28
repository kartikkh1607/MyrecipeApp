package com.example.myrecipeapp.data.source

import com.example.myrecipeapp.domain.model.CuisineType
import com.example.myrecipeapp.domain.model.DietaryFilter
import com.example.myrecipeapp.domain.model.RecipeCategory

/**
 * Provides the curated list of recipe categories.
 * Previously the `RecipeCategorySamples` object in SpoonacularCategories.kt.
 */
object CategoryDataSource {

    /**
     * Built once when CategoryDataSource is first accessed.
     * Previously a fun — rebuilding all 15 RecipeCategory objects on every call
     * (and getCategoryById called getCategories() internally, doubling the cost).
     */
    val categories: List<RecipeCategory> = listOf(
        // CUISINE-BASED CATEGORIES
        RecipeCategory(
            id = "indian", name = "Indian Cuisine",
            description = "Authentic Indian flavors with aromatic spices and rich curries",
            imageUrl = "https://images.unsplash.com/photo-1631452180519-c014fe946bc7?w=500&h=400&fit=crop",
            recipeCount = 2850, cuisineType = CuisineType.INDIAN,
            dietaryTags = listOf(
                DietaryFilter.VEGETARIAN,
                DietaryFilter.VEGAN,
                DietaryFilter.NON_VEG
            ),
            spoonacularTag = "indian"
        ),
        RecipeCategory(
            id = "italian", name = "Italian Cuisine",
            description = "Classic Italian dishes from pasta to pizza and risotto",
            imageUrl = "https://images.unsplash.com/photo-1498579397066-22750a3cb424?w=500&h=400&fit=crop",
            recipeCount = 3240, cuisineType = CuisineType.ITALIAN,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN, DietaryFilter.NON_VEG),
            spoonacularTag = "italian"
        ),
        RecipeCategory(
            id = "continental", name = "Continental",
            description = "European-style dishes with refined flavors and techniques",
            imageUrl = "https://images.unsplash.com/photo-1414235077428-338989a2e8c0?w=500&h=400&fit=crop",
            recipeCount = 2220, cuisineType = CuisineType.CONTINENTAL,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN, DietaryFilter.NON_VEG),
            spoonacularTag = "european"
        ),
        RecipeCategory(
            id = "chinese", name = "Chinese Cuisine",
            description = "Traditional Chinese dishes with bold flavors and wok cooking",
            imageUrl = "https://images.unsplash.com/photo-1582878826629-29b7ad1cdc43?w=500&h=400&fit=crop",
            recipeCount = 1980, cuisineType = CuisineType.CHINESE,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN, DietaryFilter.NON_VEG),
            spoonacularTag = "chinese"
        ),
        RecipeCategory(
            id = "mexican", name = "Mexican Cuisine",
            description = "Vibrant Mexican flavors with fresh ingredients and bold spices",
            imageUrl = "https://images.unsplash.com/photo-1555939594-58d7cb561ad1?w=500&h=400&fit=crop",
            recipeCount = 1850, cuisineType = CuisineType.MEXICAN,
            dietaryTags = listOf(
                DietaryFilter.VEGETARIAN,
                DietaryFilter.VEGAN,
                DietaryFilter.NON_VEG
            ),
            spoonacularTag = "mexican"
        ),
        RecipeCategory(
            id = "mediterranean", name = "Mediterranean",
            description = "Healthy Mediterranean diet with olive oil, fresh herbs, and seafood",
            imageUrl = "https://images.unsplash.com/photo-1544982503-9f984c14501a?w=500&h=400&fit=crop",
            recipeCount = 1760, cuisineType = CuisineType.MEDITERRANEAN,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN, DietaryFilter.NON_VEG),
            spoonacularTag = "mediterranean"
        ),
        // MEAL TYPE CATEGORIES
        RecipeCategory(
            id = "breakfast", name = "Breakfast",
            description = "Start your day right with these energizing morning meals",
            imageUrl = "https://images.unsplash.com/photo-1493770348161-369560ae357d?w=400",
            recipeCount = 1180, cuisineType = CuisineType.INTERNATIONAL,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN, DietaryFilter.VEGAN),
            spoonacularTag = "breakfast"
        ),
        RecipeCategory(
            id = "lunch", name = "Lunch",
            description = "Satisfying midday meals to fuel your afternoon",
            imageUrl = "https://images.unsplash.com/photo-1567620905732-2d1ec7ab7445?w=400",
            recipeCount = 1450, cuisineType = CuisineType.INTERNATIONAL,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN, DietaryFilter.NON_VEG),
            spoonacularTag = "lunch"
        ),
        RecipeCategory(
            id = "dinner", name = "Dinner",
            description = "Hearty evening meals perfect for sharing with family",
            imageUrl = "https://images.unsplash.com/photo-1484723091739-30a097e8f929?w=400",
            recipeCount = 1680, cuisineType = CuisineType.INTERNATIONAL,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN, DietaryFilter.NON_VEG),
            spoonacularTag = "dinner"
        ),
        // DISH TYPE CATEGORIES
        RecipeCategory(
            id = "appetizers", name = "Appetizers",
            description = "Delicious starters and small bites to begin your meal",
            imageUrl = "https://images.unsplash.com/photo-1541529086526-db283c563270?w=400",
            recipeCount = 890, cuisineType = CuisineType.INTERNATIONAL,
            dietaryTags = listOf(
                DietaryFilter.VEGETARIAN,
                DietaryFilter.VEGAN,
                DietaryFilter.NON_VEG
            ),
            spoonacularTag = "appetizer"
        ),
        RecipeCategory(
            id = "desserts", name = "Desserts",
            description = "Sweet treats and decadent desserts for any occasion",
            imageUrl = "https://images.unsplash.com/photo-1551024506-0bccd828d307?w=400",
            recipeCount = 1120, cuisineType = CuisineType.INTERNATIONAL,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN, DietaryFilter.VEGAN),
            spoonacularTag = "dessert"
        ),
        RecipeCategory(
            id = "snacks", name = "Snacks",
            description = "Quick bites and healthy snack options for any time",
            imageUrl = "https://images.unsplash.com/photo-1621939514649-280e2ee25f60?w=400",
            recipeCount = 640, cuisineType = CuisineType.INTERNATIONAL,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN, DietaryFilter.VEGAN),
            spoonacularTag = "snack"
        ),
        // DIETARY PREFERENCE CATEGORIES
        RecipeCategory(
            id = "vegetarian", name = "Vegetarian",
            description = "Delicious plant-based meals with dairy and eggs",
            imageUrl = "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=400",
            recipeCount = 2200, cuisineType = CuisineType.INTERNATIONAL,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN),
            spoonacularTag = "vegetarian"
        ),
        RecipeCategory(
            id = "vegan", name = "Vegan",
            description = "Completely plant-based recipes with no animal products",
            imageUrl = "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=400",
            recipeCount = 1580, cuisineType = CuisineType.INTERNATIONAL,
            dietaryTags = listOf(DietaryFilter.VEGAN),
            spoonacularTag = "vegan"
        ),
        RecipeCategory(
            id = "keto", name = "Keto",
            description = "Low-carb, high-fat recipes perfect for ketogenic diet",
            imageUrl = "https://images.unsplash.com/photo-1490645935967-10de6ba17061?w=400",
            recipeCount = 750, cuisineType = CuisineType.INTERNATIONAL,
            dietaryTags = listOf(DietaryFilter.KETO, DietaryFilter.LOW_CARB),
            spoonacularTag = "ketogenic"
        ),
        // SPECIALTY CATEGORIES
        RecipeCategory(
            id = "soups", name = "Soups & Stews",
            description = "Warming and comforting soups for every season",
            imageUrl = "https://images.unsplash.com/photo-1547592166-23ac45744acd?w=400",
            recipeCount = 520, cuisineType = CuisineType.INTERNATIONAL,
            dietaryTags = listOf(
                DietaryFilter.VEGETARIAN,
                DietaryFilter.VEGAN,
                DietaryFilter.NON_VEG
            ),
            spoonacularTag = "soup"
        ),
        RecipeCategory(
            id = "beverages", name = "Beverages",
            description = "Refreshing drinks, smoothies, and specialty beverages",
            imageUrl = "https://images.unsplash.com/photo-1544145945-f90425340c7e?w=400",
            recipeCount = 420, cuisineType = CuisineType.INTERNATIONAL,
            dietaryTags = listOf(DietaryFilter.VEGETARIAN, DietaryFilter.VEGAN),
            spoonacularTag = "drink"
        )
    )

    fun getCategoryById(id: String): RecipeCategory? = categories.find { it.id == id }
}
