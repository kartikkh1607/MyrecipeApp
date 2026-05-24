package com.kartik.mealtime.data.remote

import com.kartik.mealtime.BuildConfig
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Gemini AI API client for recipe chat and recommendations.
 * Uses the Gemini 2.0 Flash model via the REST API.
 */
class GeminiAiService @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    companion object {
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"
        private const val SAFETY_BLOCKED = " SAFETY_BLOCKED"
    }

    /**
     * Send a chat message to Gemini and get a text response.
     *
     * [dietaryPreferences] — comma-separated list (e.g. "Vegetarian, Gluten-Free")
     * read from the user's profile. Prepended to the system prompt so Gemini
     * filters suggestions to match the user's diet.
     */
    suspend fun sendChatMessage(
        message: String,
        conversationHistory: List<ChatMessage> = emptyList(),
        dietaryPreferences: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val prompt = buildPrompt(conversationHistory, message, dietaryPreferences)
            val requestBody = GenerateContentRequest(
                contents = listOf(
                    Content(
                        parts = listOf(Part(text = prompt))
                    )
                ),
                generationConfig = GenerationConfig(
                    temperature = 0.85f,
                    maxOutputTokens = 2048
                )
            )

            val url = "$BASE_URL?key=${BuildConfig.GEMINI_API_KEY}"
            val mediaType = "application/json".toMediaType()
            val body = gson.toJson(requestBody).toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return@withContext Result.failure(
                Exception("Empty response from Gemini API")
            )

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("API error: ${response.code} - ${response.message}")
                )
            }

            val parseResponse = gson.fromJson(responseBody, GenerateContentResponse::class.java)

            val text = parseResponse.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text
                ?.trim()
                ?: return@withContext Result.failure(Exception("No content in response"))

            // Check if response was blocked
            if (text.endsWith(SAFETY_BLOCKED)) {
                return@withContext Result.failure(
                    Exception("Response blocked by safety filters. Please rephrase your question.")
                )
            }

            Result.success(text)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generate smart recipe recommendations based on user preferences.
     */
    suspend fun getRecipeRecommendations(
        favoriteRecipes: List<String>,  // List of recipe names user has liked
        dietaryPreferences: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val prompt = buildRecommendationPrompt(favoriteRecipes, dietaryPreferences)
            val requestBody = GenerateContentRequest(
                contents = listOf(
                    Content(
                        parts = listOf(Part(text = prompt))
                    )
                ),
                generationConfig = GenerationConfig(
                    temperature = 0.7f,
                    maxOutputTokens = 1024
                )
            )

            val url = "$BASE_URL?key=${BuildConfig.GEMINI_API_KEY}"
            val mediaType = "application/json".toMediaType()
            val body = gson.toJson(requestBody).toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return@withContext Result.failure(
                Exception("Empty response")
            )

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("API error: ${response.code}"))
            }

            val parseResponse = gson.fromJson(responseBody, GenerateContentResponse::class.java)
            val text = parseResponse.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text
                ?.trim()
                ?: return@withContext Result.failure(Exception("No content in response"))

            Result.success(text)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildPrompt(
        history: List<ChatMessage>,
        newMessage: String,
        dietaryPreferences: String = ""
    ): String {
        val sb = StringBuilder()
        sb.appendLine("You are a friendly and knowledgeable recipe assistant. You help users:")
        sb.appendLine("- Find recipes based on ingredients they have")
        sb.appendLine("- Suggest recipe variations and substitutions")
        sb.appendLine("- Answer cooking questions and provide tips")
        sb.appendLine("- Suggest recipes based on preferences and dietary needs")
        sb.appendLine()
        // Per-user dietary context — only emitted when the user has set preferences.
        if (dietaryPreferences.isNotBlank()) {
            sb.appendLine("IMPORTANT — User's dietary preferences: $dietaryPreferences.")
            sb.appendLine("ONLY suggest recipes that match these preferences. If asked about")
            sb.appendLine("recipes that conflict, gently note the conflict and offer alternatives.")
            sb.appendLine()
        }
        sb.appendLine("Keep responses conversational, helpful, and concise.")
        sb.appendLine("When suggesting recipes, mention the recipe name in **bold**.")
        sb.appendLine()
        sb.appendLine("Conversation:")
        history.takeLast(6).forEach { msg ->
            sb.appendLine("${if (msg.isUser) "User" else "Assistant"}: ${msg.content}")
        }
        sb.appendLine()
        sb.appendLine("User: $newMessage")
        sb.appendLine("Assistant:")
        return sb.toString()
    }

    private fun buildRecommendationPrompt(
        favoriteRecipes: List<String>,
        dietaryPreferences: String
    ): String {
        val favList = favoriteRecipes.take(10).joinToString(", ")
        return """
            |You are a recipe recommendation expert. Based on the user's taste preferences,
            |suggest 3-5 personalized recipes they might enjoy.
            |
            |User's favorite recipes: $favList
            |${if (dietaryPreferences.isNotBlank()) "Dietary preferences: $dietaryPreferences" else ""}
            |
            |Format your response like this:
            |• **Recipe Name** - Brief description of why they'd like it
            |
            |Keep suggestions varied and interesting. Do not repeat the same cuisine or style.
        """.trimMargin()
    }
}

// ── Data classes for Gemini API ────────────────────────────────────────────────

data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null
)

data class Content(
    val parts: List<Part>
)

data class Part(
    val text: String
)

data class GenerationConfig(
    val temperature: Float? = null,
    @SerializedName("maxOutputTokens")
    val maxOutputTokens: Int? = null
)

data class GenerateContentResponse(
    val candidates: List<Candidate>?
)

data class Candidate(
    val content: Content?,
    val finishReason: String?
)

data class ChatMessage(
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)