package com.example.myrecipeapp

import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun Navigation(
    navController: NavHostController,
    viewModel: MainViewModel
) {
    
    NavHost(
        navController = navController,
        startDestination = Screen.HomeScreen.route,
        enterTransition = {
            slideInHorizontally(initialOffsetX = { 1000 }, animationSpec = tween(600)) + fadeIn(animationSpec = tween(600))
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { -1000 }, animationSpec = tween(600)) + fadeOut(animationSpec = tween(600))
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { -1000 }, animationSpec = tween(600)) + fadeIn(animationSpec = tween(600))
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { 1000 }, animationSpec = tween(600)) + fadeOut(animationSpec = tween(600))
        }
    ) {
        
        // Home Screen - Dashboard with featured recipes
        composable(route = Screen.HomeScreen.route) {
            HomeScreen(navController = navController, viewModel = viewModel)
        }

        // Categories Screen - List all recipe categories
        composable(route = Screen.CategoriesScreen.route) {
            val viewState by viewModel.recipeCategoriesState
            RecipeScreen(viewstate = viewState, navigateToDetail = {
                navController.currentBackStackEntry?.savedStateHandle?.set("cat", it)
                navController.navigate(Screen.DetailScreen.route)
            })
        }

        // Search Screen - Search for recipes
        composable(
            route = Screen.SearchScreen.route,
            enterTransition = {
                scaleIn(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                scaleOut(animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
            }
        ) {
            SearchScreen(navController = navController, viewModel = viewModel)
        }

        // Favorites Screen - User's saved recipes
        composable(route = Screen.FavoritesScreen.route) {
            FavoritesScreen(navController = navController, viewModel = viewModel)
        }

        // Settings Screen - App settings and preferences
        composable(route = Screen.SettingsScreen.route) {
            SettingsScreen(navController = navController, viewModel = viewModel)
        }

        // Category Detail Screen - Recipes in a specific category
        composable(route = Screen.DetailScreen.route) {
            val category =
                navController.previousBackStackEntry?.savedStateHandle?.get<RecipeCategory>("cat")
                    ?: RecipeCategory("", "", "", "")
            CategoryDetailScreen(
                category = category,
                onBackClick = { navController.popBackStack() },
                onRecipeClick = { recipeId ->
                    navController.navigate(Screen.RecipeDetailScreen.route + "/$recipeId")
                }
            )
        }

        // Recipe Detail Screen - Individual recipe details
        composable(
            route = Screen.RecipeDetailScreen.route + "/{recipeId}",
            arguments = listOf(
                navArgument("recipeId") {
                    type = NavType.StringType
                    defaultValue = ""
                    nullable = false
                }
            ),
            enterTransition = {
                slideInVertically(initialOffsetY = { 1000 }, animationSpec = tween(700)) + 
                fadeIn(animationSpec = tween(700))
            },
            exitTransition = {
                slideOutVertically(targetOffsetY = { 1000 }, animationSpec = tween(700)) + 
                fadeOut(animationSpec = tween(700))
            }
        ) { entry ->
            val recipeId = entry.arguments?.getString("recipeId") ?: ""
            RecipeDetailScreen(
                recipeId = recipeId,
                navController = navController,
                viewModel = viewModel
            )
        }

        // Profile Screen - User profile and preferences
        composable(route = Screen.ProfileScreen.route) {
            ProfileScreen(
                navController = navController,
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )
        }

        // About Screen - App information
        composable(route = Screen.AboutScreen.route) {
            AboutScreen(
                navController = navController,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Legacy Recipe Screen (for backward compatibility)
        composable(route = Screen.RecipeScreen.route) {
            val viewState by viewModel.recipeCategoriesState
            RecipeScreen(viewstate = viewState, navigateToDetail = {
                navController.currentBackStackEntry?.savedStateHandle?.set("cat", it)
                navController.navigate(Screen.DetailScreen.route)
            })
        }
    }
}
