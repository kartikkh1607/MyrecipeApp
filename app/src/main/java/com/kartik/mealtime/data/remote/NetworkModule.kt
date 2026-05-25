package com.kartik.mealtime.data.remote

import android.content.Context
import com.kartik.mealtime.BuildConfig
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

    private const val BASE_URL = "https://api.spoonacular.com/"
    private const val CACHE_SIZE_BYTES = 10L * 1024 * 1024

    private class ApiKeyInterceptor(private val apiKey: String) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val url = chain.request().url.newBuilder()
                .addQueryParameter("apiKey", apiKey)
                .build()
            return chain.proceed(chain.request().newBuilder().url(url).build())
        }
    }

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

    private class RetryInterceptor(
        private val maxRetries: Int = 3,
        private val initialBackoffMs: Long = 1_000L
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            var lastException: java.io.IOException? = null
            repeat(maxRetries) { attempt ->
                try {
                    val response = chain.proceed(request)
                    if (response.code == 503 && attempt < maxRetries - 1) {
                        response.close()
                        Thread.sleep(initialBackoffMs * (attempt + 1))
                        return@repeat
                    }
                    return response
                } catch (e: java.io.IOException) {
                    lastException = e
                    if (attempt < maxRetries - 1) Thread.sleep(initialBackoffMs * (attempt + 1))
                }
            }
            throw lastException ?: java.io.IOException("Request failed after $maxRetries retries")
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
            .addInterceptor(ApiKeyInterceptor(BuildConfig.SPOONACULAR_API_KEY))
            .addInterceptor(RetryInterceptor())
            .addInterceptor(offlineFallbackInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Plain OkHttp client for the AI providers (Gemini, Groq). Kept separate from the
     * Spoonacular client above, whose ApiKeyInterceptor appends the Spoonacular apiKey
     * to every request — that must never touch AI calls. Shared as a singleton so Gemini
     * and Groq don't each spin up their own thread + connection pools.
     */
    @Provides
    @Singleton
    @Named("ai")
    fun provideAiOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
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
