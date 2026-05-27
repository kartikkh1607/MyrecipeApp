package com.kartik.mealtime.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kartik.mealtime.data.billing.BillingManager
import com.kartik.mealtime.domain.model.PlannedMeal
import com.kartik.mealtime.ui.components.GeneratedRecipeSheet
import com.kartik.mealtime.ui.components.UpsellBottomSheet
import com.kartik.mealtime.ui.components.findActivity
import com.kartik.mealtime.ui.theme.Amber
import com.kartik.mealtime.ui.viewmodel.AiViewModel
import com.kartik.mealtime.ui.viewmodel.BillingViewModel
import com.kartik.mealtime.ui.viewmodel.MealPlannerViewModel
import com.kartik.mealtime.ui.viewmodel.ShoppingListViewModel

/**
 * Premium AI meal planner: pick a number of days, generate a Breakfast/Lunch/Dinner
 * plan, then add the whole thing to the shopping list or tap a meal to review/save it.
 */
@Composable
fun MealPlannerScreen(
    viewModel: MealPlannerViewModel,
    shoppingListViewModel: ShoppingListViewModel,
    onBack: () -> Unit,
    onOpenRecipe: (String) -> Unit,
    billingViewModel: BillingViewModel = hiltViewModel(),
) {
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val planState by viewModel.planState
    val selectedDays by viewModel.selectedDays
    val mealSheet by viewModel.mealSheet
    val productDetails by billingViewModel.productDetails.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    var showUpsell by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.upsellEvents.collect { showUpsell = true }
    }
    LaunchedEffect(showUpsell) {
        if (!showUpsell) return@LaunchedEffect
        billingViewModel.purchaseEvents.collect { result ->
            when (result) {
                BillingManager.PurchaseResult.Success -> {
                    showUpsell = false
                    Toast.makeText(context, "You're Premium now — enjoy!", Toast.LENGTH_SHORT).show()
                }
                BillingManager.PurchaseResult.Cancelled -> Unit
                is BillingManager.PurchaseResult.Error ->
                    Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun attemptGenerate() {
        if (isPremium) viewModel.generate() else showUpsell = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        // ── Header ─────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(Modifier.width(4.dp))
            Text(
                "Meal Planner",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }

        when (val state = planState) {
            is MealPlannerViewModel.MealPlanState.Loading -> PlannerLoading()

            is MealPlannerViewModel.MealPlanState.Error -> PlannerError(
                message = state.message,
                onRetry = { attemptGenerate() },
            )

            is MealPlannerViewModel.MealPlanState.Ready -> PlanReady(
                plan = state.plan,
                onAddAll = {
                    var count = 0
                    state.plan.days.forEach { day ->
                        day.meals.forEach { meal ->
                            shoppingListViewModel.addToShoppingList(meal.recipe)
                            count++
                        }
                    }
                    Toast.makeText(
                        context,
                        "Added $count meals to your shopping list",
                        Toast.LENGTH_SHORT,
                    ).show()
                },
                onAddMeal = { meal ->
                    shoppingListViewModel.addToShoppingList(meal.recipe)
                    Toast.makeText(context, "Added to shopping list", Toast.LENGTH_SHORT).show()
                },
                onOpenMeal = { meal -> viewModel.openMeal(meal.recipe) },
                onNewPlan = { viewModel.reset() },
            )

            is MealPlannerViewModel.MealPlanState.Idle -> PlannerConfig(
                isPremium = isPremium,
                selectedDays = selectedDays,
                onSelectDays = { viewModel.setDays(it) },
                onGenerate = { attemptGenerate() },
            )
        }
    }

    // ── Per-meal review sheet (reuses the generated-recipe sheet) ───────────────
    if (mealSheet !is AiViewModel.RecipeGenState.Idle) {
        GeneratedRecipeSheet(
            state = mealSheet,
            onSave = { viewModel.saveMeal() },
            onOpen = { recipe ->
                viewModel.dismissMeal()
                onOpenRecipe(recipe.id)
            },
            onDismiss = { viewModel.dismissMeal() },
        )
    }

    // ── Premium upsell ──────────────────────────────────────────────────────────
    if (showUpsell) {
        UpsellBottomSheet(
            title = "AI Meal Planner",
            description = "Generate a full week of meals tailored to your taste, then shop in one tap.",
            perks = listOf(
                "Breakfast, lunch & dinner for every day",
                "Add the whole plan to your shopping list",
                "Tailored to your diet & preferences",
            ),
            productDetails = productDetails,
            onSelectPlan = { offerToken ->
                activity?.let { billingViewModel.purchase(it, offerToken) }
            },
            onDismiss = { showUpsell = false },
        )
    }
}

@Composable
private fun PlannerConfig(
    isPremium: Boolean,
    selectedDays: Int,
    onSelectDays: (Int) -> Unit,
    onGenerate: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(8.dp))
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Amber.copy(alpha = 0.18f),
            modifier = Modifier.size(88.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = Amber,
                    modifier = Modifier.size(42.dp),
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "Plan your meals",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Let AI map out balanced breakfasts, lunches, and dinners for you.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(28.dp))
        Text(
            "How many days?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(3, 5, 7).forEach { days ->
                DayChip(
                    days = days,
                    selected = days == selectedDays,
                    onClick = { onSelectDays(days) },
                )
            }
        }

        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onGenerate,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = Color.Black),
        ) {
            Text("Generate plan", fontWeight = FontWeight.SemiBold)
            if (!isPremium) {
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.Lock, contentDescription = "Premium", modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun DayChip(days: Int, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) Amber else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.size(width = 72.dp, height = 64.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "$days",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (selected) Color.Black else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "days",
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PlannerLoading() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = Amber)
        Spacer(Modifier.height(16.dp))
        Text("Planning your meals…", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun PlannerError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("⚠️", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onRetry) { Text("Try again") }
    }
}

@Composable
private fun PlanReady(
    plan: com.kartik.mealtime.domain.model.MealPlan,
    onAddAll: () -> Unit,
    onAddMeal: (PlannedMeal) -> Unit,
    onOpenMeal: (PlannedMeal) -> Unit,
    onNewPlan: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Button(
                onClick = onAddAll,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = Color.Black),
            ) {
                Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add all to list", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = onNewPlan, shape = RoundedCornerShape(14.dp)) {
                Text("New plan")
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            plan.days.forEach { day ->
                item(key = "day-${day.dayNumber}") {
                    Text(
                        "Day ${day.dayNumber}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                    )
                }
                items(day.meals, key = { "${day.dayNumber}-${it.mealType}-${it.recipe.id}" }) { meal ->
                    MealCard(
                        meal = meal,
                        onClick = { onOpenMeal(meal) },
                        onAdd = { onAddMeal(meal) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MealCard(
    meal: PlannedMeal,
    onClick: () -> Unit,
    onAdd: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Amber.copy(alpha = 0.18f),
                ) {
                    Text(
                        meal.mealType,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Amber,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    meal.recipe.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val meta = buildList {
                    val total = meal.recipe.prepTime + meal.recipe.cookTime
                    if (total > 0) add("$total min")
                    meal.recipe.calories?.let { add("$it kcal") }
                }
                if (meta.isNotEmpty()) {
                    Text(
                        meta.joinToString("  •  "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Amber.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                IconButton(onClick = onAdd) {
                    Icon(
                        Icons.Default.AddShoppingCart,
                        contentDescription = "Add to shopping list",
                        tint = Amber,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}
