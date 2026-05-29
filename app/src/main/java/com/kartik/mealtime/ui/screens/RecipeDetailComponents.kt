package com.kartik.mealtime.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kartik.mealtime.domain.model.Ingredient
import com.kartik.mealtime.domain.model.NutritionInfo
import com.kartik.mealtime.domain.model.Recipe
import com.kartik.mealtime.domain.model.RecipeStep
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

// ── Hero section ──────────────────────────────────────────────────────────────
@Composable
fun RecipeHeroSection(
    recipe: Recipe,
    isFavorite: Boolean,
    parallaxOffsetPx: Float = 0f,
    onBackClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onShareClick: () -> Unit = {}
) {
    val favoriteScale by animateFloatAsState(
        targetValue = if (isFavorite) 1.25f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "favorite_scale"
    )
    val favoriteColor by animateColorAsState(
        targetValue = if (isFavorite) Color(0xFFFF4E6A) else Color.White,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 400f),
        label = "favorite_color"
    )

    var burstTrigger by remember { mutableIntStateOf(0) }
    val wasFavorite = remember { mutableStateOf(isFavorite) }
    LaunchedEffect(isFavorite) {
        if (isFavorite && !wasFavorite.value) burstTrigger++
        wasFavorite.value = isFavorite
    }

    val overlayBrush = remember {
        Brush.verticalGradient(
            colorStops = arrayOf(
                0.0f to Color.Black.copy(alpha = 0.40f),
                0.35f to Color.Transparent,
                0.70f to Color.Black.copy(alpha = 0.55f),
                1.0f to Color.Black.copy(alpha = 0.90f)
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(380.dp)
    ) {
        AsyncImage(
            model = recipe.imageUrl,
            contentDescription = recipe.name,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationY = -parallaxOffsetPx },
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(overlayBrush)
        )

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.35f), CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onShareClick,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.35f), CircleShape)
                ) {
                    Icon(Icons.Default.Share, "Share", tint = Color.White)
                }
                Box(contentAlignment = Alignment.Center) {
                    HeartBurstOverlay(trigger = burstTrigger)
                    IconButton(
                        onClick = onFavoriteClick,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                            .graphicsLayer { scaleX = favoriteScale; scaleY = favoriteScale }
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isFavorite) "Remove" else "Favorite",
                            tint = favoriteColor
                        )
                    }
                }
            }
        }

        FavoriteAddedLabel(
            trigger = burstTrigger,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(end = 16.dp, top = 56.dp)
        )

        // Bottom info
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            // Cuisine + difficulty chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (recipe.cuisine.isNotBlank()) {
                    HeroChip(recipe.cuisine)
                }
                HeroChip("${recipe.difficulty.emoji()} ${recipe.difficulty.displayName()}")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = recipe.name,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Rating + time row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (recipe.rating > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Star,
                            null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format(Locale.ROOT, "%.1f", recipe.rating),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (recipe.reviewCount > 0) {
                            Text(
                                text = " (${recipe.reviewCount})",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Timer, null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${recipe.prepTime + recipe.cookTime} min",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
                if (recipe.calories != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocalFireDepartment, null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${recipe.calories} kcal",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroChip(text: String) {
    Surface(
        color = Color.White.copy(alpha = 0.18f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

// ── Overview 2×2 stat grid ────────────────────────────────────────────────────
@Composable
fun RecipeOverviewSection(recipe: Recipe) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatTile(
                icon = Icons.Default.Timer,
                label = "Prep",
                value = "${recipe.prepTime} min",
                modifier = Modifier.weight(1f)
            )
            StatTile(
                icon = Icons.Default.LocalFireDepartment,
                label = "Cook",
                value = "${recipe.cookTime} min",
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatTile(
                icon = Icons.Default.Restaurant,
                label = "Servings",
                value = "${recipe.servings}",
                modifier = Modifier.weight(1f)
            )
            StatTile(
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                label = "Difficulty",
                value = recipe.difficulty.displayName(),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    // In dark mode, primary-on-primaryContainer is only 1.4:1 (Teal on darker Teal).
    // Flip to solid primary + onPrimary so the icon actually reads (7.5:1).
    val isDarkScheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val tileBackground = if (isDarkScheme) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.primaryContainer
    val iconTint = if (isDarkScheme) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.primary

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        tileBackground,
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── "About this recipe" tappable row — opens description bottom sheet ────────
@Composable
fun AboutRecipeRow(onClick: () -> Unit) {
    // In dark mode, primary-on-primaryContainer is only 1.4:1 (Teal on darker Teal).
    // Flip to solid primary + onPrimary so the icon actually reads (7.5:1).
    val isDarkScheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val tileBackground = if (isDarkScheme) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.primaryContainer
    val iconTint = if (isDarkScheme) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.primary

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            tileBackground,
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        "About this recipe",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Tap to read more",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer { rotationZ = 180f }
            )
        }
    }
}

// ── Action buttons ────────────────────────────────────────────────────────────
@Composable
fun CookingActionButtons(
    recipeName: String,
    onStartCooking: () -> Unit,
    onAddToShoppingList: () -> Unit,
    // Resolves a YouTube URL for the recipe (specific video if found, else search).
    // Suspends because it may hit the network — see MainViewModel.resolveYoutubeUrl.
    resolveVideoUrl: suspend (String) -> String
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isFindingVideo by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onStartCooking,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Cooking", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onAddToShoppingList,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.ShoppingCart, null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Shopping List", fontWeight = FontWeight.Medium)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            onClick = {
                if (isFindingVideo) return@Surface
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                isFindingVideo = true
                scope.launch {
                    // resolveVideoUrl never throws — it returns a search URL on miss/error.
                    val url = resolveVideoUrl(recipeName)
                    isFindingVideo = false
                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
                    context.startActivity(intent)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFFF0000).copy(alpha = 0.08f),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                Color(0xFFFF0000).copy(alpha = 0.25f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isFindingVideo) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFFFF0000)
                    )
                } else {
                    Text("▶", fontSize = 14.sp, color = Color(0xFFFF0000))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    if (isFindingVideo) "Finding video…" else "Watch Video on YouTube",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFFF0000)
                )
            }
        }
    }
}

