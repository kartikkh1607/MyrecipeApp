package com.kartik.mealtime.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kartik.mealtime.ui.theme.Amber

/**
 * Generic paywall prompt shown when a user without premium taps a gated feature.
 *
 * Behaviour is intentionally decoupled from billing: [onUnlock] is whatever the
 * caller wires. Until Play Billing exists, callers point it at a local/dev unlock;
 * once billing lands, they point it at the real purchase flow — this component
 * does not change.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpsellBottomSheet(
    title: String,
    description: String,
    perks: List<String>,
    onUnlock: () -> Unit,
    onDismiss: () -> Unit,
    ctaLabel: String = "Unlock Premium",
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Amber,
                modifier = Modifier.size(40.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = description,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))

            perks.forEach { perk ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Amber,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.size(12.dp))
                    Text(
                        text = perk,
                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onUnlock,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Amber,
                    contentColor = Color.Black,
                ),
            ) {
                Text(ctaLabel, fontWeight = FontWeight.SemiBold)
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Maybe later")
            }
        }
    }
}
