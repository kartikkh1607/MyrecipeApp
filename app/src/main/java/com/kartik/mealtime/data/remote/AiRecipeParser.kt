package com.kartik.mealtime.data.remote

import com.kartik.mealtime.data.remote.dto.AiRecipeDto
import com.kartik.mealtime.data.remote.dto.toRecipeOrNull
import com.kartik.mealtime.domain.model.Recipe
import kotlinx.serialization.json.Json

/**
 * Turns a model's raw text response into a domain [Recipe].
 *
 * Even with JSON output modes enabled, models occasionally wrap JSON in ```json
 * fences or add stray prose, so we extract the outermost `{...}` object before
 * parsing and treat any failure as a clean, user-actionable error rather than a
 * crash.
 *
 * Uses a private lenient [Json] (NOT the strict app-wide instance) so unknown
 * keys + trailing junk from the model don't blow up the parse.
 */
internal object AiRecipeParser {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    fun parse(raw: String, idPrefix: String = "ai-"): Result<Recipe> {
        val payload = extractJsonObject(raw)
            ?: return Result.failure(Exception("The AI didn't return a recipe. Please try again."))

        val dto = runCatching {
            json.decodeFromString(AiRecipeDto.serializer(), payload)
        }.getOrNull()
            ?: return Result.failure(Exception("Couldn't read the AI's recipe. Please try again."))

        val recipe = dto.toRecipeOrNull(idPrefix)
            ?: return Result.failure(Exception("The AI's recipe was incomplete. Try rephrasing your request."))

        return Result.success(recipe)
    }

    /** Returns the substring from the first '{' to the last '}', or null if absent. */
    private fun extractJsonObject(raw: String): String? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start == -1 || end == -1 || end <= start) return null
        return raw.substring(start, end + 1)
    }
}
