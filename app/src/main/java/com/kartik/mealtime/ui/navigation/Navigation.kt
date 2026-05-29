package com.kartik.mealtime.ui.navigation

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import androidx.hilt.navigation.compose.hiltViewModel
import com.kartik.mealtime.data.source.CategoryDataSource
import com.kartik.mealtime.ui.screens.AiCreationsScreen
import com.kartik.mealtime.ui.screens.AuthScreen
import com.kartik.mealtime.ui.screens.CategoryDetailScreen
import com.kartik.mealtime.ui.screens.ChatScreen
import com.kartik.mealtime.ui.screens.FavoritesScreen
import com.kartik.mealtime.ui.screens.HomeScreen
import com.kartik.mealtime.ui.screens.MealPlannerScreen
import com.kartik.mealtime.ui.screens.ProfileScreen
import com.kartik.mealtime.ui.screens.RecipeDetailScreen
import com.kartik.mealtime.ui.screens.RecipeScreen
import com.kartik.mealtime.ui.screens.SearchScreen
import com.kartik.mealtime.ui.screens.SettingsScreen
import com.kartik.mealtime.ui.screens.ShoppingListScreen
import com.kartik.mealtime.ui.viewmodel.AuthViewModel
import com.kartik.mealtime.ui.viewmodel.MainViewModel
import com.kartik.mealtime.ui.viewmodel.SearchViewModel
import com.kartik.mealtime.ui.viewmodel.ShoppingListViewModel

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

// ── Search zoom-in/out spring specs ──────────────────────────────────────────
// Extracted to avoid 4 identical copy-pasted blocks in the composable below.
private val searchSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMedium
)

@Composable
fun Navigation(
    navController: NavHostController,
    viewModel: MainViewModel,
    authViewModel: AuthViewModel,
    shoppingListViewModel: ShoppingListViewModel,
    searchViewModel: SearchViewModel
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
                    navController.navigate(CategoryDetail(categoryId = category.id)) {
                        launchSingleTop = true
                    }
                },
                onRetry = { viewModel.refreshRecipeCategories() }
            )
        }

        composable<Search>(
            // Search uses a zoom-in feel instead of a horizontal slide
            enterTransition    = { scaleIn(initialScale = 0.92f, animationSpec = searchSpring) + fadeIn(animationSpec = tween(300)) },
            exitTransition     = { scaleOut(targetScale = 0.92f, animationSpec = searchSpring) + fadeOut(animationSpec = tween(250)) },
            popEnterTransition = { scaleIn(initialScale = 0.92f, animationSpec = searchSpring) + fadeIn(animationSpec = tween(300)) },
            popExitTransition  = { scaleOut(targetScale = 0.92f, animationSpec = searchSpring) + fadeOut(animationSpec = tween(250)) }
        ) {
            SearchScreen(navController = navController, viewModel = viewModel, searchViewModel = searchViewModel)
        }

        composable<Favorites> {
            FavoritesScreen(navController = navController, viewModel = viewModel)
        }

        composable<Settings> {
            SettingsScreen(navController = navController, viewModel = viewModel)
        }

        // ── Detail screens ─────────────────────────────────────────────────────────

        composable<CategoryDetail> { backStackEntry ->
            val args = backStackEntry.toRoute<CategoryDetail>()
            val category = CategoryDataSource.getCategoryById(args.categoryId)
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
                viewModel = viewModel,
                shoppingListViewModel = shoppingListViewModel
            )
        }

        // ── Secondary screens ──────────────────────────────────────────────────────

        composable<Auth> {
            AuthScreen(viewModel = authViewModel)
        }

        composable<Profile> {
            ProfileScreen(
                navController = navController,
                viewModel = viewModel,
                authViewModel = authViewModel,
                onNavigateToAuth = {
                    navController.navigate(Auth) { launchSingleTop = true }
                }
            )
        }

        composable<ShoppingList> {
            ShoppingListScreen(
                navController = navController,
                viewModel = shoppingListViewModel,
                aiViewModel = hiltViewModel()
            )
        }

        composable<Chat> {
            ChatScreen(
                onBackClick = { navController.popBackStack() },
                onOpenCreations = { navController.navigate(AiCreations) { launchSingleTop = true } },
                onOpenMealPlanner = { navController.navigate(MealPlanner) { launchSingleTop = true } },
                onOpenRecipe = { recipeId ->
                    navController.navigate(RecipeDetail(recipeId = recipeId)) { launchSingleTop = true }
                },
                viewModel = hiltViewModel()
            )
        }

        composable<MealPlanner> {
            MealPlannerScreen(
                viewModel = hiltViewModel(),
                shoppingListViewModel = shoppingListViewModel,
                onBack = { navController.popBackStack() },
                onOpenRecipe = { recipeId ->
                    navController.navigate(RecipeDetail(recipeId = recipeId)) { launchSingleTop = true }
                }
            )
        }

        composable<AiCreations> {
            AiCreationsScreen(
                viewModel = hiltViewModel(),
                onBack = { navController.popBackStack() },
                onOpenRecipe = { recipeId ->
                    navController.navigate(RecipeDetail(recipeId = recipeId)) { launchSingleTop = true }
                }
            )
        }
    }
}
