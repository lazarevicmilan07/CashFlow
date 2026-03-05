package com.moneytracker.simplebudget.billing

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.math.min

@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context
) : PurchasesUpdatedListener {

    private var billingClient: BillingClient? = null
    private var productDetails: ProductDetails? = null
    private var reconnectAttempts = 0
    private val handler = Handler(Looper.getMainLooper())

    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    val purchaseState: StateFlow<PurchaseState> = _purchaseState.asStateFlow()

    init {
        setupBillingClient()
    }

    private fun setupBillingClient() {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .build()

        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    reconnectAttempts = 0
                    queryProductDetails()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Retry with exponential backoff, max 5 attempts
                if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                    val delayMs = min(RECONNECT_BASE_DELAY_MS * (1L shl reconnectAttempts), MAX_RECONNECT_DELAY_MS)
                    reconnectAttempts++
                    handler.postDelayed({ setupBillingClient() }, delayMs)
                }
            }
        })
    }

    private fun queryProductDetails() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PREMIUM_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient?.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                productDetails = productDetailsList.firstOrNull()
            }
        }
    }

    suspend fun getPremiumPrice(): String? {
        return productDetails?.oneTimePurchaseOfferDetails?.formattedPrice
            ?: suspendCancellableCoroutine { continuation ->
                val productList = listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PREMIUM_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )

                val params = QueryProductDetailsParams.newBuilder()
                    .setProductList(productList)
                    .build()

                billingClient?.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        productDetails = productDetailsList.firstOrNull()
                        continuation.resume(productDetails?.oneTimePurchaseOfferDetails?.formattedPrice)
                    } else {
                        continuation.resume(null)
                    }
                } ?: continuation.resume(null)
            }
    }

    fun launchBillingFlow(activity: Activity) {
        val details = productDetails ?: return

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient?.launchBillingFlow(activity, billingFlowParams)
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    handlePurchase(purchase)
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _purchaseState.value = PurchaseState.Canceled
            }
            else -> {
                _purchaseState.value = PurchaseState.Error(
                    billingResult.debugMessage.ifEmpty { "Purchase failed" }
                )
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()

                billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        _purchaseState.value = PurchaseState.Success
                    } else {
                        _purchaseState.value = PurchaseState.Error("Failed to acknowledge purchase")
                    }
                }
            } else {
                _purchaseState.value = PurchaseState.Success
            }
        }
    }

    suspend fun checkPurchases(): Boolean {
        return suspendCancellableCoroutine { continuation ->
            billingClient?.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            ) { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val hasPremium = purchases.any {
                        it.products.contains(PREMIUM_PRODUCT_ID) &&
                        it.purchaseState == Purchase.PurchaseState.PURCHASED
                    }
                    continuation.resume(hasPremium)
                } else {
                    continuation.resume(false)
                }
            } ?: continuation.resume(false)
        }
    }

    sealed class PurchaseState {
        data object Idle : PurchaseState()
        data object Success : PurchaseState()
        data class Error(val message: String) : PurchaseState()
        data object Canceled : PurchaseState()
    }

    companion object {
        const val PREMIUM_PRODUCT_ID = "premium_unlock"
        private const val MAX_RECONNECT_ATTEMPTS = 5
        private const val RECONNECT_BASE_DELAY_MS = 1000L
        private const val MAX_RECONNECT_DELAY_MS = 30_000L
    }
}
