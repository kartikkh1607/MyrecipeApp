package com.kartik.mealtime.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.kartik.mealtime.domain.model.ShoppingListItem
import com.kartik.mealtime.ui.components.BrandedSnackbarHost
import com.kartik.mealtime.ui.theme.ForestGreen
import com.kartik.mealtime.ui.viewmodel.AiViewModel
import com.kartik.mealtime.ui.viewmodel.ShoppingListViewModel
import kotlinx.coroutines.launch

// Semantic per-recipe section accents — independent of theme so each recipe stays
// visually distinct in both light & dark mode.
internal val SectionGreen = Color(0xFF006B1B)
internal val SectionOrange = Color(0xFFB02E00)
internal val SectionAmber = Color(0xFF7B5500)
internal val sectionAccents = listOf(SectionGreen, SectionOrange, SectionAmber)

// ── Sealed list entries — flat structure for LazyColumn ───────────────────────
internal sealed class SlEntry {
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

// ── Quick action chip data ─────────────────────────────────────────────────────
internal data class QuickAction(
    val emoji: String,
    val label: String,
    val buildPrompt: (items: List<ShoppingListItem>) -> String
)

internal val contextualQuickActions = listOf(
    QuickAction("🍳", "What can I cook?") { items ->
        val names = items.filter { !it.isChecked }.map { it.ingredientName }
        if (names.isEmpty()) "What meals can I cook with common pantry staples?"
        else "I have these ingredients: ${names.joinToString(", ")}. What meals can I make?"
    },
    QuickAction("🔄", "Substitutes") { items ->
        val names = items.filter { !it.isChecked }.map { it.ingredientName }
        if (names.isEmpty()) "What are some common ingredient substitutes I should know?"
        else "Suggest substitutes for: ${names.joinToString(", ")}"
    },
    QuickAction("🥗", "Healthier swaps") { items ->
        val names = items.map { it.ingredientName }
        if (names.isEmpty()) "What healthy food swaps do you recommend for everyday cooking?"
        else "Suggest healthier alternatives for: ${names.joinToString(", ")}"
    },
    QuickAction("🛒", "Organize by aisle") { items ->
        val names = items.filter { !it.isChecked }.map { it.ingredientName }
        if (names.isEmpty()) "How should I organize a typical grocery shopping list by store aisle?"
        else "Organize these grocery items by store aisle/section: ${names.joinToString(", ")}"
    },
    QuickAction("🔍", "Missing basics?") { items ->
        val names = items.map { it.ingredientName }
        if (names.isEmpty()) "What pantry staples should every home cook always have on hand?"
        else "My shopping list has: ${names.joinToString(", ")}. What common pantry basics might I be missing?"
    },
    QuickAction("🔢", "Estimate calories") { items ->
        val names = items.filter { !it.isChecked }.map {
            listOf(it.amount, it.unit, it.ingredientName).filter { s -> s.isNotBlank() }
                .joinToString(" ")
        }
        if (names.isEmpty()) "How can I estimate calories for common grocery ingredients?"
        else "Give an approximate calorie estimate for: ${names.joinToString(", ")}"
    }
)

// ── Screen ────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    navController: NavHostController,
    viewModel: ShoppingListViewModel,
    aiViewModel: AiViewModel
) {
    val allItems by viewModel.shoppingList
    val checkedCount by remember { derivedStateOf { viewModel.shoppingList.value.count { it.isChecked } } }
    val totalCount by remember { derivedStateOf { viewModel.shoppingList.value.size } }
    val remainingCount by remember { derivedStateOf { totalCount - checkedCount } }
    val hapticFeedback = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Custom item input state
    var customItemText by remember { mutableStateOf("") }

    // AI panel state
    var showAiPanel by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val chatState by aiViewModel.chatState
    var aiInputText by remember { mutableStateOf("") }

    fun submitCustomItem() {
        if (customItemText.isNotBlank()) {
            viewModel.addCustomShoppingItem(customItemText)
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            customItemText = ""
            keyboardController?.hide()
        }
    }

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
    val entries: List<SlEntry> by remember(grouped, collapsedSections.toMap()) {
        derivedStateOf {
            buildList {
                grouped.entries.forEachIndexed { sectionIdx, (recipeName, recipeItems) ->
                    val accent = sectionAccents[sectionIdx % sectionAccents.size]
                    val isCollapsed = collapsedSections[recipeName] == true

                    if (sectionIdx > 0) add(SlEntry.SectionGap("gap_$sectionIdx"))
                    add(SlEntry.Header(recipeName, accent, recipeItems.size, isCollapsed))

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
        snackbarHost = { BrandedSnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    showAiPanel = true
                },
                containerColor = ForestGreen,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = "AI Shopping Assistant",
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    ) { innerPadding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {

            // ── Top bar ───────────────────────────────────────────────────────
            item(key = "topbar") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
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

            // ── Add custom item row ────────────────────────────────────────
            item(key = "add_custom") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customItemText,
                        onValueChange = { customItemText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                "Add item (e.g. Milk, Eggs…)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { submitCustomItem() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                    Surface(
                        onClick = { submitCustomItem() },
                        shape = CircleShape,
                        color = if (customItemText.isNotBlank())
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add item",
                                tint = if (customItemText.isNotBlank())
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
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

    // ── AI Shopping Assistant bottom sheet ────────────────────────────────────
    if (showAiPanel) {
        ModalBottomSheet(
            onDismissRequest = { showAiPanel = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 10.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            CircleShape
                        )
                )
            }
        ) {
            ShoppingAiPanel(
                items = allItems,
                chatState = chatState,
                inputText = aiInputText,
                onInputChange = { aiInputText = it },
                onSend = {
                    if (aiInputText.isNotBlank()) {
                        aiViewModel.sendMessage(aiInputText)
                        aiInputText = ""
                    }
                },
                onQuickAction = { prompt ->
                    aiViewModel.sendMessage(prompt)
                    aiInputText = ""
                }
            )
        }
    }
}

// ── AI panel content ──────────────────────────────────────────────────────────
