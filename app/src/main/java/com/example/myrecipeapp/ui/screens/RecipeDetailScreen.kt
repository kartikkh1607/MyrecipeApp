package com.example.myrecipeapp.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.myrecipeapp.domain.model.Ingredient
import com.example.myrecipeapp.domain.model.NutritionInfo
import com.example.myrecipeapp.domain.model.Recipe
import com.example.myrecipeapp.domain.model.RecipeStep
import com.example.myrecipeapp.ui.navigation.ShoppingList
import com.example.myrecipeapp.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    recipeId: String,
    navController: NavHostController,
    viewModel: MainViewModel
) {
    // Get recipe from sample data or use ViewModel state
    val recipeDetailState by viewModel.recipeDetailState

    // Trigger loading of recipe details when screen is first composed
    LaunchedEffect(recipeId) {
        viewModel.fetchRecipeDetails(recipeId)
    }

    // ── Stale-state guard ─────────────────────────────────────────────────────
    // LaunchedEffect runs AFTER the first composition frame, so without this guard
    // the previous recipe would flash on screen for one frame before being cleared.
    // takeIf ensures we only render a recipe whose ID matches what was requested.
    val recipe = recipeDetailState.recipe?.takeIf { it.id == recipeId }

    if (recipe == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (recipeDetailState.error != null && !recipeDetailState.loading) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Failed to load recipe",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { navController.popBackStack() }
                    ) { Text("Go Back") }
                }
            } else {
                CircularProgressIndicator()
            }
        }
        return
    }

    var isCookingMode by remember { mutableStateOf(false) }
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current
    val isFavorite by remember(recipe.id) {
        derivedStateOf { viewModel.favoriteIds.value.contains(recipe.id) }
    }

    // ── Servings scaling ──────────────────────────────────────────────────────
    val baseServings = recipe.servings.coerceAtLeast(1)
    var currentServings by remember(recipe.id) { mutableIntStateOf(baseServings) }
    val servingScale = currentServings.toFloat() / baseServings.toFloat()

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

    // Box: cooking mode full-screen overlay sits on TOP of MainScreen's bottom nav
    Box(modifier = Modifier.fillMaxSize()) {

        // ── Recipe Detail content (always underneath) ─────────────────────────
        val listState = rememberLazyListState()
        val parallaxOffset by remember {
            derivedStateOf {
                if (listState.firstVisibleItemIndex == 0)
                    listState.firstVisibleItemScrollOffset * 0.4f
                else 300f
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            item {
                RecipeHeroSection(
                    recipe = recipe,
                    isFavorite = isFavorite,
                    parallaxOffsetPx = parallaxOffset,
                    onBackClick = { navController.popBackStack() },
                    onFavoriteClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.toggleFavorite(recipe)
                    },
                    onShareClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        shareCurrentRecipe()
                    }
                )
            }

            item {
                RecipeOverviewSection(recipe = recipe)
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                CookingActionButtons(
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
                        viewModel.addToShoppingList(scaled)
                        navController.navigate(ShoppingList) { launchSingleTop = true }
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                ServingsStepper(
                    servings = currentServings,
                    onChange = {
                        currentServings = it
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
                IngredientsSection(ingredients = recipe.ingredients, scale = servingScale)
                Spacer(modifier = Modifier.height(24.dp))
            }

            item(key = "instructions_header") {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 0.dp)) {
                    Text(
                        text = "Instructions",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            itemsIndexed(
                items = recipe.instructions,
                key = { _, step -> "step_${step.stepNumber}" }
            ) { stepIndex, step ->
                InstructionStepCard(
                    step = step,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                if (stepIndex < recipe.instructions.lastIndex) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
            item(key = "instructions_end") {
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                recipe.nutritionInfo?.let { nutrition ->
                    NutritionSection(nutritionInfo = nutrition)
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            item {
                TagsSection(tags = recipe.tags)
                Spacer(modifier = Modifier.height(100.dp))
            }
        }

        // ── Cooking Mode: true full-screen overlay covering the bottom nav ────
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


@Composable
fun RecipeHeroSection(
    recipe: Recipe,
    isFavorite: Boolean,
    parallaxOffsetPx: Float = 0f,
    onBackClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onShareClick: () -> Unit = {}
) {
    // Favorite icon bounces with a spring when toggled
    val favoriteScale by animateFloatAsState(
        targetValue = if (isFavorite) 1.25f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "favorite_scale"
    )
    val favoriteColor by animateColorAsState(
        targetValue = if (isFavorite) Color(0xFFFF4E6A) else Color.White,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 400f),
        label = "favorite_color"
    )

    // Burst trigger — increments only when recipe transitions false→true (add, not remove)
    var burstTrigger by remember { mutableIntStateOf(0) }
    val wasFavorite = remember { mutableStateOf(isFavorite) }
    LaunchedEffect(isFavorite) {
        if (isFavorite && !wasFavorite.value) burstTrigger++
        wasFavorite.value = isFavorite
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
    ) {
        // Background Image — translates with parallax via graphicsLayer (hardware-accelerated)
        AsyncImage(
            model = recipe.imageUrl,
            contentDescription = recipe.name,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationY = -parallaxOffsetPx },
            contentScale = ContentScale.Crop
        )

        // Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.35f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.75f)
                        )
                    )
                )
        )

        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.35f), CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onShareClick,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                ) {
                    Icon(Icons.Default.Share, "Share", tint = Color.White)
                }

                // Favorite button — wrapped in Box so the heart burst overlay can float above it
                Box(contentAlignment = Alignment.Center) {
                    HeartBurstOverlay(trigger = burstTrigger)
                    IconButton(
                        onClick = onFavoriteClick,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                            .scale(favoriteScale)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isFavorite) "Remove" else "Favorite",
                            tint = favoriteColor
                        )
                    }
                }
            }
        }

        // "Saved!" pill — slides in from below the top bar when recipe is added to favorites
        FavoriteAddedLabel(
            trigger = burstTrigger,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(end = 16.dp, top = 56.dp)
        )

        // Recipe Title
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = recipe.name,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = recipe.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun RecipeOverviewSection(recipe: Recipe) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            RecipeInfoItem(
                icon = Icons.Default.Timer,
                label = "Total Time",
                value = "${recipe.prepTime + recipe.cookTime} min"
            )

            RecipeInfoItem(
                icon = Icons.Default.Restaurant,
                label = "Servings",
                value = "${recipe.servings}"
            )

            RecipeInfoItem(
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                label = "Difficulty",
                value = recipe.difficulty.displayName()
            )

            if (recipe.calories != null) {
                RecipeInfoItem(
                    icon = Icons.Default.LocalFireDepartment,
                    label = "Calories",
                    value = "${recipe.calories}"
                )
            }
        }
    }
}

