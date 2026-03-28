package com.example.myrecipeapp.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.RemoveDone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.myrecipeapp.domain.model.ShoppingListItem
import com.example.myrecipeapp.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    navController: NavHostController,
    viewModel: MainViewModel
) {
    val allItems by viewModel.shoppingList
    val checkedCount = allItems.count { it.isChecked }
    val totalCount = allItems.size
    val progressFraction = if (totalCount > 0) checkedCount.toFloat() / totalCount else 0f
    val isAllDone = totalCount > 0 && checkedCount == totalCount
    val clipboardManager = LocalClipboardManager.current
    val hapticFeedback = LocalHapticFeedback.current

    // Group once — key = recipeName, value = sorted (unchecked first)
    val grouped = remember(allItems) {
        allItems.groupBy { it.recipeName }
            .mapValues { (_, items) ->
                items.sortedWith(compareBy { it.isChecked })
            }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.82f)
                            )
                        )
                    )
                    .statusBarsPadding()
            ) {
                Column(
                    modifier = Modifier.padding(
                        start = 4.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 20.dp
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                            }
                            Column {
                                Text(
                                    "Shopping List",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                AnimatedContent(
                                    targetState = when {
                                        totalCount == 0 -> "Add ingredients from a recipe"
                                        isAllDone -> "🎉 All done! Great job!"
                                        else -> "$checkedCount of $totalCount items"
                                    },
                                    transitionSpec = {
                                        fadeIn(tween(300)) togetherWith fadeOut(
                                            tween(
                                                200
                                            )
                                        )
                                    },
                                    label = "subtitle"
                                ) { subtitle ->
                                    Text(
                                        subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                        Row {
                            if (checkedCount > 0) {
                                IconButton(onClick = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.removeCheckedItems()
                                }) {
                                    Icon(
                                        Icons.Default.RemoveDone,
                                        "Remove checked",
                                        tint = Color.White
                                    )
                                }
                            }
                            if (totalCount > 0) {
                                IconButton(onClick = {
                                    val text =
                                        grouped.entries.joinToString("\n\n") { (recipe, ings) ->
                                            "🍽 $recipe\n" + ings.joinToString("\n") { ing ->
                                                val qty =
                                                    ing.amount + if (ing.unit.isNotBlank()) " ${ing.unit}" else ""
                                                "  ☐ $qty ${ing.ingredientName}"
                                            }
                                        }
                                    clipboardManager.setText(
                                        AnnotatedString(
                                            "🛒 My Shopping List\n${
                                                "─".repeat(
                                                    30
                                                )
                                            }\n\n$text"
                                        )
                                    )
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                }) {
                                    Icon(Icons.Default.Share, "Share", tint = Color.White)
                                }
                            }
                        }
                    }

                    if (totalCount > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            LinearProgressIndicator(
                                progress = { progressFraction },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(8.dp)
                                    .clip(CircleShape),
                                color = Color.White,
                                trackColor = Color.White.copy(alpha = 0.25f)
                            )
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    "$checkedCount/$totalCount",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (allItems.isEmpty()) {
            EmptyShoppingListState(
                modifier = Modifier.padding(padding),
                onGoToHome = { navController.popBackStack() }
            )
        } else {
            // ── Lazy column: each row is a genuine item{} — no eager forEachIndexed ──
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(
                    top = 16.dp, bottom = 120.dp, start = 16.dp, end = 16.dp
                )
            ) {
                // All-done celebration
                if (isAllDone) {
                    item(key = "celebration") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🛍️", fontSize = 36.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "All items collected!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    "Time to start cooking 🍳",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                grouped.forEach { (recipeName, recipeItems) ->
                    // Sticky-style section header
                    item(key = "hdr_$recipeName") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("🍽", fontSize = 16.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    recipeName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            val doneCount = recipeItems.count { it.isChecked }
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (doneCount == recipeItems.size)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    "$doneCount/${recipeItems.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Card wrapping all items of this recipe
                    item(key = "card_start_$recipeName") {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp
                        ) { Spacer(modifier = Modifier.height(6.dp)) }
                    }

                    items(
                        items = recipeItems,
                        key = { it.key }
                    ) { shopItem ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp
                        ) {
                            SwipeableShoppingItem(
                                item = shopItem,
                                onToggle = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.toggleShoppingItem(shopItem.key)
                                },
                                onDelete = { viewModel.removeItem(shopItem.key) }
                            )
                        }
                        HorizontalDivider(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(start = 56.dp, end = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                        )
                    }

                    item(key = "card_end_$recipeName") {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp
                        ) { Spacer(modifier = Modifier.height(6.dp)) }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // Clear all button
                item(key = "clear_all") {
                    OutlinedButton(
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.clearShoppingList()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.DeleteSweep, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear All Items", fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

// ── Swipeable item row ────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableShoppingItem(
    item: ShoppingListItem,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete(); true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val fraction = dismissState.progress.coerceIn(0f, 1f)
            val bgAlpha =
                if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) fraction else 0f
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = bgAlpha)),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(end = 20.dp)
                )
            }
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true
    ) {
        ShoppingItemRow(item = item, onToggle = onToggle)
    }
}

@Composable
private fun ShoppingItemRow(
    item: ShoppingListItem,
    onToggle: () -> Unit
) {
    val checkScale by animateFloatAsState(
        targetValue = if (item.isChecked) 1.1f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, 500f),
        label = "check_scale"
    )
    val textAlpha = if (item.isChecked) 0.4f else 1f
    val qty = item.amount + if (item.unit.isNotBlank()) " ${item.unit}" else ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 12.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Checkbox(
            checked = item.isChecked,
            onCheckedChange = { onToggle() },
            modifier = Modifier.scale(checkScale),
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.outline
            )
        )
        Text(
            text = item.ingredientName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (item.isChecked) FontWeight.Normal else FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = textAlpha),
            textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
            modifier = Modifier.weight(1f),
            maxLines = 2
        )
        if (qty.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (item.isChecked)
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = qty,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (item.isChecked)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────
@Composable
private fun EmptyShoppingListState(
    modifier: Modifier = Modifier,
    onGoToHome: () -> Unit
) {
    var iconVisible by remember { mutableStateOf(false) }
    var textVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(120); iconVisible = true; delay(180); textVisible = true }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedVisibility(
            visible = iconVisible,
            enter = scaleIn(
                initialScale = 0.6f,
                animationSpec = spring(Spring.DampingRatioLowBouncy, 280f)
            ) + fadeIn()
        ) {
            Surface(
                modifier = Modifier.size(120.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            ) {
                Box(contentAlignment = Alignment.Center) { Text("🛒", fontSize = 52.sp) }
            }
        }
        Spacer(modifier = Modifier.height(28.dp))
        AnimatedVisibility(
            visible = textVisible,
            enter = slideInVertically(
                initialOffsetY = { 30 },
                animationSpec = spring(stiffness = 300f)
            ) + fadeIn()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Your cart is empty",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Open any recipe, tap\n\"Shopping List\" to add ingredients.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = onGoToHome,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Fastfood, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Browse Recipes", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