// ── Ingredients section with check-off ───────────────────────────────────────
@Composable
fun IngredientsSection(
    ingredients: List<Ingredient>,
    scale: Float = 1f,
    checkedSet: Set<Int> = emptySet(),
    onToggleChecked: (Int) -> Unit = {}
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Ingredients",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (ingredients.isNotEmpty()) {
                val checkedCount = checkedSet.size
                AnimatedContent(
                    targetState = checkedCount,
                    transitionSpec = {
                        slideInVertically { -it } + fadeIn() togetherWith
                                slideOutVertically { it } + fadeOut()
                    },
                    label = "checked_count"
                ) { count ->
                    Text(
                        text = "$count / ${ingredients.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (count == ingredients.size)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                ingredients.forEachIndexed { index, ingredient ->
                    IngredientItem(
                        ingredient = ingredient,
                        scale = scale,
                        isChecked = index in checkedSet,
                        onToggle = { onToggleChecked(index) },
                        isLast = index == ingredients.lastIndex
                    )
                }
            }
        }
    }
}

/**
 * Scales a numeric amount string by [scale], preserving the original string for
 * non-numeric values (e.g. "a pinch"). Whole numbers render without decimals;
 * fractional values get up to two decimal places with trailing zeros stripped.
 */
internal fun scaleAmount(original: String, scale: Float): String {
    val d = original.toDoubleOrNull() ?: return original
    val scaled = d * scale
    if (scaled == kotlin.math.floor(scaled)) return scaled.toInt().toString()
    return String.format(Locale.ROOT, "%.2f", scaled).trimEnd('0').trimEnd('.')
}

