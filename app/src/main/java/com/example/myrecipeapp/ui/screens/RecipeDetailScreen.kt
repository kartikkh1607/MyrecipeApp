package com.example.myrecipeapp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.myrecipeapp.domain.model.Ingredient
import com.example.myrecipeapp.domain.model.NutritionInfo
import com.example.myrecipeapp.domain.model.Recipe
import com.example.myrecipeapp.domain.model.RecipeStep
import com.example.myrecipeapp.ui.navigation.ShoppingList
import com.example.myrecipeapp.ui.viewmodel.MainViewModel

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

    // Only block the screen when there is NO recipe to show at all.
    // If a previous recipe is already loaded, keep it visible while the new one loads
    // (avoids a blank spinner with no back button trapping the user).
    val recipe = recipeDetailState.recipe
    if (recipe == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (recipeDetailState.error != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Failed to load recipe",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.material3.TextButton(
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
    val favoriteIds by viewModel.favoriteIds
    val isFavorite = favoriteIds.contains(recipe.id)

    AnimatedContent(
        targetState = isCookingMode,
        transitionSpec = {
            if (targetState) {
                // Entering cooking mode: slide up from bottom + fade in
                (slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = 320f
                    )
                ) + fadeIn(tween(220))) togetherWith
                        (slideOutVertically(
                            targetOffsetY = { -it / 4 },
                            animationSpec = tween(200)
                        ) + fadeOut(tween(180)))
            } else {
                // Exiting cooking mode: slide back down + fade in recipe detail
                (slideInVertically(
                    initialOffsetY = { -it / 4 },
                    animationSpec = tween(220)
                ) + fadeIn(tween(220))) togetherWith
                        (slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = 320f
                            )
                        ) + fadeOut(tween(180)))
            }
        },
        label = "cooking_mode_transition"
    ) { inCookingMode ->
        if (inCookingMode) {
            BackHandler {
                isCookingMode = false
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            CookingModeScreen(
                recipe = recipe,
                onExitCookingMode = {
                    isCookingMode = false
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            )
        } else {
            val listState = rememberLazyListState()
            // Parallax: hero image translates at 40% of scroll speed
            val parallaxOffset by remember {
                derivedStateOf {
                    if (listState.firstVisibleItemIndex == 0)
                        listState.firstVisibleItemScrollOffset * 0.4f
                    else 300f  // hero fully scrolled away
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
                        }
                    )
                }

                item {
                    // Recipe Overview
                    RecipeOverviewSection(recipe = recipe)
                    Spacer(modifier = Modifier.height(24.dp))
                }

                item {
                    // Cooking Action Buttons
                    CookingActionButtons(
                        onStartCooking = {
                            isCookingMode = true
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onAddToShoppingList = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.addToShoppingList(recipe)
                            navController.navigate(ShoppingList) {
                                launchSingleTop = true
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                item {
                    // Ingredients Section
                    IngredientsSection(ingredients = recipe.ingredients)
                    Spacer(modifier = Modifier.height(24.dp))
                }

                item {
                    // Instructions Section
                    InstructionsSection(instructions = recipe.instructions)
                    Spacer(modifier = Modifier.height(24.dp))
                }

                item {
                    // Nutrition Info (if available)
                    recipe.nutritionInfo?.let { nutrition ->
                        NutritionSection(nutritionInfo = nutrition)
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                item {
                    // Tags
                    TagsSection(tags = recipe.tags)
                    Spacer(modifier = Modifier.height(100.dp)) // Bottom padding
                }
            }       // end LazyColumn
        }       // end else (recipe detail view)
    }           // end AnimatedContent lambda
}               // end RecipeDetailScreen


@Composable
fun RecipeHeroSection(
    recipe: Recipe,
    isFavorite: Boolean,
    parallaxOffsetPx: Float = 0f,
    onBackClick: () -> Unit,
    onFavoriteClick: () -> Unit
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
                .padding(12.dp)
                .statusBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.35f), CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
            }

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
                icon = Icons.Default.TrendingUp,
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
fun IngredientsSection(ingredients: List<Ingredient>) {
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
                        isLast = index == ingredients.lastIndex
                    )
                }
            }
        }
    }
}

@Composable
fun IngredientItem(
    ingredient: Ingredient,
    isLast: Boolean
) {
    // Format "4.0" → "4", "1.5" stays "1.5"
    val amount = remember(ingredient.amount) {
        val d = ingredient.amount.toDoubleOrNull()
        if (d != null && d == kotlin.math.floor(d)) d.toInt().toString()
        else ingredient.amount
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
fun InstructionsSection(instructions: List<RecipeStep>) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Instructions",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        instructions.forEach { step ->
            InstructionStepCard(step = step)
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun InstructionStepCard(step: RecipeStep) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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

// CookingActionButtons function moved to avoid duplication
