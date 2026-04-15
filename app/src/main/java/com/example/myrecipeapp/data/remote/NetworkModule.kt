package com.example.myrecipeapp.data.remote

import com.example.myrecipeapp.BuildConfig
import com.example.myrecipeapp.MyRecipeApplication
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * NetworkModule centralises the construction of OkHttp + Retrofit.
 */
object NetworkModule {

    private const val BASE_URL = "https://api.spoonacular.com/"

    /** 10 MB disk cache — Spoonacular sets Cache-Control: max-age=900 so repeat
     *  calls within 15 min are served from disk at zero API cost and return instantly. */
    private const val CACHE_SIZE_BYTES = 10L * 1024 * 1024  // 10 MB

    /**
     * Appends the Spoonacular API key as a query parameter to every request.
     * This removes the need to pass apiKey in every Retrofit method signature.
     */
    private class ApiKeyInterceptor(private val apiKey: String) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val url = chain.request().url.newBuilder()
                .addQueryParameter("apiKey", apiKey)
                .build()
            return chain.proceed(chain.request().newBuilder().url(url).build())
        }
    }

    /** If offline, serve stale cache for up to 24 h rather than showing an error. */
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

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        // BODY logs the entire JSON response (~100 KB per call) to Logcat — big CPU hit.
        // HEADERS is enough for debugging: shows URL, status code, and timing.
        level = if (BuildConfig.DEBUG)
            HttpLoggingInterceptor.Level.HEADERS
        else
            HttpLoggingInterceptor.Level.NONE
    }

    private val httpCache: Cache by lazy {
        val cacheDir = File(MyRecipeApplication.instance.cacheDir, "http_cache")
        Cache(cacheDir, CACHE_SIZE_BYTES)
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .cache(httpCache)
            .addInterceptor(ApiKeyInterceptor(BuildConfig.SPOONACULAR_API_KEY))
            .addInterceptor(offlineFallbackInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(10, TimeUnit.SECONDS)   // fail fast on no connection
            .readTimeout(30, TimeUnit.SECONDS)       // API can be slow, allow 30s
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /** Returns the singleton [SpoonacularApiService] instance. */
    fun provideApiService(): SpoonacularApiService =
        retrofit.create(SpoonacularApiService::class.java)
}
