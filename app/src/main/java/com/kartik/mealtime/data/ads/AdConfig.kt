package com.kartik.mealtime.data.ads

import android.os.Build
import com.kartik.mealtime.BuildConfig

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

    // ── Production IDs ─────────────────────────────────────────────────────
    // Injected from local.properties via BuildConfig (see app/build.gradle.kts:
    // admob.banner.id / admob.interstitial.id). Intentionally NOT hardcoded here so
    // Google's test IDs can never ship as production. Blank until you configure them.
    private val PROD_BANNER_ID: String get() = BuildConfig.ADMOB_BANNER_ID
    private val PROD_INTERSTITIAL_ID: String get() = BuildConfig.ADMOB_INTERSTITIAL_ID

    val bannerAdUnitId: String
        get() = if (BuildConfig.DEBUG) TEST_BANNER_ID else PROD_BANNER_ID

    val interstitialAdUnitId: String
        get() = if (BuildConfig.DEBUG) TEST_INTERSTITIAL_ID else PROD_INTERSTITIAL_ID

    /** Show an interstitial every Nth recipe-detail open. Lower = more aggressive. */
    const val INTERSTITIAL_FREQUENCY = 3

    /**
     * Whether to serve ads at all.
     *
     * Debug: on, except on emulators — AdMob spins up a Chromium WebView child
     * process, and emulator system images ship with a frozen WebView that often
     * crashes (Binder BR_DEAD_REPLY, GMS service-descriptor mismatch). Real
     * devices auto-update WebView and are unaffected; debug always uses test IDs.
     *
     * Release: on ONLY when production ad-unit IDs are configured. If they're
     * missing we disable ads entirely rather than fall back to Google's test IDs —
     * serving test IDs in production violates AdMob policy and earns no revenue.
     */
    val adsEnabled: Boolean
        get() = if (BuildConfig.DEBUG) {
            !isEmulator
        } else {
            BuildConfig.ADMOB_BANNER_ID.isNotBlank() &&
                BuildConfig.ADMOB_INTERSTITIAL_ID.isNotBlank()
        }

    private val isEmulator: Boolean
        get() = Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.startsWith("unknown") ||
            Build.MODEL.contains("google_sdk") ||
            Build.MODEL.contains("Emulator") ||
            Build.MODEL.contains("Android SDK built for") ||
            Build.MANUFACTURER.contains("Genymotion") ||
            (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) ||
            Build.PRODUCT == "google_sdk" ||
            Build.PRODUCT.contains("sdk_gphone") ||
            Build.HARDWARE.contains("goldfish") ||
            Build.HARDWARE.contains("ranchu")
}
