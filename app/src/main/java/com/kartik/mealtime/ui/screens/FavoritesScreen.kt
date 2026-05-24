package com.kartik.mealtime.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.kartik.mealtime.domain.model.Recipe
import com.kartik.mealtime.ui.components.BrandedSnackbarHost
import com.kartik.mealtime.ui.navigation.Favorites
import com.kartik.mealtime.ui.navigation.LocalTabReselectEvents
import com.kartik.mealtime.ui.navigation.RecipeDetail
import com.kartik.mealtime.ui.navigation.Search
import com.kartik.mealtime.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    navController: NavHostController,
    viewModel: MainViewModel
) {
    val favoriteRecipes by viewModel.favoriteRecipes
    val sortedFavorites by viewModel.sortedFavoriteRecipes
    val favoriteIds by viewModel.favoriteIds
    val isGridMode by viewModel.favoritesGridMode
    val currentSort by viewModel.favoritesSortOrder
    val hapticFeedback = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    val gridState = rememberLazyGridState()
    val listState = rememberLazyListState()
    val tabReselectEvents = LocalTabReselectEvents.current
    LaunchedEffect(tabReselectEvents) {
        tabReselectEvents.events.filter { it is Favorites }.collect {
            if (isGridMode) {
                if (gridState.layoutInfo.totalItemsCount > 0) gridState.animateScrollToItem(0)
            } else {
                if (listState.layoutInfo.totalItemsCount > 0) listState.animateScrollToItem(0)
            }
        }
    }

    Scaffold(
        snackbarHost = { BrandedSnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            // ── Header ────────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Favorite Recipes",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (favoriteRecipes.isNotEmpty()) {
                        Text(
                            text = "${favoriteRecipes.size} saved recipe${if (favoriteRecipes.size > 1) "s" else ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (favoriteRecipes.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            viewModel.toggleFavoritesGridMode()
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = if (isGridMode) Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = "Toggle view",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // ── Sort chips row ─────────────────────────────────────────────────
            if (favoriteRecipes.isNotEmpty()) {
                FavoritesSortBar(
                    currentSort = currentSort,
                    onSortSelected = { order ->
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.setFavoritesSortOrder(order)
                    }
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Stable method references hoisted to avoid recomposition of list items
            val onRemoveFavorite = remember { viewModel::removeFavorite }
            val onAddFavorite = remember { viewModel::addFavorite }

            // ── Content ──────────────────────────────────────────────────────────
            AnimatedContent(
                targetState = favoriteRecipes.isEmpty(),
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                label = "favorites_content"
            ) { isEmpty ->
                if (isEmpty) {
                    EmptyFavoritesState(
                        onExploreClick = {
                            navController.navigate(Search) { launchSingleTop = true }
                        }
                    )
                } else {
                    AnimatedContent(
                        targetState = isGridMode,
                        transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
                        label = "grid_list_toggle"
                    ) { grid ->
                        if (grid) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                state = gridState,
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(
                                    sortedFavorites,
                                    key = { _, recipe -> recipe.id }
                                ) { index, recipe ->
                                    var visible by remember { mutableStateOf(false) }
                                    LaunchedEffect(recipe.id) {
                                        delay(minOf(index, 5) * 50L)
                                        visible = true
                                    }
                                    val onRemove = remember(recipe) {
                                        {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onRemoveFavorite(recipe.id)
                                            scope.launch {
                                                val result = snackbarHostState.showSnackbar(
                                                    message = "${recipe.name} removed from favorites",
                                                    actionLabel = "Undo",
                                                    duration = androidx.compose.material3.SnackbarDuration.Short
                                                )
                                                if (result == SnackbarResult.ActionPerformed) {
                                                    onAddFavorite(recipe)
                                                }
                                            }
                                            Unit
                                        }
                                    }
                                    val onGridClick = remember(recipe.id, sortedFavorites) {
                                        {
                                            viewModel.setRecipeSwipeList(sortedFavorites.map { it.id })
                                            navController.navigate(RecipeDetail(recipeId = recipe.id))
                                        }
                                    }
                                    AnimatedVisibility(
                                        visible = visible,
                                        enter = scaleIn(
                                            initialScale = 0.85f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = 300f
                                            )
                                        ) + fadeIn(tween(200))
                                    ) {
                                        CompactFavoriteCard(
                                            recipe = recipe,
                                            onRemove = onRemove,
                                            onClick = onGridClick
                                        )
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(
                                    sortedFavorites,
                                    key = { _, recipe -> recipe.id }
                                ) { index, recipe ->
                                    var visible by remember { mutableStateOf(false) }
                                    LaunchedEffect(recipe.id) {
                                        delay(minOf(index, 5) * 60L)
                                        visible = true
                                    }
                                    val onDelete = remember(recipe) {
                                        {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onRemoveFavorite(recipe.id)
                                            scope.launch {
                                                val result = snackbarHostState.showSnackbar(
                                                    message = "${recipe.name} removed from favorites",
                                                    actionLabel = "Undo",
                                                    duration = androidx.compose.material3.SnackbarDuration.Short
                                                )
                                                if (result == SnackbarResult.ActionPerformed) {
                                                    onAddFavorite(recipe)
                                                }
                                            }
                                            Unit
                                        }
                                    }
                                    val onListClick = remember(recipe.id, sortedFavorites) {
                                        {
                                            viewModel.setRecipeSwipeList(sortedFavorites.map { it.id })
                                            navController.navigate(RecipeDetail(recipeId = recipe.id)) {
                                                launchSingleTop = true
                                            }
                                        }
                                    }
                                    AnimatedVisibility(
                                        visible = visible,
                                        enter = slideInVertically(
                                            initialOffsetY = { 40 },
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = 280f
                                            )
                                        ) + fadeIn(tween(200))
                                    ) {
                                        SwipeToDeleteFavoriteCard(
                                            recipe = recipe,
                                            isFavorite = favoriteIds.contains(recipe.id),
                                            onDelete = onDelete,
                                            onClick = onListClick
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Sort chips row ────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FavoritesSortBar(
    currentSort: MainViewModel.FavoritesSortOrder,
    onSortSelected: (MainViewModel.FavoritesSortOrder) -> Unit
) {
    val sortOptions = remember { MainViewModel.FavoritesSortOrder.values().toList() }
    val sortEmojis = remember {
        mapOf(
            MainViewModel.FavoritesSortOrder.RECENTLY_ADDED to "🕐",
            MainViewModel.FavoritesSortOrder.NAME_AZ to "🔤",
            MainViewModel.FavoritesSortOrder.NAME_ZA to "🔡",
            MainViewModel.FavoritesSortOrder.RATING to "★",
            MainViewModel.FavoritesSortOrder.COOK_TIME to "⏱",
            MainViewModel.FavoritesSortOrder.DIFFICULTY to "💪"
        )
    }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        items(sortOptions.size) { index ->
            val option = sortOptions[index]
            val isSelected = currentSort == option
            val emoji = sortEmojis[option] ?: ""
            FilterChip(
                selected = isSelected,
                onClick = { onSortSelected(option) },
                label = {
                    Text(
                        text = "$emoji ${option.label}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

// ── Swipe-to-delete wrapper for list mode ─────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteFavoriteCard(
    recipe: Recipe,
    isFavorite: Boolean,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart ||
                value == SwipeToDismissBoxValue.StartToEnd
            ) {
                onDelete()
                true
            } else false
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.35f }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val isActive = dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart ||
                    dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd
            val bgColor = if (isActive)
                MaterialTheme.colorScheme.errorContainer
            else
                MaterialTheme.colorScheme.surfaceVariant

            val iconScale by animateFloatAsState(
                targetValue = if (isActive) 1.2f else 0.85f,
                animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium),
                label = "delete_icon_scale"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(bgColor),
                contentAlignment = Alignment.CenterEnd
            ) {
                Column(
                    modifier = Modifier.padding(end = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .size(28.dp)
                            .scale(iconScale)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Remove",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true
    ) {
        EnhancedRecipeCard(
            recipe = recipe,
            isFavorite = isFavorite,
            onFavoriteToggle = { onDelete() },
            onClick = onClick
        )
    }
}

// ── Compact grid card ─────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactFavoriteCard(
    recipe: Recipe,
    onRemove: () -> Unit,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 400f),
        label = "card_scale",
        finishedListener = { isPressed = false }
    )
    val cardOverlay = remember {
        Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)))
    }
    val ratingText = remember(recipe.rating) { String.format("%.1f", recipe.rating) }

    Card(
        onClick = {
            isPressed = true
            onClick()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .scale(scale),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    model = recipe.imageUrl,
                    contentDescription = recipe.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                )
            }
            Box(modifier = Modifier.fillMaxSize().background(cardOverlay))
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                    .size(32.dp)
            ) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = "Remove favorite",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
            ) {
                Text(
                    recipe.name,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Star,
                        null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        ratingText,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Text(
                        "·",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        "${recipe.prepTime + recipe.cookTime} min",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

// ── Empty state ────────────────────────────────────────────────────────────────
@Composable
fun EmptyFavoritesState(onExploreClick: () -> Unit = {}) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(100); visible = true }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = scaleIn(
                initialScale = 0.6f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = 260f
                )
            ) + fadeIn()
        ) {
            Surface(
                modifier = Modifier.size(110.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { 30 },
                animationSpec = spring(stiffness = 280f)
            ) + fadeIn()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "No favorites yet",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Tap the ♥ on any recipe to\nsave it here for quick access.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(28.dp))
                Button(
                    onClick = onExploreClick,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Explore, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Explore Recipes", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
