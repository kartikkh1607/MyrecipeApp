package com.kartik.mealtime.ui.screens

import android.Manifest
import android.content.Intent
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kartik.mealtime.domain.model.Recipe
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CookingModeScreen(
    recipe: Recipe,
    onExitCookingMode: () -> Unit
) {
    val steps = recipe.instructions
    var currentStepIndex by remember { mutableIntStateOf(0) }
    var completedSteps by remember { mutableStateOf(setOf<Int>()) }
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isAllDone = completedSteps.size == steps.size && steps.isNotEmpty()
    val isLast = steps.isNotEmpty() && currentStepIndex == steps.lastIndex

    // ── Voice recognition (unchanged) ────────────────────────────────────────
    var isListening by remember { mutableStateOf(false) }
    var voiceHint by remember { mutableStateOf("") }

    val speechRecognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context))
            SpeechRecognizer.createSpeechRecognizer(context)
        else null
    }
    DisposableEffect(Unit) { onDispose { speechRecognizer?.destroy() } }

    fun handleVoiceCommand(text: String) {
        val lower = text.lowercase().trim()
        when {
            lower.contains("next") || lower.contains("forward") -> {
                voiceHint = "👉 Next step"
                if (!isLast) {
                    completedSteps = completedSteps + currentStepIndex
                    currentStepIndex++
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                } else onExitCookingMode()
            }

            lower.contains("back") || lower.contains("previous") || lower.contains("prev") -> {
                voiceHint = "👈 Previous step"
                if (currentStepIndex > 0) {
                    completedSteps = completedSteps - currentStepIndex
                    currentStepIndex--
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }

            lower.contains("finish") || lower.contains("done") || lower.contains("exit") -> {
                voiceHint = "✅ Finishing"
                onExitCookingMode()
            }

            else -> voiceHint = "Try: \"next\", \"back\", \"finish\""
        }
        scope.launch { delay(2000); voiceHint = "" }
    }

    fun startListening() {
        val sr = speechRecognizer ?: run {
            voiceHint = "Voice not available"
            scope.launch { delay(2000); voiceHint = "" }
            return
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        sr.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: android.os.Bundle?) {
                voiceHint = "Listening…"
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                isListening = false
            }

            override fun onError(error: Int) {
                isListening = false
                voiceHint = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
                    else -> "Try again"
                }
                scope.launch { delay(2000); voiceHint = "" }
            }

            override fun onResults(results: android.os.Bundle?) {
                isListening = false
                val best =
                    results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                        ?: return
                handleVoiceCommand(best)
            }

            override fun onPartialResults(partialResults: android.os.Bundle?) {}
            override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
        })
        isListening = true
        sr.startListening(intent)
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startListening()
        else {
            voiceHint = "Mic permission denied"; scope.launch { delay(2000); voiceHint = "" }
        }
    }

    fun onMicClick() {
        if (isListening) {
            speechRecognizer?.stopListening(); isListening = false; voiceHint = ""; return
        }
        val granted = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        if (granted) startListening() else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    // ── Layout ────────────────────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        CookingHeader(
            recipe = recipe,
            currentStep = currentStepIndex,
            totalSteps = steps.size,
            completedCount = completedSteps.size,
            onExit = onExitCookingMode
        )

        // ── Step progress dots ────────────────────────────────────────────────
        if (steps.isNotEmpty()) {
            StepProgressDots(
                totalSteps = steps.size,
                currentStep = currentStepIndex,
                completedSteps = completedSteps,
                onStepClick = { index ->
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    currentStepIndex = index
                }
            )
        }

        // ── Animated step content ─────────────────────────────────────────────
        AnimatedContent(
            targetState = currentStepIndex,
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                (slideInHorizontally(tween(300)) { dir * it } + fadeIn(tween(200))) togetherWith
                        (slideOutHorizontally(tween(300)) { -dir * it } + fadeOut(tween(200)))
            },
            modifier = Modifier.weight(1f),
            label = "step_content"
        ) { stepIdx ->
            val step = steps.getOrNull(stepIdx)

            if (step == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📋", fontSize = 48.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "No step-by-step instructions\navailable for this recipe.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Per-step timer state — resets automatically when AnimatedContent re-composes for a new step
                val timerTotal = remember { (step.duration ?: 0) * 60 }
                var timerSecondsLeft by remember { mutableIntStateOf(timerTotal) }
                var timerActive by remember { mutableStateOf(false) }
                val timerDone = timerTotal > 0 && timerSecondsLeft == 0

                LaunchedEffect(timerActive) {
                    if (timerActive) {
                        while (timerSecondsLeft > 0) {
                            delay(1000L)
                            timerSecondsLeft--
                        }
                        timerActive = false
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                ) {
                    Spacer(Modifier.height(24.dp))

                    // Large faded step number
                    Text(
                        text = "%02d".format(step.stepNumber),
                        fontSize = 80.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(Modifier.height(4.dp))

                    // First sentence as bold headline
                    val sentences = step.instruction.split(Regex("(?<=[.!?])\\s+"), limit = 2)
                    val headline =
                        sentences.firstOrNull()?.trimEnd('.', '!', '?') ?: step.instruction
                    val detailBody = if (sentences.size > 1) sentences[1] else null

                    Text(
                        text = "$headline.",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        lineHeight = 34.sp
                    )

                    Spacer(Modifier.height(20.dp))

                    // Detail card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = detailBody ?: step.instruction,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 22.sp
                            )

                            // Timer + heat chips row
                            val heatLabel = when {
                                step.instruction.contains("high", ignoreCase = true) -> "HIGH HEAT"
                                step.instruction.contains("low", ignoreCase = true) -> "LOW HEAT"
                                listOf(
                                    "heat",
                                    "boil",
                                    "simmer",
                                    "fry",
                                    "toast",
                                    "roast",
                                    "bake",
                                    "cook",
                                    "sauté",
                                    "medium"
                                )
                                    .any {
                                        step.instruction.contains(
                                            it,
                                            ignoreCase = true
                                        )
                                    } -> "MEDIUM HEAT"

                                else -> null
                            }

                            if (timerTotal > 0 || heatLabel != null) {
                                Spacer(Modifier.height(16.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (heatLabel != null) {
                                        StepChip(
                                            icon = {
                                                Icon(
                                                    Icons.Default.LocalFireDepartment, null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            label = heatLabel
                                        )
                                    }
                                }
                            }

                            // Countdown timer — shown when step has a duration
                            if (timerTotal > 0) {
                                Spacer(Modifier.height(20.dp))
                                CountdownTimer(
                                    totalSeconds = timerTotal,
                                    secondsLeft = timerSecondsLeft,
                                    isActive = timerActive,
                                    isDone = timerDone,
                                    onToggle = { timerActive = !timerActive },
                                    onReset = {
                                        timerActive = false
                                        timerSecondsLeft = timerTotal
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Recipe photo + tip overlay
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(20.dp))
                    ) {
                        AsyncImage(
                            model = recipe.imageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Dark gradient at the bottom for tip readability
                        if (step.tips != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.65f)
                                            ),
                                            startY = 300f
                                        )
                                    )
                            )
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(14.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.TipsAndUpdates, null,
                                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Text(
                                    step.tips,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Step completed badge
                    AnimatedVisibility(
                        visible = stepIdx in completedSteps,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Check,
                                            null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Text(
                                    "Step completed",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    // All done banner
                    if (isAllDone && stepIdx == steps.lastIndex) {
                        Spacer(Modifier.height(20.dp))
                        AllDoneBanner(recipeName = recipe.name, onExit = onExitCookingMode)
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }

        // ── Bottom nav bar ────────────────────────────────────────────────────
        CookingBottomBar(
            currentStep = currentStepIndex,
            totalSteps = steps.size,
            isLast = isLast,
            isListening = isListening,
            voiceHint = voiceHint,
            onPrev = {
                if (currentStepIndex > 0) {
                    completedSteps = completedSteps - currentStepIndex
                    currentStepIndex--
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            },
            onNext = {
                if (steps.isNotEmpty()) {
                    completedSteps = completedSteps + currentStepIndex
                    if (!isLast) currentStepIndex++ else onExitCookingMode()
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            },
            onMic = { onMicClick() }
        )
    }
}

// ── Header with progress strip ────────────────────────────────────────────────
@Composable
private fun CookingHeader(
    recipe: Recipe,
    currentStep: Int,
    totalSteps: Int,
    completedCount: Int,
    onExit: () -> Unit
) {
    val overallProgress by animateFloatAsState(
        targetValue = if (totalSteps > 0) completedCount.toFloat() / totalSteps else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "header_progress"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 12.dp, top = 8.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onExit) {
                Icon(
                    Icons.Default.Close,
                    "Exit",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "COOKING MODE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f),
                    letterSpacing = 1.sp
                )
                Text(
                    recipe.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1
                )
            }

            Spacer(Modifier.width(8.dp))

            // Recipe thumbnail
            AsyncImage(
                model = recipe.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
            )

            Spacer(Modifier.width(10.dp))

            // Step counter pill
            if (totalSteps > 0) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        "${currentStep + 1} / $totalSteps",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Overall progress strip
        LinearProgressIndicator(
            progress = { overallProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
            strokeCap = StrokeCap.Round
        )
    }
}

// ── Step progress dots ────────────────────────────────────────────────────────
@Composable
private fun StepProgressDots(
    totalSteps: Int,
    currentStep: Int,
    completedSteps: Set<Int>,
    onStepClick: (Int) -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(currentStep) {
        listState.animateScrollToItem(currentStep.coerceAtMost((totalSteps - 1).coerceAtLeast(0)))
    }

    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        items(List(totalSteps) { it }) { index ->
            val isCompleted = index in completedSteps
            val isCurrent = index == currentStep

            val dotWidth by animateDpAsState(
                targetValue = if (isCurrent) 24.dp else 8.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "dot_width_$index"
            )
            val dotColor by animateColorAsState(
                targetValue = when {
                    isCurrent -> MaterialTheme.colorScheme.primary
                    isCompleted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                },
                label = "dot_color_$index"
            )

            Box(
                modifier = Modifier
                    .size(width = dotWidth, height = 8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
                    .clickable { onStepClick(index) }
            )
        }
    }
}

// ── Countdown timer ───────────────────────────────────────────────────────────
@Composable
private fun CountdownTimer(
    totalSeconds: Int,
    secondsLeft: Int,
    isActive: Boolean,
    isDone: Boolean,
    onToggle: () -> Unit,
    onReset: () -> Unit
) {
    val progress by animateFloatAsState(
        targetValue = if (totalSeconds > 0) secondsLeft.toFloat() / totalSeconds.toFloat() else 0f,
        animationSpec = tween(800),
        label = "timer_progress"
    )

    val timerColor = when {
        isDone -> Color(0xFF4CAF50)
        secondsLeft <= 30 && !isDone -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Label
        Column {
            Text(
                "Step Timer",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                when {
                    isDone -> "Done!"
                    isActive -> "Counting down…"
                    secondsLeft == totalSeconds -> "${totalSeconds / 60} min"
                    else -> "Paused"
                },
                style = MaterialTheme.typography.bodySmall,
                color = timerColor
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Reset button
            if (secondsLeft < totalSeconds) {
                IconButton(onClick = onReset, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Refresh,
                        "Reset",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Circular timer + play/pause
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(64.dp),
                    strokeWidth = 4.dp,
                    color = timerColor,
                    trackColor = timerColor.copy(alpha = 0.15f),
                    strokeCap = StrokeCap.Round
                )
                if (isDone) {
                    Icon(
                        Icons.Default.Check,
                        null,
                        tint = timerColor,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            formatTimerSeconds(secondsLeft),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = timerColor
                        )
                        Icon(
                            imageVector = if (isActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isActive) "Pause" else "Start",
                            tint = timerColor,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { onToggle() }
                        )
                    }
                }
            }
        }
    }
}

private fun formatTimerSeconds(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}

// ── All done banner ───────────────────────────────────────────────────────────
@Composable
private fun AllDoneBanner(recipeName: String, onExit: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🎉", fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                "Recipe Complete!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Enjoy your $recipeName!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            Surface(
                onClick = onExit,
                shape = RoundedCornerShape(50.dp),
                color = MaterialTheme.colorScheme.primary
            ) {
                Text(
                    "Done  ✓",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 14.dp)
                )
            }
        }
    }
}

// ── Bottom navigation bar ─────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CookingBottomBar(
    currentStep: Int,
    totalSteps: Int,
    isLast: Boolean,
    isListening: Boolean,
    voiceHint: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onMic: () -> Unit
) {
    val pulseTransition = rememberInfiniteTransition(label = "mic_pulse")
    val micScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.18f else 1f,
        animationSpec = infiniteRepeatable(animation = tween(600), repeatMode = RepeatMode.Reverse),
        label = "mic_scale"
    )
    val micBgColor by animateColorAsState(
        targetValue = if (isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
        label = "mic_bg"
    )
    val micIconColor by animateColorAsState(
        targetValue = if (isListening) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
        label = "mic_icon"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ← Previous
            Surface(
                onClick = onPrev,
                shape = CircleShape,
                color = if (currentStep > 0)
                    MaterialTheme.colorScheme.surfaceVariant
                else
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack, "Previous",
                        tint = if (currentStep > 0) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Mic
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    onClick = onMic,
                    shape = CircleShape,
                    color = micBgColor,
                    modifier = Modifier
                        .size(52.dp)
                        .scale(micScale)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = if (isListening) "Stop" else "Voice",
                            tint = micIconColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                if (voiceHint.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        voiceHint,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isListening) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }

            // Next / Finish
            Surface(
                onClick = onNext,
                shape = RoundedCornerShape(50.dp),
                color = if (totalSteps == 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.primary
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        if (isLast) "Finish" else "Next Step",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Icon(
                        if (isLast) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                        null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ── Chip ──────────────────────────────────────────────────────────────────────
@Composable
private fun StepChip(icon: @Composable () -> Unit, label: String) {
    Surface(
        shape = RoundedCornerShape(50.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Spacer(Modifier.width(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp
            )
        }
    }
}
