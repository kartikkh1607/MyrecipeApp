package com.kartik.mealtime.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.kartik.mealtime.ui.components.findActivity
import com.kartik.mealtime.ui.components.rememberInterstitialAdManager
import com.kartik.mealtime.ui.navigation.ShoppingList
import com.kartik.mealtime.ui.viewmodel.FavoritesViewModel
import com.kartik.mealtime.ui.viewmodel.MainViewModel
import com.kartik.mealtime.ui.viewmodel.ShoppingListViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    recipeId: String,
    navController: NavHostController,
    viewModel: MainViewModel,
    shoppingListViewModel: ShoppingListViewModel
) {
    val swipeIds by viewModel.recipeSwipeIds
    val hasSwipeList = swipeIds.size > 1 && swipeIds.contains(recipeId)

    if (hasSwipeList) {
        val initialPage = remember(recipeId, swipeIds) {
            swipeIds.indexOf(recipeId).coerceAtLeast(0)
        }
        val pagerState = rememberPagerState(initialPage = initialPage) { swipeIds.size }
        Box(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                RecipeDetailPage(
                    recipeId = swipeIds[page],
                    navController = navController,
                    viewModel = viewModel,
                    shoppingListViewModel = shoppingListViewModel
                )
            }
            SwipePageIndicator(
                count = swipeIds.size,
                current = pagerState.currentPage,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 28.dp)
            )
        }
    } else {
        RecipeDetailPage(
            recipeId = recipeId,
            navController = navController,
            viewModel = viewModel,
            shoppingListViewModel = shoppingListViewModel
        )
    }
}

