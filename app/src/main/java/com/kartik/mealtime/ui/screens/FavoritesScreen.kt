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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.kartik.mealtime.domain.model.Recipe
import com.kartik.mealtime.ui.components.BrandedSnackbarHost
import com.kartik.mealtime.ui.components.GridListToggle
import com.kartik.mealtime.ui.components.RecipeCardH
import com.kartik.mealtime.ui.components.RecipeCardV
import com.kartik.mealtime.ui.navigation.Favorites
import com.kartik.mealtime.ui.navigation.LocalTabReselectEvents
import com.kartik.mealtime.ui.navigation.RecipeDetail
import com.kartik.mealtime.ui.navigation.Search
import com.kartik.mealtime.ui.viewmodel.FavoritesViewModel
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
    val favoritesViewModel: FavoritesViewModel = hiltViewModel()
    val favoriteRecipes by favoritesViewModel.favoriteRecipes.collectAsStateWithLifecycle()
    val sortedFavorites by favoritesViewModel.sortedFavoriteRecipes.collectAsStateWithLifecycle()
    val favoriteIds by favoritesViewModel.favoriteIds.collectAsStateWithLifecycle()
    val isGridMode by favoritesViewModel.favoritesGridMode.collectAsStateWithLifecycle()
    val currentSort by favoritesViewModel.favoritesSortOrder.collectAsStateWithLifecycle()
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

    val onRemoveFavorite = remember { favoritesViewModel::removeFavorite }
    val onAddFavorite = remember { favoritesViewModel::addFavorite }
    val onToggleFavorite = remember { favoritesViewModel::toggleFavorite }

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
                .padding(horizontal = 20.dp)
        ) {
            // ── Header: "Saved" + count, grid/list toggle on the right ──────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 0.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Saved",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 26.sp,
                            lineHeight = 28.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (favoriteRecipes.isEmpty())
                            "Tap the ♥ to save recipes here"
                        else
                            "${favoriteRecipes.size} ${if (favoriteRecipes.size == 1) "recipe" else "recipes"} you love",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (favoriteRecipes.isNotEmpty()) {
                    GridListToggle(
                        isGrid = isGridMode,
                        onChange = { wantGrid ->
                            if (wantGrid != isGridMode) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                favoritesViewModel.toggleFavoritesGridMode()
                            }
                        }
                    )
                }
            }

            // ── Sort chips row ──────────────────────────────────────────────────
            if (favoriteRecipes.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                FavoritesSortBar(
                    currentSort = currentSort,
                    onSortSelected = { order ->
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        favoritesViewModel.setFavoritesSortOrder(order)
                    }
                )
                Spacer(Modifier.height(16.dp))
            }

            // ── Content ─────────────────────────────────────────────────────────
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
                                horizontalArrangement = Arrangement.spacedBy(13.dp),
                                verticalArrangement = Arrangement.spacedBy(13.dp),
                                contentPadding = PaddingValues(bottom = 24.dp)
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
                                            initialScale = 0.9f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = 300f
                                            )
                                        ) + fadeIn(tween(200))
                                    ) {
                                        RecipeCardV(
                                            recipe = recipe,
                                            isFavorite = favoriteIds.contains(recipe.id),
                                            onClick = onGridClick,
                                            onFavoriteToggle = { onToggleFavorite(recipe) },
                                            onDelete = onRemove
                                        )
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(bottom = 24.dp)
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
                                        SwipeToDeleteLinenCard(
                                            recipe = recipe,
                                            isFavorite = favoriteIds.contains(recipe.id),
                                            onDelete = onDelete,
                                            onClick = onListClick,
                                            onFavoriteToggle = { onToggleFavorite(recipe) }
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
    currentSort: FavoritesViewModel.FavoritesSortOrder,
    onSortSelected: (FavoritesViewModel.FavoritesSortOrder) -> Unit
) {
    val sortOptions = remember { FavoritesViewModel.FavoritesSortOrder.values().toList() }
    val sortEmojis = remember {
        mapOf(
            FavoritesViewModel.FavoritesSortOrder.RECENTLY_ADDED to "🕐",
            FavoritesViewModel.FavoritesSortOrder.NAME_AZ to "🔤",
            FavoritesViewModel.FavoritesSortOrder.NAME_ZA to "🔡",
            FavoritesViewModel.FavoritesSortOrder.RATING to "★",
            FavoritesViewModel.FavoritesSortOrder.COOK_TIME to "⏱",
            FavoritesViewModel.FavoritesSortOrder.DIFFICULTY to "💪"
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
                shape = RoundedCornerShape(999.dp)
            )
        }
    }
}

// ── Swipe-to-delete wrapper for list mode ─────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteLinenCard(
    recipe: Recipe,
    isFavorite: Boolean,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
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
                    .clip(RoundedCornerShape(18.dp))
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
                            .graphicsLayer { scaleX = iconScale; scaleY = iconScale }
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
        RecipeCardH(
            recipe = recipe,
            isFavorite = isFavorite,
            onClick = onClick,
            onFavoriteToggle = onFavoriteToggle
        )
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────
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
