package com.kartik.mealtime.data.billing

import com.kartik.mealtime.data.preferences.UserPreferencesRepository
import com.kartik.mealtime.domain.repository.EntitlementRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Billing-backed [EntitlementRepository] — the production implementation bound in
 * [com.kartik.mealtime.di.RepositoryModule].
 *
 * [isPremium] reads the local DataStore flag, which [BillingManager] keeps in sync with
 * the server-set `premium` custom claim (the real source of truth). Reading the mirror
 * makes the UI gate instant, offline-safe, and stable across restarts; [BillingManager]
 * handles purchase, server verification, restore, and reconciliation.
 */
@Singleton
class BillingEntitlementRepository @Inject constructor(
    private val userPrefs: UserPreferencesRepository,
    private val billingManager: BillingManager,
) : EntitlementRepository {

    init {
        // Creating the entitlement source brings the billing client online (connect +
        // reconcile + auth listener). The entitlement repo is injected early (by gates
        // such as AiViewModel), so this reliably starts billing without coupling it to
        // an Activity lifecycle.
        billingManager.start()
    }

    override val isPremium: Flow<Boolean> = userPrefs.isPremium

    override suspend fun setPremiumOverride(enabled: Boolean) = userPrefs.setPremium(enabled)
}