@Composable
fun RecipeInfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CookingActionButtons(
    onStartCooking: () -> Unit,
    onAddToShoppingList: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onStartCooking,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Start Cooking",
                fontWeight = FontWeight.Bold
            )
        }

        OutlinedButton(
            onClick = onAddToShoppingList,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Shopping List",
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun IngredientsSection(ingredients: List<Ingredient>, scale: Float = 1f) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Ingredients",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                ingredients.forEachIndexed { index, ingredient ->
                    IngredientItem(
                        ingredient = ingredient,
                        scale = scale,
                        isLast = index == ingredients.lastIndex
                    )
                }
            }
        }
    }
}

/**
 * Scales a numeric amount string by [scale], preserving the original string for
 * non-numeric values (e.g. "a pinch"). Whole numbers render without decimals;
 * fractional values get up to two decimal places with trailing zeros stripped.
 */
private fun scaleAmount(original: String, scale: Float): String {
    val d = original.toDoubleOrNull() ?: return original
    val scaled = d * scale
    if (scaled == kotlin.math.floor(scaled)) return scaled.toInt().toString()
    return String.format(Locale.ROOT, "%.2f", scaled).trimEnd('0').trimEnd('.')
}

@Composable
fun IngredientItem(
    ingredient: Ingredient,
    scale: Float = 1f,
    isLast: Boolean
) {
    val amount = remember(ingredient.amount, scale) {
        scaleAmount(ingredient.amount, scale)
    }
    val amountLabel = if (ingredient.unit.isBlank()) amount else "$amount ${ingredient.unit}"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Colored dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Name only — notes removed (they were long "original" strings)
        Text(
            text = ingredient.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Amount as a styled pill badge
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Text(
                text = amountLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }

    if (!isLast) {
        HorizontalDivider(
            modifier = Modifier.padding(start = 20.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        )
    }
}


@Composable
fun InstructionStepCard(step: RecipeStep, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Step Number Circle
            Surface(
                modifier = Modifier.size(32.dp),
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = step.stepNumber.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = step.instruction,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.2f
                )

                if (step.duration != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${step.duration} minutes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (step.tips != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "💡 Tip: ${step.tips}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun NutritionSection(nutritionInfo: NutritionInfo) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Nutrition Facts",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    NutritionItem("Calories", "${nutritionInfo.calories}", "kcal")
                    NutritionItem("Protein", "${nutritionInfo.protein.toInt()}", "g")
                    NutritionItem("Carbs", "${nutritionInfo.carbs.toInt()}", "g")
                    NutritionItem("Fat", "${nutritionInfo.fat.toInt()}", "g")
                }
            }
        }
    }
}

