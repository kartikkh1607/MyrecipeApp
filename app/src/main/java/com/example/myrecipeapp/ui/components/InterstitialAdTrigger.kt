package com.example.myrecipeapp.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.myrecipeapp.data.ads.InterstitialAdEntryPoint
import com.example.myrecipeapp.data.ads.InterstitialAdManager
import dagger.hilt.android.EntryPointAccessors

/**
 * Composable helper that fetches the singleton [InterstitialAdManager] via Hilt
 * entry point. Use from any composable without needing a dedicated ViewModel.
 */
@Composable
fun rememberInterstitialAdManager(): InterstitialAdManager {
    val context = LocalContext.current
    return remember(context.applicationContext) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            InterstitialAdEntryPoint::class.java
        ).interstitialAdManager()
    }
}

/**
 * Walks the [ContextWrapper] chain looking for an [Activity]. Returns null if the
 * context isn't tied to an Activity (e.g. inside a Compose preview).
 */
fun Context.findActivity(): Activity? {
    var ctx: Context = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
