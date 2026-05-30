package com.kartik.mealtime.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kartik.mealtime.domain.model.Recipe
import com.kartik.mealtime.ui.theme.Amber
import com.kartik.mealtime.ui.viewmodel.AiViewModel.RecipeGenState

/**
 * Bottom sheet that reviews an AI-generated recipe before the user keeps it.
 *
 * Driven entirely by [state]: shows a spinner while generating, an error with a
 * retry/close, or the full recipe with Save / Open actions. Shown only when the
 * caller decides [state] is not Idle.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratedRecipeSheet(
    state: RecipeGenState,
    onSave: () -> Unit,
    onOpen: (Recipe) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        when (state) {
            is RecipeGenState.Loading -> LoadingBody()
            is RecipeGenState.Error -> ErrorBody(state.message, onDismiss)
            is RecipeGenState.Ready -> ReadyBody(state, onSave, onOpen, onDismiss)
            RecipeGenState.Idle -> Unit
        }
    }
}

@Composable
private fun LoadingBody() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = Amber)
        Spacer(Modifier.height(16.dp))
        Text("Crafting your recipe…", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ErrorBody(message: String, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("⚠️", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Close") }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ReadyBody(
    state: RecipeGenState.Ready,
    onSave: () -> Unit,
    onOpen: (Recipe) -> Unit,
    onDismiss: () -> Unit,
) {
    val recipe = state.recipe
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState())
            .heightIn(max = 560.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Amber,
                )
            }
            Spacer(Modifier.size(8.dp))
            Text(
                "AI-generated recipe",
                style = MaterialTheme.typography.labelLarge,
                color = Amber,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            recipe.name,
            style = MaterialTheme.typography.headlineSmall,
        )
        if (recipe.description.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                recipe.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(12.dp))
        MetaRow(recipe)

        if (recipe.ingredients.isNotEmpty()) {
            SectionTitle("Ingredients")
            recipe.ingredients.forEach { ing ->
                val qty = listOf(ing.amount, ing.unit).filter { it.isNotBlank() }.joinToString(" ")
                Text(
                    text = if (qty.isBlank()) "• ${ing.name}" else "• $qty ${ing.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
        }

        if (recipe.instructions.isNotEmpty()) {
            SectionTitle("Steps")
            recipe.instructions.forEach { step ->
                Text(
                    text = "${step.stepNumber}. ${step.instruction}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 3.dp),
                )
                step.tips?.takeIf { it.isNotBlank() }?.let { tip ->
                    Text(
                        text = "💡 $tip",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, bottom = 2.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        if (state.saved) {
            Button(
                onClick = { onOpen(recipe) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = Color.Black),
            ) {
                Text("Open recipe", fontWeight = FontWeight.SemiBold)
            }
        } else {
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = Color.Black),
            ) {
                Text("Save to AI Creations", fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.saved) "Done" else "Discard")
        }
    }
}

@Composable
private fun MetaRow(recipe: Recipe) {
    val parts = buildList {
        if (recipe.prepTime > 0) add("Prep ${recipe.prepTime}m")
        if (recipe.cookTime > 0) add("Cook ${recipe.cookTime}m")
        if (recipe.servings > 0) add("Serves ${recipe.servings}")
        recipe.calories?.let { add("$it kcal") }
    }
    if (parts.isNotEmpty()) {
        Text(
            parts.joinToString("  •  "),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Spacer(Modifier.height(16.dp))
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(4.dp))
}
