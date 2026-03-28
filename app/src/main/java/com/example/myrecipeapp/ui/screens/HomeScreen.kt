package com.example.myrecipeapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.myrecipeapp.domain.model.RecipeCategory
import com.example.myrecipeapp.ui.navigation.CategoryDetail
import com.example.myrecipeapp.ui.navigation.Profile
import com.example.myrecipeapp.ui.navigation.RecipeDetail
import com.example.myrecipeapp.ui.navigation.ShoppingList
import com.example.myrecipeapp.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: MainViewModel
) {
    val hapticFeedback = LocalHapticFeedback.current
    val categoriesState by viewModel.recipeCategoriesState
    val homeRecipeState by viewModel.homeRecipeState
    var selectedCategory by remember { mutableStateOf<RecipeCategory?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // ── Header ─────────────────────────────────────────────────────────────
        ModernHeaderSection(
            onProfileClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                navController.navigate(Profile)
            }
        )

        // ── Featured Carousel ───────────────────────────────────────────────────
        when {
            homeRecipeState.loading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(280.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Loading delicious recipes…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
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
                    navController.navigate(RecipeDetail(recipeId = random.recipe.id)) {
                        launchSingleTop = true
                    }
                }
            },
            onShoppingList = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                navController.navigate(ShoppingList) { launchSingleTop = true }
            }
        )

        Spacer(modifier = Modifier.height(28.dp))

        // ── Categories ─────────────────────────────────────────────────────────
        when {
            categoriesState.loading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            categoriesState.error != null -> {
                ErrorSection(
                    message = categoriesState.error ?: "Unknown error",
                    onRetry = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.refreshRecipeCategories()
                    }
                )
            }
            else -> {
                Column {
                    // Section header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Categories",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            "${categoriesState.categories.size} types",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    // Image-backed category cards
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(categoriesState.categories) { index, category ->
                            var entered by remember { mutableStateOf(false) }
                            LaunchedEffect(index) {
                                delay(index * 55L)
                                entered = true
                            }
                            AnimatedVisibility(
                                visible = entered,
                                enter = slideInVertically(
                                    initialOffsetY = { 30 },
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = 280f
                                    )
                                ) + fadeIn(animationSpec = tween(200))
                            ) {
                                CategoryImageCard(
                                    category = category,
                                    isSelected = selectedCategory == category,
                                    onClick = {
                                        selectedCategory = category
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        navController.navigate(CategoryDetail) { launchSingleTop = true }
                                        navController.currentBackStackEntry
                                            ?.savedStateHandle?.set("cat", category)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

// ── Header ─────────────────────────────────────────────────────────────────────
@Composable
fun ModernHeaderSection(onProfileClick: () -> Unit) {
    val greeting = remember {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        when (hour) {
            in 0..11  -> "Good Morning ☀️"
            in 12..16 -> "Good Afternoon 🌤️"
            else      -> "Good Evening 🌙"
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = greeting,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "What will you cook today?",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        IconButton(
            onClick = onProfileClick,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Profile",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// ── Quick Actions — compact pill row ──────────────────────────────────────────
@Composable
private fun QuickActionsRow(
    onRandomRecipe: () -> Unit,
    onShoppingList: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Random Recipe — tonal filled button
            var rndPressed by remember { mutableStateOf(false) }
            val rndScale by animateFloatAsState(
                targetValue = if (rndPressed) 0.96f else 1f,
                animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessHigh),
                label = "rnd_scale",
                finishedListener = { rndPressed = false }
            )
            FilledTonalButton(
                onClick = { rndPressed = true; onRandomRecipe() },
                modifier = Modifier.weight(1f).height(52.dp).scale(rndScale),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Icon(Icons.Default.Shuffle, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Random Recipe", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }

            // Shopping List — outlined button
            var shpPressed by remember { mutableStateOf(false) }
            val shpScale by animateFloatAsState(
                targetValue = if (shpPressed) 0.96f else 1f,
                animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessHigh),
                label = "shp_scale",
                finishedListener = { shpPressed = false }
            )
            Button(
                onClick = { shpPressed = true; onShoppingList() },
                modifier = Modifier.weight(1f).height(52.dp).scale(shpScale),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Default.ShoppingCart, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Shopping List", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }
    }
}

// ── Category image card ────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryImageCard(
    category: RecipeCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 300f),
        label = "border"
    )
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessHigh),
        label = "cat_scale",
        finishedListener = { pressed = false }
    )

    Card(
        onClick = { pressed = true; onClick() },
        modifier = Modifier
            .width(110.dp)
            .height(130.dp)
            .scale(scale),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 2.dp),
        border = if (isSelected)
            androidx.compose.foundation.BorderStroke(2.dp, borderColor)
        else null
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Category image
            AsyncImage(
                model = category.imageUrl,
                contentDescription = category.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.65f)
                            )
                        )
                    )
            )
            // Name at bottom
            Text(
                text = category.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            )
            // Selected indicator
            if (isSelected) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(20.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("✓", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}

// ── Legacy chip kept for backward compat (used nowhere now but safe to keep) ───
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryChip(
    category: RecipeCategory,
    onClick: () -> Unit,
    isSelected: Boolean
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceVariant,
        label = "chip_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "chip_fg"
    )
    Card(
        onClick = onClick,
        modifier = Modifier.height(40.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

// ── Quick Action Card (kept for backward compat) ───────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionCard(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "quick_action_scale"
    )
    Card(
        onClick = { isPressed = true; onClick() },
        modifier = modifier.height(120.dp).scale(scale),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPressed) 0.dp else 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = text, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

// ── Error section ──────────────────────────────────────────────────────────────
@Composable
fun ErrorSection(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Oops! Something went wrong",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
            Text("Retry")
        }
    }
}
