package com.kartik.mealtime.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.kartik.mealtime.ui.components.BannerAd
import com.kartik.mealtime.ui.components.FeaturedCarouselSkeleton
import com.kartik.mealtime.ui.navigation.Categories
import com.kartik.mealtime.ui.navigation.CategoryDetail
import com.kartik.mealtime.ui.navigation.Chat
import com.kartik.mealtime.ui.navigation.Home
import com.kartik.mealtime.ui.navigation.LocalTabReselectEvents
import com.kartik.mealtime.ui.navigation.Profile
import com.kartik.mealtime.ui.navigation.RecipeDetail
import com.kartik.mealtime.ui.navigation.Search
import com.kartik.mealtime.ui.navigation.ShoppingList
import com.kartik.mealtime.ui.viewmodel.CategoryViewModel
import com.kartik.mealtime.ui.viewmodel.MainViewModel
import com.kartik.mealtime.ui.viewmodel.UserViewModel
import kotlinx.coroutines.flow.filter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: MainViewModel,
    categoryViewModel: CategoryViewModel,
    userViewModel: UserViewModel = hiltViewModel()
) {
    val hapticFeedback = LocalHapticFeedback.current
    val categoriesState by categoryViewModel.recipeCategoriesState
    val homeRecipeState by viewModel.homeRecipeState
    val userPrefs by userViewModel.preferences.collectAsStateWithLifecycle()
    val recentRecipes by userViewModel.recentRecipes.collectAsStateWithLifecycle()

    // Home now uses a "paper" header on the app background — match the status-bar
    // icon contrast to the background luminance (dark icons on light, light on dark).
    val view = LocalView.current
    val lightStatusBarIcons = MaterialTheme.colorScheme.background.luminance() > 0.5f
    DisposableEffect(lightStatusBarIcons) {
        val window = (view.context as android.app.Activity).window
        val controller = WindowCompat.getInsetsController(window, view)
        val previous = controller.isAppearanceLightStatusBars
        controller.isAppearanceLightStatusBars = lightStatusBarIcons
        onDispose { controller.isAppearanceLightStatusBars = previous }
    }

    // Pull-to-refresh — track manual refresh locally so the indicator doesn't
    // show during the initial load (which already has skeletons).
    var manualRefresh by remember { mutableStateOf(false) }
    val isRefreshing = manualRefresh && (homeRecipeState.loading || categoriesState.loading)
    LaunchedEffect(homeRecipeState.loading, categoriesState.loading) {
        if (!homeRecipeState.loading && !categoriesState.loading) manualRefresh = false
    }

    // Hoist scroll state so reselecting the Home tab can smooth-scroll us to top.
    val scrollState = rememberScrollState()
    val tabReselectEvents = LocalTabReselectEvents.current
    LaunchedEffect(tabReselectEvents) {
        tabReselectEvents.events.filter { it is Home }.collect {
            scrollState.animateScrollTo(0)
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            manualRefresh = true
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.refreshFeaturedRecipes()
            categoryViewModel.refreshRecipeCategories()
        },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
        ) {
            // ── Header ─────────────────────────────────────────────────────────────
            HeroHeaderSection(
                displayName = userPrefs.displayName,
                avatarEmoji = userPrefs.avatarEmoji,
                onProfileClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    navController.navigate(Profile)
                },
                onSearchClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    navController.navigate(Search) { launchSingleTop = true }
                }
            )

            // ── Recipe of the week — editorial hero banner ───────────────────────────
            if (homeRecipeState.featuredRecipes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(18.dp))
                RecipeOfTheWeekHero(
                    featured = homeRecipeState.featuredRecipes.first(),
                    onClick = { recipeId ->
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.setRecipeSwipeList(homeRecipeState.featuredRecipes.map { it.recipe.id })
                        navController.navigate(RecipeDetail(recipeId = recipeId)) {
                            launchSingleTop = true
                        }
                    }
                )
                Spacer(modifier = Modifier.height(28.dp))
            }

            // ── Featured Carousel ───────────────────────────────────────────────────
            when {
                homeRecipeState.loading -> {
                    FeaturedCarouselSkeleton()
                }

                homeRecipeState.error != null -> {
                    ErrorSection(
                        message = homeRecipeState.error ?: "Unknown error",
                        onRetry = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.refreshFeaturedRecipes()
                        }
                    )
                }

                homeRecipeState.featuredRecipes.isNotEmpty() -> {
                    FeaturedRecipeCarousel(navController = navController, viewModel = viewModel)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Quick Actions banner ────────────────────────────────────────────────
            QuickActionsRow(
                onRandomRecipe = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    val featured = homeRecipeState.featuredRecipes
                    if (featured.isNotEmpty()) {
                        val random = featured.random()
                        viewModel.setRecipeSwipeList(featured.map { it.recipe.id })
                        navController.navigate(RecipeDetail(recipeId = random.recipe.id)) {
                            launchSingleTop = true
                        }
                    }
                },
                onShoppingList = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    navController.navigate(ShoppingList) { launchSingleTop = true }
                },
                onAIChat = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    navController.navigate(Chat) { launchSingleTop = true }
                }
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ── Browse by mood — category chip rail ─────────────────────────────────
            if (categoriesState.categories.isNotEmpty()) {
                BrowseByMoodRow(
                    categories = categoriesState.categories,
                    onCategoryClick = { category ->
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        navController.navigate(CategoryDetail(categoryId = category.id)) {
                            launchSingleTop = true
                        }
                    },
                    onSeeAll = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        navController.navigate(Categories) {
                            popUpTo<Home> { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
                Spacer(modifier = Modifier.height(28.dp))
            }

            // ── Recently Viewed ────────────────────────────────────────────────────
            // Shown only after the user has opened at least one recipe.
            if (recentRecipes.isNotEmpty()) {
                RecentlyViewedSection(
                    recipes = recentRecipes,
                    onRecipeClick = { recipeId ->
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        // Set swipe list so pager can navigate across recents
                        viewModel.setRecipeSwipeList(recentRecipes.map { it.id })
                        navController.navigate(RecipeDetail(recipeId = recipeId)) {
                            launchSingleTop = true
                        }
                    }
                )
                Spacer(modifier = Modifier.height(28.dp))
            }

            // ── Today's Pick ───────────────────────────────────────────────────────
            if (homeRecipeState.featuredRecipes.isNotEmpty()) {
                TodaysPickCard(
                    featured = homeRecipeState.featuredRecipes,
                    onClick = { recipeId ->
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.setRecipeSwipeList(homeRecipeState.featuredRecipes.map { it.recipe.id })
                        navController.navigate(RecipeDetail(recipeId = recipeId)) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            // ── Banner Ad ──────────────────────────────────────────────────────────
            Spacer(modifier = Modifier.height(24.dp))
            BannerAd(modifier = Modifier.padding(horizontal = 16.dp))
        }
    } // end PullToRefreshBox
}

// ── Hero Header ────────────────────────────────────────────────────────────────
