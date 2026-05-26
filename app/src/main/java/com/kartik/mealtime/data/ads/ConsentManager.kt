package com.kartik.mealtime.data.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.kartik.mealtime.BuildConfig
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hilt entry-point for accessing [ConsentManager] from non-injected call sites
 * (e.g. composables). Used by `rememberConsentManager`.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ConsentManagerEntryPoint {
    fun consentManager(): ConsentManager
}

/**
 * Drives the Google UMP (User Messaging Platform) consent flow that's required to
 * serve AdMob ads to users in the EEA / UK. Nothing here matters unless
 * [AdConfig.adsEnabled] — callers gate on that before invoking [gatherConsent].
 *
 * Flow (must be driven from an Activity — see MainActivity):
 *  1. [gatherConsent] asks UMP whether consent info needs updating, then shows the
 *     consent form if one is required (first EEA launch, or after a reset).
 *  2. Once UMP reports [ConsentInformation.canRequestAds] == true (the user consented,
 *     or isn't in a region that requires consent) we initialize the Mobile Ads SDK
 *     exactly once and flip [canRequestAds], which is what the banner/interstitial
 *     code waits on before loading anything.
 *
 * Outside the EEA/UK, `canRequestAds()` is true immediately and no form is shown, so
 * this is effectively a no-op for most of the world.
 */
@Singleton
class ConsentManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(context)

    private val mobileAdsInitialized = AtomicBoolean(false)

    private val _canRequestAds = MutableStateFlow(consentInformation.canRequestAds())
    /** True once ads may be requested. Banner/interstitial code gates loading on this. */
    val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(computePrivacyOptionsRequired())
    /** True only for users (EEA/UK) who must be offered a way to change consent later. */
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    /**
     * Request a consent-info update and show the form if required. Safe to call on
     * every launch — UMP no-ops when consent is already current. Must run from an
     * [Activity] because the consent form is a full-screen dialog.
     */
    fun gatherConsent(activity: Activity) {
        // A returning user who already consented (or isn't in a consent region) can
        // start showing ads right away, without waiting for the info update to return.
        refreshState()
        if (consentInformation.canRequestAds()) initializeMobileAds()

        val params = ConsentRequestParameters.Builder()
            .apply { if (BuildConfig.DEBUG) setConsentDebugSettings(debugSettings(activity)) }
            .build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                // Info is current — show the form if UMP says one is required.
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Log.w(TAG, "Consent form error: ${formError.errorCode} ${formError.message}")
                    }
                    refreshState()
                    if (consentInformation.canRequestAds()) initializeMobileAds()
                }
            },
            { requestError ->
                Log.w(TAG, "Consent info update failed: ${requestError.errorCode} ${requestError.message}")
                // canRequestAds may still be true from a prior session — honor that.
                refreshState()
                if (consentInformation.canRequestAds()) initializeMobileAds()
            }
        )
    }

    /**
     * Show the "Manage privacy choices" form so a user can change consent later.
     * Only meaningful when [privacyOptionsRequired] is true (EEA/UK).
     */
    fun showPrivacyOptionsForm(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            if (formError != null) {
                Log.w(TAG, "Privacy options form error: ${formError.errorCode} ${formError.message}")
            }
            refreshState()
        }
    }

    private fun refreshState() {
        _canRequestAds.value = consentInformation.canRequestAds()
        _privacyOptionsRequired.value = computePrivacyOptionsRequired()
    }

    private fun computePrivacyOptionsRequired(): Boolean =
        consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    private fun initializeMobileAds() {
        if (mobileAdsInitialized.getAndSet(true)) return
        // Off the main thread — init does disk I/O + network and would jank the first frame.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                MobileAds.initialize(context) { /* init complete */ }
            } catch (t: Throwable) {
                // Defense in depth: a broken WebView / GMS shouldn't take the app down.
                Log.w(TAG, "MobileAds.initialize failed", t)
            }
        }
    }

    // In debug, force EEA geography so the consent form can be exercised. This only takes
    // effect on registered test devices / emulators — copy the hashed device ID that UMP
    // logs on first run into addTestDeviceHashedId(...) to see the EEA form on a physical
    // device. (Note: AdConfig.adsEnabled is false on emulators, so gatherConsent isn't
    // called there — test the form on a real device registered as a test device.)
    private fun debugSettings(activity: Activity): ConsentDebugSettings =
        ConsentDebugSettings.Builder(activity)
            .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
            .build()

    companion object {
        private const val TAG = "ConsentManager"
    }
}
