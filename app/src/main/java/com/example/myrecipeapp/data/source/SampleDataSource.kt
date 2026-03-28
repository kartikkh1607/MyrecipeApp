package com.example.myrecipeapp.data.source

import com.example.myrecipeapp.domain.model.*

/**
 * Provides hard-coded sample recipes used as a fallback when the Spoonacular API
 * is not configured or temporarily unavailable.
 *
 * Moved here from RecipeModels.kt (was the `SampleData` object).
 */
object SampleDataSource {

    /**
     * Built once when SampleDataSource is first accessed.
     * Previously a fun — which reconstructed all 9 Recipe + nested objects on every call.
     */
    val featuredRecipes: List<FeaturedRecipe> = listOf(
        FeaturedRecipe(
            recipe = Recipe(
                id = "1",
                name = "Spicy Korean Ramen",
                description = "Authentic Korean ramen with a perfect balance of spice and umami flavors",
                imageUrl = "https://images.unsplash.com/photo-1569718212165-3a8278d5f624?w=500",
                category = "Asian",
                cuisine = "Korean",
                difficulty = RecipeDifficulty.MEDIUM,
                prepTime = 15, cookTime = 25, servings = 2,
                rating = 4.8f, reviewCount = 234, calories = 520,
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
                isVegetarian = false, isVegan = false
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
                prepTime = 10, cookTime = 15, servings = 4,
                rating = 4.6f, reviewCount = 189, calories = 380,
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
                isVegetarian = true, isVegan = false, isGlutenFree = true
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
                prepTime = 30, cookTime = 12, servings = 4,
                rating = 4.9f, reviewCount = 456, calories = 280,
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
                isVegetarian = true, isVegan = false
            ),
            type = FeaturedType.POPULAR_THIS_WEEK,
            subtitle = "Most Loved",
            badgeText = "⭐ POPULAR",
            gradientColors = listOf("#F093FB", "#F5576C")
        ),
        FeaturedRecipe(
            recipe = Recipe(
                id = "4",
                name = "Butter Chicken",
                description = "Rich and creamy Indian curry with tender chicken in aromatic spices",
                imageUrl = "https://images.unsplash.com/photo-1588166524941-3bf61a9c41db?w=500",
                category = "Indian Cuisine",
                cuisine = "Indian",
                difficulty = RecipeDifficulty.MEDIUM,
                prepTime = 20, cookTime = 30, servings = 4,
                rating = 4.7f, reviewCount = 312, calories = 420,
                ingredients = listOf(
                    Ingredient("1", "Chicken breast", "500", "g"),
                    Ingredient("2", "Heavy cream", "200", "ml"),
                    Ingredient("3", "Tomato puree", "400", "ml"),
                    Ingredient("4", "Garam masala", "2", "tsp"),
                    Ingredient("5", "Ginger-garlic paste", "2", "tbsp"),
                    Ingredient("6", "Butter", "3", "tbsp"),
                    Ingredient("7", "Basmati rice", "1", "cup")
                ),
                instructions = listOf(
                    RecipeStep(1, "Marinate chicken pieces with yogurt, garam masala, and salt for 30 minutes", 30),
                    RecipeStep(2, "Cook chicken in a pan until golden brown, then set aside", 8),
                    RecipeStep(3, "In the same pan, sauté onions and ginger-garlic paste until fragrant", 5),
                    RecipeStep(4, "Add tomato puree, spices, and cook until thick", 10),
                    RecipeStep(5, "Return chicken to pan, add cream, and simmer until tender", 15)
                ),
                tags = listOf("Indian", "Spicy", "Creamy", "Main Course"),
                isVegetarian = false, isVegan = false
            ),
            type = FeaturedType.COMFORT_FOOD,
            subtitle = "Authentic Indian",
            badgeText = "🍛 INDIAN",
            gradientColors = listOf("#FF9A56", "#FFAD56")
        ),
        FeaturedRecipe(
            recipe = Recipe(
                id = "5",
                name = "Chicken Parmesan",
                description = "Classic Italian-American dish with breaded chicken, marinara, and mozzarella",
                imageUrl = "https://images.unsplash.com/photo-1565299507177-b0ac66763828?w=500",
                category = "Italian Cuisine",
                cuisine = "Italian",
                difficulty = RecipeDifficulty.MEDIUM,
                prepTime = 25, cookTime = 35, servings = 4,
                rating = 4.6f, reviewCount = 287, calories = 480,
                ingredients = listOf(
                    Ingredient("1", "Chicken breast", "4", "pieces"),
                    Ingredient("2", "Breadcrumbs", "2", "cups"),
                    Ingredient("3", "Parmesan cheese", "1", "cup"),
                    Ingredient("4", "Mozzarella cheese", "2", "cups"),
                    Ingredient("5", "Marinara sauce", "2", "cups"),
                    Ingredient("6", "Eggs", "2", "pieces"),
                    Ingredient("7", "Olive oil", "1/4", "cup")
                ),
                instructions = listOf(
                    RecipeStep(1, "Pound chicken breasts to even thickness and season with salt and pepper", 5),
                    RecipeStep(2, "Dip chicken in beaten eggs, then coat with breadcrumb-parmesan mixture", 8),
                    RecipeStep(3, "Pan-fry chicken until golden brown and cooked through", 12),
                    RecipeStep(4, "Top with marinara sauce and mozzarella cheese", 2),
                    RecipeStep(5, "Bake in oven until cheese is melted and bubbly", 15)
                ),
                tags = listOf("Italian", "Comfort Food", "Cheesy", "Baked"),
                isVegetarian = false, isVegan = false
            ),
            type = FeaturedType.COMFORT_FOOD,
            subtitle = "Italian Classic",
            badgeText = "🇮🇹 ITALIAN",
            gradientColors = listOf("#56C596", "#56C5C5")
        ),
        FeaturedRecipe(
            recipe = Recipe(
                id = "6",
                name = "Palak Paneer",
                description = "Creamy spinach curry with cottage cheese cubes, a classic North Indian dish",
                imageUrl = "https://images.unsplash.com/photo-1631452180519-c014fe946bc7?w=500",
                category = "Indian Cuisine",
                cuisine = "Indian",
                difficulty = RecipeDifficulty.MEDIUM,
                prepTime = 15, cookTime = 25, servings = 4,
                rating = 4.5f, reviewCount = 198, calories = 320,
                ingredients = listOf(
                    Ingredient("1", "Fresh spinach", "500", "g"),
                    Ingredient("2", "Paneer", "200", "g"),
                    Ingredient("3", "Onion", "1", "medium"),
                    Ingredient("4", "Ginger-garlic paste", "1", "tbsp"),
                    Ingredient("5", "Cumin seeds", "1", "tsp"),
                    Ingredient("6", "Garam masala", "1", "tsp"),
                    Ingredient("7", "Heavy cream", "100", "ml")
                ),
                instructions = listOf(
                    RecipeStep(1, "Blanch spinach leaves and blend into smooth paste", 10),
                    RecipeStep(2, "Cut paneer into cubes and lightly fry until golden", 5),
                    RecipeStep(3, "Sauté onions, add ginger-garlic paste and spices", 8),
                    RecipeStep(4, "Add spinach puree and simmer for 10 minutes", 10),
                    RecipeStep(5, "Add paneer cubes and cream, cook for 5 more minutes", 5)
                ),
                tags = listOf("Indian", "Vegetarian", "Creamy", "Healthy"),
                isVegetarian = true, isVegan = false
            ),
            type = FeaturedType.HEALTHY_CHOICES,
            subtitle = "Vegetarian Delight",
            badgeText = "🌿 VEGETARIAN",
            gradientColors = listOf("#4CAF50", "#66BB6A")
        ),
        FeaturedRecipe(
            recipe = Recipe(
                id = "7",
                name = "Biryani",
                description = "Aromatic basmati rice layered with spiced meat and fragrant herbs",
                imageUrl = "https://plus.unsplash.com/premium_photo-1694141252774-c937d97641da?q=80&w=688&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D",
                category = "Indian Cuisine",
                cuisine = "Indian",
                difficulty = RecipeDifficulty.HARD,
                prepTime = 45, cookTime = 60, servings = 6,
                rating = 4.9f, reviewCount = 445, calories = 580,
                ingredients = listOf(
                    Ingredient("1", "Basmati rice", "2", "cups"),
                    Ingredient("2", "Mutton/Chicken", "500", "g"),
                    Ingredient("3", "Yogurt", "1", "cup"),
                    Ingredient("4", "Fried onions", "1", "cup"),
                    Ingredient("5", "Saffron", "1", "pinch"),
                    Ingredient("6", "Whole spices", "1", "set"),
                    Ingredient("7", "Fresh mint", "1/4", "cup")
                ),
                instructions = listOf(
                    RecipeStep(1, "Marinate meat with yogurt and spices for 2 hours", 120),
                    RecipeStep(2, "Cook rice with whole spices until 70% done", 15),
                    RecipeStep(3, "Cook marinated meat until tender", 40),
                    RecipeStep(4, "Layer rice and meat alternately with fried onions and mint", 10),
                    RecipeStep(5, "Dum cook on low heat for 45 minutes", 45)
                ),
                tags = listOf("Indian", "Festive", "Aromatic", "Special"),
                isVegetarian = false, isVegan = false
            ),
            type = FeaturedType.TRENDING_NOW,
            subtitle = "Royal Feast",
            badgeText = "👑 ROYAL",
            gradientColors = listOf("#FFD700", "#FFA500")
        ),
        FeaturedRecipe(
            recipe = Recipe(
                id = "8",
                name = "Pasta Carbonara",
                description = "Classic Roman pasta with eggs, cheese, pancetta, and black pepper",
                imageUrl = "https://images.unsplash.com/photo-1608756687911-aa1599ab3bd9?q=80&w=687&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D",
                category = "Italian Cuisine",
                cuisine = "Italian",
                difficulty = RecipeDifficulty.MEDIUM,
                prepTime = 10, cookTime = 20, servings = 4,
                rating = 4.7f, reviewCount = 356, calories = 450,
                ingredients = listOf(
                    Ingredient("1", "Spaghetti", "400", "g"),
                    Ingredient("2", "Pancetta", "150", "g"),
                    Ingredient("3", "Eggs", "3", "whole + 1 yolk"),
                    Ingredient("4", "Pecorino Romano", "100", "g"),
                    Ingredient("5", "Black pepper", "1", "tsp"),
                    Ingredient("6", "Garlic", "2", "cloves"),
                    Ingredient("7", "Olive oil", "2", "tbsp")
                ),
                instructions = listOf(
                    RecipeStep(1, "Cook spaghetti in salted boiling water until al dente", 12),
                    RecipeStep(2, "Crisp pancetta with garlic in olive oil", 6),
                    RecipeStep(3, "Whisk eggs with grated cheese and black pepper", 3),
                    RecipeStep(4, "Toss hot pasta with pancetta, remove from heat", 2),
                    RecipeStep(5, "Quickly mix in egg mixture, creating creamy sauce", 2)
                ),
                tags = listOf("Italian", "Classic", "Creamy", "Quick"),
                isVegetarian = false, isVegan = false
            ),
            type = FeaturedType.QUICK_MEALS,
            subtitle = "Roman Classic",
            badgeText = "⚡ QUICK",
            gradientColors = listOf("#FF9800", "#FFC107")
        ),
        FeaturedRecipe(
            recipe = Recipe(
                id = "9",
                name = "Risotto Milanese",
                description = "Creamy saffron risotto from Milan, the perfect comfort food",
                imageUrl = "https://images.unsplash.com/photo-1476124369491-e7addf5db371?w=500",
                category = "Italian Cuisine",
                cuisine = "Italian",
                difficulty = RecipeDifficulty.HARD,
                prepTime = 10, cookTime = 35, servings = 4,
                rating = 4.8f, reviewCount = 278, calories = 380,
                ingredients = listOf(
                    Ingredient("1", "Arborio rice", "320", "g"),
                    Ingredient("2", "Chicken stock", "1.5", "L"),
                    Ingredient("3", "White wine", "200", "ml"),
                    Ingredient("4", "Saffron", "1", "pinch"),
                    Ingredient("5", "Parmesan", "100", "g"),
                    Ingredient("6", "Butter", "50", "g"),
                    Ingredient("7", "Onion", "1", "small")
                ),
                instructions = listOf(
                    RecipeStep(1, "Heat stock and dissolve saffron in it", 5),
                    RecipeStep(2, "Sauté finely chopped onion in butter until soft", 8),
                    RecipeStep(3, "Add rice, stir for 2 minutes until translucent", 3),
                    RecipeStep(4, "Add wine, stir until absorbed", 5),
                    RecipeStep(5, "Add warm stock ladle by ladle, stirring constantly for 20 minutes", 20),
                    RecipeStep(6, "Finish with butter and parmesan, serve immediately", 2)
                ),
                tags = listOf("Italian", "Creamy", "Luxurious", "Comfort Food"),
                isVegetarian = true, isVegan = false
            ),
            type = FeaturedType.COMFORT_FOOD,

            subtitle = "Milanese Gold",
            badgeText = "🏅 PREMIUM",
            gradientColors = listOf("#E91E63", "#F06292")
        )
    )
}
