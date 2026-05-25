package com.kartik.mealtime.data.remote

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
}

/** A single turn in the chat history, shared across providers. */
data class ChatMessage(
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
