package com.kartik.mealtime.data.remote.dto

import com.kartik.mealtime.domain.model.MealPlan
import com.kartik.mealtime.domain.model.MealPlanDay
import com.kartik.mealtime.domain.model.PlannedMeal
import kotlinx.serialization.Serializable

/**
 * Wire shape the AI is instructed to emit for a meal plan. Each meal embeds a full
 * [AiRecipeDto] so [toMealPlanOrNull] can reuse [toRecipeOrNull] verbatim — the plan
 * is just a grouping of recipes by day and meal type.
 *
 * All fields nullable: a language model can't be trusted to fill them, so the mapper
 * drops incomplete meals/days and rejects an empty plan rather than crash.
 */
@Serializable
data class MealPlanDto(
    val title: String? = null,
    val days: List<MealPlanDayDto>? = null,
)

@Serializable
data class MealPlanDayDto(
    val day: Int? = null,
    val meals: List<MealEntryDto>? = null,
)

@Serializable
data class MealEntryDto(
    val mealType: String? = null,
    val recipe: AiRecipeDto? = null,
)

fun MealPlanDto.toMealPlanOrNull(): MealPlan? {
    val mappedDays = days.orEmpty().mapIndexedNotNull { dayIndex, dayDto ->
        val meals = dayDto.meals.orEmpty().mapNotNull { entry ->
            val recipe = entry.recipe?.toRecipeOrNull() ?: return@mapNotNull null
            PlannedMeal(
                mealType = entry.mealType?.trim().orEmpty().ifBlank { "Meal" },
                recipe = recipe,
            )
        }
        if (meals.isEmpty()) null
        else MealPlanDay(
            dayNumber = dayDto.day?.takeIf { it > 0 } ?: (dayIndex + 1),
            meals = meals,
        )
    }
    if (mappedDays.isEmpty()) return null
    return MealPlan(
        title = title?.trim().orEmpty().ifBlank { "Your Meal Plan" },
        days = mappedDays,
    )
}
