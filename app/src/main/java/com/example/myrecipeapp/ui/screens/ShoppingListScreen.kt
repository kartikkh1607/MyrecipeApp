package com.example.myrecipeapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.myrecipeapp.domain.model.ShoppingListItem
import com.example.myrecipeapp.ui.components.BrandedSnackbarHost
import com.example.myrecipeapp.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Semantic per-recipe section accents — independent of theme so each recipe stays
// visually distinct in both light & dark mode.
private val SectionGreen = Color(0xFF006B1B)
private val SectionOrange = Color(0xFFB02E00)
private val SectionAmber = Color(0xFF7B5500)
private val sectionAccents = listOf(SectionGreen, SectionOrange, SectionAmber)

// ── Sealed list entries — flat structure for LazyColumn ───────────────────────
private sealed class SlEntry {
    data class SectionGap(val id: String) : SlEntry()
    data class Header(
        val recipeName: String,
        val accent: Color,
        val itemCount: Int,
        val isCollapsed: Boolean
    ) : SlEntry()

    data class ItemRow(
        val shopItem: ShoppingListItem,
        val recipeName: String,
        val isLast: Boolean,
        val showDivider: Boolean
    ) : SlEntry()
}

// ── Screen ────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    navController: NavHostController,
    viewModel: MainViewModel
) {
    val allItems by viewModel.shoppingList
    val checkedCount by remember { derivedStateOf { viewModel.shoppingList.value.count { it.isChecked } } }
    val totalCount by remember { derivedStateOf { viewModel.shoppingList.value.size } }
    val remainingCount by remember { derivedStateOf { totalCount - checkedCount } }
    val hapticFeedback = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    // Read focus from ViewModel — set by addToShoppingList before navigation
    val focusRecipeName by viewModel.lastAddedRecipeName

    // false (absent) = expanded; true = collapsed
    val collapsedSections = remember { mutableStateMapOf<String, Boolean>() }

    // Group by recipe
    val grouped = remember(allItems) {
        allItems.groupBy { it.recipeName }
            .mapValues { (_, v) -> v.sortedWith(compareBy { it.isChecked }) }
    }

    // When a focusRecipeName is provided (came from a recipe), collapse all other sections
    LaunchedEffect(focusRecipeName) {
        val focus = focusRecipeName
        if (focus != null && grouped.isNotEmpty()) {
            grouped.keys.forEach { name ->
                collapsedSections[name] = (name != focus)
            }
        }
    }

    // ── Flatten into a list of sealed entries ─────────────────────────────────
    // Re-computed any time grouped changes OR collapsedSections changes.
    val entries: List<SlEntry> by remember(grouped, collapsedSections.toMap()) {
        derivedStateOf {
            buildList {
                grouped.entries.forEachIndexed { sectionIdx, (recipeName, recipeItems) ->
                    val accent = sectionAccents[sectionIdx % sectionAccents.size]
                    val isCollapsed = collapsedSections[recipeName] == true

                    // Gap between sections
                    if (sectionIdx > 0) add(SlEntry.SectionGap("gap_$sectionIdx"))

                    // Section header
                    add(SlEntry.Header(recipeName, accent, recipeItems.size, isCollapsed))

                    // Item rows — only when expanded
                    if (!isCollapsed) {
                        recipeItems.forEachIndexed { itemIdx, item ->
                            add(
                                SlEntry.ItemRow(
                                    shopItem = item,
                                    recipeName = recipeName,
                                    isLast = itemIdx == recipeItems.lastIndex,
                                    showDivider = itemIdx < recipeItems.lastIndex
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { BrandedSnackbarHost(snackbarHostState) }
    ) { innerPadding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 48.dp)
        ) {

            // ── Top bar ───────────────────────────────────────────────────────
            item(key = "topbar") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .statusBarsPadding()
                        .padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, "Back",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        "Shopping List",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    if (checkedCount > 0) {
                        IconButton(onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.removeCheckedItems()
                        }) {
                            Icon(
                                Icons.Default.Delete,
                                "Clear completed",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // ── Summary strip ─────────────────────────────────────────────────
            item(key = "summary") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val allDone = remainingCount == 0 && totalCount > 0
                    Text(
                        text = when {
                            totalCount == 0 -> "No items yet"
                            remainingCount == 0 -> "All items checked ✓"
                            else -> "$remainingCount of $totalCount items remaining"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (allDone) SectionGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (allDone) FontWeight.SemiBold else FontWeight.Normal
                    )
                    if (checkedCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
                        ) {
                            Text(
                                "$checkedCount done",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // ── Empty state ───────────────────────────────────────────────────
            if (allItems.isEmpty()) {
                item(key = "empty") { SlEmptyState(onBrowse = { navController.popBackStack() }) }
                return@LazyColumn
            }

            // ── Single items() call over the flat sealed list ─────────────────
            items(
                items = entries,
                key = { entry ->
                    when (entry) {
                        is SlEntry.SectionGap -> entry.id
                        is SlEntry.Header -> "hdr_${entry.recipeName}"
                        is SlEntry.ItemRow -> "item_${entry.recipeName}_${entry.shopItem.key}"
                    }
                }
            ) { entry ->
                when (entry) {
                    is SlEntry.SectionGap -> Spacer(Modifier.height(12.dp))

                    is SlEntry.Header -> SlSectionHeader(
                        recipeName = entry.recipeName,
                        accent = entry.accent,
                        itemCount = entry.itemCount,
                        isCollapsed = entry.isCollapsed,
                        onToggle = {
                            collapsedSections[entry.recipeName] = !entry.isCollapsed
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    )

                    is SlEntry.ItemRow -> SlSwipeableItemRow(
                        item = entry.shopItem,
                        isLast = entry.isLast,
                        showDivider = entry.showDivider,
                        onToggle = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.toggleShoppingItem(entry.shopItem.key)
                        },
                        onDelete = {
                            val deleted = entry.shopItem
                            viewModel.removeItem(deleted.key)
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "\"${deleted.ingredientName}\" removed",
                                    actionLabel = "Undo",
                                    duration = androidx.compose.material3.SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.restoreShoppingItem(deleted)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

// ── Section header ────────────────────────────────────────────────────────────
@Composable
private fun SlSectionHeader(
    recipeName: String,
    accent: Color,
    itemCount: Int,
    isCollapsed: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(
                if (isCollapsed) RoundedCornerShape(16.dp)
                else RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
            .clickable { onToggle() },
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 26.dp)
                    .background(accent, RoundedCornerShape(2.dp))
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "FOR",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                    fontSize = 9.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    recipeName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(12.dp))
            Surface(shape = CircleShape, color = accent.copy(alpha = 0.10f)) {
                Text(
                    "$itemCount",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                if (isCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// ── Swipeable item row ────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SlSwipeableItemRow(
    item: ShoppingListItem,
    isLast: Boolean,
    showDivider: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { v ->
            if (v == SwipeToDismissBoxValue.EndToStart) {
                onDelete(); true
            } else false
        }
    )
    val bottomShape = if (isLast)
        RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
    else
        RoundedCornerShape(0.dp)

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val alpha = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart)
                dismissState.progress.coerceIn(0f, 1f) else 0f
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .clip(bottomShape)
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = alpha)),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(end = 24.dp)
                )
            }
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true
    ) {
        val surface = MaterialTheme.colorScheme.surface
        val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
        val outline = MaterialTheme.colorScheme.outlineVariant
        // Checked rows get a subtle green-tinted background; the tint reads on both light & dark surface.
        val checkedBg = surface.compositeOver(SectionGreen.copy(alpha = 0.08f))
        val rowBg = if (item.isChecked) checkedBg else surface
        val qty = item.amount + if (item.unit.isNotBlank()) " ${item.unit}" else ""

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(bottomShape)
                .background(rowBg)
                .clickable { onToggle() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Checkbox
                val checkBg = if (item.isChecked) SectionGreen else rowBg
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(checkBg)
                        .then(
                            if (!item.isChecked)
                                Modifier
                                    .padding(1.5.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(outline)
                                    .padding(1.5.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(rowBg)
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.isChecked) {
                        Icon(
                            Icons.Default.Check,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.ingredientName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (item.isChecked) FontWeight.Normal else FontWeight.SemiBold,
                        color = if (item.isChecked) onSurfaceVariant.copy(alpha = 0.55f) else MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (qty.isNotBlank()) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = qty.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = onSurfaceVariant.copy(alpha = if (item.isChecked) 0.4f else 0.7f),
                            letterSpacing = 0.9.sp
                        )
                    }
                }
            }

            if (showDivider) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .padding(start = 58.dp, end = 18.dp)
                        .background(outline.copy(alpha = 0.5f))
                )
            }
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SlEmptyState(onBrowse: () -> Unit) {
    var iconVisible by remember { mutableStateOf(false) }
    var textVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(100); iconVisible = true; delay(200); textVisible = true }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedVisibility(
            visible = iconVisible,
            enter = scaleIn(
                initialScale = 0.6f,
                animationSpec = spring(Spring.DampingRatioLowBouncy, 260f)
            ) + fadeIn()
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(100.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.ShoppingBasket,
                        null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        AnimatedVisibility(
            visible = textVisible,
            enter = slideInVertically(
                initialOffsetY = { 24 },
                animationSpec = spring(stiffness = 300f)
            ) + fadeIn()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Your list is empty",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Open a recipe and tap\n\"Add to Shopping List\" to get started.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
                Spacer(Modifier.height(36.dp))
                Surface(
                    onClick = onBrowse,
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Restaurant,
                            null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Browse Recipes",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}