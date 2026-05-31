package com.kartik.mealtime.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.kartik.mealtime.domain.model.DietaryFilter
import com.kartik.mealtime.domain.model.RecipeCategory
import com.kartik.mealtime.ui.components.GridListToggle
import com.kartik.mealtime.ui.components.RecipeCardH
import com.kartik.mealtime.ui.components.RecipeCardV
import com.kartik.mealtime.ui.viewmodel.CategoryViewModel
import com.kartik.mealtime.ui.viewmodel.FavoritesViewModel
import com.kartik.mealtime.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailScreen(
    category: RecipeCategory,
    viewModel: MainViewModel,
    categoryViewModel: CategoryViewModel,
    onBackClick: () -> Unit = {},
    onRecipeClick: (String) -> Unit = {}
) {
    var selectedDietaryFilter by remember(category.id) {
        mutableStateOf(categoryViewModel.getCategoryFilter(category.id))
    }
    var showFilterBottomSheet by remember { mutableStateOf(false) }
    var sortByRating by remember { mutableStateOf(false) }
    var isGrid by remember { mutableStateOf(true) }

    val updateFilter = { newFilter: DietaryFilter ->
        selectedDietaryFilter = newFilter
        categoryViewModel.setCategoryFilter(category.id, newFilter)
    }

    val favoritesViewModel: FavoritesViewModel = hiltViewModel()
    val favoriteIds by favoritesViewModel.favoriteIds.collectAsStateWithLifecycle()
    val onFavToggle = remember { favoritesViewModel::toggleFavorite }

    val categoryRecipesState by categoryViewModel.categoryRecipesState.collectAsStateWithLifecycle()

    LaunchedEffect(category.id) {
        categoryViewModel.getRecipesByCategory(category.id)
    }

    val filteredRecipes =
        remember(selectedDietaryFilter, categoryRecipesState.recipes, sortByRating) {
            val base = if (selectedDietaryFilter == DietaryFilter.ALL) {
                categoryRecipesState.recipes
            } else {
                categoryRecipesState.recipes.filter { r ->
                    when (selectedDietaryFilter) {
                        DietaryFilter.VEGETARIAN -> r.isVegetarian
                        DietaryFilter.VEGAN -> r.isVegan
                        DietaryFilter.GLUTEN_FREE -> r.isGlutenFree
                        DietaryFilter.DAIRY_FREE -> r.isDairyFree
                        DietaryFilter.KETO -> r.isKeto
                        DietaryFilter.LOW_CARB -> r.isLowCarb
                        else -> true
                    }
                }
            }
            if (sortByRating) base.sortedByDescending { it.rating } else base
        }

    var manualRefresh by remember { mutableStateOf(false) }
    val isRefreshing = manualRefresh && categoryRecipesState.loading
    LaunchedEffect(categoryRecipesState.loading) {
        if (!categoryRecipesState.loading) manualRefresh = false
    }

    val scheme = MaterialTheme.colorScheme

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            manualRefresh = true
            categoryViewModel.refreshCategoryRecipes(category.id)
        },
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background)
    ) {
        if (isGrid) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 0.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(13.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "hero") {
                    CategoryHero(category = category)
                }
                item(span = { GridItemSpan(maxLineSpan) }, key = "controls") {
                    CategoryControlsRow(
                        isGrid = isGrid,
                        onToggleGrid = { isGrid = it },
                        onFiltersClick = { showFilterBottomSheet = true },
                        sortByRating = sortByRating,
                        onSortToggle = { sortByRating = !sortByRating },
                        activeFilter = selectedDietaryFilter,
                        onClearFilter = { updateFilter(DietaryFilter.ALL) }
                    )
                }
                stateItems(
                    state = categoryRecipesState,
                    filteredCount = filteredRecipes.size,
                    activeFilter = selectedDietaryFilter,
                    onClearFilter = { updateFilter(DietaryFilter.ALL) },
                    onRetry = { categoryViewModel.getRecipesByCategory(category.id) }
                )

                items(
                    items = filteredRecipes,
                    key = { it.id }
                ) { recipe ->
                    val onCardClick = remember(recipe.id, filteredRecipes) {
                        {
                            viewModel.setRecipeSwipeList(filteredRecipes.map { it.id })
                            onRecipeClick(recipe.id)
                        }
                    }
                    RecipeCardV(
                        recipe = recipe,
                        isFavorite = favoriteIds.contains(recipe.id),
                        onClick = onCardClick,
                        onFavoriteToggle = { onFavToggle(recipe) }
                    )
                }

                if (categoryRecipesState.hasMore && !categoryRecipesState.loading && filteredRecipes.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }, key = "load_more") {
                        LoadMoreButton(
                            isLoading = categoryRecipesState.isLoadingMore,
                            totalLoaded = categoryRecipesState.totalLoaded,
                            onClick = { categoryViewModel.loadMoreCategoryRecipes(category.id) }
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 0.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "hero") { CategoryHero(category = category) }
                item(key = "controls") {
                    CategoryControlsRow(
                        isGrid = isGrid,
                        onToggleGrid = { isGrid = it },
                        onFiltersClick = { showFilterBottomSheet = true },
                        sortByRating = sortByRating,
                        onSortToggle = { sortByRating = !sortByRating },
                        activeFilter = selectedDietaryFilter,
                        onClearFilter = { updateFilter(DietaryFilter.ALL) }
                    )
                }
                stateItemsColumn(
                    state = categoryRecipesState,
                    filteredCount = filteredRecipes.size,
                    activeFilter = selectedDietaryFilter,
                    onClearFilter = { updateFilter(DietaryFilter.ALL) },
                    onRetry = { categoryViewModel.getRecipesByCategory(category.id) }
                )

                items(
                    items = filteredRecipes,
                    key = { it.id }
                ) { recipe ->
                    val onCardClick = remember(recipe.id, filteredRecipes) {
                        {
                            viewModel.setRecipeSwipeList(filteredRecipes.map { it.id })
                            onRecipeClick(recipe.id)
                        }
                    }
                    RecipeCardH(
                        recipe = recipe,
                        isFavorite = favoriteIds.contains(recipe.id),
                        onClick = onCardClick,
                        onFavoriteToggle = { onFavToggle(recipe) }
                    )
                }

                if (categoryRecipesState.hasMore && !categoryRecipesState.loading && filteredRecipes.isNotEmpty()) {
                    item(key = "load_more") {
                        LoadMoreButton(
                            isLoading = categoryRecipesState.isLoadingMore,
                            totalLoaded = categoryRecipesState.totalLoaded,
                            onClick = { categoryViewModel.loadMoreCategoryRecipes(category.id) }
                        )
                    }
                }
            }
        }

        // Floating glass back button (over hero)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 14.dp, top = 14.dp)
        ) {
            GlassIconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    if (showFilterBottomSheet) {
        ModalBottomSheet(onDismissRequest = { showFilterBottomSheet = false }) {
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

// ── Hero header ──────────────────────────────────────────────────────────────
@Composable
private fun CategoryHero(category: RecipeCategory) {
    val gradient = remember {
        Brush.verticalGradient(
            0f to Color(0x73141210),
            0.4f to Color(0x1A141210),
            1f to Color(0xB8141210)
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
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
        Box(modifier = Modifier.fillMaxSize().background(gradient))
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, end = 20.dp, bottom = 18.dp)
        ) {
            Text(
                text = "CATEGORY",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    fontSize = 11.sp
                ),
                color = Color.White.copy(alpha = 0.82f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = category.name,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 32.sp,
                    lineHeight = 34.sp
                ),
                color = Color.White
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = category.description.ifBlank { "${category.name} recipes" },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

// ── Controls row: Filters pill + grid/list toggle ────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryControlsRow(
    isGrid: Boolean,
    onToggleGrid: (Boolean) -> Unit,
    onFiltersClick: () -> Unit,
    sortByRating: Boolean,
    onSortToggle: () -> Unit,
    activeFilter: DietaryFilter,
    onClearFilter: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(scheme.surface)
                    .border(1.dp, scheme.outlineVariant, RoundedCornerShape(999.dp))
                    .clickable(onClick = onFiltersClick)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null,
                    tint = scheme.onSurface,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "Filters",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    ),
                    color = scheme.onSurface
                )
            }

            GridListToggle(isGrid = isGrid, onChange = onToggleGrid)
        }

        if (activeFilter != DietaryFilter.ALL || sortByRating) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (activeFilter != DietaryFilter.ALL) {
                    TextButton(onClick = onClearFilter) {
                        Text("Clear: ${activeFilter.name.lowercase().replaceFirstChar { it.uppercase() }}")
                    }
                }
                TextButton(onClick = onSortToggle) {
                    Text(if (sortByRating) "✓ Sort by rating" else "Sort by rating")
                }
            }
        }
    }
}

