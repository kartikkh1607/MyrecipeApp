package com.kartik.mealtime.data.remote

import com.kartik.mealtime.domain.model.Recipe
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Composes the AI providers: Gemini is primary (generous free tier), Groq is the
 * fallback. On a Gemini failure, if a Groq key is configured we retry the same
 * request against Groq. If both fail, the original Gemini error is surfaced — its
 * messages are the most actionable for diagnosing the primary outage.
 *
 * This is what [com.kartik.mealtime.ui.viewmodel.AiViewModel] injects.
 */
@Singleton
class AiServiceRouter @Inject constructor(
    private val gemini: GeminiAiService,
    private val groq: GroqAiService
) : AiService {

    override suspend fun sendChatMessage(
        message: String,
        conversationHistory: List<ChatMessage>,
        dietaryPreferences: String
    ): Result<String> {
        val primary = gemini.sendChatMessage(message, conversationHistory, dietaryPreferences)
        if (primary.isSuccess || !groq.isConfigured) return primary

        val fallback = groq.sendChatMessage(message, conversationHistory, dietaryPreferences)
        return if (fallback.isSuccess) fallback else primary
    }

    override suspend fun getRecipeRecommendations(
        favoriteRecipes: List<String>,
        dietaryPreferences: String
    ): Result<String> {
        val primary = gemini.getRecipeRecommendations(favoriteRecipes, dietaryPreferences)
        if (primary.isSuccess || !groq.isConfigured) return primary

        val fallback = groq.getRecipeRecommendations(favoriteRecipes, dietaryPreferences)
        return if (fallback.isSuccess) fallback else primary
    }

    override suspend fun generateRecipe(
        query: String,
        dietaryPreferences: String
    ): Result<Recipe> {
        val primary = gemini.generateRecipe(query, dietaryPreferences)
        // A premium-gate rejection must NOT fall back to Groq: the Groq route isn't gated
        // server-side, so falling back would be a paywall bypass. Surface it for the upsell.
        if (primary.isSuccess ||
            primary.exceptionOrNull() is PremiumRequiredException ||
            !groq.isConfigured
        ) {
            return primary
        }

        val fallback = groq.generateRecipe(query, dietaryPreferences)
        return if (fallback.isSuccess) fallback else primary
    }
}
