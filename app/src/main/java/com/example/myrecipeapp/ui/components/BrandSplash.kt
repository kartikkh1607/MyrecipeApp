package com.example.myrecipeapp.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.myrecipeapp.ui.theme.ForestGreen
import kotlinx.coroutines.delay

/**
 * Post-splash brand reveal. Shown for ~1200 ms after the system SplashScreen
 * dismisses, then calls [onFinished] which lets [MainActivity] swap it for
 * [com.example.myrecipeapp.ui.screens.MainScreen].
 *
 * Sequence:
 *   0 ms    Splash bg paints (forest-green gradient) — same color family as
 *           the system splash so the hand-off has no flash.
 *   80 ms   Icon scales in (low-bounce spring).
 *   240 ms  Title slides up + fades in.
 *   420 ms  Tagline fades in (slight delay so reading flow is staggered).
 *   1200 ms onFinished() fires — caller swaps us out for the app.
 */
@Composable
fun BrandSplash(onFinished: () -> Unit) {
    var iconVisible by remember { mutableStateOf(false) }
    var titleVisible by remember { mutableStateOf(false) }
    var taglineVisible by remember { mutableStateOf(false) }

    // Force light status-bar icons against the dark forest-green background.
    // Restores the previous value on exit so the next screen (HomeScreen) can
    // set its own preference without fighting our cleanup.
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as android.app.Activity).window
        val controller = WindowCompat.getInsetsController(window, view)
        val previous = controller.isAppearanceLightStatusBars
        controller.isAppearanceLightStatusBars = false
        onDispose { controller.isAppearanceLightStatusBars = previous }
    }

    LaunchedEffect(Unit) {
        delay(80);  iconVisible = true
        delay(160); titleVisible = true
        delay(180); taglineVisible = true
        delay(780); onFinished()
    }

    val iconScale by animateFloatAsState(
        targetValue = if (iconVisible) 1f else 0.6f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = 280f
        ),
        label = "icon_scale"
    )
    val iconAlpha by animateFloatAsState(
        targetValue = if (iconVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "icon_alpha"
    )
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(ForestGreen, Color(0xFF1F4040))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            // Plate emoji in a cream-tinted circle — matches the launcher icon's vibe
            Surface(
                modifier = Modifier
                    .size(108.dp)
                    .scale(iconScale)
                    .alpha(iconAlpha)
                    .clip(CircleShape),
                color = Color.White.copy(alpha = 0.14f),
                shape = CircleShape
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "🍽️", fontSize = 54.sp)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "MealTime",
                fontSize = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFFAF7F5),  // cream / linen
                letterSpacing = 0.5.sp,
                modifier = Modifier
                    .alpha(titleAlpha)
                    .graphicsLayer { translationY = titleTranslateY }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Cook with joy",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFFAF7F5).copy(alpha = 0.78f),
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(taglineAlpha)
            )
        }
    }
}
