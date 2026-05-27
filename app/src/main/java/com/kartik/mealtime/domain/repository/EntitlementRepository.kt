package com.kartik.mealtime.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * The single contract the app uses to decide whether premium features are unlocked.
 *
 * This is the seam that isolates the rest of the app from *how* entitlement is
 * determined. Today it is a local DataStore flag (see the local implementation);
 * when Play Billing + server receipt validation are built (see the deferred
 * freemium plan), only the implementation bound in the DI module changes — every
 * gate, paywall, and [com.kartik.mealtime.ui.components.UpsellBottomSheet] that
 * reads [isPremium] keeps working untouched.
 */
interface EntitlementRepository {

    /** Emits the user's current premium state; gates observe this. */
    val isPremium: Flow<Boolean>

    /**
     * Local override used until real billing exists (dev unlock + tests).
     * The billing-backed implementation will ignore/replace this.
     */
    suspend fun setPremiumOverride(enabled: Boolean)
}
