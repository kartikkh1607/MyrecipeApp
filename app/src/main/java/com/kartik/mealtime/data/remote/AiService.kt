package com.kartik.mealtime.data.remote

import com.kartik.mealtime.domain.model.MealPlan
import com.kartik.mealtime.domain.model.Recipe

/**
 * Provider-neutral contract for the AI recipe assistant.
 *
 * Implemented by [GeminiAiService] (primary, free tier) and [GroqAiService]
 * (Groq fallback). [AiServiceRouter] composes both and is what the app injects.
 */
interface AiService {

    suspend fun sendChatMessage(
        message: String,
        conversationHistory: List<ChatMessage> = emptyList(),
        dietaryPreferences: String = ""
    ): Result<String>

    suspend fun getRecipeRecommendations(
        favoriteRecipes: List<String>,
        dietaryPreferences: String = ""
    ): Result<String>

    /**
     * Generates a complete, structured [Recipe] from a free-text [query]
     * (e.g. "a spicy paneer curry using yogurt"). Returns a domain recipe with an
     * `ai-` id ready to persist; failures carry a user-facing message.
     */
    suspend fun generateRecipe(
        query: String,
        dietaryPreferences: String = ""
    ): Result<Recipe>

    /**
     * Transforms an existing [base] recipe per a free-text [instruction] (e.g. "make it
     * vegan", "double the servings", "make it spicier") and returns a new structured
     * [Recipe] with a fresh `ai-` id — the original is never mutated. Like
     * [generateRecipe] this is a premium, structured-output call.
     */
    suspend fun transformRecipe(
        base: Recipe,
        instruction: String,
        dietaryPreferences: String = ""
    ): Result<Recipe>

    /**
     * Builds a [days]-day [MealPlan] (Breakfast/Lunch/Dinner each day). Like
     * [generateRecipe] this is a premium, structured-output call; each meal is a full
     * recipe with an `ai-` id ready to open, save, or add to the shopping list.
     */
    suspend fun generateMealPlan(
        days: Int,
        dietaryPreferences: String = "",
        favoriteRecipes: List<String> = emptyList()
    ): Result<MealPlan>
}

/** A single turn in the chat history, shared across providers. */
data class ChatMessage(
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Thrown when the AI proxy rejects a premium-only request (HTTP 402, server-side
 * entitlement gate). The UI maps this to the upsell paywall rather than a generic error —
 * it means the user isn't currently entitled (e.g. a stale local mirror), not that the
 * request itself failed.
 */
class PremiumRequiredException : Exception("Premium subscription required")

/**
 * Thrown when the AI proxy rejects a request because the user already hit their daily
 * Gemini cap (HTTP 429 with `user_quota_exceeded`). Distinct from a plain upstream 429
 * (Gemini's own quota) so the UI can show a clear "you've used your daily AI limit"
 * message instead of a generic outage error, and so the router never silently falls
 * back to Groq (that would defeat the cap).
 *
 * [tier] is the server-reported tier the cap was checked against ("free" or "premium",
 * null if absent). Free-tier callers route to the upsell sheet — subscribing lifts the
 * cap immediately — while premium-tier callers see the message verbatim (the action
 * is to wait for UTC midnight, not to subscribe again).
 */
class QuotaExceededException(message: String, val tier: String? = null) : Exception(message)
