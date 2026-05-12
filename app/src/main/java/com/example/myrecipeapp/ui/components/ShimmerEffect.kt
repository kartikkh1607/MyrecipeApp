package com.example.myrecipeapp.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────────────────────────
// Core shimmer brush — reused by all skeleton composables below
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun shimmerBrush(
    shimmerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    highlightColor: Color = MaterialTheme.colorScheme.surface,
    durationMillis: Int = 1200
): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnimation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )
    return Brush.linearGradient(
        colors = listOf(shimmerColor, highlightColor, shimmerColor),
        start = Offset(translateAnimation - 300f, translateAnimation - 300f),
        end = Offset(translateAnimation, translateAnimation)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Generic shimmer box — use for any shape
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 8.dp,
    brush: Brush = shimmerBrush()
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(brush)
    )
}

/**
 * Modifier extension that applies the animated shimmer brush as a background.
 * Must be called from a Composable scope.
 * Usage: `Modifier.shimmerEffect()`
 */
@Composable
fun Modifier.shimmerEffect(): Modifier = this.background(shimmerBrush())

// ─────────────────────────────────────────────────────────────────────────────
// Featured Carousel skeleton — matches the real FeaturedRecipeCarousel height
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun FeaturedCarouselSkeleton() {
    val brush = shimmerBrush()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(280.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(brush)
    ) {
        // Simulate the overlay text at the bottom of the card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(20.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.45f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Full page grid skeleton — for the Categories / Search results grid
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun GridSkeletonScreen(itemCount: Int = 6) {
    val brush = shimmerBrush()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(itemCount / 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(2) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ShimmerBox(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            cornerRadius = 16.dp,
                            brush = brush
                        )
                        ShimmerBox(
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(12.dp),
                            cornerRadius = 6.dp,
                            brush = brush
                        )
                        ShimmerBox(
                            modifier = Modifier
                                .fillMaxWidth(0.45f)
                                .height(10.dp),
                            cornerRadius = 5.dp,
                            brush = brush
                        )
                    }
                }
            }
        }
    }
}

