package com.example.myrecipeapp.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.myrecipeapp.R
import com.example.myrecipeapp.ui.navigation.LocalTabReselectEvents
import com.example.myrecipeapp.ui.navigation.RecipeDetail
import com.example.myrecipeapp.ui.viewmodel.MainViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import com.example.myrecipeapp.ui.navigation.Search as SearchRoute

// Popular search tags now live in res/values/strings.xml (search_popular_tags,
// search_fallback_suggestions) so they can be edited without a code change.

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun SearchScreen(
    navController: NavHostController,
    viewModel: MainViewModel
) {
    var searchText by remember { mutableStateOf(viewModel.searchState.value.query) }
    val hapticFeedback = LocalHapticFeedback.current
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    // Focus + show keyboard on first entry only — if the user has already typed
    // a query in this session, don't steal focus back when they navigate here again.
    LaunchedEffect(Unit) {
        if (searchText.isEmpty()) {
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }

    // Debounce search queries in the UI layer — 300 ms of inactivity before we
    // fire the use case. Keeps the ViewModel free of timing concerns.
    LaunchedEffect(Unit) {
        snapshotFlow { searchText }
            .debounce(300)
            .distinctUntilChanged()
            .collect { q ->
                if (q.isNotEmpty()) viewModel.searchRecipes(q)
            }
    }

    // Hoist results list state so reselecting Search tab smooth-scrolls results to top.
    val resultsListState = rememberLazyListState()
    val tabReselectEvents = LocalTabReselectEvents.current
    LaunchedEffect(tabReselectEvents) {
        tabReselectEvents.events.filter { it is SearchRoute }.collect {
            if (resultsListState.layoutInfo.totalItemsCount > 0) {
                resultsListState.animateScrollToItem(0)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
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
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
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
                SearchIdleState(
                    viewModel = viewModel,
                    onTagClick = { tag ->
                        // Strip emoji prefix (e.g., "🍝 Pasta" → "Pasta")
                        searchText = tag.substringAfter(" ")
                    }
                )
            } else {
                SearchResults(
                    searchText = searchText,
                    viewModel = viewModel,
                    listState = resultsListState,
                    onTagClick = { tag ->
                        searchText = tag.substringAfter(" ")
                    },
                    onRecipeClick = { recipeId ->
                        viewModel.setRecipeSwipeList(viewModel.searchState.value.recipes.map { it.id })
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
private fun SearchIdleState(
    viewModel: MainViewModel,
    onTagClick: (String) -> Unit
) {
    val popularTags = stringArrayResource(R.array.search_popular_tags).toList()
    val recentSearches by viewModel.recentSearches
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
            enter = slideInVertically(
                initialOffsetY = { 20 },
                animationSpec = spring(stiffness = 300f)
            ) + fadeIn()
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

                // ── Recent searches ──────────────────────────────────────────
                if (recentSearches.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Recent",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(
                            onClick = { viewModel.clearAllRecentSearches() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text(
                                "Clear all",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(recentSearches, key = { it }) { query ->
                            var chipVisible by remember { mutableStateOf(false) }
                            LaunchedEffect(query) { chipVisible = true }
                            AnimatedVisibility(
                                visible = chipVisible,
                                enter = scaleIn(
                                    initialScale = 0.85f,
                                    animationSpec = spring(Spring.DampingRatioNoBouncy, 320f)
                                ) + fadeIn()
                            ) {
                                // Custom chip: [🕐 query  ✕]
                                Surface(
                                    onClick = { onTagClick(query) },
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Row(
                                        modifier = Modifier.padding(
                                            start = 10.dp,
                                            end = 6.dp,
                                            top = 7.dp,
                                            bottom = 7.dp
                                        ),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.History,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            query,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Surface(
                                            onClick = { viewModel.clearRecentSearch(query) },
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                                alpha = 0.12f
                                            ),
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Remove $query",
                                                    modifier = Modifier.size(10.dp),
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // ── Popular tag chips ────────────────────────────────────────
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
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = 320f
                                    )
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
    listState: androidx.compose.foundation.lazy.LazyListState = rememberLazyListState(),
    onTagClick: (String) -> Unit = {},
    onRecipeClick: (String) -> Unit = {}
) {
    // Searches are launched from SearchScreen via a debounced snapshotFlow.
    val searchState by viewModel.searchState

    // Hoisted above the list — same lambda instance across ALL recompositions,
    // so no card recomposes just because the parent SearchResults recomposed.
    val onFavToggle = remember { viewModel::toggleFavorite }

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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
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
                var noResultVisible by remember { mutableStateOf(false) }
                LaunchedEffect(searchText) {
                    noResultVisible = false; delay(80); noResultVisible = true
                }

                AnimatedVisibility(
                    visible = noResultVisible,
                    enter = slideInVertically(
                        initialOffsetY = { it / 5 },
                        animationSpec = spring(
                            Spring.DampingRatioMediumBouncy,
                            Spring.StiffnessMediumLow
                        )
                    ) + fadeIn(animationSpec = tween(300))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Illustrated empty state card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Layered emoji illustration
                                Box(contentAlignment = Alignment.Center) {
                                    Text("🍽️", fontSize = 72.sp)
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(32.dp),
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.errorContainer
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("❌", fontSize = 14.sp)
                                        }
                                    }
                                }

                                Text(
                                    text = "No recipes found",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "\"$searchText\" didn't match any recipes.\nTry a different ingredient or dish name.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 22.sp
                                )
                            }
                        }

                        // Quick-try suggestion chips
                        Text(
                            "Try instead:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val fallbackSuggestions =
                            stringArrayResource(R.array.search_fallback_suggestions).toList()
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            items(fallbackSuggestions, key = { it }) { suggestion ->
                                SuggestionChip(
                                    onClick = { onTagClick(suggestion.substringAfter(" ")) },
                                    label = {
                                        Text(
                                            suggestion,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }
                }
            }

            else -> {
                // Auto-paginate when the user scrolls within 3 items of the end.
                LaunchedEffect(listState, searchState.recipes.size, searchState.totalResults) {
                    snapshotFlow {
                        val info = listState.layoutInfo
                        val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
                        last to info.totalItemsCount
                    }.collect { (last, total) ->
                        val hasMore = searchState.recipes.size < searchState.totalResults
                        if (hasMore && !searchState.loadingMore && total > 0 && last >= total - 3) {
                            viewModel.loadMoreSearchResults()
                        }
                    }
                }
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    itemsIndexed(
                        items = searchState.recipes,
                        key = { _, recipe -> recipe.id }
                    ) { index, recipe ->
                        // derivedStateOf: only THIS card recomposes when its own favorite status changes
                        val isFavorite by remember(recipe.id) {
                            derivedStateOf { viewModel.favoriteIds.value.contains(recipe.id) }
                        }
                        // Stable per-recipe click — only recreated if recipe.id changes
                        val onCardClick = remember(recipe.id) { { onRecipeClick(recipe.id) } }

                        var visible by remember { mutableStateOf(false) }
                        LaunchedEffect(recipe.id) {
                            delay(minOf(index, 5) * 50L)  // cap: max 250ms stagger, not 50*50ms
                            visible = true
                        }
                        AnimatedVisibility(
                            visible = visible,
                            enter = slideInVertically(
                                initialOffsetY = { 30 },
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = 300f
                                )
                            ) + fadeIn(tween(200))
                        ) {
                            EnhancedRecipeCard(
                                recipe = recipe,
                                isFavorite = isFavorite,
                                onFavoriteToggle = onFavToggle,
                                onClick = onCardClick
                            )
                        }
                    }

                    if (searchState.recipes.size < searchState.totalResults) {
                        item {
                            // Footer spinner while auto-pagination loads the next page.
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (searchState.loadingMore) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(32.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 3.dp
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
