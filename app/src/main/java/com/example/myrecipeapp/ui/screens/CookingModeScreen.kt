package com.example.myrecipeapp.ui.screens

import android.Manifest
import android.content.Intent
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.myrecipeapp.domain.model.Recipe
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

    val currentStep = steps.getOrNull(currentStepIndex)
    val isAllDone = completedSteps.size == steps.size && steps.isNotEmpty()
    val isLast = steps.isNotEmpty() && currentStepIndex == steps.lastIndex

    // ── Voice recognition state ───────────────────────────────────────────────
    var isListening by remember { mutableStateOf(false) }
    // "" = idle, "Listening…" = active, any other string = feedback text
    var voiceHint by remember { mutableStateOf("") }

    // SpeechRecognizer created once, released on dispose
    val speechRecognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context))
            SpeechRecognizer.createSpeechRecognizer(context)
        else null
    }
    DisposableEffect(Unit) {
        onDispose { speechRecognizer?.destroy() }
    }

    /** Processes a raw transcript into a step command. */
    fun handleVoiceCommand(text: String) {
        val lower = text.lowercase().trim()
        when {
            lower.contains("next") || lower.contains("forward") -> {
                voiceHint = "👉 Next step"
                if (!isLast) {
                    completedSteps = completedSteps + currentStepIndex
                    currentStepIndex++
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                } else {
                    onExitCookingMode()
                }
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

            else -> {
                voiceHint = "Try: \"next\", \"back\", \"finish\""
            }
        }
        scope.launch { delay(2000); voiceHint = "" }
    }

    /** Starts listening. Wires the RecognitionListener inline so it has access to state. */
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
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val best = matches?.firstOrNull() ?: return
                handleVoiceCommand(best)
            }

            override fun onPartialResults(partialResults: android.os.Bundle?) {}
            override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
        })
        isListening = true
        sr.startListening(intent)
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    // Permission launcher — requests RECORD_AUDIO, then starts listening if granted
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startListening()
        else {
            voiceHint = "Mic permission denied"
            scope.launch { delay(2000); voiceHint = "" }
        }
    }

    /** Checks permission then either starts immediately or requests. */
    fun onMicClick() {
        if (isListening) {
            speechRecognizer?.stopListening()
            isListening = false
            voiceHint = ""
            return
        }
        val granted = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        if (granted) startListening()
        else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        // ── Header bar ──────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onExitCookingMode) {
                    Icon(
                        Icons.Default.Close,
                        "Exit",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "COOKING",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        letterSpacing = 1.sp
                    )
                    Text(
                        recipe.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 2
                    )
                }
                if (steps.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            "Step\n${currentStepIndex + 1}/${steps.size}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // ── Scrollable body ───────────────────────────────────────────────────
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
                // No steps
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                ) {
                    Spacer(Modifier.height(28.dp))

                    // ── Large faded step number ───────────────────────────────
                    Text(
                        text = "%02d".format(step.stepNumber),
                        fontSize = 80.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Spacer(Modifier.height(4.dp))

                    // ── Step headline (large, bold) ───────────────────────────
                    // First sentence of instruction as the big headline
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

                    // ── Description card ─────────────────────────────────────
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            // Body text
                            Text(
                                text = detailBody ?: step.instruction,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 22.sp
                            )

                            Spacer(Modifier.height(16.dp))

                            // Time chip
                            if (step.duration != null && step.duration > 0) {
                                StepChip(
                                    icon = {
                                        Icon(
                                            Icons.Default.Schedule,
                                            null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    label = "${step.duration}-${step.duration + 1} MINUTES"
                                )
                                Spacer(Modifier.height(8.dp))
                            }

                            // Heat chip (shown if step mentions heat-related words)
                            val heatKeywords = listOf(
                                "heat",
                                "boil",
                                "simmer",
                                "fry",
                                "toast",
                                "roast",
                                "bake",
                                "cook",
                                "sauté",
                                "medium",
                                "high",
                                "low"
                            )
                            val hasHeat = heatKeywords.any {
                                step.instruction.contains(
                                    it,
                                    ignoreCase = true
                                )
                            }
                            if (hasHeat) {
                                val heatLabel = when {
                                    step.instruction.contains(
                                        "high",
                                        ignoreCase = true
                                    ) -> "HIGH HEAT"

                                    step.instruction.contains(
                                        "low",
                                        ignoreCase = true
                                    ) -> "LOW HEAT"

                                    else -> "MEDIUM HEAT"
                                }
                                StepChip(
                                    icon = {
                                        Icon(
                                            Icons.Default.LocalFireDepartment,
                                            null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    label = heatLabel
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // ── Recipe food photo + overlaid tip card (exactly like Stitch) ───
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(20.dp))
                    ) {
                        AsyncImage(
                            model = recipe.imageUrl,
                            contentDescription = null,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Tip card overlaid at the bottom of the photo
                        if (step.tips != null) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(12.dp)
                                    .fillMaxWidth(0.9f),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.93f),
                                shadowElevation = 4.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.TipsAndUpdates, null,
                                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        step.tips,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 18.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // ── Step completed checkmark indicator ────────────────────
                    if (completedSteps.contains(stepIdx)) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
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
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    "Step completed",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                    }


                    // ── All done banner ───────────────────────────────────────
                    if (isAllDone && stepIdx == steps.lastIndex) {
                        Spacer(Modifier.height(4.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🎉", fontSize = 40.sp)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Recipe Complete!",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Enjoy your ${recipe.name}!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                    }
                }
            }
        }

        // ── Bottom bar: ← | mic | Next Step → ────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ← back arrow
                IconButton(
                    onClick = {
                        if (currentStepIndex > 0) {
                            completedSteps = completedSteps - currentStepIndex
                            currentStepIndex--
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    },
                    enabled = currentStepIndex > 0,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack, "Previous",
                        tint = if (currentStepIndex > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.25f
                        ),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Mic button (centre) — now wired to SpeechRecognizer
                // Pulses while listening
                val pulseTransition = rememberInfiniteTransition(label = "mic_pulse")
                val micScale by pulseTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = if (isListening) 1.18f else 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "mic_scale"
                )
                val micBgColor by animateColorAsState(
                    targetValue = if (isListening)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.secondaryContainer,
                    label = "mic_bg"
                )
                val micIconColor by animateColorAsState(
                    targetValue = if (isListening)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSecondaryContainer,
                    label = "mic_icon"
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        onClick = { onMicClick() },
                        shape = CircleShape,
                        color = micBgColor,
                        modifier = Modifier
                            .size(52.dp)
                            .scale(micScale)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = if (isListening) "Stop listening" else "Voice command",
                                tint = micIconColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    // Voice hint label below mic
                    if (voiceHint.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            voiceHint,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isListening)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }

                // Next Step / Finish button
                Surface(
                    onClick = {
                        if (steps.isNotEmpty()) {
                            completedSteps = completedSteps + currentStepIndex
                            if (!isLast) currentStepIndex++
                            else onExitCookingMode()
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    },
                    shape = RoundedCornerShape(50.dp),
                    color = if (steps.isEmpty()) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.primary
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (isLast) "Finish" else "Next\nStep",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelLarge,
                            lineHeight = 16.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.width(8.dp))
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
}

// ── Chip component ────────────────────────────────────────────────────────────
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
