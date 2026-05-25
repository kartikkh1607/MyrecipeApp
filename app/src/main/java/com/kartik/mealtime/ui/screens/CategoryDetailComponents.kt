package com.kartik.mealtime.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kartik.mealtime.domain.model.DietaryFilter
import com.kartik.mealtime.domain.model.Recipe
import com.kartik.mealtime.domain.model.RecipeDifficulty
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedRecipeCard(
    recipe: Recipe,
    onClick: () -> Unit,
    isFavorite: Boolean = false,
    onFavoriteToggle: (Recipe) -> Unit = {}
) {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "card_scale"
    )

    val favoriteColor by animateColorAsState(
        targetValue = if (isFavorite) Color(0xFFE91E63) else Color.White.copy(alpha = 0.9f),
        label = "favorite_color"
    )

    var heartBounce by remember { mutableStateOf(false) }
    val heartScale by animateFloatAsState(
        targetValue = if (heartBounce) 1.4f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "heart_scale",
        finishedListener = { heartBounce = false }
    )

    val ratingText = remember(recipe.rating) {
        String.format(Locale.ROOT, "%.1f", recipe.rating)
    }
    val difficultyBgColor = remember(recipe.difficulty) {
        when (recipe.difficulty) {
            RecipeDifficulty.EASY -> Color(0xFF2E7D32)
            RecipeDifficulty.MEDIUM -> Color(0xFFE65100)
            RecipeDifficulty.HARD -> Color(0xFFC62828)
        }
    }
    val imageScrim = remember {
        Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))
        )
    }

    Card(
        onClick = {
            isPressed = true
            onClick()
        },
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp,
            pressedElevation = 8.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            // Hero image section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                AsyncImage(
                    model = recipe.imageUrl,
                    contentDescription = recipe.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Bottom scrim so bottom badges stay readable
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(imageScrim)
                )

                // Rating badge — top start
                if (recipe.rating > 0) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp),
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFC107),
                                modifier = Modifier.size(13.dp)
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

                // Favorite button — top end
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(36.dp),
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.45f),
                    onClick = {
                        heartBounce = true
                        onFavoriteToggle(recipe)
                    }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = favoriteColor,
                            modifier = Modifier
                                .size(18.dp)
                                .graphicsLayer { scaleX = heartScale; scaleY = heartScale }
                        )
                    }
                }

                // Difficulty badge — bottom start
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp),
                    color = difficultyBgColor.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${recipe.difficulty.emoji()} ${recipe.difficulty.displayName()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }

                // Cook time badge — bottom end
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "${recipe.cookTime}m",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Info section
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = recipe.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (recipe.description.isNotBlank()) {
                    Text(
                        text = recipe.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (recipe.cuisine.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = recipe.cuisine,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (recipe.servings > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = "👤 ${recipe.servings}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (recipe.calories != null && recipe.calories > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = "🔥 ${recipe.calories} kcal",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }

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

        // Hoist .distinct() out of the items() call so it runs once per filter-list
        // change rather than on every recomposition of the sheet.
        val distinctFilters = remember(availableFilters) { availableFilters.distinct() }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(distinctFilters, key = { it.name }) { filter ->
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