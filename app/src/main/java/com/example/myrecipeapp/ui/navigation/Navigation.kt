package com.example.myrecipeapp.ui.navigation

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.myrecipeapp.domain.model.RecipeCategory
import com.example.myrecipeapp.ui.screens.*
import com.example.myrecipeapp.ui.viewmodel.MainViewModel

// ── iOS-style spring specs ────────────────────────────────────────────────────
// No bounce, critically damped — mirrors UIKit UISpringTimingParameters
private val iosNavSpring: FiniteAnimationSpec<IntOffset> = spring<IntOffset>(
    dampingRatio = 0.86f,
    stiffness = 380f,
    visibilityThreshold = IntOffset.VisibilityThreshold
)
private val iosNavSpringFloat = spring<Float>(
    dampingRatio = 0.86f,
    stiffness = 380f
)

@Composable
fun Navigation(
    navController: NavHostController,
    viewModel: MainViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Home,
        // ── Push: new screen slides in from right, old screen parallax-slides to left ─
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = iosNavSpring
            ) + fadeIn(animationSpec = tween(durationMillis = 280))
        },
        // Old screen moves at 1/3 speed to the left (iOS parallax depth effect)
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> -(fullWidth / 3) },
                animationSpec = iosNavSpring
            ) + fadeOut(animationSpec = tween(durationMillis = 200))
        },
        // ── Pop: current screen slides out to right, returning screen slides in from left ─
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> -(fullWidth / 3) },
                animationSpec = iosNavSpring
            ) + fadeIn(animationSpec = tween(durationMillis = 280))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = iosNavSpring
            ) + fadeOut(animationSpec = tween(durationMillis = 240))
        }
    ) {

        // ── Bottom nav screens ─────────────────────────────────────────────────────

        composable<Home> {
            HomeScreen(navController = navController, viewModel = viewModel)
        }

        composable<Categories> {
            RecipeScreen(
                viewstate = viewModel.recipeCategoriesState.value,
                navigateToDetail = { category ->
                    navController.navigate(CategoryDetail) {
                        launchSingleTop = true
                    }
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.set("cat", category)
                }
            )
        }

        composable<Search>(
            // Search uses a zoom-in feel instead of a horizontal slide
            enterTransition = {
                scaleIn(
                    initialScale = 0.92f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                scaleOut(
                    targetScale = 0.92f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeOut(animationSpec = tween(250))
            },
            popEnterTransition = {
                scaleIn(
                    initialScale = 0.92f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                scaleOut(
                    targetScale = 0.92f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeOut(animationSpec = tween(250))
            }
        ) {
            SearchScreen(navController = navController, viewModel = viewModel)
        }

        composable<Favorites> {
            FavoritesScreen(navController = navController, viewModel = viewModel)
        }

        composable<Settings> {
            SettingsScreen(navController = navController, viewModel = viewModel)
        }

        // ── Detail screens ─────────────────────────────────────────────────────────

        composable<CategoryDetail> { backStackEntry ->
            val category = backStackEntry.savedStateHandle.get<RecipeCategory>("cat")
            if (category == null) {
                LaunchedEffect(Unit) { navController.popBackStack() }
                return@composable
            }
            CategoryDetailScreen(
                category = category,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onRecipeClick = { recipeId ->
                    navController.navigate(RecipeDetail(recipeId = recipeId)) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable<RecipeDetail>(
            // Recipe detail slides up from bottom (iOS sheet-style)
            enterTransition = {
                slideInVertically(
                    initialOffsetY = { fullHeight -> (fullHeight * 0.12f).toInt() },
                    animationSpec = spring(
                        dampingRatio = 0.86f,
                        stiffness = 320f
                    )
                ) + fadeIn(animationSpec = tween(320))
            },
            exitTransition = {
                slideOutVertically(
                    targetOffsetY = { fullHeight -> (fullHeight * 0.06f).toInt() },
                    animationSpec = spring(dampingRatio = 0.86f, stiffness = 320f)
                ) + fadeOut(animationSpec = tween(250))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(280))
            },
            popExitTransition = {
                slideOutVertically(
                    targetOffsetY = { fullHeight -> (fullHeight * 0.12f).toInt() },
                    animationSpec = spring(dampingRatio = 0.86f, stiffness = 320f)
                ) + fadeOut(animationSpec = tween(280))
            }
        ) { backStackEntry ->
            val args = backStackEntry.toRoute<RecipeDetail>()
            RecipeDetailScreen(
                recipeId = args.recipeId,
                navController = navController,
                viewModel = viewModel
            )
        }

        // ── Secondary screens ──────────────────────────────────────────────────────

        composable<Profile> {
            ProfileScreen(navController = navController, viewModel = viewModel)
        }

        composable<About> {
            AboutScreen(navController = navController)
        }

        composable<ShoppingList> {
            ShoppingListScreen(navController = navController, viewModel = viewModel)
        }
    }
}
