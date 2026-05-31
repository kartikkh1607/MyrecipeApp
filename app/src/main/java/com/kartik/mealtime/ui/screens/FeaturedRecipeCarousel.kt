package com.kartik.mealtime.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.kartik.mealtime.domain.model.FeaturedRecipe
import com.kartik.mealtime.domain.model.Recipe
import com.kartik.mealtime.ui.navigation.Categories
import com.kartik.mealtime.ui.navigation.Home
import com.kartik.mealtime.ui.navigation.RecipeDetail
import com.kartik.mealtime.ui.theme.StarGold
import com.kartik.mealtime.ui.viewmodel.FavoritesViewModel
import com.kartik.mealtime.ui.viewmodel.MainViewModel
import java.util.Locale
import androidx.compose.ui.text.intl.Locale as ComposeLocale
import androidx.compose.ui.text.toUpperCase

// ── Featured today — horizontal rail of editorial cards (Linen prototype) ─────
// A plain snapping rail (no dot indicators): 256-wide cards, diet pills top-left,
// a heart save button top-right, cuisine kicker + serif name + time/rating.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeaturedRecipeCarousel(
    navController: NavHostController,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    favoritesViewModel: FavoritesViewModel = hiltViewModel()
) {
    val homeRecipeState by viewModel.homeRecipeState.collectAsStateWithLifecycle()
    val featuredRecipes = homeRecipeState.featuredRecipes
    val favoriteIds by favoritesViewModel.favoriteIds.collectAsStateWithLifecycle()
    val hapticFeedback = LocalHapticFeedback.current

    val listState = rememberLazyListState()
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    Column(modifier = modifier.fillMaxWidth()) {
        HomeSectionHead(
            title = "Featured today",
            action = "See all",
            onAction = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                navController.navigate(Categories) {
                    popUpTo<Home> { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        LazyRow(
            state = listState,
            flingBehavior = flingBehavior,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(horizontal = 20.dp)
        ) {
            items(featuredRecipes, key = { it.recipe.id }) { featured ->
                FeaturedRecipeCard(
                    featuredRecipe = featured,
                    saved = favoriteIds.contains(featured.recipe.id),
                    onSave = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        favoritesViewModel.toggleFavorite(featured.recipe)
                    },
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.setRecipeSwipeList(featuredRecipes.map { it.recipe.id })
                        navController.navigate(RecipeDetail(recipeId = featured.recipe.id)) {
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeaturedRecipeCard(
    featuredRecipe: FeaturedRecipe,
    saved: Boolean,
    onSave: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val recipe = featuredRecipe.recipe
    val diets = remember(recipe) { recipe.dietShorts() }
    val totalTime = recipe.prepTime + recipe.cookTime

    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium),
        label = "featured_scale",
        finishedListener = { pressed = false }
    )

    val scrim = remember {
        Brush.verticalGradient(
            colorStops = arrayOf(
                0.0f to Color.Transparent,
                0.38f to Color.Transparent,
                1.0f to Color.Black.copy(alpha = 0.82f)
            )
        )
    }

    Card(
        onClick = { pressed = true; onClick() },
        modifier = modifier
            .width(256.dp)
            .height(300.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale },
        shape = RoundedCornerShape(26.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = recipe.imageUrl,
                contentDescription = recipe.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scrim)
            )

            // Top-left: diet pills (frosted glass)
            if (diets.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(13.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    diets.forEach { short ->
                        Surface(
                            shape = RoundedCornerShape(7.dp),
                            color = Color.White.copy(alpha = 0.18f)
                        ) {
                            Text(
                                text = short,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Top-right: heart save button (glass circle)
            Surface(
                onClick = onSave,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(11.dp)
                    .size(34.dp),
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.34f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (saved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (saved) "Remove from saved" else "Save",
                        tint = if (saved) MaterialTheme.colorScheme.error else Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            // Bottom content
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                if (recipe.cuisine.isNotBlank()) {
                    Text(
                        text = recipe.cuisine.toUpperCase(ComposeLocale.current),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.82f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                }
                Text(
                    text = recipe.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(9.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (totalTime > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${totalTime}m",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White
                            )
                        }
                    }
                    if (recipe.rating > 0f) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = StarGold,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = String.format(Locale.ROOT, "%.1f", recipe.rating),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

// Short diet labels derived from the recipe flags (max 2, matching the prototype).
private fun Recipe.dietShorts(): List<String> = buildList {
    when {
        isVegan -> add("VGN")
        isVegetarian -> add("VEG")
    }
    if (isGlutenFree) add("GF")
    if (isDairyFree) add("DF")
    if (isKeto) add("KETO")
    if (isLowCarb) add("LC")
}.take(2)
