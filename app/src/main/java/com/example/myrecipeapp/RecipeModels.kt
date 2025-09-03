package com.example.myrecipeapp

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
    val isLowCarb: Boolean = false,
    val isFavorite: Boolean = false,
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable

@Parcelize
data class Ingredient(
    val id: String = "",
    val name: String,
    val amount: String,
    val unit: String = "",
    val notes: String = "",
    val isOptional: Boolean = false,
    val isCompleted: Boolean = false // For cooking mode checklist
) : Parcelable

@Parcelize
data class RecipeStep(
    val stepNumber: Int,
    val instruction: String,
    val duration: Int? = null, // in minutes
    val temperature: String? = null,
    val imageUrl: String? = null,
    val tips: String? = null,
    val isCompleted: Boolean = false // For cooking mode
) : Parcelable

@Parcelize
data class NutritionInfo(
    val calories: Int,
    val protein: Float, // in grams
    val carbs: Float, // in grams
    val fat: Float, // in grams
    val fiber: Float, // in grams
    val sugar: Float, // in grams
    val sodium: Float // in mg
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

// Featured Recipe Types for Carousel
enum class FeaturedType {
    RECIPE_OF_THE_DAY,
    POPULAR_THIS_WEEK,
    TRENDING_NOW,
    QUICK_MEALS,
    HEALTHY_CHOICES,
    COMFORT_FOOD
}

@Parcelize
data class FeaturedRecipe(
    val recipe: Recipe,
    val type: FeaturedType,
    val subtitle: String = "",
    val badgeText: String = "",
    val gradientColors: List<String> = emptyList()
) : Parcelable

// Filter options for search
data class RecipeFilters(
    val categories: List<String> = emptyList(),
    val cuisines: List<String> = emptyList(),
    val difficulties: List<RecipeDifficulty> = emptyList(),
    val maxPrepTime: Int? = null,
    val maxCookTime: Int? = null,
    val maxCalories: Int? = null,
    val dietaryRestrictions: DietaryRestrictions = DietaryRestrictions(),
    val ingredients: List<String> = emptyList()
)

data class DietaryRestrictions(
    val vegetarian: Boolean = false,
    val vegan: Boolean = false,
    val glutenFree: Boolean = false,
    val dairyFree: Boolean = false,
    val keto: Boolean = false,
    val lowCarb: Boolean = false,
    val lowSodium: Boolean = false,
    val sugarFree: Boolean = false
)

// Sample data for demo
object SampleData {
    fun getFeaturedRecipes(): List<FeaturedRecipe> = listOf(
        FeaturedRecipe(
            recipe = Recipe(
                id = "1",
                name = "Spicy Korean Ramen",
                description = "Authentic Korean ramen with a perfect balance of spice and umami flavors",
                imageUrl = "https://images.unsplash.com/photo-1569718212165-3a8278d5f624?w=500",
                category = "Asian",
                cuisine = "Korean",
                difficulty = RecipeDifficulty.MEDIUM,
                prepTime = 15,
                cookTime = 25,
                servings = 2,
                rating = 4.8f,
                reviewCount = 234,
                calories = 520,
                ingredients = listOf(
                    Ingredient("1", "Ramen noodles", "2", "packs"),
                    Ingredient("2", "Gochujang", "2", "tbsp"),
                    Ingredient("3", "Soy sauce", "3", "tbsp"),
                    Ingredient("4", "Garlic", "4", "cloves"),
                    Ingredient("5", "Green onions", "3", "stalks"),
                    Ingredient("6", "Eggs", "2", "pieces"),
                    Ingredient("7", "Sesame oil", "1", "tsp")
                ),
                instructions = listOf(
                    RecipeStep(1, "Boil water in a large pot and cook ramen noodles according to package instructions", 3),
                    RecipeStep(2, "In a small bowl, mix gochujang, soy sauce, and minced garlic", 2),
                    RecipeStep(3, "Drain noodles and toss with the sauce mixture", 1),
                    RecipeStep(4, "Top with sliced green onions, soft-boiled eggs, and drizzle with sesame oil", 2)
                ),
                tags = listOf("Spicy", "Quick", "Asian", "Comfort Food"),
                isVegetarian = false,
                isVegan = false
            ),
            type = FeaturedType.RECIPE_OF_THE_DAY,
            subtitle = "Today's Special",
            badgeText = "🔥 SPICY",
            gradientColors = listOf("#FF6B6B", "#FF8E53")
        ),
        FeaturedRecipe(
            recipe = Recipe(
                id = "2",
                name = "Mediterranean Quinoa Bowl",
                description = "Fresh and healthy quinoa bowl packed with Mediterranean flavors",
                imageUrl = "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=500",
                category = "Healthy",
                cuisine = "Mediterranean",
                difficulty = RecipeDifficulty.EASY,
                prepTime = 10,
                cookTime = 15,
                servings = 4,
                rating = 4.6f,
                reviewCount = 189,
                calories = 380,
                ingredients = listOf(
                    Ingredient("1", "Quinoa", "1", "cup"),
                    Ingredient("2", "Cherry tomatoes", "1", "cup"),
                    Ingredient("3", "Cucumber", "1", "medium"),
                    Ingredient("4", "Feta cheese", "100", "g"),
                    Ingredient("5", "Olive oil", "3", "tbsp"),
                    Ingredient("6", "Lemon juice", "2", "tbsp"),
                    Ingredient("7", "Fresh herbs", "1/4", "cup")
                ),
                instructions = listOf(
                    RecipeStep(1, "Cook quinoa according to package instructions and let cool", 15),
                    RecipeStep(2, "Dice cucumber and halve cherry tomatoes", 5),
                    RecipeStep(3, "Whisk together olive oil, lemon juice, salt and pepper", 2),
                    RecipeStep(4, "Combine quinoa, vegetables, feta, and dressing. Garnish with herbs", 3)
                ),
                tags = listOf("Healthy", "Vegetarian", "Mediterranean", "Quick"),
                isVegetarian = true,
                isVegan = false,
                isGlutenFree = true
            ),
            type = FeaturedType.HEALTHY_CHOICES,
            subtitle = "Nutritious & Delicious",
            badgeText = "💚 HEALTHY",
            gradientColors = listOf("#4ECDC4", "#44A08D")
        ),
        FeaturedRecipe(
            recipe = Recipe(
                id = "3",
                name = "Classic Margherita Pizza",
                description = "Authentic Italian pizza with fresh basil, mozzarella, and san marzano tomatoes",
                imageUrl = "https://images.unsplash.com/photo-1574071318508-1cdbab80d002?w=500",
                category = "Italian",
                cuisine = "Italian",
                difficulty = RecipeDifficulty.MEDIUM,
                prepTime = 30,
                cookTime = 12,
                servings = 4,
                rating = 4.9f,
                reviewCount = 456,
                calories = 280,
                ingredients = listOf(
                    Ingredient("1", "Pizza dough", "1", "ball"),
                    Ingredient("2", "San Marzano tomatoes", "1", "can"),
                    Ingredient("3", "Fresh mozzarella", "200", "g"),
                    Ingredient("4", "Fresh basil", "10", "leaves"),
                    Ingredient("5", "Extra virgin olive oil", "2", "tbsp"),
                    Ingredient("6", "Sea salt", "1", "pinch")
                ),
                instructions = listOf(
                    RecipeStep(1, "Preheat oven to 500°F (260°C) with pizza stone inside", 30),
                    RecipeStep(2, "Stretch pizza dough on floured surface", 5),
                    RecipeStep(3, "Spread crushed tomatoes evenly, leaving 1-inch border", 2),
                    RecipeStep(4, "Add torn mozzarella and fresh basil leaves", 2),
                    RecipeStep(5, "Bake for 10-12 minutes until crust is golden and cheese bubbles", 12)
                ),
                tags = listOf("Italian", "Classic", "Vegetarian", "Comfort Food"),
                isVegetarian = true,
                isVegan = false
            ),
            type = FeaturedType.POPULAR_THIS_WEEK,
            subtitle = "Most Loved",
            badgeText = "⭐ POPULAR",
            gradientColors = listOf("#F093FB", "#F5576C")
        )
    )
}
