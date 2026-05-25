package com.kartik.mealtime.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.kartik.mealtime.ui.navigation.LocalTabReselectEvents
import com.kartik.mealtime.ui.navigation.RecipeDetail
import com.kartik.mealtime.ui.viewmodel.MainViewModel
import com.kartik.mealtime.ui.viewmodel.SearchViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import com.kartik.mealtime.ui.navigation.Search as SearchRoute

// Popular search tags now live in res/values/strings.xml (search_popular_tags,
// search_fallback_suggestions) so they can be edited without a code change.

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun SearchScreen(
    navController: NavHostController,
    viewModel: MainViewModel,
    searchViewModel: SearchViewModel
) {
    var searchText by remember { mutableStateOf(searchViewModel.searchState.value.query) }
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
                if (q.isNotEmpty()) searchViewModel.searchRecipes(q)
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
                    viewModel = searchViewModel,
                    onTagClick = { tag ->
                        // Strip emoji prefix (e.g., "🍝 Pasta" → "Pasta")
                        searchText = tag.substringAfter(" ")
                    }
                )
            } else {
                SearchResults(
                    searchText = searchText,
                    searchViewModel = searchViewModel,
                    listState = resultsListState,
                    onTagClick = { tag ->
                        searchText = tag.substringAfter(" ")
                    },
                    onRecipeClick = { recipeId ->
                        viewModel.setRecipeSwipeList(searchViewModel.searchState.value.recipes.map { it.id })
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
