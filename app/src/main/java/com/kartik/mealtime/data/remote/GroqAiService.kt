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
import javax.inject.Inject
import javax.inject.Named

/**
 * Groq (groq.com) client used as a fallback when Gemini is unavailable.
 *
 * NOTE: This is Groq the fast-inference cloud (Llama/etc., keys start with
 * "gsk_"), NOT xAI's Grok. Groq exposes an OpenAI-compatible Chat Completions
 * API, so the request / response shapes here mirror that schema. Reuses
 * [AiPrompts] so responses are formatted identically to Gemini's.
 *
 * Only invoked by [AiServiceRouter] when [isConfigured] is true (i.e. a key is
 * present in local.properties).
 */
class GroqAiService @Inject constructor(
    @Named("ai") private val client: OkHttpClient
) : AiService {

    private val gson = Gson()

    companion object {
        private const val BASE_URL = "https://api.groq.com/openai/v1/chat/completions"
        // Change this to whichever model your Groq account has access to
        // (e.g. "llama-3.3-70b-versatile", "llama-3.1-8b-instant", "gemma2-9b-it").
        private const val MODEL = "llama-3.3-70b-versatile"
    }

    /** True when a Groq API key is configured; gates whether fallback is attempted. */
    val isConfigured: Boolean
        get() = BuildConfig.GROQ_API_KEY.isNotBlank()

    override suspend fun sendChatMessage(
        message: String,
        conversationHistory: List<ChatMessage>,
        dietaryPreferences: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val prompt = AiPrompts.buildChatPrompt(conversationHistory, message, dietaryPreferences)
        complete(prompt, temperature = 0.85f, maxTokens = 2048)
    }

    override suspend fun getRecipeRecommendations(
        favoriteRecipes: List<String>,
        dietaryPreferences: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val prompt = AiPrompts.buildRecommendationPrompt(favoriteRecipes, dietaryPreferences)
        complete(prompt, temperature = 0.7f, maxTokens = 1024)
    }

    // The combined prompt (system instructions + history + "Assistant:") is sent
    // as a single user message. This reuses Gemini's exact prompt verbatim, so
    // fallback output matches; the conversation is already embedded in the text.
    private fun complete(prompt: String, temperature: Float, maxTokens: Int): Result<String> {
        if (!isConfigured) {
            return Result.failure(Exception("Groq API key is not configured."))
        }
        return try {
            val requestBody = ChatCompletionRequest(
                model = MODEL,
                messages = listOf(ChatCompletionMessage(role = "user", content = prompt)),
                temperature = temperature,
                maxTokens = maxTokens
            )

            val mediaType = "application/json".toMediaType()
            val body = gson.toJson(requestBody).toRequestBody(mediaType)

            val request = Request.Builder()
                .url(BASE_URL)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer ${BuildConfig.GROQ_API_KEY}")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
                ?: return Result.failure(Exception("Empty response from Groq API"))

            if (!response.isSuccessful) {
                return Result.failure(friendlyError(response.code, responseBody))
            }

            val parsed = gson.fromJson(responseBody, ChatCompletionResponse::class.java)
            val text = parsed.choices
                ?.firstOrNull()
                ?.message
                ?.content
                ?.trim()
                ?: return Result.failure(Exception("No content in Groq response"))

            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Maps Groq HTTP errors to actionable messages. OpenAI-compatible error shape:
    // { "error": { "message": "...", "type": "...", "code": "..." } }
    private fun friendlyError(code: Int, body: String?): Exception {
        val apiMsg = runCatching {
            gson.fromJson(body, ChatCompletionErrorEnvelope::class.java)?.error?.message
        }.getOrNull()
        val suffix = if (!apiMsg.isNullOrBlank()) " ($apiMsg)" else ""
        return Exception(
            when (code) {
                400 -> "Groq rejected the request (bad request).$suffix"
                401 -> "Groq API key is invalid or missing — check groq.api.key in local.properties.$suffix"
                403 -> "Groq access denied: the key may lack permission for model '$MODEL'.$suffix"
                404 -> "Groq model '$MODEL' not found — your account may not have access. Try another model in GroqAiService.$suffix"
                429 -> "Groq rate limit or credits exceeded. Check your Groq usage at console.groq.com.$suffix"
                in 500..599 -> "Groq is having trouble right now. Please try again in a moment.$suffix"
                else -> "Groq error ($code).$suffix"
            }
        )
    }
}

// ── OpenAI-compatible DTOs for the Groq Chat Completions API ────────────────────

data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatCompletionMessage>,
    val temperature: Float? = null,
    @SerializedName("max_tokens")
    val maxTokens: Int? = null
)

data class ChatCompletionMessage(
    val role: String,
    val content: String
)

data class ChatCompletionResponse(
    val choices: List<ChatCompletionChoice>?
)

data class ChatCompletionChoice(
    val message: ChatCompletionMessage?,
    @SerializedName("finish_reason")
    val finishReason: String?
)

data class ChatCompletionErrorEnvelope(val error: ChatCompletionErrorBody?)
data class ChatCompletionErrorBody(val message: String?, val type: String?, val code: String?)
