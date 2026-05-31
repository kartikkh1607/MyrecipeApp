package com.kartik.mealtime.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.kartik.mealtime.BuildConfig
import com.kartik.mealtime.data.preferences.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Wraps the Play [BillingClient] for the single "premium" subscription (monthly +
 * annual base plans). Responsibilities:
 *
 *  - Maintain a resilient connection (reconnect with exponential backoff).
 *  - Expose the subscription [ProductDetails] (localized prices + offer tokens) so the
 *    upsell UI can render real plans.
 *  - Launch the purchase flow and, on a completed purchase, verify it **server-side**
 *    (the `verifyPurchase` callable sets the `premium` Firebase custom claim), then
 *    acknowledge it, force-refresh the ID token so the claim is live, and mirror the
 *    result into the local DataStore flag the UI gate reads.
 *  - Reconcile on connect / auth change so restores and cross-device subs are honoured.
 *
 * The server-set custom claim is the source of truth; the DataStore flag is just an
 * instant, offline mirror. See [BillingEntitlementRepository].
 */
@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPrefs: UserPreferencesRepository,
    // The "ai" client carries FirebaseAuthInterceptor, so requests to the proxy's
    // /billing/verify endpoint already include the user's Firebase ID token.
    @Named("ai") private val httpClient: OkHttpClient,
    private val auth: FirebaseAuth,
) : PurchasesUpdatedListener, BillingClientStateListener {

    private val gson = Gson()

    /** Outcome of a user-initiated purchase, surfaced to the UI for feedback. */
    sealed interface PurchaseResult {
        data object Success : PurchaseResult
        data object Cancelled : PurchaseResult
        data class Error(val message: String) : PurchaseResult
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    /** The "premium" subscription, with its monthly + annual offers. Null until loaded. */
    private val _productDetails = MutableStateFlow<ProductDetails?>(null)
    val productDetails: StateFlow<ProductDetails?> = _productDetails.asStateFlow()

    /** One-shot purchase outcomes for the UI (replay 0; buffered so emits never drop). */
    private val _purchaseEvents = MutableSharedFlow<PurchaseResult>(extraBufferCapacity = 1)
    val purchaseEvents: SharedFlow<PurchaseResult> = _purchaseEvents.asSharedFlow()

    private var reconnectDelayMs = INITIAL_RECONNECT_DELAY_MS

    init {
        // Re-mirror entitlement whenever the signed-in user changes (sign-in/out): a
        // new user's premium state must not be inherited from the previous session.
        auth.addAuthStateListener { scope.launch { refreshAndMirror(forceRefresh = false) } }
    }

    /** Connects the billing client if it isn't already. Safe to call repeatedly. */
    fun start() {
        val state = billingClient.connectionState
        if (state == BillingClient.ConnectionState.DISCONNECTED ||
            state == BillingClient.ConnectionState.CLOSED
        ) {
            billingClient.startConnection(this)
        }
    }

    override fun onBillingSetupFinished(result: BillingResult) {
        if (result.responseCode != BillingClient.BillingResponseCode.OK) return
        reconnectDelayMs = INITIAL_RECONNECT_DELAY_MS
        scope.launch {
            queryProducts()
            reconcilePurchases()
        }
    }

    override fun onBillingServiceDisconnected() {
        scope.launch {
            delay(reconnectDelayMs)
            reconnectDelayMs = (reconnectDelayMs * 2).coerceAtMost(MAX_RECONNECT_DELAY_MS)
            start()
        }
    }

    private suspend fun queryProducts() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(SUBSCRIPTION_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            )
            .build()
        val result = runCatching { billingClient.queryProductDetails(params) }.getOrNull() ?: return
        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            _productDetails.value = result.productDetailsList?.firstOrNull()
        }
    }

    /** Launches the Play purchase sheet for the given base-plan [offerToken]. */
    fun launchPurchase(activity: Activity, offerToken: String) {
        val product = _productDetails.value ?: return
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(product)
                        .setOfferToken(offerToken)
                        .build()
                )
            )
            .build()
        billingClient.launchBillingFlow(activity, flowParams)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK ->
                purchases?.forEach { purchase -> scope.launch { handlePurchase(purchase) } }

            BillingClient.BillingResponseCode.USER_CANCELED ->
                _purchaseEvents.tryEmit(PurchaseResult.Cancelled)

            else ->
                _purchaseEvents.tryEmit(PurchaseResult.Error(result.debugMessage))
        }
    }

    /**
     * Reconciles entitlement on connect: verifies/acknowledges any active subscription
     * (restores after reinstall), otherwise mirrors whatever the current claim says
     * (covers subs activated on another device, where the claim was set elsewhere).
     */
    private suspend fun reconcilePurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        val result = runCatching { billingClient.queryPurchasesAsync(params) }.getOrNull() ?: return
        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) return

        val activePurchases = result.purchasesList.filter {
            it.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        if (activePurchases.isNotEmpty()) {
            activePurchases.forEach { handlePurchase(it) }
        } else {
            refreshAndMirror(forceRefresh = false)
        }
    }

    private suspend fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return

        // Server verification is what actually grants premium (sets the custom claim).
        if (!verifyOnServer(purchase)) {
            _purchaseEvents.tryEmit(PurchaseResult.Error("We couldn't verify your subscription. Please try again."))
            return
        }
        if (!purchase.isAcknowledged) {
            val ackParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            runCatching { billingClient.acknowledgePurchase(ackParams) }
        }
        // Force-refresh so the freshly-set premium claim is in the ID token, then mirror.
        refreshAndMirror(forceRefresh = true)
        _purchaseEvents.tryEmit(PurchaseResult.Success)
    }

    /**
     * POSTs the purchase to the proxy's /billing/verify endpoint, which validates it with
     * the Play Developer API and sets the `premium` custom claim. The Firebase ID token is
     * attached by the "ai" client's FirebaseAuthInterceptor. Returns true on HTTP 200.
     *
     * Retries with exponential backoff. Critical because if verification never succeeds we
     * skip [BillingClient.acknowledgePurchase] and Google auto-refunds the user after
     * 3 days, even though the purchase went through — a transient network blip during
     * checkout would silently cost us the sale and leave the user without premium.
     */
    private suspend fun verifyOnServer(purchase: Purchase): Boolean {
        val payload = gson.toJson(
            mapOf(
                "productId" to (purchase.products.firstOrNull() ?: SUBSCRIPTION_PRODUCT_ID),
                "purchaseToken" to purchase.purchaseToken,
            )
        )
        val request = Request.Builder()
            .url("${BuildConfig.PROXY_BASE_URL}/billing/verify")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()

        var delayMs = VERIFY_INITIAL_DELAY_MS
        repeat(VERIFY_MAX_ATTEMPTS) { attempt ->
            val ok = runCatching {
                httpClient.newCall(request).execute().use { it.isSuccessful }
            }.getOrDefault(false)
            if (ok) return true
            if (attempt < VERIFY_MAX_ATTEMPTS - 1) {
                delay(delayMs)
                delayMs = (delayMs * 2).coerceAtMost(VERIFY_MAX_DELAY_MS)
            }
        }
        return false
    }

    /**
     * Reads the `premium` custom claim from the Firebase ID token and mirrors it into
     * the local DataStore flag. [forceRefresh] re-fetches the token from the server
     * (needed right after a purchase); otherwise the cached token is used.
     */
    suspend fun refreshAndMirror(forceRefresh: Boolean) {
        val user = auth.currentUser
        if (user == null) {
            userPrefs.setPremium(false)
            return
        }
        val premium = runCatching {
            user.getIdToken(forceRefresh).await().claims["premium"] == true
        }.getOrDefault(false)
        userPrefs.setPremium(premium)
    }

    companion object {
        const val SUBSCRIPTION_PRODUCT_ID = "premium"
        const val BASE_PLAN_MONTHLY = "monthly"
        const val BASE_PLAN_ANNUAL = "annual"

        private const val INITIAL_RECONNECT_DELAY_MS = 1_000L
        private const val MAX_RECONNECT_DELAY_MS = 30_000L

        // 5 attempts with 2s → 4s → 8s → 16s (~30s total) gives the proxy ample
        // headroom to ride out a transient blip without keeping the user staring
        // at a spinner. Tuned against the 3-day acknowledgement deadline: any
        // outage longer than this is operational and needs human attention.
        private const val VERIFY_MAX_ATTEMPTS = 5
        private const val VERIFY_INITIAL_DELAY_MS = 2_000L
        private const val VERIFY_MAX_DELAY_MS = 16_000L
    }
}
