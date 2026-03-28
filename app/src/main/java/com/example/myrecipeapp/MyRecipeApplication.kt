package com.example.myrecipeapp

import android.app.Application

/**
 * Custom Application class.
 *
 * Holds the application-level [Context] so that infrastructure components
 * (e.g. [com.example.myrecipeapp.data.remote.NetworkModule]) can access the
 * disk-cache directory without needing to thread context through every layer.
 */
class MyRecipeApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: MyRecipeApplication
            private set
    }
}
