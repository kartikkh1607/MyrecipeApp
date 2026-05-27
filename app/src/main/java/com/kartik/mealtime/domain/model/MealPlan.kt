package com.kartik.mealtime.domain.model

import androidx.compose.runtime.Immutable

/**
 * An AI-generated multi-day meal plan. In-memory only for v1 — produced by
 * `AiService.generateMealPlan`, held in the planner ViewModel, and never persisted
 * as a whole. Individual [PlannedMeal] recipes can be saved to AI Creations or added
 * to the shopping list.
 */
@Immutable
data class MealPlan(
    val title: String,
    val days: List<MealPlanDay>,
)

@Immutable
data class MealPlanDay(
    val dayNumber: Int,
    val meals: List<PlannedMeal>,
)

@Immutable
data class PlannedMeal(
    /** "Breakfast" | "Lunch" | "Dinner" (or whatever the model labelled it). */
    val mealType: String,
    /** A full [Recipe] with an `ai-` id, so it opens, saves, and shops like any other. */
    val recipe: Recipe,
)
