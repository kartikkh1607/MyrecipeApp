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
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
