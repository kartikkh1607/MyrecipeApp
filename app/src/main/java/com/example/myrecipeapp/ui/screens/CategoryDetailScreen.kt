package com.example.myrecipeapp.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.myrecipeapp.domain.model.DietaryFilter
import com.example.myrecipeapp.domain.model.Recipe
import com.example.myrecipeapp.domain.model.RecipeCategory
import com.example.myrecipeapp.ui.viewmodel.MainViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailScreen(
    category: RecipeCategory,
    viewModel: MainViewModel,
    onBackClick: () -> Unit = {},
    onRecipeClick: (String) -> Unit = {}
) {
    // Restore persisted filter so it survives back-navigation
    var selectedDietaryFilter by remember(category.id) {
        mutableStateOf(viewModel.getCategoryFilter(category.id))
    }
    var showFilterBottomSheet by remember { mutableStateOf(false) }
    var sortByRating by remember { mutableStateOf(false) }

    // Persist filter change to ViewModel so it's restored on re-visit
    val updateFilter = { newFilter: com.example.myrecipeapp.domain.model.DietaryFilter ->
        selectedDietaryFilter = newFilter
        viewModel.setCategoryFilter(category.id, newFilter)
    }

    // Observe favorites at composable scope — reads inside items{} won't
    // trigger recomposition on their own without this delegation.
    val favoriteIds by viewModel.favoriteIds

    // Get category recipes state from ViewModel
    val categoryRecipesState by viewModel.categoryRecipesState

    // Fetch recipes when category changes or screen is first loaded
    LaunchedEffect(category.id) {
        viewModel.getRecipesByCategory(category.id)
    }

    // remember() so the filter only re-runs when recipes, filter, or sort changes
    val filteredRecipes =
        remember(selectedDietaryFilter, categoryRecipesState.recipes, sortByRating) {
            val base = if (selectedDietaryFilter == DietaryFilter.ALL) {
                categoryRecipesState.recipes
            } else {
                categoryRecipesState.recipes.filter { recipe ->
                    when (selectedDietaryFilter) {
                        DietaryFilter.VEGETARIAN -> recipe.isVegetarian
                        DietaryFilter.VEGAN -> recipe.isVegan
                        DietaryFilter.GLUTEN_FREE -> recipe.isGlutenFree
                        DietaryFilter.DAIRY_FREE -> recipe.isDairyFree
                        DietaryFilter.KETO -> recipe.isKeto
                        DietaryFilter.LOW_CARB -> recipe.isLowCarb
                        else -> true
                    }
                }
            }
            if (sortByRating) base.sortedByDescending { it.rating } else base
        }

    // Pull-to-refresh — guarded by manualRefresh so the indicator only shows
    // when the user actually pulled (not during initial fetch).
    var manualRefresh by remember { mutableStateOf(false) }
    val isRefreshing = manualRefresh && categoryRecipesState.loading
    LaunchedEffect(categoryRecipesState.loading) {
        if (!categoryRecipesState.loading) manualRefresh = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header with hero image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
        ) {
            if (category.imageResId != 0) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(category.imageResId),
                    contentDescription = category.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                AsyncImage(
                    model = category.imageUrl,
                    contentDescription = category.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            val headerOverlay = remember {
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.3f),
                        Color.Black.copy(alpha = 0.7f)
                    )
                )
            }

            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(brush = headerOverlay)
            )

            // Back button
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            // Filter button
            IconButton(
                onClick = { showFilterBottomSheet = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filter",
                    tint = Color.White
                )
            }

            // Category info
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = category.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )

                // Recipe count badge
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${filteredRecipes.size} Recipes",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        val onFavToggle = remember { viewModel::toggleFavorite }

        // Recipes list with lazy loading indicator + pull-to-refresh
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                manualRefresh = true
                viewModel.refreshCategoryRecipes(category.id)
            },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Active filter indicator
                if (selectedDietaryFilter != DietaryFilter.ALL) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Showing ${
                                        selectedDietaryFilter.name.lowercase()
                                            .replaceFirstChar { it.uppercase() }
                                    } recipes",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                TextButton(
                                    onClick = { updateFilter(DietaryFilter.ALL) }
                                ) {
                                    Text("Clear Filter")
                                }
                            }
                        }
                    }
                }

                // Show loading state with progress indicator
                if (categoryRecipesState.loading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator()
                                Text(
                                    text = "Loading recipes...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "Fetching up to 20 recipes",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }

                // Show error state
                if (categoryRecipesState.error != null && !categoryRecipesState.loading) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Oops! Something went wrong",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = categoryRecipesState.error ?: "Unknown error occurred",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                                    textAlign = TextAlign.Center
                                )
                                Button(
                                    onClick = { viewModel.getRecipesByCategory(category.id) },
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Text("Try Again")
                                }
                            }
                        }
                    }
                }

                // Show empty state when no recipes found
                if (!categoryRecipesState.loading && categoryRecipesState.error == null && filteredRecipes.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "No recipes found",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (selectedDietaryFilter != DietaryFilter.ALL) {
                                        "No recipes match your dietary filter. Try clearing the filter or selecting a different one."
                                    } else {
                                        "No recipes available for this category at the moment. Please try again later."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                                if (selectedDietaryFilter != DietaryFilter.ALL) {
                                    Button(
                                        onClick = { updateFilter(DietaryFilter.ALL) }
                                    ) {
                                        Text("Clear Filter")
                                    }
                                }
                            }
                        }
                    }
                }

                // Show recipes when available with count header
                if (!categoryRecipesState.loading && filteredRecipes.isNotEmpty()) {
                    // Recipe count and sort options
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${filteredRecipes.size} recipes found",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )

                            FilterChip(
                                onClick = { sortByRating = !sortByRating },
                                label = {
                                    Text(
                                        "Sort by Rating",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                selected = sortByRating
                            )
                        }
                    }

                    items(filteredRecipes, key = { it.id }) { recipe ->
                        val onCardClick = remember(recipe.id) { { onRecipeClick(recipe.id) } }
                        EnhancedRecipeCard(
                            recipe = recipe,
                            isFavorite = favoriteIds.contains(recipe.id),
                            onFavoriteToggle = onFavToggle,
                            onClick = onCardClick
                        )
                    }

                    // Load More button
                    if (categoryRecipesState.hasMore && !categoryRecipesState.loading) {
                        item(key = "load_more") {
                            Spacer(modifier = Modifier.height(8.dp))
                            androidx.compose.material3.Button(
                                onClick = { viewModel.loadMoreCategoryRecipes(category.id) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                enabled = !categoryRecipesState.isLoadingMore
                            ) {
                                if (categoryRecipesState.isLoadingMore) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Loading...")
                                } else {
                                    Text("Load More (${categoryRecipesState.totalLoaded} loaded)")
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            } // end LazyColumn
        } // end PullToRefreshBox
    }

    // Filter Bottom Sheet
    if (showFilterBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterBottomSheet = false }
        ) {
            DietaryFilterBottomSheet(
                selectedFilter = selectedDietaryFilter,
                onFilterSelected = { filter ->
                    updateFilter(filter)
                    showFilterBottomSheet = false
                },
                availableFilters = category.dietaryTags + DietaryFilter.ALL
            )
        }
    }
}


