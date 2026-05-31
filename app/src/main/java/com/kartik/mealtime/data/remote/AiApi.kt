package com.kartik.mealtime.data.remote

import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit interface for the AI providers (Gemini, Groq) routed through the
 * Cloudflare Worker proxy. The proxy injects upstream API keys server-side, so
 * neither endpoint needs an Authorization header — the FirebaseAuthInterceptor
 * on the "ai" OkHttpClient attaches the user's Firebase ID token instead.
 *
 * Both calls are simple POST + JSON body; replaces the hand-rolled OkHttp
 * Request.Builder + gson.toJson plumbing that lived in [GeminiAiService] /
 * [GroqAiService] before 2026-05-31.
 */
interface AiApi {

    /**
     * POST /gemini — generateContent on the Gemini model selected server-side.
     * Used for chat, recipe generation, recipe transforms, and meal plans.
     * Throws [retrofit2.HttpException] on non-2xx; callers map 402 to
     * [PremiumRequiredException] and other codes via `friendlyError`.
     */
    @POST("gemini")
    suspend fun generateContent(
        @Body request: GenerateContentRequest,
    ): GenerateContentResponse

    /**
     * POST /groq — OpenAI-compatible chat completions, used as the Gemini
     * fallback. Same error contract.
     */
    @POST("groq")
    suspend fun chatCompletion(
        @Body request: ChatCompletionRequest,
    ): ChatCompletionResponse
}