@Composable
fun IngredientItem(
    ingredient: Ingredient,
    scale: Float = 1f,
    isChecked: Boolean = false,
    onToggle: () -> Unit = {},
    isLast: Boolean
) {
    val amount = remember(ingredient.amount, scale) { scaleAmount(ingredient.amount, scale) }
    val amountLabel = if (ingredient.unit.isBlank()) amount else "$amount ${ingredient.unit}"

    val alpha by animateFloatAsState(
        targetValue = if (isChecked) 0.45f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "ingredient_alpha"
    )
    val checkScale by animateFloatAsState(
        targetValue = if (isChecked) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "check_scale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 10.dp)
            .alpha(alpha),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Checkmark / dot indicator
        Box(
            modifier = Modifier.size(22.dp),
            contentAlignment = Alignment.Center
        ) {
            // Unchecked dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .graphicsLayer { this.alpha = 1f - checkScale }
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
            // Checked icon
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer {
                        this.alpha = checkScale
                        scaleX = checkScale
                        scaleY = checkScale
                    }
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = ingredient.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Text(
                text = amountLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }

    if (!isLast) {
        HorizontalDivider(
            modifier = Modifier.padding(start = 34.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
        )
    }
}

// ── Instructions header with progress ────────────────────────────────────────
@Composable
internal fun InstructionsHeader(totalSteps: Int, completedCount: Int) {
    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Instructions",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            AnimatedContent(
                targetState = completedCount,
                transitionSpec = {
                    slideInVertically { -it } + fadeIn() togetherWith slideOutVertically { it } + fadeOut()
                },
                label = "steps_count"
            ) { count ->
                Text(
                    text = "$count / $totalSteps",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (count == totalSteps && totalSteps > 0)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (totalSteps > 0) {
            Spacer(modifier = Modifier.height(10.dp))
            val progress by animateFloatAsState(
                targetValue = completedCount.toFloat() / totalSteps.toFloat(),
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "steps_progress"
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                strokeCap = StrokeCap.Round
            )
        }
    }
}

// ── Instruction step card with completion ────────────────────────────────────
@Composable
fun InstructionStepCard(
    step: RecipeStep,
    isCompleted: Boolean = false,
    onToggle: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val cardAlpha by animateFloatAsState(
        targetValue = if (isCompleted) 0.6f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "step_alpha"
    )
    val accentColor by animateColorAsState(
        targetValue = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary,
        label = "step_accent"
    )
    val checkScale by animateFloatAsState(
        targetValue = if (isCompleted) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "step_check_scale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .alpha(cardAlpha)
            .clickable(onClick = onToggle),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Step number / check circle
            Box(
                modifier = Modifier.size(36.dp),
                contentAlignment = Alignment.Center
            ) {
                // Number circle (fades out when completed)
                Surface(
                    modifier = Modifier
                        .size(36.dp)
                        .graphicsLayer { this.alpha = 1f - checkScale },
                    color = accentColor,
                    shape = CircleShape
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = step.stepNumber.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                // Check icon (springs in when completed)
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Completed",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(36.dp)
                        .graphicsLayer {
                            this.alpha = checkScale
                            scaleX = checkScale
                            scaleY = checkScale
                        }
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = step.instruction,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.2f
                )

                if (step.duration != null && !isCompleted) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Timer, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${step.duration} minutes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (step.tips != null && !isCompleted) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "💡 ${step.tips}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── Nutrition section with macro bars ─────────────────────────────────────────
@Composable
fun NutritionSection(nutritionInfo: NutritionInfo) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "Nutrition Facts",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Calories — prominent
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocalFireDepartment,
                            null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Calories",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        "${nutritionInfo.calories} kcal",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                Spacer(modifier = Modifier.height(20.dp))

                // Macro bars
                val totalMacroG = nutritionInfo.protein + nutritionInfo.carbs + nutritionInfo.fat
                if (totalMacroG > 0) {
                    MacroBar(
                        label = "Protein",
                        value = nutritionInfo.protein,
                        total = totalMacroG,
                        unit = "g",
                        color = Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    MacroBar(
                        label = "Carbs",
                        value = nutritionInfo.carbs,
                        total = totalMacroG,
                        unit = "g",
                        color = Color(0xFF2196F3)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    MacroBar(
                        label = "Fat",
                        value = nutritionInfo.fat,
                        total = totalMacroG,
                        unit = "g",
                        color = Color(0xFFFF9800)
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        NutritionItem("Protein", "${nutritionInfo.protein.toInt()}", "g")
                        NutritionItem("Carbs", "${nutritionInfo.carbs.toInt()}", "g")
                        NutritionItem("Fat", "${nutritionInfo.fat.toInt()}", "g")
                    }
                }
            }
        }
    }
}

@Composable
private fun MacroBar(label: String, value: Float, total: Float, unit: String, color: Color) {
    val fraction = (value / total).coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "macro_bar_$label"
    )
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "${value.toInt()} $unit",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { animatedFraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.15f),
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
fun NutritionItem(label: String, value: String, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = unit,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── Tags section — FlowRow ────────────────────────────────────────────────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagsSection(tags: List<String>) {
    if (tags.isNotEmpty()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "Tags",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tags.forEach { tag ->
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── Section jump nav ──────────────────────────────────────────────────────────
@Composable
internal fun RecipeSectionNav(
    hasNutrition: Boolean,
    onIngredients: () -> Unit,
    onInstructions: () -> Unit,
    onNutrition: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(50.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shadowElevation = 8.dp,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SectionNavPill(label = "Ingredients", onClick = onIngredients)
            SectionNavPill(label = "Instructions", onClick = onInstructions)
            if (hasNutrition) SectionNavPill(label = "Nutrition", onClick = onNutrition)
        }
    }
}

@Composable
private fun SectionNavPill(label: String, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val bgScale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessHigh),
        label = "nav_pill_scale",
        finishedListener = { pressed = false }
    )
    Surface(
        onClick = { pressed = true; onClick() },
        shape = RoundedCornerShape(40.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.graphicsLayer { scaleX = bgScale; scaleY = bgScale }
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

// ── Servings stepper ──────────────────────────────────────────────────────────
@Composable
internal fun ServingsStepper(servings: Int, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Servings",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 1.dp
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { if (servings > 1) onChange(servings - 1) },
                    enabled = servings > 1
                ) {
                    Icon(
                        Icons.Default.Remove,
                        "Decrease servings",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                AnimatedContent(
                    targetState = servings,
                    transitionSpec = {
                        val up = targetState > initialState
                        slideInVertically { if (up) -it else it } + fadeIn() togetherWith
                                slideOutVertically { if (up) it else -it } + fadeOut()
                    },
                    label = "servings_number"
                ) { count ->
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(32.dp)
                    )
                }
                IconButton(
                    onClick = { if (servings < 20) onChange(servings + 1) },
                    enabled = servings < 20
                ) {
                    Icon(
                        Icons.Default.Add,
                        "Increase servings",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

// ── Recipe overview helper (kept for external callers if any) ─────────────────
@Composable
fun RecipeInfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── Heart burst effect ────────────────────────────────────────────────────────
@Composable
private fun HeartBurstOverlay(trigger: Int) {
    val particles = remember {
        (0 until 8).map { i ->
            val rad = Math.toRadians(i * 45.0 + 22.5)
            Triple(cos(rad).toFloat(), sin(rad).toFloat(), (i * 55L) % 180L)
        }
    }
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
        particles.forEach { (dx, dy, delayMs) ->
            key(trigger) {
                if (trigger > 0) FloatingHeart(dx = dx, dy = dy, delayMs = delayMs)
            }
        }
    }
}

@Composable
private fun FloatingHeart(dx: Float, dy: Float, delayMs: Long) {
    val progress = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        delay(delayMs)
        scope.launch { progress.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
        alpha.animateTo(1f, tween(100))
        delay(350)
        alpha.animateTo(0f, tween(350))
    }

    Icon(
        imageVector = Icons.Default.Favorite,
        contentDescription = null,
        tint = Color(0xFFFF4E6A),
        modifier = Modifier
            .size(18.dp)
            .graphicsLayer {
                val maxPx = 55.dp.toPx()
                translationX = dx * maxPx * progress.value
                translationY = dy * maxPx * progress.value
                this.alpha = alpha.value
                scaleX = 0.3f + progress.value * 0.7f
                scaleY = 0.3f + progress.value * 0.7f
            }
    )
}

// ── "Saved!" pill label ────────────────────────────────────────────────────────
@Composable
private fun FavoriteAddedLabel(trigger: Int, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        visible = true
        delay(1_500)
        visible = false
    }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(initialOffsetY = { 16 }, animationSpec = tween(250)) + fadeIn(
            tween(200)
        ),
        exit = slideOutVertically(targetOffsetY = { -16 }, animationSpec = tween(250)) + fadeOut(
            tween(200)
        )
    ) {
        Surface(
            color = Color(0xFFFF4E6A),
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.Favorite,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    "Saved!",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
