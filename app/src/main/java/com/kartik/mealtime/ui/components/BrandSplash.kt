package com.kartik.mealtime.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.kartik.mealtime.ui.theme.ForestGreen
import com.kartik.mealtime.ui.theme.OnForest
import kotlinx.coroutines.delay

/**
 * Second half of the app's single splash. The system SplashScreen paints the
 * forest-green background + brand badge (ic_splash_badge) on cold launch; this
 * composable takes over seamlessly — same green, same badge at the same spot —
 * and animates the wordmark + tagline in beneath the (static) badge before
 * calling [onFinished], which lets [MainActivity] swap it for the app.
 *
 * The badge does NOT animate in: it's already on screen from the system splash,
 * so re-animating it would produce a visible pop/jump at the hand-off. Only the
 * text below it reveals.
 *
 * Sequence:
 *   0 ms    Solid forest-green bg + badge (identical to the system splash, so
 *           the hand-off is invisible) with a faint diagonal hatch.
 *   180 ms  Wordmark slides up + fades in (below the badge).
 *   380 ms  Tagline fades in (slight delay so reading flow is staggered).
 *   1200 ms onFinished() fires — caller swaps us out for the app.
 */
@Composable
fun BrandSplash(onFinished: () -> Unit) {
    var titleVisible by remember { mutableStateOf(false) }
    var taglineVisible by remember { mutableStateOf(false) }

    // Force light status-bar icons against the dark forest-green background.
    // Restores the previous value on exit so the next screen can set its own.
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as android.app.Activity).window
        val controller = WindowCompat.getInsetsController(window, view)
        val previous = controller.isAppearanceLightStatusBars
        controller.isAppearanceLightStatusBars = false
        onDispose { controller.isAppearanceLightStatusBars = previous }
    }

    LaunchedEffect(Unit) {
        delay(180); titleVisible = true
        delay(200); taglineVisible = true
        delay(820); onFinished()
    }

    val titleTranslateY by animateFloatAsState(
        targetValue = if (titleVisible) 0f else 60f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = 240f
        ),
        label = "title_translate"
    )
    val titleAlpha by animateFloatAsState(
        targetValue = if (titleVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 350),
        label = "title_alpha"
    )
    val taglineAlpha by animateFloatAsState(
        targetValue = if (taglineVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "tagline_alpha"
    )

    // Faint diagonal hatch over the solid forest background (prototype detail).
    val hatch = Color.White.copy(alpha = 0.04f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ForestGreen)
            .drawBehind {
                val gap = 16.dp.toPx()
                val stroke = 1.dp.toPx()
                var x = -size.height
                while (x < size.width) {
                    drawLine(
                        color = hatch,
                        start = Offset(x, 0f),
                        end = Offset(x + size.height, size.height),
                        strokeWidth = stroke
                    )
                    x += gap
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Brand badge — pinned to the optical center, identical to the static
        // system-splash badge (ic_splash_badge) so the hand-off is invisible.
        // It never animates; only the text below it reveals.
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .size(100.dp),
            color = Color.White,
            shape = RoundedCornerShape(30.dp),
            shadowElevation = 16.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Restaurant,
                    contentDescription = null,
                    tint = ForestGreen,
                    modifier = Modifier.size(50.dp)
                )
            }
        }

        // Wordmark + tagline animate in *beneath* the badge without shifting it.
        // Offset places the text block below the centered badge (half badge +
        // gap + ~half text height ≈ 120dp).
        val textOffsetY = with(LocalDensity.current) { 120.dp.toPx() }
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer { translationY = textOffsetY },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "MealTime",
                style = MaterialTheme.typography.displayLarge,  // Newsreader serif
                color = OnForest,                               // warm cream
                modifier = Modifier
                    .alpha(titleAlpha)
                    .graphicsLayer { translationY = titleTranslateY }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Cook something worth the table.",
                style = MaterialTheme.typography.bodyLarge,
                color = OnForest.copy(alpha = 0.78f),
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(taglineAlpha)
            )
        }

        // Three pulsing dots near the bottom (prototype detail).
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 56.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            repeat(3) { i -> PulsingDot(delayMillis = i * 200) }
        }
    }
}

@Composable
private fun PulsingDot(delayMillis: Int) {
    val transition = rememberInfiniteTransition(label = "dot")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(delayMillis)
        ),
        label = "dot_pulse"
    )
    Box(
        modifier = Modifier
            .size(7.dp)
            .scale(1f + progress * 0.35f)
            .alpha(0.3f + progress * 0.7f)
            .background(OnForest, CircleShape)
    )
}
