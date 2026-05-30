package com.kartik.mealtime.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kartik.mealtime.ui.theme.ForestGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrandedSnackbarHost(hostState: SnackbarHostState) {
    SnackbarHost(
        hostState = hostState,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    ) { data ->
        val shape = RoundedCornerShape(18.dp)
        val gradient = Brush.horizontalGradient(
            colors = listOf(Color(0xFF201E18), Color(0xFF15140F))
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 16.dp, shape = shape, clip = false)
                .clip(shape)
                .background(gradient)
        ) {
            // Left accent strip
            Box(
                modifier = Modifier
                    .padding(start = 0.dp)
                    .size(width = 4.dp, height = 64.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(com.kartik.mealtime.ui.theme.Heart, com.kartik.mealtime.ui.theme.Heart.copy(alpha = 0.3f))
                        )
                    )
                    .align(Alignment.CenterStart)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Heart icon in a soft pink circle
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(com.kartik.mealtime.ui.theme.Heart.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = com.kartik.mealtime.ui.theme.Heart,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Message
                Text(
                    text = data.visuals.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.92f),
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Undo pill
                if (data.visuals.actionLabel != null) {
                    Surface(
                        onClick = { data.performAction() },
                        shape = RoundedCornerShape(20.dp),
                        color = ForestGreen
                    ) {
                        Text(
                            text = "↩ ${data.visuals.actionLabel}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
