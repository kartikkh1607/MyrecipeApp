package com.example.myrecipeapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myrecipeapp.domain.model.Ingredient
import com.example.myrecipeapp.domain.model.Recipe
import com.example.myrecipeapp.domain.model.RecipeStep
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun CookingModeScreen(
    recipe: Recipe,
    onExitCookingMode: () -> Unit
) {
    var currentStepIndex by remember { mutableStateOf(0) }
    var completedSteps by remember { mutableStateOf(setOf<Int>()) }
    var completedIngredients by remember { mutableStateOf(setOf<String>()) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var timerSeconds by remember { mutableStateOf(0) }
    var timerTotal by remember { mutableStateOf(1) }
    val hapticFeedback = LocalHapticFeedback.current
    val isAllDone = completedSteps.size == recipe.instructions.size && recipe.instructions.isNotEmpty()

    // Countdown timer
    LaunchedEffect(isTimerRunning, timerSeconds) {
        if (isTimerRunning && timerSeconds > 0) {
            delay(1000)
            timerSeconds -= 1
        } else if (timerSeconds == 0 && isTimerRunning) {
            isTimerRunning = false
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val progressFraction = if (recipe.instructions.isEmpty()) 0f
    else completedSteps.size.toFloat() / recipe.instructions.size

    val currentStep = recipe.instructions.getOrNull(currentStepIndex)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Header ──────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
                        )
                    )
                )
                .statusBarsPadding()
                .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onExitCookingMode) {
                            Icon(Icons.Default.Close, "Exit", tint = Color.White)
                        }
                        Column {
                            Text(
                                "Cooking Mode",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                recipe.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    // Step counter badge
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Text(
                            "Step ${currentStepIndex + 1}/${recipe.instructions.size}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Progress bar
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${completedSteps.size} of ${recipe.instructions.size} steps done",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        // ── Content ─────────────────────────────────────────────────────────────
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Completed celebration
            if (isAllDone) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🎉", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Recipe Complete!",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "Enjoy your ${recipe.name}!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onExitCookingMode) {
                                Text("Done Cooking")
                            }
                        }
                    }
                }
            }

            // Timer card (if current step has duration)
            if (currentStep?.duration != null) {
                item {
                    ImprovedTimer(
                        isRunning = isTimerRunning,
                        seconds = timerSeconds,
                        total = timerTotal,
                        onStart = {
                            timerTotal = (currentStep.duration ?: 1) * 60
                            timerSeconds = timerTotal
                            isTimerRunning = true
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onPause = {
                            isTimerRunning = !isTimerRunning
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        onReset = {
                            timerTotal = (currentStep.duration ?: 1) * 60
                            timerSeconds = timerTotal
                            isTimerRunning = false
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    )
                }
            }

            // Current step card with animation
            if (recipe.instructions.isNotEmpty()) {
                item {
                    AnimatedContent(
                        targetState = currentStepIndex,
                        transitionSpec = {
                            val dir = if (targetState > initialState) 1 else -1
                            (slideInHorizontally { dir * it } + fadeIn()) togetherWith
                                    (slideOutHorizontally { -dir * it } + fadeOut())
                        },
                        label = "step_transition"
                    ) { stepIdx ->
                        val step = recipe.instructions[stepIdx]
                        val isDone = completedSteps.contains(stepIdx)
                        ImprovedStepCard(
                            step = step,
                            isCompleted = isDone,
                            onToggleComplete = {
                                completedSteps = if (isDone) completedSteps - stepIdx
                                else completedSteps + stepIdx
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        )
                    }
                }
            }

            // Steps overview
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "All Steps",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        recipe.instructions.forEachIndexed { index, step ->
                            val isCurrent = index == currentStepIndex
                            val isDone = completedSteps.contains(index)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        currentStepIndex = index
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                    .background(
                                        if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                        else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier.size(24.dp),
                                    color = when {
                                        isDone -> MaterialTheme.colorScheme.primary
                                        isCurrent -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                    },
                                    shape = CircleShape
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (isDone) Icon(
                                            Icons.Default.Check, null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        else Text(
                                            (index + 1).toString(),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCurrent) Color.White
                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    step.instruction,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDone) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    else if (isCurrent) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                                    maxLines = 2,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (index < recipe.instructions.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 36.dp, top = 4.dp, bottom = 4.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                                )
                            }
                        }
                    }
                }
            }

            // Ingredients checklist
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Ingredients",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        recipe.ingredients.forEach { ing ->
                            val key = ing.id.ifEmpty { ing.name }
                            val done = completedIngredients.contains(key)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { completedIngredients = if (done) completedIngredients - key else completedIngredients + key }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = done,
                                    onCheckedChange = { completedIngredients = if (done) completedIngredients - key else completedIngredients + key },
                                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                                )
                                Text(
                                    "${ing.amount} ${ing.unit} ${ing.name}".trim(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (done) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    else MaterialTheme.colorScheme.onSurface,
                                    textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        // ── Bottom Navigation ────────────────────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 12.dp,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        if (currentStepIndex > 0) {
                            currentStepIndex--
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    },
                    enabled = currentStepIndex > 0,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Previous")
                }
                Button(
                    onClick = {
                        val isLast = currentStepIndex == recipe.instructions.size - 1
                        if (isLast) {
                            completedSteps = completedSteps + currentStepIndex
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        } else {
                            // Auto-mark current step done when advancing
                            completedSteps = completedSteps + currentStepIndex
                            currentStepIndex++
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    },
                    enabled = currentStepIndex < recipe.instructions.size,
                    modifier = Modifier.weight(1.4f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    val isLast = currentStepIndex == recipe.instructions.size - 1
                    Text(if (isLast) "Finish ✓" else "Next Step", fontWeight = FontWeight.Bold)
                    if (!isLast) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ImprovedStepCard(
    step: RecipeStep,
    isCompleted: Boolean,
    onToggleComplete: () -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = if (isCompleted)
            MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
        animationSpec = tween(400),
        label = "step_card_color"
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Large step number badge
                Surface(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isCompleted) Icon(
                            Icons.Default.Check,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        else Text(
                            step.stepNumber.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                // Mark done button
                FilledTonalButton(
                    onClick = onToggleComplete,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isCompleted)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Icon(
                        if (isCompleted) Icons.Default.Undo else Icons.Default.Check,
                        null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isCompleted) "Undo" else "Done")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                step.instruction,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 26.sp,
                color = if (isCompleted)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface
            )

            if (step.tips != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp)) {
                        Text("💡", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            step.tips,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ImprovedTimer(
    isRunning: Boolean,
    seconds: Int,
    total: Int,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit
) {
    val progress = if (total > 0) seconds.toFloat() / total else 0f
    val timerColor = if (isRunning)
        MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isRunning)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Circular progress ring
            Box(
                modifier = Modifier.size(80.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
                    drawArc(
                        color = timerColor.copy(alpha = 0.2f),
                        startAngle = -90f, sweepAngle = 360f,
                        useCenter = false, style = stroke
                    )
                    drawArc(
                        color = timerColor,
                        startAngle = -90f, sweepAngle = 360f * progress,
                        useCenter = false, style = stroke
                    )
                }
                Text(
                    formatTime(seconds),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = timerColor
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Step Timer",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isRunning) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (seconds == 0 && !isRunning) {
                        Button(
                            onClick = onStart,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(38.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Start", style = MaterialTheme.typography.labelMedium)
                        }
                    } else {
                        FilledTonalIconButton(onClick = onPause, modifier = Modifier.size(38.dp)) {
                            Icon(
                                if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                null, modifier = Modifier.size(18.dp)
                            )
                        }
                        FilledTonalIconButton(onClick = onReset, modifier = Modifier.size(38.dp)) {
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}
