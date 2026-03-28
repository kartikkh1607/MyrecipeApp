package com.example.myrecipeapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.myrecipeapp.ui.navigation.RecipeDetail
import com.example.myrecipeapp.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay

// Popular search tags shown when the search box is empty
private val popularTags = listOf(
    "🍝 Pasta", "🍛 Curry", "🥗 Salad", "🍕 Pizza",
    "🍜 Noodles", "🥩 Grilled", "🥞 Breakfast", "🍰 Dessert"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavHostController,
    viewModel: MainViewModel
) {
    var searchText by remember { mutableStateOf(viewModel.searchState.value.query) }
    val hapticFeedback = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        // ── Header ─────────────────────────────────────────────────────────────
        Text(
            text = "Search Recipes",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
        )

        // ── Search bar (filled surface style) ──────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                TextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            "Recipes, ingredients, cuisines…",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    singleLine = true
                )
                AnimatedVisibility(
                    visible = searchText.isNotEmpty(),
                    enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    IconButton(onClick = {
                        searchText = ""
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Content ────────────────────────────────────────────────────────────
        AnimatedContent(
            targetState = searchText.isEmpty(),
            transitionSpec = {
                fadeIn(tween(220)) togetherWith fadeOut(tween(150))
            },
            label = "search_content"
        ) { isEmpty ->
            if (isEmpty) {
                SearchIdleState(onTagClick = { tag ->
                    // Strip emoji prefix (e.g., "🍝 Pasta" → "Pasta")
                    searchText = tag.substringAfter(" ")
                })
            } else {
                SearchResults(
                    searchText = searchText,
                    viewModel = viewModel,
                    onRecipeClick = { recipeId ->
                        navController.navigate(RecipeDetail(recipeId = recipeId)) {
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    }
}

// ── Idle / empty state ────────────────────────────────────────────────────────
@Composable
private fun SearchIdleState(onTagClick: (String) -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(80); visible = true }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Animated large search icon
        AnimatedVisibility(
            visible = visible,
            enter = scaleIn(
                initialScale = 0.7f,
                animationSpec = spring(Spring.DampingRatioLowBouncy, 260f)
            ) + fadeIn()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.size(100.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(initialOffsetY = { 20 }, animationSpec = spring(stiffness = 300f)) + fadeIn()
        ) {
            Column {
                Text(
                    "What are you craving?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Search or pick a popular category below",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(20.dp))

                // Popular tag chips
                Text(
                    "Popular",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Two rows of chips
                popularTags.chunked(4).forEachIndexed { rowIndex, rowTags ->
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        itemsIndexed(rowTags) { i, tag ->
                            var tagVisible by remember { mutableStateOf(false) }
                            LaunchedEffect(tag) {
                                delay((rowIndex * 4 + i) * 40L)
                                tagVisible = true
                            }
                            AnimatedVisibility(
                                visible = tagVisible,
                                enter = slideInHorizontally(
                                    initialOffsetX = { 30 },
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 320f)
                                ) + fadeIn()
                            ) {
                                SuggestionChip(
                                    onClick = { onTagClick(tag) },
                                    label = {
                                        Text(
                                            tag,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Search results ────────────────────────────────────────────────────────────
@Composable
fun SearchResults(
    searchText: String,
    viewModel: MainViewModel,
    onRecipeClick: (String) -> Unit = {}
) {
    // favoriteIds MUST be observed here (inside this composable) for proper recomposition
    val favoriteIds by viewModel.favoriteIds

    LaunchedEffect(searchText) {
        if (searchText.isNotEmpty()) {
            delay(300) // debounce
            viewModel.searchRecipes(searchText)
        }
    }

    val searchState by viewModel.searchState

    Column {
        // Results header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Results for \"$searchText\"",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            AnimatedVisibility(visible = !searchState.loading && searchState.recipes.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        "${searchState.recipes.size} found",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        when {
            searchState.loading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text(
                            "Searching…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            searchState.error != null -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("⚠️", fontSize = 28.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Search Error",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            searchState.error ?: "Unknown error",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            searchState.recipes.isEmpty() && !searchState.loading -> {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🔍", fontSize = 40.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "No recipes found",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        "Try different keywords",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    itemsIndexed(searchState.recipes) { index, recipe ->
                        var visible by remember { mutableStateOf(false) }
                        LaunchedEffect(recipe.id) {
                            delay(index * 50L)
                            visible = true
                        }
                        AnimatedVisibility(
                            visible = visible,
                            enter = slideInVertically(
                                initialOffsetY = { 30 },
                                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 300f)
                            ) + fadeIn(tween(200))
                        ) {
                            EnhancedRecipeCard(
                                recipe = recipe,
                                isFavorite = favoriteIds.contains(recipe.id),
                                onFavoriteToggle = { viewModel.toggleFavorite(it) },
                                onClick = { onRecipeClick(recipe.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}
