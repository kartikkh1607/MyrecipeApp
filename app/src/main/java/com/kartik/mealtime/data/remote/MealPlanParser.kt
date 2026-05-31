package com.kartik.mealtime.data.remote

import com.kartik.mealtime.data.remote.dto.MealPlanDto
import com.kartik.mealtime.data.remote.dto.toMealPlanOrNull
import com.kartik.mealtime.domain.model.MealPlan
import kotlinx.serialization.json.Json

/**
 * Turns a model's raw text response into a domain [MealPlan]. Mirrors
 * [AiRecipeParser]: tolerate ```json fences / stray prose by extracting the outer
 * `{...}`, then map; any failure becomes a clean, user-actionable error.
 */
internal object MealPlanParser {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    fun parse(raw: String): Result<MealPlan> {
        val payload = extractJsonObject(raw)
            ?: return Result.failure(Exception("The AI didn't return a meal plan. Please try again."))

        val dto = runCatching {
            json.decodeFromString(MealPlanDto.serializer(), payload)
        }.getOrNull()
            ?: return Result.failure(Exception("Couldn't read the AI's meal plan. Please try again."))

        val plan = dto.toMealPlanOrNull()
            ?: return Result.failure(Exception("The AI's meal plan came back incomplete. Try again, or pick fewer days."))

        return Result.success(plan)
    }

    private fun extractJsonObject(raw: String): String? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start == -1 || end == -1 || end <= start) return null
        return raw.substring(start, end + 1)
    }
}
