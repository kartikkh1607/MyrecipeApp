package com.kartik.mealtime.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kartik.mealtime.domain.model.RecipeCategory
import com.kartik.mealtime.ui.components.GridSkeletonScreen
import com.kartik.mealtime.ui.navigation.Categories
import com.kartik.mealtime.ui.navigation.LocalTabReselectEvents
import com.kartik.mealtime.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter

@Composable
fun RecipeScreen(
    modifier: Modifier = Modifier,
    viewstate: MainViewModel.RecipeCategoryState,
    navigateToDetail: (RecipeCategory) -> Unit,
    onRetry: () -> Unit = {}
) {
    // Hoist grid state so reselecting the Categories tab smooth-scrolls to top.
    val gridState = rememberLazyGridState()
    val tabReselectEvents = LocalTabReselectEvents.current
    LaunchedEffect(tabReselectEvents) {
        tabReselectEvents.events.filter { it is Categories }.collect {
            gridState.animateScrollToItem(0)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                    )
                )
            )
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Recipe Categories",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "Discover delicious recipes by category",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            )
            if (!viewstate.loading && viewstate.error == null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.GridView,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${viewstate.categories.size} Categories",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                viewstate.loading -> {
                    GridSkeletonScreen(itemCount = 6)
                }

                viewstate.error != null -> {
                    ErrorDisplay(
                        message = viewstate.error,
                        onRetry = onRetry,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    CategoryGridDisplay(
                        categories = viewstate.categories,
                        gridState = gridState,
                        navigateToDetail = navigateToDetail
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryGridDisplay(
    categories: List<RecipeCategory>,
    navigateToDetail: (RecipeCategory) -> Unit,
    gridState: LazyGridState = rememberLazyGridState()
) {
    // Captured once per screen entry. Items composing later — whether via fresh
    // scroll-into-view or after scrolling back to the top (LazyGrid disposes
    // off-screen cells) — read this to decide whether the entrance animation is
    // still in its "initial render" window. Without this, every scroll-to-top
    // re-played the cascade.
    val screenOpenedAt = remember { System.currentTimeMillis() }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(categories, key = { _, c -> c.id }) { index, category ->
            CategoryGridItem(
                category = category,
                index = index,
                screenOpenedAt = screenOpenedAt,
                navigateToDetail = navigateToDetail
            )
        }
    }
}

@Composable
fun CategoryGridItem(
    category: RecipeCategory,
    index: Int = 0,
    screenOpenedAt: Long = 0L,
    navigateToDetail: (RecipeCategory) -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }

    // Entrance cascade runs only for the initial viewport AND only while the screen
    // is still fresh (first 800ms after open). The time guard is what stops the
    // cascade from re-firing when the user scrolls fast back to the top — LazyGrid
    // disposes off-screen cells, so on scroll-back the first 6 cards recompose and
    // would otherwise animate again. `remember` evaluates the guard at composition
    // time, so the elapsed-millis check works correctly on every (re)compose.
    val shouldAnimate = remember {
        index < 6 && System.currentTimeMillis() - screenOpenedAt < 800
    }
    var visible by remember { mutableStateOf(!shouldAnimate) }
    LaunchedEffect(Unit) {
        if (shouldAnimate) {
            delay(index * 60L)
            visible = true
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "category_scale",
        finishedListener = { isPressed = false }  // auto-reset after animation completes
    )
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 12.dp else 8.dp,
        animationSpec = tween(durationMillis = 300),
        label = "category_elevation"
    )

    // Staggered slide-up + fade entrance
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it / 4 },        // slides up from 25% below
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeIn(animationSpec = tween(durationMillis = 280))
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.8f)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .shadow(
                    elevation = elevation, shape = RoundedCornerShape(20.dp),
                    ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )
                .clickable {
                    isPressed = true
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    navigateToDetail(category)
                },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (category.imageResId != 0) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(category.imageResId),
                        contentDescription = category.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(20.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    AsyncImage(
                        model = category.imageUrl, contentDescription = category.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(20.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.3f),
                                    Color.Black.copy(alpha = 0.7f)
                                )
                            )
                        )
                )
                Card(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(
                            alpha = 0.9f
                        )
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Restaurant,
                            null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            category.recipeCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        category.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        category.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 14.sp
                    )
                }
                if (isPressed) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(40.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.GridView,
                            "View Category",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    } // end AnimatedVisibility
}

@Composable
fun ErrorDisplay(message: String, onRetry: () -> Unit = {}, modifier: Modifier = Modifier) {
    var retryPressed by remember { mutableStateOf(false) }
    val iconScale by animateFloatAsState(
        targetValue = if (retryPressed) 0.88f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "error_icon_scale",
        finishedListener = { retryPressed = false }
    )
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.22f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.WifiOff,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .graphicsLayer { scaleX = iconScale; scaleY = iconScale },
                tint = MaterialTheme.colorScheme.error
            )
        }
        Text(
            "You're offline",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Button(
            onClick = { retryPressed = true; onRetry() },
            shape = RoundedCornerShape(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                "Try Again",
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}
