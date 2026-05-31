package com.kartik.mealtime.data.remote

import com.kartik.mealtime.domain.model.MealPlan
import com.kartik.mealtime.domain.model.Recipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import javax.inject.Inject

/**
 * Gemini AI API client for recipe chat and recommendations.
 *
 * Wraps [AiApi.generateContent] with prompt building, JSON-mode toggling, and
 * Gemini-specific error mapping. The transport, JSON (de)serialisation, and
 * Authorization header are all delegated — see [com.kartik.mealtime.di.NetworkModule]
 * for the Retrofit + kotlinx.serialization wiring.
 */
class GeminiAiService @Inject constructor(
    private val api: AiApi,
    private val json: Json,
) : AiService {

    private companion object {
        private const val SAFETY_BLOCKED = " SAFETY_BLOCKED"
    }

    // Maps Gemini HTTP errors to messages users can act on, instead of "API error: 429".
    private fun friendlyError(code: Int, body: String?): Exception {
        // The proxy's per-user daily cap also returns 429, but with body
        // {"error":"user_quota_exceeded","limit":N,"tier":"free|premium"}. Catch it
        // first so the user sees "you've hit your daily AI limit" instead of the
        // upstream "Gemini quota exceeded" message.
        if (code == 429) {
            val proxy = runCatching {
                body?.let { json.decodeFromString(ProxyErrorEnvelope.serializer(), it) }
            }.getOrNull()
            if (proxy?.error == "user_quota_exceeded") {
                val limit = proxy.limit
                val isPremium = proxy.tier == "premium"
                val message = when {
                    limit != null && isPremium ->
                        "You've used your daily AI limit ($limit recipes/chats). It resets at midnight UTC."
                    limit != null ->
                        "You've used your free daily AI limit ($limit chats). Upgrade to premium or try again tomorrow."
                    isPremium ->
                        "You've used your daily AI limit. It resets at midnight UTC."
                    else ->
                        "You've used your free daily AI limit. Upgrade to premium or try again tomorrow."
                }
                return QuotaExceededException(message, tier = proxy.tier)
            }
        }
        // Gemini error JSON shape: { "error": { "code": ..., "message": "...", "status": "..." } }
        val geminiMsg = runCatching {
            body?.let { json.decodeFromString(GeminiErrorEnvelope.serializer(), it).error?.message }
        }.getOrNull()
        val suffix = if (!geminiMsg.isNullOrBlank()) " ($geminiMsg)" else ""
        return Exception(
            when (code) {
                400 -> "Couldn't reach the AI assistant (bad request). Please try again.$suffix"
                401 -> "Your session expired. Please sign out and back in, then try again.$suffix"
                403 -> "AI assistant access denied. Please try again later.$suffix"
                404 -> "AI model not found — please update the app.$suffix"
                429 -> "The AI assistant is busy right now. Please try again in a minute.$suffix"
                in 500..599 -> "Gemini is having trouble right now. Please try again in a moment.$suffix"
                else -> "AI assistant error ($code).$suffix"
            }
        )
    }

    /** Maps an HttpException to the right typed exception for the caller. */
    private fun mapHttpException(e: HttpException, mapPremium: Boolean): Throwable {
        if (mapPremium && e.code() == 402) return PremiumRequiredException()
        return friendlyError(e.code(), runCatching { e.response()?.errorBody()?.string() }.getOrNull())
    }

    /** Reads `candidates[0].content.parts[0].text` and validates it. */
    private fun extractText(response: GenerateContentResponse): Result<String> {
        val text = response.candidates
            ?.firstOrNull()
            ?.content
            ?.parts
            ?.firstOrNull()
            ?.text
            ?.trim()
            ?: return Result.failure(Exception("No content in response"))
        if (text.endsWith(SAFETY_BLOCKED)) {
            return Result.failure(
                Exception("Response blocked by safety filters. Please rephrase your question.")
            )
        }
        return Result.success(text)
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
            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                generationConfig = GenerationConfig(
                    temperature = 0.85f,
                    maxOutputTokens = 2048
                )
            )
            extractText(api.generateContent(request))
        } catch (e: HttpException) {
            // Chat is free-tier eligible, so don't map 402 to PremiumRequired here —
            // a 402 would be a server bug for chat and should surface as a clear error.
            Result.failure(mapHttpException(e, mapPremium = false))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generate smart recipe recommendations based on user preferences.
     */
    override suspend fun getRecipeRecommendations(
        favoriteRecipes: List<String>,
        dietaryPreferences: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val prompt = AiPrompts.buildRecommendationPrompt(favoriteRecipes, dietaryPreferences)
            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                generationConfig = GenerationConfig(
                    temperature = 0.7f,
                    maxOutputTokens = 1024
                )
            )
            extractText(api.generateContent(request))
        } catch (e: HttpException) {
            Result.failure(mapHttpException(e, mapPremium = false))
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

    override suspend fun transformRecipe(
        base: Recipe,
        instruction: String,
        dietaryPreferences: String
    ): Result<Recipe> = withContext(Dispatchers.IO) {
        val prompt = AiPrompts.buildTransformPrompt(base, instruction, dietaryPreferences)
        postForJson(prompt, temperature = 0.6f, maxTokens = 2048)
            .fold(
                onSuccess = { AiRecipeParser.parse(it) },
                onFailure = { Result.failure(it) }
            )
    }

    override suspend fun generateMealPlan(
        days: Int,
        dietaryPreferences: String,
        favoriteRecipes: List<String>
    ): Result<MealPlan> = withContext(Dispatchers.IO) {
        val prompt = AiPrompts.buildMealPlanPrompt(days, dietaryPreferences, favoriteRecipes)
        // A multi-meal plan is large — give it the full output budget.
        postForJson(prompt, temperature = 0.6f, maxTokens = 8192)
            .fold(
                onSuccess = { MealPlanParser.parse(it) },
                onFailure = { Result.failure(it) }
            )
    }

    /**
     * Posts [prompt] with JSON response mode and returns the raw model text.
     * Shared by the structured-output features (recipe generation, transform, etc.).
     *
     * Maps 402 to [PremiumRequiredException] so the UI shows the upsell sheet
     * instead of a generic failure — structured generation is premium-only.
     */
    private suspend fun postForJson(
        prompt: String,
        temperature: Float,
        maxTokens: Int
    ): Result<String> {
        return try {
            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                generationConfig = GenerationConfig(
                    temperature = temperature,
                    maxOutputTokens = maxTokens,
                    responseMimeType = "application/json"
                )
            )
            extractText(api.generateContent(request))
        } catch (e: HttpException) {
            Result.failure(mapHttpException(e, mapPremium = true))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// ── Data classes for Gemini API ────────────────────────────────────────────────

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null
)

@Serializable
data class Content(
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String
)

@Serializable
data class GenerationConfig(
    val temperature: Float? = null,
    val maxOutputTokens: Int? = null,
    // Set to "application/json" to make Gemini return parseable JSON (structured output).
    val responseMimeType: String? = null
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

@Serializable
data class Candidate(
    val content: Content? = null,
    val finishReason: String? = null
)

// Gemini error response shape — used to surface the underlying reason in the chat.
@Serializable
data class GeminiErrorEnvelope(val error: GeminiErrorBody? = null)

@Serializable
data class GeminiErrorBody(val code: Int? = null, val message: String? = null, val status: String? = null)

// Our Worker's typed error shape (e.g. {"error":"user_quota_exceeded","limit":25,"tier":"free"}).
// Distinguished from Gemini errors by `error` being a string, not an object.
@Serializable
data class ProxyErrorEnvelope(
    val error: String? = null,
    val limit: Int? = null,
    val used: Int? = null,
    val tier: String? = null,
)
