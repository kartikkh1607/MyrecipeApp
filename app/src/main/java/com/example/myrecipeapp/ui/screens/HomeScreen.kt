package com.example.myrecipeapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.myrecipeapp.domain.model.FeaturedRecipe
import com.example.myrecipeapp.domain.model.RecipeDifficulty
import com.example.myrecipeapp.ui.components.FeaturedCarouselSkeleton
import com.example.myrecipeapp.ui.navigation.Chat
import com.example.myrecipeapp.ui.navigation.Home
import com.example.myrecipeapp.ui.navigation.LocalTabReselectEvents
import com.example.myrecipeapp.ui.navigation.Profile
import com.example.myrecipeapp.ui.navigation.RecipeDetail
import com.example.myrecipeapp.ui.navigation.Search
import com.example.myrecipeapp.ui.navigation.ShoppingList
import com.example.myrecipeapp.ui.theme.ForestGreen
import com.example.myrecipeapp.ui.viewmodel.MainViewModel
import kotlinx.coroutines.flow.filter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: MainViewModel
) {
    val hapticFeedback = LocalHapticFeedback.current
    val categoriesState by viewModel.recipeCategoriesState
    val homeRecipeState by viewModel.homeRecipeState

    // Hero is dark green → force light status-bar icons while Home is visible.
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as android.app.Activity).window
        val controller = WindowCompat.getInsetsController(window, view)
        val previous = controller.isAppearanceLightStatusBars
        controller.isAppearanceLightStatusBars = false
        onDispose { controller.isAppearanceLightStatusBars = previous }
    }

    // Pull-to-refresh — track manual refresh locally so the indicator doesn't
    // show during the initial load (which already has skeletons).
    var manualRefresh by remember { mutableStateOf(false) }
    val isRefreshing = manualRefresh && (homeRecipeState.loading || categoriesState.loading)
    LaunchedEffect(homeRecipeState.loading, categoriesState.loading) {
        if (!homeRecipeState.loading && !categoriesState.loading) manualRefresh = false
    }

    // Hoist scroll state so reselecting the Home tab can smooth-scroll us to top.
    val scrollState = rememberScrollState()
    val tabReselectEvents = LocalTabReselectEvents.current
    LaunchedEffect(tabReselectEvents) {
        tabReselectEvents.events.filter { it is Home }.collect {
            scrollState.animateScrollTo(0)
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            manualRefresh = true
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.refreshFeaturedRecipes()
            viewModel.refreshRecipeCategories()
        },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
        ) {
            // ── Header ─────────────────────────────────────────────────────────────
            HeroHeaderSection(
                onProfileClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    navController.navigate(Profile)
                },
                onSearchClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    navController.navigate(Search) { launchSingleTop = true }
                }
            )

            // ── Featured Carousel ───────────────────────────────────────────────────
            when {
                homeRecipeState.loading -> {
                    FeaturedCarouselSkeleton()
                }

                homeRecipeState.error != null -> {
                    ErrorSection(
                        message = homeRecipeState.error ?: "Unknown error",
                        onRetry = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.refreshFeaturedRecipes()
                        }
                    )
                }

                homeRecipeState.featuredRecipes.isNotEmpty() -> {
                    FeaturedRecipeCarousel(navController = navController, viewModel = viewModel)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Quick Actions banner ────────────────────────────────────────────────
            QuickActionsRow(
                onRandomRecipe = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    val featured = homeRecipeState.featuredRecipes
                    if (featured.isNotEmpty()) {
                        val random = featured.random()
                        navController.navigate(RecipeDetail(recipeId = random.recipe.id)) {
                            launchSingleTop = true
                        }
                    }
                },
                onShoppingList = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    navController.navigate(ShoppingList) { launchSingleTop = true }
                },
                onAIChat = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    navController.navigate(Chat) { launchSingleTop = true }
                }
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ── Today's Pick ───────────────────────────────────────────────────────
            if (homeRecipeState.featuredRecipes.isNotEmpty()) {
                TodaysPickCard(
                    featured = homeRecipeState.featuredRecipes,
                    onClick = { recipeId ->
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        navController.navigate(RecipeDetail(recipeId = recipeId)) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    } // end PullToRefreshBox
}

// ── Hero Header ────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeroHeaderSection(
    onProfileClick: () -> Unit,
    onSearchClick: () -> Unit
) {

    // ── Time-based greeting + date ────────────────────────────────────────────
    val now = remember { java.time.LocalDateTime.now() }
    val hour = now.hour

    val greeting = remember(hour) {
        when (hour) {
            in 5..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..20 -> "Good Evening"
            else -> "Good Night"
        }
    }
    val greetingEmoji = remember(hour) {
        when (hour) {
            in 5..11 -> "🌅"
            in 12..16 -> "☀️"
            in 17..20 -> "🌆"
            else -> "🌙"
        }
    }

    val dateString = remember(now) {
        // e.g. "Monday, May 11"
        now.format(
            java.time.format.DateTimeFormatter.ofPattern(
                "EEEE, MMMM d",
                Locale.getDefault()
            )
        )
    }

    // ── Entrance animation ────────────────────────────────────────────────────
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val heroBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                ForestGreen,
                Color(0xFF1F4040)
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(heroBrush)
    ) {
        // ── Decorative orbs (depth without using blur, which needs API 31+) ──
        Box(
            modifier = Modifier
                .size(240.dp)
                .offset(x = (-90).dp, y = (-80).dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.07f))
        )
        Box(
            modifier = Modifier
                .size(170.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = 30.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f))
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 22.dp)
        ) {
            // ── Top row: date chip + profile button ──────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AnimatedVisibility(
                    visible = visible,
                    enter = slideInVertically(
                        initialOffsetY = { -20 },
                        animationSpec = tween(400)
                    ) + fadeIn(animationSpec = tween(400))
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.18f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color.White.copy(alpha = 0.25f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                            Text(
                                text = dateString,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(animationSpec = tween(500))
                ) {
                    Surface(
                        onClick = onProfileClick,
                        modifier = Modifier.size(44.dp),
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.20f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color.White.copy(alpha = 0.35f)
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = "Profile",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // ── Greeting block ───────────────────────────────────────────────
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(
                    initialOffsetY = { 30 },
                    animationSpec = tween(500)
                ) + fadeIn(animationSpec = tween(500))
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = greeting,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = greetingEmoji,
                            fontSize = 30.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "What will you cook today?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // ── Floating search pill ─────────────────────────────────────────
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(
                    initialOffsetY = { 40 },
                    animationSpec = tween(600)
                ) + fadeIn(animationSpec = tween(600))
            ) {
                Surface(
                    onClick = onSearchClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(27.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 10.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Search recipes, ingredients…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ── Today's Pick — magazine-style hero card ──────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TodaysPickCard(
    featured: List<FeaturedRecipe>,
    onClick: (recipeId: String) -> Unit
) {
    val hour = remember { java.time.LocalDateTime.now().hour }
    val mood = remember(hour) {
        when (hour) {
            in 5..10 -> Triple("Today's breakfast", "☀️", "Cook this morning")
            in 11..14 -> Triple("Today's lunch", "🥗", "Make for lunch")
            in 15..16 -> Triple("Afternoon bite", "🍪", "Snack time")
            in 17..21 -> Triple("Tonight's special", "🌙", "Cook this tonight")
            else -> Triple("Late-night bite", "🌙", "Cook this now")
        }
    }
    val (badgeLabel, badgeEmoji, ctaLabel) = mood

    // Stable daily pick — same recipe all day, rotates by day-of-year
    val pick = remember(featured) {
        featured[java.time.LocalDate.now().dayOfYear % featured.size]
    }
    val recipe = pick.recipe

    // Other featured recipes for the "More ideas" row (skip the daily pick)
    val morePicks = remember(featured, pick) {
        featured.filter { it != pick }.take(4)
    }

    val difficultyBgColor = remember(recipe.difficulty) {
        when (recipe.difficulty) {
            RecipeDifficulty.EASY -> Color(0xFF2E7D32)
            RecipeDifficulty.MEDIUM -> Color(0xFFE65100)
            RecipeDifficulty.HARD -> Color(0xFFC62828)
        }
    }

    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessHigh),
        label = "pick_scale",
        finishedListener = { pressed = false }
    )

    // Entrance slide-up animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val heroGradient = remember {
        Brush.verticalGradient(
            colorStops = arrayOf(
                0.0f to Color.Transparent,
                0.35f to Color.Transparent,
                0.65f to Color.Black.copy(alpha = 0.4f),
                1.0f to Color.Black.copy(alpha = 0.92f)
            )
        )
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        // Section header with subtitle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Today's pick",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("✨", fontSize = 16.sp)
                }
                Text(
                    text = "Curated for $badgeLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { 60 },
                animationSpec = tween(500)
            ) + fadeIn(animationSpec = tween(500))
        ) {
            Card(
                onClick = {
                    pressed = true
                    onClick(recipe.id)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .scale(scale),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = recipe.imageUrl,
                        contentDescription = recipe.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // 4-stop gradient for strong bottom legibility
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(heroGradient)
                    )

                    // Top-left: time-of-day mood badge
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(14.dp),
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.92f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(badgeEmoji, fontSize = 12.sp)
                            Text(
                                badgeLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = ForestGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Top-right: difficulty badge
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(14.dp),
                        shape = RoundedCornerShape(50),
                        color = difficultyBgColor.copy(alpha = 0.9f)
                    ) {
                        Text(
                            text = "${recipe.difficulty.emoji()} ${recipe.difficulty.displayName()}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    // Bottom content block
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // Stat chips — rating, time, servings, calories
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (recipe.rating > 0f) {
                                PickStatChip("★", String.format(Locale.ROOT, "%.1f", recipe.rating))
                            }
                            val totalTime = recipe.prepTime + recipe.cookTime
                            if (totalTime > 0) PickStatChip("⏱", "${totalTime}m")
                            if (recipe.servings > 0) PickStatChip("👤", "${recipe.servings}")
                            recipe.calories?.let { PickStatChip("🔥", "$it kcal") }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = recipe.name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 30.sp
                        )

                        if (recipe.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = recipe.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.80f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // CTA — solid ForestGreen, white text (prominent on dark image)
                        Surface(
                            onClick = {
                                pressed = true
                                onClick(recipe.id)
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = ForestGreen
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    ctaLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        } // end AnimatedVisibility

        // ── "More ideas" mini-card row ───────────────────────────────────────
        if (morePicks.isNotEmpty()) {
            Spacer(modifier = Modifier.height(22.dp))
            Text(
                "More ideas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(
                contentPadding = PaddingValues(0.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(morePicks, key = { it.recipe.id }) { featuredItem ->
                    MiniPickCard(featured = featuredItem, onClick = onClick)
                }
            }
        }
    }
}

@Composable
private fun PickStatChip(emoji: String, value: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.Black.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(emoji, fontSize = 11.sp)
            Text(
                value,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MiniPickCard(
    featured: FeaturedRecipe,
    onClick: (recipeId: String) -> Unit
) {
    val recipe = featured.recipe
    val cardScrim = remember {
        Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f))
        )
    }

    Card(
        onClick = { onClick(recipe.id) },
        modifier = Modifier
            .width(150.dp)
            .height(110.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                    .background(cardScrim)
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = recipe.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (recipe.cookTime > 0) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "⏱ ${recipe.cookTime}m",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

// ── Quick Actions — horizontally scrollable LazyRow ───────────────────────────
@Composable
private fun QuickActionsRow(
    onRandomRecipe: () -> Unit,
    onShoppingList: () -> Unit,
    onAIChat: () -> Unit
) {
    Column {
        Text(
            "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // AI Chat Assistant
            item {
                var aiPressed by remember { mutableStateOf(false) }
                val aiScale by animateFloatAsState(
                    targetValue = if (aiPressed) 0.96f else 1f,
                    animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessHigh),
                    label = "ai_scale",
                    finishedListener = { aiPressed = false }
                )
                Button(
                    onClick = { aiPressed = true; onAIChat() },
                    modifier = Modifier
                        .height(52.dp)
                        .scale(aiScale),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ForestGreen,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("AI Chef", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, softWrap = false)
                }
            }

            // Random Recipe
            item {
                var rndPressed by remember { mutableStateOf(false) }
                val rndScale by animateFloatAsState(
                    targetValue = if (rndPressed) 0.96f else 1f,
                    animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessHigh),
                    label = "rnd_scale",
                    finishedListener = { rndPressed = false }
                )
                FilledTonalButton(
                    onClick = { rndPressed = true; onRandomRecipe() },
                    modifier = Modifier
                        .height(52.dp)
                        .scale(rndScale),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(Icons.Default.Shuffle, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Random Recipe", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, softWrap = false)
                }
            }

            // Shopping List
            item {
                var shpPressed by remember { mutableStateOf(false) }
                val shpScale by animateFloatAsState(
                    targetValue = if (shpPressed) 0.96f else 1f,
                    animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessHigh),
                    label = "shp_scale",
                    finishedListener = { shpPressed = false }
                )
                Button(
                    onClick = { shpPressed = true; onShoppingList() },
                    modifier = Modifier
                        .height(52.dp)
                        .scale(shpScale),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Default.ShoppingCart, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Shopping List", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

// ── Error section ──────────────────────────────────────────────────────────────
@Composable
fun ErrorSection(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Oops! Something went wrong",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Retry")
        }
    }
}
