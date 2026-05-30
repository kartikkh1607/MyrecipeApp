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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.Check
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
    showInfo: Boolean = true,
    heroHeight: androidx.compose.ui.unit.Dp = 380.dp,
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
        targetValue = if (isFavorite) com.kartik.mealtime.ui.theme.Heart else Color.White,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 400f),
        label = "favorite_color"
    )

    var burstTrigger by remember { mutableIntStateOf(0) }
    val wasFavorite = remember { mutableStateOf(isFavorite) }
    LaunchedEffect(isFavorite) {
        if (isFavorite && !wasFavorite.value) burstTrigger++
        wasFavorite.value = isFavorite
    }

    val pageBg = MaterialTheme.colorScheme.background
    val overlayBrush = if (showInfo) {
        Brush.verticalGradient(
            colorStops = arrayOf(
                0.0f to Color.Black.copy(alpha = 0.40f),
                0.35f to Color.Transparent,
                0.70f to Color.Black.copy(alpha = 0.55f),
                1.0f to Color.Black.copy(alpha = 0.90f)
            )
        )
    } else {
        // Lean hero: keep top contrast for the controls, then fade into the page so
        // the editorial title block below blends onto the paper background.
        Brush.verticalGradient(
            colorStops = arrayOf(
                0.0f to Color.Black.copy(alpha = 0.38f),
                0.28f to Color.Transparent,
                0.74f to Color.Transparent,
                1.0f to pageBg
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(heroHeight)
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
        if (showInfo) {
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
                            tint = com.kartik.mealtime.ui.theme.StarGold,
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
                            tint = com.kartik.mealtime.ui.theme.StarGold,
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

// ═══ Prototype-faithful detail pieces (editorial title, meta strip, tabs…) ════

// ── Editorial title block — sits below the lean hero on the paper background ───
@Composable
fun RecipeTitleBlock(recipe: Recipe) {
    Column(modifier = Modifier.padding(horizontal = 22.dp)) {
        val kicker = buildList {
            if (recipe.cuisine.isNotBlank()) add(recipe.cuisine)
            if (recipe.category.isNotBlank()) add(recipe.category)
        }.joinToString(" · ").uppercase()
        if (kicker.isNotBlank()) {
            Text(
                kicker,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(7.dp))
        }
        Text(
            recipe.name,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (recipe.rating > 0) {
            Spacer(modifier = Modifier.height(11.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Star, null,
                    tint = com.kartik.mealtime.ui.theme.StarGold,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    String.format(Locale.ROOT, "%.1f", recipe.rating),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (recipe.reviewCount > 0) {
                    Text(
                        "  ·  ${recipe.reviewCount} reviews",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (recipe.description.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                recipe.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.3f
            )
        }
    }
}

// ── 4-cell meta strip (Total · Level · kcal · Serves) ─────────────────────────
@Composable
fun RecipeMetaStrip(recipe: Recipe, servings: Int) {
    val total = recipe.prepTime + recipe.cookTime
    val cells = listOf(
        Triple(Icons.Default.Timer, "$total min", "Total"),
        Triple(Icons.AutoMirrored.Filled.TrendingUp, recipe.difficulty.displayName(), "Level"),
        Triple(Icons.Default.LocalFireDepartment, recipe.calories?.let { "$it" } ?: "—", "kcal"),
        Triple(Icons.Default.Restaurant, "$servings", "Serves")
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            cells.forEachIndexed { i, (icon, value, label) ->
                if (i > 0) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 14.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
                    Text(
                        value,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ── Nutrition macro grid (serif values) ───────────────────────────────────────
@Composable
fun RecipeNutritionGrid(nutrition: NutritionInfo) {
    Column(modifier = Modifier.padding(horizontal = 22.dp)) {
        Text(
            "Nutrition",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            "per serving · ${nutrition.calories} kcal",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            listOf(
                "Protein" to nutrition.protein,
                "Carbs" to nutrition.carbs,
                "Fat" to nutrition.fat
            ).forEach { (label, value) ->
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(13.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 13.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "${value.toInt()}g",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ── Ingredients / Method segmented tab toggle ─────────────────────────────────
@Composable
fun RecipeDetailTabs(selected: Int, onSelect: (Int) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp),
        shape = RoundedCornerShape(50.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            listOf("Ingredients", "Method").forEachIndexed { i, label ->
                val on = i == selected
                Surface(
                    onClick = { onSelect(i) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(50.dp),
                    color = if (on) MaterialTheme.colorScheme.surface else Color.Transparent,
                    shadowElevation = if (on) 2.dp else 0.dp
                ) {
                    Text(
                        label,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (on) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ── Ingredients tab header: count + compact servings stepper ──────────────────
@Composable
fun IngredientsTabHeader(count: Int, servings: Int, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "$count items",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Surface(
            shape = RoundedCornerShape(50.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { if (servings > 1) onChange(servings - 1) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Remove, "Decrease", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(15.dp))
                }
                Text(
                    "$servings serves",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { if (servings < 20) onChange(servings + 1) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, "Increase", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(15.dp))
                }
            }
        }
    }
}

// ── Ingredient check row (square checkbox · name · qty) ───────────────────────
@Composable
fun IngredientCheckRow(
    ingredient: Ingredient,
    scale: Float,
    isChecked: Boolean,
    onToggle: () -> Unit,
    isLast: Boolean
) {
    val amount = remember(ingredient.amount, scale) { scaleAmount(ingredient.amount, scale) }
    val qty = if (ingredient.unit.isBlank()) amount else "$amount ${ingredient.unit}"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(23.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(if (isChecked) MaterialTheme.colorScheme.primary else Color.Transparent)
                .then(
                    if (!isChecked)
                        Modifier.border(1.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(7.dp))
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isChecked) {
                Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
            }
        }
        Spacer(modifier = Modifier.width(13.dp))
        Text(
            ingredient.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = if (isChecked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
            textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            qty,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1
        )
    }
    if (!isLast) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    }
}

// ── Method numbered step row ──────────────────────────────────────────────────
@Composable
fun MethodStepRow(step: RecipeStep) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "${step.stepNumber}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            step.instruction,
            modifier = Modifier
                .weight(1f)
                .padding(top = 4.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.3f
        )
    }
}

// ── Ghost "Add all to shopping list" button ───────────────────────────────────
@Composable
fun AddAllToListButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp),
        shape = RoundedCornerShape(13.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 13.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.ShoppingCart, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Add all to shopping list",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

// ── "Watch on YouTube" secondary button (keeps the app's video resolve) ───────
@Composable
fun WatchYoutubeButton(recipeName: String, resolveVideoUrl: suspend (String) -> String) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var finding by remember { mutableStateOf(false) }
    Surface(
        onClick = {
            if (finding) return@Surface
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            finding = true
            scope.launch {
                val url = resolveVideoUrl(recipeName)
                finding = false
                context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp),
        shape = RoundedCornerShape(13.dp),
        color = Color(0xFFFF0000).copy(alpha = 0.08f),
        border = BorderStroke(1.dp, Color(0xFFFF0000).copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 13.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (finding) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFFFF0000))
            } else {
                Text("▶", fontSize = 14.sp, color = Color(0xFFFF0000))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                if (finding) "Finding video…" else "Watch on YouTube",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFFF0000)
            )
        }
    }
}

// ── Sticky bottom CTA bar (shopping bag · Start cooking) ──────────────────────
@Composable
fun RecipeBottomBar(
    modifier: Modifier = Modifier,
    onAddToList: () -> Unit,
    onStartCooking: () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onAddToList,
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.ShoppingCart, "Add to shopping list", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
                }
            }
            Button(
                onClick = onStartCooking,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Restaurant, null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start cooking", fontWeight = FontWeight.Bold)
            }
        }
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
        tint = com.kartik.mealtime.ui.theme.Heart,
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
            color = com.kartik.mealtime.ui.theme.Heart,
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