@Composable
fun NutritionItem(label: String, value: String, unit: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = unit,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TagsSection(tags: List<String>) {
    if (tags.isNotEmpty()) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Tags",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tags.take(4).forEach { tag ->
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── Heart burst effect ────────────────────────────────────────────────────────
@Composable
private fun HeartBurstOverlay(trigger: Int) {
    val particles = remember {
        (0 until 8).map { i ->
            val rad = Math.toRadians(i * 45.0 + 22.5)
            Triple(cos(rad).toFloat(), sin(rad).toFloat(), (i * 55L) % 180L)
        }
    }
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
        particles.forEach { (dx, dy, delayMs) ->
            key(trigger) {
                if (trigger > 0) FloatingHeart(dx = dx, dy = dy, delayMs = delayMs)
            }
        }
    }
}

@Composable
private fun FloatingHeart(dx: Float, dy: Float, delayMs: Long) {
    val progress = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        delay(delayMs)
        scope.launch { progress.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
        alpha.animateTo(1f, tween(100))
        delay(350)
        alpha.animateTo(0f, tween(350))
    }

    Icon(
        imageVector = Icons.Default.Favorite,
        contentDescription = null,
        tint = Color(0xFFFF4E6A),
        modifier = Modifier
            .size(18.dp)
            .graphicsLayer {
                val maxPx = 55.dp.toPx()
                translationX = dx * maxPx * progress.value
                translationY = dy * maxPx * progress.value
                this.alpha = alpha.value
                scaleX = 0.3f + progress.value * 0.7f
                scaleY = 0.3f + progress.value * 0.7f
            }
    )
}

// ── "Saved!" pill label ───────────────────────────────────────────────────────
@Composable
private fun FavoriteAddedLabel(trigger: Int, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        visible = true
        delay(1_500)
        visible = false
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(initialOffsetY = { 16 }, animationSpec = tween(250)) +
                fadeIn(tween(200)),
        exit = slideOutVertically(targetOffsetY = { -16 }, animationSpec = tween(250)) +
                fadeOut(tween(200))
    ) {
        Surface(
            color = Color(0xFFFF4E6A),
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.Favorite,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    "Saved!",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ── Servings stepper ──────────────────────────────────────────────────────────
@Composable
private fun ServingsStepper(
    servings: Int,
    onChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Servings",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 1.dp
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { if (servings > 1) onChange(servings - 1) },
                    enabled = servings > 1
                ) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = "Decrease servings",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = servings.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(32.dp)
                )
                IconButton(
                    onClick = { if (servings < 20) onChange(servings + 1) },
                    enabled = servings < 20
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Increase servings",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