@Composable
private fun GlassIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Color(0x66141210))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

// ── Loading / error / empty / load-more — shared item builders ───────────────
private fun LazyGridScope.stateItems(
    state: CategoryViewModel.CategoryRecipesState,
    filteredCount: Int,
    activeFilter: DietaryFilter,
    onClearFilter: () -> Unit,
    onRetry: () -> Unit,
) {
    if (state.loading) {
        item(span = { GridItemSpan(maxLineSpan) }, key = "loading") { LoadingBlock() }
    }
    if (state.error != null && !state.loading) {
        item(span = { GridItemSpan(maxLineSpan) }, key = "error") {
            ErrorBlock(message = state.error ?: "Unknown error", onRetry = onRetry)
        }
    }
    if (!state.loading && state.error == null && filteredCount == 0) {
        item(span = { GridItemSpan(maxLineSpan) }, key = "empty") {
            EmptyBlock(activeFilter = activeFilter, onClearFilter = onClearFilter)
        }
    }
}

private fun LazyListScope.stateItemsColumn(
    state: CategoryViewModel.CategoryRecipesState,
    filteredCount: Int,
    activeFilter: DietaryFilter,
    onClearFilter: () -> Unit,
    onRetry: () -> Unit,
) {
    if (state.loading) item(key = "loading") { LoadingBlock() }
    if (state.error != null && !state.loading) {
        item(key = "error") { ErrorBlock(message = state.error ?: "Unknown error", onRetry = onRetry) }
    }
    if (!state.loading && state.error == null && filteredCount == 0) {
        item(key = "empty") { EmptyBlock(activeFilter = activeFilter, onClearFilter = onClearFilter) }
    }
}

@Composable
private fun LoadingBlock() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator()
            Text(
                "Loading recipes…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun ErrorBlock(message: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Oops! Something went wrong",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            Button(onClick = onRetry, modifier = Modifier.padding(top = 4.dp)) {
                Text("Try Again")
            }
        }
    }
}

@Composable
private fun EmptyBlock(activeFilter: DietaryFilter, onClearFilter: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("No recipes found", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                if (activeFilter != DietaryFilter.ALL)
                    "No recipes match your dietary filter. Try clearing the filter or selecting a different one."
                else
                    "No recipes available for this category right now. Please try again later.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            if (activeFilter != DietaryFilter.ALL) {
                Button(onClick = onClearFilter) { Text("Clear filter") }
            }
        }
    }
}

@Composable
private fun LoadMoreButton(isLoading: Boolean, totalLoaded: Int, onClick: () -> Unit) {
    Column {
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
                Text("Loading…")
            } else {
                Text("Load more ($totalLoaded loaded)")
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

