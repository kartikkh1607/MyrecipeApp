package com.example.myrecipeapp

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookingModeScreen(
    recipe: Recipe,
    onExitCookingMode: () -> Unit
) {
    var currentStepIndex by remember { mutableStateOf(0) }
    var completedIngredients by remember { mutableStateOf(setOf<String>()) }
    var completedSteps by remember { mutableStateOf(setOf<Int>()) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var timerSeconds by remember { mutableStateOf(0) }
    val hapticFeedback = LocalHapticFeedback.current

    // Timer logic
    LaunchedEffect(isTimerRunning, timerSeconds) {
        if (isTimerRunning && timerSeconds > 0) {
            delay(1000)
            timerSeconds -= 1
        } else if (timerSeconds == 0 && isTimerRunning) {
            isTimerRunning = false
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            // Timer completed - could add notification here
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Cooking Mode Header
        CookingModeHeader(
            recipeName = recipe.name,
            currentStep = currentStepIndex + 1,
            totalSteps = recipe.instructions.size,
            onExit = onExitCookingMode
        )

        // Main Content
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Timer Section (if current step has duration)
            item {
                recipe.instructions.getOrNull(currentStepIndex)?.duration?.let { duration ->
                    CookingTimer(
                        isRunning = isTimerRunning,
                        seconds = timerSeconds,
                        onStart = {
                            timerSeconds = duration * 60
                            isTimerRunning = true
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onPause = {
                            isTimerRunning = !isTimerRunning
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        onReset = {
                            timerSeconds = duration * 60
                            isTimerRunning = false
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    )
                }
            }

            // Current Step
            item {
                if (recipe.instructions.isNotEmpty()) {
                    CurrentStepCard(
                        step = recipe.instructions[currentStepIndex],
                        isCompleted = completedSteps.contains(currentStepIndex),
                        onToggleComplete = {
                            completedSteps = if (completedSteps.contains(currentStepIndex)) {
                                completedSteps - currentStepIndex
                            } else {
                                completedSteps + currentStepIndex
                            }
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    )
                }
            }

            // Ingredients Checklist
            item {
                IngredientsChecklist(
                    ingredients = recipe.ingredients,
                    completedIngredients = completedIngredients,
                    onToggleIngredient = { ingredientId ->
                        completedIngredients = if (completedIngredients.contains(ingredientId)) {
                            completedIngredients - ingredientId
                        } else {
                            completedIngredients + ingredientId
                        }
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                )
            }

            // All Steps Progress
            item {
                AllStepsProgress(
                    steps = recipe.instructions,
                    currentStepIndex = currentStepIndex,
                    completedSteps = completedSteps,
                    onStepClick = { index ->
                        currentStepIndex = index
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                )
            }
        }

        // Navigation Controls
        CookingNavigationControls(
            currentStepIndex = currentStepIndex,
            totalSteps = recipe.instructions.size,
            onPreviousStep = {
                if (currentStepIndex > 0) {
                    currentStepIndex -= 1
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            },
            onNextStep = {
                if (currentStepIndex < recipe.instructions.size - 1) {
                    currentStepIndex += 1
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }
        )
    }
}

@Composable
fun CookingModeHeader(
    recipeName: String,
    currentStep: Int,
    totalSteps: Int,
    onExit: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onExit) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Exit Cooking Mode",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Cooking Mode",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = recipeName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Step $currentStep of $totalSteps",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Keep Screen Awake Toggle (placeholder)
            IconButton(onClick = { /* Toggle screen awake */ }) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = "Keep Screen Awake",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun CookingTimer(
    isRunning: Boolean,
    seconds: Int,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isRunning) MaterialTheme.colorScheme.primary 
                           else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Timer",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isRunning) MaterialTheme.colorScheme.onPrimary
                       else MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Timer Display
            Text(
                text = formatTime(seconds),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = if (isRunning) MaterialTheme.colorScheme.onPrimary
                       else MaterialTheme.colorScheme.onSurface,
                fontSize = 48.sp
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Timer Controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (seconds > 0) {
                    IconButton(
                        onClick = onPause,
                        modifier = Modifier
                            .background(
                                if (isRunning) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.primary,
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isRunning) "Pause" else "Resume",
                            tint = if (isRunning) MaterialTheme.colorScheme.onPrimary
                                  else MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    
                    IconButton(
                        onClick = onReset,
                        modifier = Modifier
                            .background(
                                if (isRunning) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.outline,
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset",
                            tint = if (isRunning) MaterialTheme.colorScheme.onPrimary
                                  else MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else {
                    Button(
                        onClick = onStart,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRunning) MaterialTheme.colorScheme.onPrimary
                                           else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Timer")
                    }
                }
            }
        }
    }
}

@Composable
fun CurrentStepCard(
    step: RecipeStep,
    isCompleted: Boolean,
    onToggleComplete: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "step_card_scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable {
                isPressed = !isPressed
                onToggleComplete()
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) MaterialTheme.colorScheme.primaryContainer
                           else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = step.stepNumber.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                
                Checkbox(
                    checked = isCompleted,
                    onCheckedChange = { onToggleComplete() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = step.instruction,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.3f
            )
            
            if (step.tips != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "💡 ${step.tips}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun IngredientsChecklist(
    ingredients: List<Ingredient>,
    completedIngredients: Set<String>,
    onToggleIngredient: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Ingredients Checklist",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            ingredients.forEach { ingredient ->
                val isCompleted = completedIngredients.contains(ingredient.id.ifEmpty { ingredient.name })
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                            onToggleIngredient(ingredient.id.ifEmpty { ingredient.name })
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isCompleted,
                        onCheckedChange = { 
                            onToggleIngredient(ingredient.id.ifEmpty { ingredient.name })
                        },
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    
                    Text(
                        text = "${ingredient.amount} ${ingredient.unit} ${ingredient.name}",
                        style = if (isCompleted) {
                            MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        } else {
                            MaterialTheme.typography.bodyMedium
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun AllStepsProgress(
    steps: List<RecipeStep>,
    currentStepIndex: Int,
    completedSteps: Set<Int>,
    onStepClick: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "All Steps",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            steps.forEachIndexed { index, step ->
                val isCurrentStep = index == currentStepIndex
                val isCompleted = completedSteps.contains(index)
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onStepClick(index) }
                        .padding(vertical = 8.dp)
                        .background(
                            color = if (isCurrentStep) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                   else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(24.dp),
                        color = when {
                            isCompleted -> MaterialTheme.colorScheme.primary
                            isCurrentStep -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.outline
                        },
                        shape = CircleShape
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isCompleted) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Completed",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Text(
                                    text = (index + 1).toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isCurrentStep) MaterialTheme.colorScheme.onPrimary
                                           else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Text(
                        text = step.instruction,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isCurrentStep) MaterialTheme.colorScheme.onSurface
                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 2,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun CookingNavigationControls(
    currentStepIndex: Int,
    totalSteps: Int,
    onPreviousStep: () -> Unit,
    onNextStep: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onPreviousStep,
                enabled = currentStepIndex > 0,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Previous")
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Button(
                onClick = onNextStep,
                enabled = currentStepIndex < totalSteps - 1,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Next")
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%02d:%02d".format(minutes, remainingSeconds)
}