@Composable
fun EnhancedRecipeCard(
    recipe: Recipe,
    onClick: () -> Unit,
    isFavorite: Boolean = false,
    onFavoriteToggle: (Recipe) -> Unit = {}
) {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "card_scale"
    )

    val favoriteColor by animateColorAsState(
        targetValue = if (isFavorite) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurface.copy(
            alpha = 0.6f
        ),
        label = "favorite_color"
    )

    // Heart bounce — pops on tap then snaps back with spring
    var heartBounce by remember { mutableStateOf(false) }
    val heartScale by animateFloatAsState(
        targetValue = if (heartBounce) 1.45f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "heart_scale",
        finishedListener = { heartBounce = false }
    )

    Card(
        onClick = {
            isPressed = true
            onClick()
        },
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 8.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Enhanced image with overlay + badges
            val ratingText = remember(recipe.rating) {
                String.format(Locale.ROOT, "%.1f", recipe.rating)
            }
            val difficultyBgColor = remember(recipe.difficulty) {
                when (recipe.difficulty) {
                    com.example.myrecipeapp.domain.model.RecipeDifficulty.EASY ->
                        Color(0xFF2E7D32)
                    com.example.myrecipeapp.domain.model.RecipeDifficulty.MEDIUM ->
                        Color(0xFFE65100)
                    com.example.myrecipeapp.domain.model.RecipeDifficulty.HARD ->
                        Color(0xFFC62828)
                }
            }
            Box(modifier = Modifier.size(100.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    AsyncImage(
                        model = recipe.imageUrl,
                        contentDescription = recipe.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }

                // Rating badge — top end
                if (recipe.rating > 0) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Black.copy(alpha = 0.7f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = ratingText,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Difficulty badge — bottom start
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(5.dp),
                    color = difficultyBgColor.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${recipe.difficulty.emoji()} ${recipe.difficulty.displayName()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            // Recipe information
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Title and favorite button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = recipe.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )

                    IconButton(
                        onClick = {
                            heartBounce = true
                            onFavoriteToggle(recipe)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = favoriteColor,
                            modifier = Modifier
                                .size(20.dp)
                                .scale(heartScale)
                        )
                    }
                }

                // Description
                Text(
                    text = recipe.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.weight(1f))

                // Recipe metadata — styled chip pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cook time chip
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "${recipe.cookTime}m",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // Calories chip
                    if (recipe.calories != null && recipe.calories > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text("🔥", fontSize = 10.sp)
                                Text(
                                    text = "${recipe.calories} kcal",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Reset pressed state after animation
    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(100)
            isPressed = false
        }
    }
}

@Composable
fun DietaryFilterBottomSheet(
    selectedFilter: DietaryFilter,
    onFilterSelected: (DietaryFilter) -> Unit,
    availableFilters: List<DietaryFilter>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Filter by Dietary Preference",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(availableFilters.distinct()) { filter ->
                FilterChip(
                    onClick = { onFilterSelected(filter) },
                    label = {
                        Text(
                            text = when (filter) {
                                DietaryFilter.ALL -> "All"
                                DietaryFilter.NON_VEG -> "Non-Veg"
                                else -> filter.name.lowercase().replaceFirstChar { it.uppercase() }
                            }
                        )
                    },
                    selected = selectedFilter == filter
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}


