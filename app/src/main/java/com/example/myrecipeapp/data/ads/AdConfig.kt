package com.example.myrecipeapp.data.ads

import com.example.myrecipeapp.BuildConfig

/**
 * Centralized AdMob ad unit IDs.
 *
 * In debug builds we always serve Google's official test ads — they're free,
 * always available, and click-safe (clicking won't generate fake revenue or
 * get your account banned). In release builds we serve production ads.
 *
 * To publish:
 *  1. Create an AdMob account at https://apps.admob.com/
 *  2. Register the app and create one Banner + one Interstitial ad unit
 *  3. Paste the production IDs below (and the App ID into AndroidManifest.xml)
 */
object AdConfig {

    // ── Test IDs (Google-provided, always show test ads) ───────────────────
    private const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
    private const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"

    // ── Production IDs — REPLACE before release ────────────────────────────
    // TODO: Replace with real ad unit IDs from your AdMob account.
    private const val PROD_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
    private const val PROD_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"

    val bannerAdUnitId: String
        get() = if (BuildConfig.DEBUG) TEST_BANNER_ID else PROD_BANNER_ID

    val interstitialAdUnitId: String
        get() = if (BuildConfig.DEBUG) TEST_INTERSTITIAL_ID else PROD_INTERSTITIAL_ID

    /** Show an interstitial every Nth recipe-detail open. Lower = more aggressive. */
    const val INTERSTITIAL_FREQUENCY = 3
}
