package com.kartik.mealtime.data.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hilt entry-point for accessing [InterstitialAdManager] from non-injected
 * call sites (e.g. composables). Used by [rememberInterstitialAdManager].
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface InterstitialAdEntryPoint {
    fun interstitialAdManager(): InterstitialAdManager
}

/**
 * Pre-loads and shows interstitial ads.
 *
 * Strategy: keeps one interstitial pre-loaded at all times. When [maybeShowOnRecipeView]
 * is called every Nth time (controlled by [AdConfig.INTERSTITIAL_FREQUENCY]) and an ad
 * is ready, it's shown and the next one is pre-loaded immediately.
 *
 * The view counter survives navigation but resets on process death — that's fine,
 * we'd rather under-show ads than annoy returning users.
 *
 * Future: when subscription ships, gate [maybeShowOnRecipeView] on `!premiumManager.isPremium`.
 */
@Singleton
class InterstitialAdManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var ad: InterstitialAd? = null
    private var isLoading = false
    private var recipeViewCount = 0

    init {
        // Warm cache so the first eligible recipe view has an ad ready.
        loadAd()
    }

    /**
     * Call from each RecipeDetailScreen open. Shows an ad on every Nth view and
     * silently no-ops otherwise. Safe to call from anywhere — needs an [Activity]
     * because interstitials must show from one.
     */
    fun maybeShowOnRecipeView(activity: Activity) {
        recipeViewCount++
        if (recipeViewCount % AdConfig.INTERSTITIAL_FREQUENCY != 0) return

        val ready = ad
        if (ready == null) {
            // Nothing loaded yet — kick off a load for next time and skip this one.
            loadAd()
            return
        }

        ready.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                ad = null
                loadAd()  // pre-load the next one
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w(TAG, "Interstitial failed to show: ${error.message}")
                ad = null
                loadAd()
            }
        }
        ready.show(activity)
    }

    private fun loadAd() {
        if (isLoading || ad != null) return
        isLoading = true
        InterstitialAd.load(
            context,
            AdConfig.interstitialAdUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(loaded: InterstitialAd) {
                    ad = loaded
                    isLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Interstitial load failed: ${error.message}")
                    ad = null
                    isLoading = false
                }
            }
        )
    }

    companion object {
        private const val TAG = "InterstitialAdManager"
    }
}
