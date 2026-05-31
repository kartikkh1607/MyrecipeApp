package com.kartik.mealtime.ui.viewmodel

import android.app.Activity
import com.android.billingclient.api.ProductDetails
import com.kartik.mealtime.data.billing.BillingManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertSame
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

/**
 * Unit tests for [BillingViewModel].
 *
 * [BillingViewModel] is a thin pass-through over [BillingManager], so the tests
 * just verify (a) the exposed flows are the manager's flows and (b) [purchase]
 * forwards the activity + offer token through unchanged.
 */
class BillingViewModelTest {

    @Test
    fun productDetailsIsTheManagerStateFlow() {
        val productFlow = MutableStateFlow<ProductDetails?>(null)
        val manager = mock(BillingManager::class.java)
        `when`(manager.productDetails).thenReturn(productFlow)
        `when`(manager.purchaseEvents).thenReturn(MutableSharedFlow())

        val vm = BillingViewModel(manager)

        assertSame(productFlow, vm.productDetails)
    }

    @Test
    fun purchaseEventsIsTheManagerSharedFlow() {
        val events = MutableSharedFlow<BillingManager.PurchaseResult>()
        val manager = mock(BillingManager::class.java)
        `when`(manager.productDetails).thenReturn(MutableStateFlow(null))
        `when`(manager.purchaseEvents).thenReturn(events)

        val vm = BillingViewModel(manager)

        assertSame(events, vm.purchaseEvents)
    }

    @Test
    fun purchaseForwardsActivityAndOfferTokenToTheManager() {
        val manager = mock(BillingManager::class.java)
        `when`(manager.productDetails).thenReturn(MutableStateFlow(null))
        `when`(manager.purchaseEvents).thenReturn(MutableSharedFlow())
        val activity = mock(Activity::class.java)
        val vm = BillingViewModel(manager)

        vm.purchase(activity, offerToken = "offer-token-123")

        verify(manager).launchPurchase(activity, "offer-token-123")
    }
}
