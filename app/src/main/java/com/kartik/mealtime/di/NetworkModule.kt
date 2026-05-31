package com.kartik.mealtime.di

import android.content.Context
import com.kartik.mealtime.BuildConfig
import com.kartik.mealtime.data.remote.AiApi
import com.kartik.mealtime.data.remote.FirebaseAuthInterceptor
import com.kartik.mealtime.data.remote.SpoonacularApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // Spoonacular is reached through the Cloudflare Worker proxy, which adds the
    // real API key server-side (see server/cloudflare-worker). The trailing
    // "/spoonacular/" + relative endpoint paths resolve to /spoonacular/<path>,
    // which the Worker forwards to api.spoonacular.com/<path>.
    private val BASE_URL = "${BuildConfig.PROXY_BASE_URL}/spoonacular/"

    // AI providers (Gemini, Groq) share a base URL — the path differentiates them.
    // Trailing slash is required for Retrofit to resolve the relative "gemini" / "groq" paths.
    private val AI_BASE_URL = "${BuildConfig.PROXY_BASE_URL}/"

    private const val CACHE_SIZE_BYTES = 10L * 1024 * 1024

    // On network failure, fall back to the OkHttp disk cache (up to 24 h stale) so
    // the user sees something instead of an empty error state. Retries for transient
    // failures (IOException / 503) now live in the repository layer — see
    // data/remote/NetworkRetry.kt — so they suspend via `delay` instead of blocking
    // an OkHttp dispatcher thread with `Thread.sleep`.
    private val offlineFallbackInterceptor = Interceptor { chain ->
        val request = chain.request()
        try {
            chain.proceed(request)
        } catch (e: java.io.IOException) {
            val cacheRequest = request.newBuilder()
                .header("Cache-Control", "public, only-if-cached, max-stale=${60 * 60 * 24}")
                .build()
            chain.proceed(cacheRequest)
        }
    }

    /**
     * App-wide kotlinx.serialization Json instance.
     *
     * - `ignoreUnknownKeys = true` — Spoonacular + Gemini return many fields we
     *   don't model; without this, deserialization throws on every new field
     *   they add.
     * - `explicitNulls = false` — `null` values in serialised output are dropped,
     *   matching Gson's default and keeping cache JSON / API request bodies
     *   compact.
     * - `coerceInputValues = true` — when a non-null Kotlin field receives a
     *   wire null, fall back to the declared default instead of throwing
     *   (matches Gson's lenient behaviour we relied on).
     */
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
        // The AI sometimes wraps content fields in single-element lists; allow Retrofit
        // converter + manual parsers to keep flowing rather than crash on shape drift.
        isLenient = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(@ApplicationContext context: Context): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.HEADERS
                    else HttpLoggingInterceptor.Level.NONE
        }
        val cache = Cache(File(context.cacheDir, "http_cache"), CACHE_SIZE_BYTES)
        return OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor(FirebaseAuthInterceptor())
            .addInterceptor(offlineFallbackInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    /**
     * OkHttp client for the AI providers (Gemini, Groq). Like the Spoonacular client,
     * it carries the FirebaseAuthInterceptor (the proxy requires an ID token) but uses
     * longer timeouts suited to AI latency. Shared as a singleton so Gemini and Groq
     * don't each spin up their own thread + connection pools.
     */
    @Provides
    @Singleton
    @Named("ai")
    fun provideAiOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(FirebaseAuthInterceptor())
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): SpoonacularApiService =
        retrofit.create(SpoonacularApiService::class.java)

    /**
     * Retrofit instance for the AI endpoints. Separate from the Spoonacular Retrofit
     * because they need a different OkHttp client (longer timeouts, no disk cache)
     * and a different base URL.
     */
    @Provides
    @Singleton
    @Named("ai")
    fun provideAiRetrofit(
        @Named("ai") okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(AI_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideAiApi(@Named("ai") retrofit: Retrofit): AiApi =
        retrofit.create(AiApi::class.java)
}
