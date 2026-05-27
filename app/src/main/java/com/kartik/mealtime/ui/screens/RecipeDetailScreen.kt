package com.kartik.mealtime.ui.screens

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.kartik.mealtime.data.billing.BillingManager
import com.kartik.mealtime.ui.components.GeneratedRecipeSheet
import com.kartik.mealtime.ui.components.UpsellBottomSheet
import com.kartik.mealtime.ui.components.findActivity
import com.kartik.mealtime.ui.components.rememberInterstitialAdManager
import com.kartik.mealtime.ui.navigation.RecipeDetail
import com.kartik.mealtime.ui.navigation.ShoppingList
import com.kartik.mealtime.ui.theme.Amber
import com.kartik.mealtime.ui.viewmodel.AiViewModel
import com.kartik.mealtime.ui.viewmodel.BillingViewModel
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
    // Keyed per recipe so each page in the swipe pager owns its own remix state — without
    // a key the shared genState would render a GeneratedRecipeSheet on every composed page.
    val aiViewModel: AiViewModel = hiltViewModel(key = "remix-$recipeId")
    val billingViewModel: BillingViewModel = hiltViewModel()
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

    // ── AI Remix (premium) ────────────────────────────────────────────────────
    val isPremium by aiViewModel.isPremium.collectAsStateWithLifecycle()
    val remixState by aiViewModel.recipeGenState
    val productDetails by billingViewModel.productDetails.collectAsStateWithLifecycle()
    var showRemixOptions by remember { mutableStateOf(false) }
    var showUpsell by remember { mutableStateOf(false) }
    val activity = remember(context) { context.findActivity() }

    fun attemptRemix(instruction: String) {
        if (instruction.isBlank()) return
        showRemixOptions = false
        if (isPremium) aiViewModel.transformRecipe(recipe, instruction) else showUpsell = true
    }

    // 402 from the proxy (entitlement lapsed / mirror stale) → open the paywall, not an error.
    LaunchedEffect(aiViewModel) {
        aiViewModel.upsellEvents.collect { showUpsell = true }
    }
    // React to purchase outcomes only while the paywall is up, so swipe-pager siblings
    // (which share the singleton BillingManager) don't each fire a duplicate toast.
    LaunchedEffect(showUpsell) {
        if (!showUpsell) return@LaunchedEffect
        billingViewModel.purchaseEvents.collect { result ->
            when (result) {
                BillingManager.PurchaseResult.Success -> {
                    showUpsell = false
                    Toast.makeText(context, "You're Premium now — enjoy!", Toast.LENGTH_SHORT).show()
                }
                BillingManager.PurchaseResult.Cancelled -> Unit
                is BillingManager.PurchaseResult.Error ->
                    Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
            }
        }
    }

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
                Spacer(modifier = Modifier.height(16.dp))
                RemixWithAiButton(
                    isPremium = isPremium,
                    onClick = { showRemixOptions = true }
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

        // ── Remix options sheet ───────────────────────────────────────────────
        if (showRemixOptions) {
            RemixOptionsSheet(
                onPreset = { attemptRemix(it) },
                onCustom = { attemptRemix(it) },
                onDismiss = { showRemixOptions = false }
            )
        }

        // ── Remixed recipe review sheet (reuses the generated-recipe sheet) ────
        if (remixState !is AiViewModel.RecipeGenState.Idle) {
            GeneratedRecipeSheet(
                state = remixState,
                onSave = { aiViewModel.saveGeneratedRecipe() },
                onOpen = { newRecipe ->
                    aiViewModel.dismissGeneratedRecipe()
                    navController.navigate(RecipeDetail(recipeId = newRecipe.id)) {
                        launchSingleTop = true
                    }
                },
                onDismiss = { aiViewModel.dismissGeneratedRecipe() }
            )
        }

        // ── Premium upsell ────────────────────────────────────────────────────
        if (showUpsell) {
            UpsellBottomSheet(
                title = "AI Recipe Remix",
                description = "Make any recipe vegan, spicier, healthier, or scaled to your table.",
                perks = listOf(
                    "Transform any recipe with one tap",
                    "Saved to your AI Creations",
                    "Tailored to your diet & preferences"
                ),
                productDetails = productDetails,
                onSelectPlan = { offerToken ->
                    activity?.let { billingViewModel.purchase(it, offerToken) }
                },
                onDismiss = { showUpsell = false }
            )
        }
    }
}

/** Amber call-to-action that opens the AI remix options; shows a lock when gated. */
@Composable
private fun RemixWithAiButton(
    isPremium: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = Amber.copy(alpha = 0.16f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Amber,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = "Remix with AI",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!isPremium) {
                Spacer(Modifier.size(6.dp))
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Premium",
                    tint = Amber,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/** Bottom sheet of one-tap remix presets plus a free-text box. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun RemixOptionsSheet(
    onPreset: (String) -> Unit,
    onCustom: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val presets = remember {
        listOf(
            "Make it vegetarian",
            "Make it vegan",
            "Double the servings",
            "Halve the servings",
            "Make it spicier",
            "Make it healthier",
            "Suggest ingredient substitutions",
            "Make it gluten-free",
        )
    }
    var custom by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Amber,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    "Remix with AI",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Pick a quick change, or describe your own.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.forEach { preset ->
                    Surface(
                        onClick = { onPreset(preset) },
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp),
                    ) {
                        Text(
                            preset,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = custom,
                onValueChange = { custom = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. swap chicken for tofu") },
                shape = RoundedCornerShape(14.dp),
                maxLines = 3,
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { onCustom(custom) },
                enabled = custom.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Amber,
                    contentColor = Color.Black
                ),
            ) {
                Text("Remix", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

