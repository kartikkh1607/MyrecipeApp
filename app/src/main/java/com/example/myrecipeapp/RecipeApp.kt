package com.example.myrecipeapp

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun RecipeApp(navController: NavController) {
    val navController = rememberNavController()
    val recipeViewModel: MainViewModel = viewModel()
    val ViewState by recipeViewModel.recipeCategoriesState


    NavHost(navController = navController, startDestination = Screen.RecipeScreen.route)
    {
        composable(route = Screen.RecipeScreen.route) {
            RecipeScreen(viewstate = ViewState, navigateToDetail = {

                navController.currentBackStackEntry?.savedStateHandle?.set("cat", it)
                navController.navigate(Screen.DetailScreen.route)

            })
        }

        composable(route = Screen.DetailScreen.route) {
            val category =
                navController.previousBackStackEntry?.savedStateHandle?.get<RecipeCategory>("cat")
                    ?: RecipeCategory("", "", "", "")
            CategoryDetailScreen(category = category)
        }

    }

}