package com.kartik.mealtime.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kartik.mealtime.domain.model.Recipe

@Composable
internal fun CookingHeader(
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

// ── Step progress segments (equal-width bars that fill as you advance) ────────
@Composable
internal fun StepProgressDots(
    totalSteps: Int,
    currentStep: Int,
    completedSteps: Set<Int>,
    onStepClick: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        for (index in 0 until totalSteps) {
            val filled = index <= currentStep
            val segColor by animateColorAsState(
                targetValue = if (filled)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceVariant,
                label = "seg_color_$index"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(segColor)
                    .clickable { onStepClick(index) }
            )
        }
    }
}

// ── Countdown timer ───────────────────────────────────────────────────────────
@Composable
internal fun CountdownTimer(
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

internal fun formatTimerSeconds(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}

// ── All done banner ───────────────────────────────────────────────────────────
@Composable
internal fun AllDoneBanner(recipeName: String, onExit: () -> Unit) {
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
internal fun CookingBottomBar(
    currentStep: Int,
    totalSteps: Int,
    isLast: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
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
internal fun StepChip(icon: @Composable () -> Unit, label: String) {
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