package com.kartik.mealtime.ui.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.android.billingclient.api.ProductDetails
import com.kartik.mealtime.data.billing.BillingManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Thin presentation seam over [BillingManager] for paywall UIs. Holds no state of its
 * own — products and purchase outcomes live in the app-scoped [BillingManager] singleton
 * so they survive across the (multiple) screens an upsell can appear from.
 */
@HiltViewModel
class BillingViewModel @Inject constructor(
    private val billingManager: BillingManager,
) : ViewModel() {

    /** The "premium" subscription with its monthly + annual offers; null until loaded. */
    val productDetails: StateFlow<ProductDetails?> = billingManager.productDetails

    /** One-shot purchase outcomes (success / cancelled / error) for UI feedback. */
    val purchaseEvents: SharedFlow<BillingManager.PurchaseResult> = billingManager.purchaseEvents

    /** Launches the Play purchase sheet for the chosen base-plan offer token. */
    fun purchase(activity: Activity, offerToken: String) {
        billingManager.launchPurchase(activity, offerToken)
    }
}
