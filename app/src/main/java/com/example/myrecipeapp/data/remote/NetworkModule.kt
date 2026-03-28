package com.example.myrecipeapp.data.remote

import com.example.myrecipeapp.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * NetworkModule centralises the construction of OkHttp + Retrofit.
 */
object NetworkModule {

    private const val BASE_URL = "https://api.spoonacular.com/"

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

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG)
            HttpLoggingInterceptor.Level.BODY
        else
            HttpLoggingInterceptor.Level.NONE
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(ApiKeyInterceptor(BuildConfig.SPOONACULAR_API_KEY))
        .addInterceptor(loggingInterceptor)
        .connectTimeout(10, TimeUnit.SECONDS)   // fail fast on no connection
        .readTimeout(30, TimeUnit.SECONDS)       // API can be slow, allow 30s
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    /** Returns the singleton [SpoonacularApiService] instance. */
    fun provideApiService(): SpoonacularApiService =
        retrofit.create(SpoonacularApiService::class.java)
}
