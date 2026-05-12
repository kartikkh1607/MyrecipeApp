package com.example.myrecipeapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.myrecipeapp.domain.model.ShoppingListItem
import com.example.myrecipeapp.ui.components.BrandedSnackbarHost
import com.example.myrecipeapp.ui.theme.ForestGreen
import com.example.myrecipeapp.ui.viewmodel.AiViewModel
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

// ── Quick action chip data ─────────────────────────────────────────────────────
private data class QuickAction(
    val emoji: String,
    val label: String,
    val buildPrompt: (items: List<ShoppingListItem>) -> String
)

private val contextualQuickActions = listOf(
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
            listOf(it.amount, it.unit, it.ingredientName).filter { s -> s.isNotBlank() }.joinToString(" ")
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
    viewModel: MainViewModel,
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShoppingAiPanel(
    items: List<ShoppingListItem>,
    chatState: AiViewModel.ChatState,
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onQuickAction: (String) -> Unit
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()

    LaunchedEffect(chatState.messages.size, chatState.isTyping) {
        if (chatState.messages.isNotEmpty()) {
            listState.animateScrollToItem(chatState.messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // ── Panel header ──────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = CircleShape,
                color = ForestGreen
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "AI Shopping Assistant",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Powered by Gemini",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Quick actions — LazyRow ───────────────────────────────────────────
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(contextualQuickActions, key = { it.label }) { action ->
                AiQuickActionChip(
                    emoji = action.emoji,
                    label = action.label,
                    onClick = { onQuickAction(action.buildPrompt(items)) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Messages area ─────────────────────────────────────────────────────
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val visibleMessages = chatState.messages.takeLast(20)
            items(visibleMessages, key = { it.id }) { message ->
                AiMiniChatBubble(message = message)
            }
            if (chatState.isTyping) {
                item { AiTypingDots() }
            }
            if (chatState.error != null) {
                item {
                    Text(
                        "⚠️ ${chatState.error}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(10.dp)
                    )
                }
            }
        }

        // ── Input bar ─────────────────────────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            "Ask about your list…",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend(); keyboard?.hide() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ForestGreen,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium
                )

                var pressed by remember { mutableStateOf(false) }
                val btnScale by animateFloatAsState(
                    targetValue = if (pressed) 0.88f else 1f,
                    animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessHigh),
                    label = "ai_send_scale",
                    finishedListener = { pressed = false }
                )

                FilledIconButton(
                    onClick = { pressed = true; onSend(); keyboard?.hide() },
                    modifier = Modifier
                        .size(46.dp)
                        .scale(btnScale),
                    enabled = !chatState.isTyping && inputText.isNotBlank(),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = ForestGreen,
                        contentColor = Color.White
                    )
                ) {
                    if (chatState.isTyping) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── Quick action chip ─────────────────────────────────────────────────────────
@Composable
private fun AiQuickActionChip(
    emoji: String,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = ForestGreen.copy(alpha = 0.10f),
        modifier = Modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(emoji, fontSize = 14.sp, maxLines = 1, softWrap = false)
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = ForestGreen,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

// ── Mini chat bubble ──────────────────────────────────────────────────────────
@Composable
private fun AiMiniChatBubble(message: AiViewModel.ChatUiMessage) {
    val isUser = message.isUser
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isUser) {
            Surface(
                modifier = Modifier.size(22.dp),
                shape = CircleShape,
                color = ForestGreen
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
            Spacer(Modifier.width(6.dp))
        }

        Surface(
            modifier = Modifier.widthIn(max = 260.dp),
            shape = RoundedCornerShape(
                topStart = 14.dp, topEnd = 14.dp,
                bottomStart = if (isUser) 14.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 14.dp
            ),
            color = if (isUser)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodySmall,
                color = if (isUser)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

// ── Typing dots indicator ─────────────────────────────────────────────────────
@Composable
private fun AiTypingDots() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(22.dp),
            shape = CircleShape,
            color = ForestGreen
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.AutoAwesome,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
        Spacer(Modifier.width(6.dp))
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { i ->
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { delay(i * 150L); visible = true }
                    AnimatedVisibility(visible = visible, enter = fadeIn()) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    CircleShape
                                )
                        )
                    }
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
