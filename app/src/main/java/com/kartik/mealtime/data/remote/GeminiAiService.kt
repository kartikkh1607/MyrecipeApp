package com.kartik.mealtime.data.remote

import com.kartik.mealtime.BuildConfig
import com.kartik.mealtime.domain.model.Recipe
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Named

/**
 * Gemini AI API client for recipe chat and recommendations.
 * Uses the Gemini 2.0 Flash model via the REST API.
 */
class GeminiAiService @Inject constructor(
    @Named("ai") private val client: OkHttpClient
) : AiService {

    private val gson = Gson()

    companion object {
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"
        private const val SAFETY_BLOCKED = " SAFETY_BLOCKED"
    }

    // Maps Gemini HTTP errors to messages users can act on, instead of "API error: 429".
    private fun friendlyError(code: Int, body: String?): Exception {
        // Gemini error JSON shape: { "error": { "code": ..., "message": "...", "status": "..." } }
        val geminiMsg = runCatching {
            gson.fromJson(body, GeminiErrorEnvelope::class.java)?.error?.message
        }.getOrNull()
        val suffix = if (!geminiMsg.isNullOrBlank()) " ($geminiMsg)" else ""
        return Exception(
            when (code) {
                400 -> "Couldn't reach the AI assistant (bad request). The API key may be invalid — check local.properties.$suffix"
                401, 403 -> "AI assistant is unavailable: the API key is missing, restricted, or the Generative Language API isn't enabled.$suffix"
                404 -> "AI model not found. The 'gemini-2.0-flash' model may have changed — try updating the app.$suffix"
                429 -> "AI assistant quota exceeded. Free tier allows 15 req/min and 1500 req/day per project. Check Google Cloud quotas or enable billing.$suffix"
                in 500..599 -> "Gemini is having trouble right now. Please try again in a moment.$suffix"
                else -> "AI assistant error ($code).$suffix"
            }
        )
    }

    /**
     * Send a chat message to Gemini and get a text response.
     *
     * [dietaryPreferences] — comma-separated list (e.g. "Vegetarian, Gluten-Free")
     * read from the user's profile. Prepended to the system prompt so Gemini
     * filters suggestions to match the user's diet.
     */
    override suspend fun sendChatMessage(
        message: String,
        conversationHistory: List<ChatMessage>,
        dietaryPreferences: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val prompt = AiPrompts.buildChatPrompt(conversationHistory, message, dietaryPreferences)
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
                return@withContext Result.failure(friendlyError(response.code, responseBody))
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
    override suspend fun getRecipeRecommendations(
        favoriteRecipes: List<String>,  // List of recipe names user has liked
        dietaryPreferences: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val prompt = AiPrompts.buildRecommendationPrompt(favoriteRecipes, dietaryPreferences)
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
                return@withContext Result.failure(friendlyError(response.code, responseBody))
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

    override suspend fun generateRecipe(
        query: String,
        dietaryPreferences: String
    ): Result<Recipe> = withContext(Dispatchers.IO) {
        val prompt = AiPrompts.buildGenerateRecipePrompt(query, dietaryPreferences)
        // temperature 0.6 — creative enough to vary recipes, low enough for valid JSON.
        postForJson(prompt, temperature = 0.6f, maxTokens = 2048)
            .fold(
                onSuccess = { AiRecipeParser.parse(it) },
                onFailure = { Result.failure(it) }
            )
    }

    /**
     * Posts [prompt] with JSON response mode and returns the raw model text.
     * Shared by the structured-output features (recipe generation, transform, etc.).
     */
    private fun postForJson(
        prompt: String,
        temperature: Float,
        maxTokens: Int
    ): Result<String> {
        return try {
            val requestBody = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                generationConfig = GenerationConfig(
                    temperature = temperature,
                    maxOutputTokens = maxTokens,
                    responseMimeType = "application/json"
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
            val responseBody = response.body?.string()
                ?: return Result.failure(Exception("Empty response from Gemini API"))

            if (!response.isSuccessful) {
                return Result.failure(friendlyError(response.code, responseBody))
            }

            val parsed = gson.fromJson(responseBody, GenerateContentResponse::class.java)
            val text = parsed.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text
                ?.trim()
                ?: return Result.failure(Exception("No content in response"))

            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
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
    val maxOutputTokens: Int? = null,
    // Set to "application/json" to make Gemini return parseable JSON (structured output).
    @SerializedName("responseMimeType")
    val responseMimeType: String? = null
)

data class GenerateContentResponse(
    val candidates: List<Candidate>?
)

data class Candidate(
    val content: Content?,
    val finishReason: String?
)

// Gemini error response shape — used to surface the underlying reason in the chat.
data class GeminiErrorEnvelope(val error: GeminiErrorBody?)
data class GeminiErrorBody(val code: Int?, val message: String?, val status: String?)