@Composable
private fun SwipePageIndicator(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count.coerceAtMost(10)) { i ->
            Box(
                modifier = Modifier
                    .size(if (i == current) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(
                        if (i == current) Color.White
                        else Color.White.copy(alpha = 0.4f)
                    )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecipeDetailPage(
    recipeId: String,
    navController: NavHostController,
    viewModel: MainViewModel,
    shoppingListViewModel: ShoppingListViewModel
) {
    val favoritesViewModel: FavoritesViewModel = hiltViewModel()
    val cachedRecipe = viewModel.recipeDetailCache[recipeId]
    val recipeDetailState by viewModel.recipeDetailState

    LaunchedEffect(recipeId) {
        if (cachedRecipe == null) viewModel.fetchRecipeDetails(recipeId)
    }

    // ── Interstitial ad trigger ───────────────────────────────────────────────
    // Fires on every Nth recipe view (controlled by AdConfig.INTERSTITIAL_FREQUENCY).
    // 800 ms delay lets the enter animation finish AND debounces rapid pager swipes —
    // if the user swipes to a new recipe before the delay elapses, this coroutine is
    // cancelled by the LaunchedEffect key change, so no ad shows for skipped pages.
    val adManager = rememberInterstitialAdManager()
    val context = LocalContext.current
    LaunchedEffect(recipeId) {
        delay(800)
        context.findActivity()?.let { adManager.maybeShowOnRecipeView(it) }
    }

    val recipe = cachedRecipe ?: recipeDetailState.recipe?.takeIf { it.id == recipeId }

    if (recipe == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (recipeDetailState.error != null && !recipeDetailState.loading) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Failed to load recipe", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { navController.popBackStack() }) { Text("Go Back") }
                }
            } else {
                CircularProgressIndicator()
            }
        }
        return
    }

    var isCookingMode by remember { mutableStateOf(false) }
    val hapticFeedback = LocalHapticFeedback.current
    val isFavorite by remember(recipe.id) {
        derivedStateOf { favoritesViewModel.favoriteIds.value.contains(recipe.id) }
    }

    // Personalization: record this view exactly once per unique recipe.id.
    // Fires for cached recipes too (cache short-circuits fetch, but we still
    // want streak + recently-viewed updated). Key on recipe.id so swiping
    // through the pager tracks each new page.
    LaunchedEffect(recipe.id) {
        viewModel.recordRecipeView(recipe)
    }

    val baseServings = recipe.servings.coerceAtLeast(1)
    var currentServings by remember(recipe.id, recipe.servings) { mutableIntStateOf(baseServings) }
    val servingScale = currentServings.toFloat() / baseServings.toFloat()

    // Per-session interaction state — resets when recipe changes
    var checkedIngredients by remember(recipe.id) { mutableStateOf(setOf<Int>()) }
    var completedSteps by remember(recipe.id) { mutableStateOf(setOf<Int>()) }
    var showDescriptionSheet by remember { mutableStateOf(false) }

    fun shareCurrentRecipe() {
        val body = buildString {
            append(recipe.name)
            if (recipe.description.isNotBlank()) append("\n\n").append(recipe.description)
            if (recipe.ingredients.isNotEmpty()) {
                append("\n\nIngredients:")
                recipe.ingredients.forEach { ing ->
                    append("\n- ")
                    if (ing.amount.isNotBlank()) append(ing.amount).append(' ')
                    if (ing.unit.isNotBlank()) append(ing.unit).append(' ')
                    append(ing.name)
                }
            }
        }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, recipe.name)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        context.startActivity(Intent.createChooser(send, "Share recipe"))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val listState = rememberLazyListState()
        val scope = rememberCoroutineScope()
        val parallaxOffset by remember {
            derivedStateOf {
                if (listState.firstVisibleItemIndex == 0)
                    listState.firstVisibleItemScrollOffset * 0.4f
                else 380f
            }
        }
        val showStickyNav by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }

        fun findSectionIndex(key: String): Int {
            listState.layoutInfo.visibleItemsInfo.forEach { item ->
                if (item.key == key) return item.index
            }
            return -1
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // ── Hero ──────────────────────────────────────────────────────────
            item {
                RecipeHeroSection(
                    recipe = recipe,
                    isFavorite = isFavorite,
                    parallaxOffsetPx = parallaxOffset,
                    onBackClick = { navController.popBackStack() },
                    onFavoriteClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        favoritesViewModel.toggleFavorite(recipe)
                    },
                    onShareClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        shareCurrentRecipe()
                    }
                )
            }

            // ── Stats grid ────────────────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(16.dp))
                RecipeOverviewSection(recipe = recipe)
                Spacer(modifier = Modifier.height(20.dp))
            }

            // ── "About" row — opens description bottom sheet ──────────────────
            if (recipe.description.isNotBlank()) {
                item {
                    AboutRecipeRow(onClick = { showDescriptionSheet = true })
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // ── Action buttons ────────────────────────────────────────────────
            item {
                CookingActionButtons(
                    recipeName = recipe.name,
                    onStartCooking = {
                        isCookingMode = true
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onAddToShoppingList = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        val scaled = recipe.copy(
                            servings = currentServings,
                            ingredients = recipe.ingredients.map {
                                it.copy(amount = scaleAmount(it.amount, servingScale))
                            }
                        )
                        shoppingListViewModel.addToShoppingList(scaled)
                        navController.navigate(ShoppingList) { launchSingleTop = true }
                    },
                    resolveVideoUrl = { name -> viewModel.resolveYoutubeUrl(name) }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // ── Ingredients ───────────────────────────────────────────────────
            item(key = "ingredients_section") {
                ServingsStepper(
                    servings = currentServings,
                    onChange = {
                        currentServings = it
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
                IngredientsSection(
                    ingredients = recipe.ingredients,
                    scale = servingScale,
                    checkedSet = checkedIngredients,
                    onToggleChecked = { index ->
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        checkedIngredients = if (index in checkedIngredients)
                            checkedIngredients - index else checkedIngredients + index
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // ── Instructions header ───────────────────────────────────────────
            item(key = "instructions_header") {
                InstructionsHeader(
                    totalSteps = recipe.instructions.size,
                    completedCount = completedSteps.size
                )
            }

            // ── Instruction step cards ────────────────────────────────────────
            itemsIndexed(
                items = recipe.instructions,
                key = { _, step -> "step_${step.stepNumber}" }
            ) { stepIndex, step ->
                InstructionStepCard(
                    step = step,
                    isCompleted = stepIndex in completedSteps,
                    onToggle = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        completedSteps = if (stepIndex in completedSteps)
                            completedSteps - stepIndex else completedSteps + stepIndex
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                if (stepIndex < recipe.instructions.lastIndex) {
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            item(key = "instructions_end") { Spacer(modifier = Modifier.height(24.dp)) }

            // ── Nutrition ─────────────────────────────────────────────────────
            item(key = "nutrition_section") {
                recipe.nutritionInfo?.let { nutrition ->
                    NutritionSection(nutritionInfo = nutrition)
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // ── Tags ──────────────────────────────────────────────────────────
            item {
                TagsSection(tags = recipe.tags)
                Spacer(modifier = Modifier.height(100.dp))
            }
        }

        // ── Sticky section jump nav ───────────────────────────────────────────
        AnimatedVisibility(
            visible = showStickyNav && !isCookingMode,
            enter = slideInVertically(initialOffsetY = { -it }, animationSpec = tween(280)) +
                    fadeIn(tween(200)),
            exit = slideOutVertically(targetOffsetY = { -it }, animationSpec = tween(240)) +
                    fadeOut(tween(180)),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            RecipeSectionNav(
                hasNutrition = recipe.nutritionInfo != null,
                onIngredients = {
                    val idx = findSectionIndex("ingredients_section")
                    scope.launch { listState.animateScrollToItem(if (idx >= 0) idx else 5) }
                },
                onInstructions = {
                    val idx = findSectionIndex("instructions_header")
                    scope.launch { listState.animateScrollToItem(if (idx >= 0) idx else 6) }
                },
                onNutrition = {
                    val idx = findSectionIndex("nutrition_section")
                    if (idx >= 0) scope.launch { listState.animateScrollToItem(idx) }
                }
            )
        }

        // ── Description bottom sheet ─────────────────────────────────────────
        if (showDescriptionSheet) {
            ModalBottomSheet(
                onDismissRequest = { showDescriptionSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        text = "About this recipe",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = recipe.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4f
                    )
                }
            }
        }

        // ── Cooking mode overlay ──────────────────────────────────────────────
        AnimatedVisibility(
            visible = isCookingMode,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 320f)
            ) + fadeIn(tween(220)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 320f)
            ) + fadeOut(tween(200))
        ) {
            BackHandler(enabled = isCookingMode) {
                isCookingMode = false
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                CookingModeScreen(
                    recipe = recipe,
                    onExitCookingMode = {
                        isCookingMode = false
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                )
            }
        }
    }
}

