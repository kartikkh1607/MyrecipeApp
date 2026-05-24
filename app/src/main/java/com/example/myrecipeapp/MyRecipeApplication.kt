package com.example.myrecipeapp

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.myrecipeapp.BuildConfig
import com.google.android.gms.ads.MobileAds
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class MyRecipeApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        // Only collect crash reports in release — keeps the Firebase console free of
        // development noise and avoids uploading mapping files on every debug run.
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)

        // Initialize AdMob off the main thread — it does disk I/O and network calls
        // on init, which would jank the first frame if run synchronously.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            MobileAds.initialize(this@MyRecipeApplication) { /* init complete */ }
        }
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .crossfade(300)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100L * 1024 * 1024)
                    .build()
            }
            .build()
}
