package com.kartik.mealtime

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.kartik.mealtime.BuildConfig
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyRecipeApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        // Only collect crash reports in release — keeps the Firebase console free of
        // development noise and avoids uploading mapping files on every debug run.
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)

        // AdMob is NOT initialized here. ConsentManager.gatherConsent() (driven from
        // MainActivity) initializes the Mobile Ads SDK only after the UMP consent flow
        // resolves — initializing earlier risks requesting ads before EEA/UK users
        // have given consent.
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
