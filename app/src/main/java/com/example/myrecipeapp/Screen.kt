package com.example.myrecipeapp

sealed class Screen(val route: String) {
    // Main screens with bottom navigation
    object HomeScreen : Screen("home")
    object CategoriesScreen : Screen("categories")
    object FavoritesScreen : Screen("favorites")
    object SearchScreen : Screen("search")
    object SettingsScreen : Screen("settings")
    
    // Detail screens
    object CategoryDetailScreen : Screen("category_detail")
    object RecipeDetailScreen : Screen("recipe_detail")
    
    // Additional screens
    object ProfileScreen : Screen("profile")
    object AboutScreen : Screen("about")
    
    // Legacy screens (keeping for compatibility)
    object RecipeScreen : Screen("recipescreen")
    object DetailScreen : Screen("detailscreen")
}