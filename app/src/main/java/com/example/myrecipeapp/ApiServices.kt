package com.example.myrecipeapp

// end goal is “Hey Retrofit! Go to www.themealdb.com/api/json/v1/1/categories.php,
// get me the data, convert it into nice Kotlin objects, and I’ll use it in my app.”

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

// This creates a Retrofit machine — like a delivery guy who knows where to go,
// and how to bring the food (data) back.

private val retrofit = Retrofit.Builder()
    .baseUrl("https://www.themealdb.com/api/json/v1/1/")           // this is the website
    .addConverterFactory(GsonConverterFactory.create())    // convert json to kotlin
    .build()                                               // puts everything together

// Hey Retrofit, here's the menu (ApiServices)
// — please give me a working waiter (recepieService) who knows how to go and get the data.”

val recepieService = retrofit.create(ApiServices::class.java)

interface ApiServices {
    @GET("categories.php")                            // go to this page on this website
    suspend fun getCategories(): CategoriesResponse  // fetch the data and (wait for it) and get something like
    // CategoriesResponse
}