package com.kartik.mealtime.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kartik.mealtime.domain.model.ShoppingListItem
import com.kartik.mealtime.ui.theme.ForestGreen
import com.kartik.mealtime.ui.viewmodel.AiViewModel
import kotlinx.coroutines.delay


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShoppingAiPanel(
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
                        .graphicsLayer { scaleX = btnScale; scaleY = btnScale },
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
internal fun AiQuickActionChip(
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
internal fun AiMiniChatBubble(message: AiViewModel.ChatUiMessage) {
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
internal fun AiTypingDots() {
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
internal fun SlSectionHeader(
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
internal fun SlSwipeableItemRow(
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
internal fun SlEmptyState(onBrowse: () -> Unit) {
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
