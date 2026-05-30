package com.kartik.mealtime.di

import android.content.Context
import com.kartik.mealtime.BuildConfig
import com.kartik.mealtime.data.remote.FirebaseAuthInterceptor
import com.kartik.mealtime.data.remote.SpoonacularApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
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
    private const val CACHE_SIZE_BYTES = 10L * 1024 * 1024

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
     * Linear-backoff retry for transient failures (IOException or 503). Backoff is
     * 1s / 2s / 3s — so the worst-case extra wall time is 6s across three attempts.
     *
     * `Thread.sleep` blocks the dispatcher thread holding this interceptor — fine in
     * practice for a single-user mobile client (OkHttp's default dispatcher has 64
     * slots, of which we use ≤2 concurrently), but we honour [Call.isCanceled] before
     * and after each sleep so a coroutine cancellation during backoff aborts the
     * request promptly instead of waiting out the timer. [InterruptedException] is
     * also surfaced as an IOException so it doesn't disappear into a "Request failed"
     * generic at the end of the loop.
     */
    private class RetryInterceptor(
        private val maxRetries: Int = 3,
        private val initialBackoffMs: Long = 1_000L
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            var lastException: java.io.IOException? = null
            repeat(maxRetries) { attempt ->
                if (chain.call().isCanceled()) throw java.io.IOException("Canceled")
                try {
                    val response = chain.proceed(request)
                    if (response.code == 503 && attempt < maxRetries - 1) {
                        response.close()
                        if (!backoff(chain, initialBackoffMs * (attempt + 1))) {
                            throw java.io.IOException("Canceled")
                        }
                        return@repeat
                    }
                    return response
                } catch (e: java.io.IOException) {
                    lastException = e
                    if (attempt < maxRetries - 1 &&
                        !backoff(chain, initialBackoffMs * (attempt + 1))
                    ) {
                        throw java.io.IOException("Canceled", e)
                    }
                }
            }
            throw lastException ?: java.io.IOException("Request failed after $maxRetries retries")
        }

        /** Sleeps in 100 ms chunks so a Call.cancel during backoff is observed within
         *  ~100 ms rather than waiting out the full backoff. Returns false if the call
         *  was cancelled or the thread was interrupted, true otherwise. */
        private fun backoff(chain: Interceptor.Chain, totalMs: Long): Boolean {
            val deadline = System.currentTimeMillis() + totalMs
            while (System.currentTimeMillis() < deadline) {
                if (chain.call().isCanceled()) return false
                try {
                    Thread.sleep(minOf(100L, deadline - System.currentTimeMillis()).coerceAtLeast(0))
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    return false
                }
            }
            return true
        }
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
            .addInterceptor(RetryInterceptor())
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
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): SpoonacularApiService =
        retrofit.create(SpoonacularApiService::class.java)
}
