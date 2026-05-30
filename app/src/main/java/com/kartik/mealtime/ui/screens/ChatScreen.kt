package com.kartik.mealtime.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kartik.mealtime.data.billing.BillingManager
import com.kartik.mealtime.ui.components.GeneratedRecipeSheet
import com.kartik.mealtime.ui.components.UpsellBottomSheet
import com.kartik.mealtime.ui.theme.ForestGreen
import com.kartik.mealtime.ui.viewmodel.AiViewModel
import com.kartik.mealtime.ui.viewmodel.BillingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onBackClick: () -> Unit,
    onOpenCreations: () -> Unit,
    onOpenMealPlanner: () -> Unit,
    onOpenRecipe: (String) -> Unit,
    viewModel: AiViewModel,
    billingViewModel: BillingViewModel = hiltViewModel()
) {
    val chatState by viewModel.chatState
    val genState by viewModel.recipeGenState
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val productDetails by billingViewModel.productDetails.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val keyboard = LocalSoftwareKeyboardController.current
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    var inputText by remember { mutableStateOf("") }
    var showUpsell by remember { mutableStateOf(false) }

    // The ViewModel asks for the paywall when the proxy rejects generation with 402
    // (entitlement lapsed / mirror out of sync).
    LaunchedEffect(Unit) {
        viewModel.upsellEvents.collect { showUpsell = true }
    }

    // Purchase outcomes from the (app-scoped) BillingManager: dismiss the paywall on
    // success, surface a message on failure, stay silent on user-cancel.
    LaunchedEffect(Unit) {
        billingViewModel.purchaseEvents.collect { result ->
            when (result) {
                BillingManager.PurchaseResult.Success -> {
                    showUpsell = false
                    Toast.makeText(context, "You're Premium now — enjoy!", Toast.LENGTH_SHORT)
                        .show()
                }

                BillingManager.PurchaseResult.Cancelled -> Unit
                is BillingManager.PurchaseResult.Error ->
                    Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    // Premium gate: generate when unlocked, otherwise prompt to upgrade.
    fun attemptGenerate(query: String) {
        val q = query.trim()
        if (q.isBlank()) return
        keyboard?.hide()
        if (isPremium) viewModel.generateRecipe(q) else showUpsell = true
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(chatState.messages.size, chatState.isTyping) {
        if (chatState.messages.isNotEmpty()) {
            listState.animateScrollToItem(chatState.messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            // Bottom inset = max(navBar, IME). Stacking navigationBarsPadding + imePadding
            // double-pads, since the IME inset already covers the nav-bar area when shown.
            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
    ) {
        // ── Header ─────────────────────────────────────────────────────────────
        ChatHeader(
            onBackClick = onBackClick,
            onMealPlannerClick = onOpenMealPlanner,
            onCreationsClick = onOpenCreations,
            onClearClick = {
                hapticFeedback.performHapticFeedback(
                    androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress
                )
                viewModel.clearChat()
            }
        )

        // ── Messages ────────────────────────────────────────────────────────────
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(chatState.messages, key = { it.id }) { message ->
                ChatBubble(
                    message = message,
                    // Tapping a suggested recipe turns it into a full, savable recipe.
                    onRecipeClick = { name -> attemptGenerate(name) }
                )
            }

            // Typing indicator
            if (chatState.isTyping) {
                item {
                    TypingIndicator()
                }
            }

            // Error message
            if (chatState.error != null) {
                item {
                    ErrorBubble(message = chatState.error!!)
                }
            }

            // Bottom spacer
            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }

        // ── "Generate full recipe" quick action ──────────────────────────────────
        // Appears once the user has typed something; turns that text into a recipe.
        AnimatedVisibility(visible = inputText.isNotBlank()) {
            GenerateRecipeChip(
                isPremium = isPremium,
                onClick = { attemptGenerate(inputText) }
            )
        }

        // ── Input Bar ───────────────────────────────────────────────────────────
        ChatInputBar(
            value = inputText,
            onValueChange = { inputText = it },
            onSend = {
                if (inputText.isNotBlank()) {
                    viewModel.sendMessage(inputText)
                    inputText = ""
                    keyboard?.hide()
                }
            },
            isLoading = chatState.isTyping
        )
    }

    // ── Generated recipe review sheet ─────────────────────────────────────────
    if (genState !is AiViewModel.RecipeGenState.Idle) {
        GeneratedRecipeSheet(
            state = genState,
            onSave = { viewModel.saveGeneratedRecipe() },
            onOpen = { recipe ->
                viewModel.dismissGeneratedRecipe()
                onOpenRecipe(recipe.id)
            },
            onDismiss = { viewModel.dismissGeneratedRecipe() }
        )
    }

    // ── Premium upsell ────────────────────────────────────────────────────────
    if (showUpsell) {
        UpsellBottomSheet(
            title = "AI Recipe Generator",
            description = "Turn any idea into a complete, cookable recipe.",
            perks = listOf(
                "Full ingredients & step-by-step method",
                "Saved to your AI Creations",
                "Tailored to your diet & preferences"
            ),
            productDetails = productDetails,
            onSelectPlan = { offerToken ->
                activity?.let { billingViewModel.purchase(it, offerToken) }
            },
            onDismiss = { showUpsell = false }
        )
    }
}

/** Unwraps the host [Activity] from a Compose [Context] — needed to launch the Play
 *  billing flow, which requires an Activity. */
private fun Context.findActivity(): Activity? {
    var ctx: Context = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/** Amber assist chip that triggers full-recipe generation; shows a lock when gated. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GenerateRecipeChip(
    isPremium: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
        color = com.kartik.mealtime.ui.theme.Amber.copy(alpha = 0.16f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = com.kartik.mealtime.ui.theme.Amber,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Generate full recipe",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!isPremium) {
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Premium",
                    tint = com.kartik.mealtime.ui.theme.Amber,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun ChatHeader(
    onBackClick: () -> Unit,
    onMealPlannerClick: () -> Unit,
    onCreationsClick: () -> Unit,
    onClearClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // AI Avatar — primary→secondary gradient (matches Linen prototype)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "AI Chef",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(com.kartik.mealtime.ui.theme.Success)
                    )
                    Text(
                        text = "Always ready",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onMealPlannerClick) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Meal Planner",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onCreationsClick) {
                Icon(
                    imageVector = Icons.Default.Bookmarks,
                    contentDescription = "AI Creations",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onClearClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Clear chat",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: AiViewModel.ChatUiMessage,
    onRecipeClick: (String) -> Unit
) {
    val isUser = message.isUser

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Surface(
                modifier = Modifier.size(28.dp),
                shape = CircleShape,
                color = ForestGreen
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            modifier = Modifier.widthIn(max = 280.dp),
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 18.dp
            ),
            color = if (isUser)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(
                modifier = Modifier.padding(14.dp)
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Show suggested recipes if any
                if (message.suggestedRecipes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Suggested Recipes:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isUser)
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        else
                            MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    message.suggestedRecipes.forEach { recipe ->
                        RecipeSuggestionChip(
                            name = recipe.name,
                            description = recipe.description,
                            isUser = isUser,
                            onClick = { onRecipeClick(recipe.name) }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                modifier = Modifier.size(28.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "👤",
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun RecipeSuggestionChip(
    name: String,
    description: String,
    isUser: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isUser)
            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f)
        else
            MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            Text(
                text = "★ $name",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (isUser)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = if (isUser)
                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                else
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TypingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(28.dp),
            shape = CircleShape,
            color = ForestGreen
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(3) { i ->
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(i * 150L)
                        visible = true
                    }
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn()
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        ) {}
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorBubble(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⚠️",
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    isLoading: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "Ask about recipes, ingredients...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() })
            )

            Spacer(modifier = Modifier.width(8.dp))

            var pressed by remember { mutableStateOf(false) }
            val scale by animateFloatAsState(
                targetValue = if (pressed) 0.9f else 1f,
                animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessHigh),
                label = "send_scale",
                finishedListener = { pressed = false }
            )

            FilledIconButton(
                onClick = {
                    pressed = true
                    onSend()
                },
                modifier = Modifier
                    .size(48.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale },
                enabled = !isLoading && value.isNotBlank(),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = ForestGreen,
                    contentColor = Color.White
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send"
                    )
                }
            }
        }
    }
}