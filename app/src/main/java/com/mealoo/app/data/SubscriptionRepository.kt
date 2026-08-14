package com.mealoo.app.data.repository

import android.app.Activity
import android.util.Log
import com.revenuecat.purchases.*
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.models.StoreTransaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SubscriptionRepository {

    private val entitlementId = "Mealoo Pro"

    private val _isPro = MutableStateFlow(false)
    val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    private val _offerings = MutableStateFlow<List<Package>>(emptyList())
    val offerings: StateFlow<List<Package>> = _offerings.asStateFlow()

    init {
        // 初始化時設定監聽器，當權限變更時 (例如訂閱過期、退款) 自動更新
        Purchases.sharedInstance.updatedCustomerInfoListener =
            UpdatedCustomerInfoListener { customerInfo ->
                updateProStatus(customerInfo)
            }

        // 主動抓取一次最新狀態與商品列表
        fetchCustomerInfo()
        fetchOfferings()
    }

    // 檢查使用者是否擁有 Pro 權限 (包含訂閱中或已買斷)
    private fun updateProStatus(customerInfo: CustomerInfo) {
        val isActive = customerInfo.entitlements[entitlementId]?.isActive == true
        _isPro.value = isActive
        Log.d("SubscriptionRepo", "Pro Status Updated: $isActive")
    }

    // 從 RevenueCat 伺服器獲取最新使用者資訊
    private fun fetchCustomerInfo() {
        Purchases.sharedInstance.getCustomerInfo(object : ReceiveCustomerInfoCallback {
            override fun onReceived(customerInfo: CustomerInfo) {
                updateProStatus(customerInfo)
            }

            override fun onError(error: PurchasesError) {
                Log.e("SubscriptionRepo", "Error fetching customer info: ${error.message}")
            }
        })
    }

    // 獲取可購買的商品列表 (Offerings)
    private fun fetchOfferings() {
        Purchases.sharedInstance.getOfferings(object : ReceiveOfferingsCallback {
            override fun onReceived(offerings: Offerings) {
                // 通常我們只關心 "current" offering，這是你在後台設定的預設商品組
                val currentPackages = offerings.current?.availablePackages ?: emptyList()
                _offerings.value = currentPackages
                Log.d("SubscriptionRepo", "Fetched ${currentPackages.size} packages")
            }

            override fun onError(error: PurchasesError) {
                Log.e("SubscriptionRepo", "Error fetching offerings: ${error.message}")
            }
        })
    }

    // 執行購買
    fun purchase(activity: Activity, packageToBuy: Package, onResult: (String?) -> Unit) {
        Purchases.sharedInstance.purchasePackage(
            activity,
            packageToBuy,
            object : PurchaseCallback {
                override fun onCompleted(storeTransaction: StoreTransaction, customerInfo: CustomerInfo) {
                    updateProStatus(customerInfo)
                    onResult(null) // null 代表成功，無錯誤訊息
                }

                fun onUserCancelled(storeTransaction: StoreTransaction, customerInfo: CustomerInfo) {
                }

                override fun onError(error: PurchasesError, userCancelled: Boolean) {
                    if (!userCancelled) {
                        onResult(error.message)
                    }
                }
            }
        )
    }

    // 恢復購買 (Restore Purchases) - 用於換手機或重裝 App
    fun restorePurchases(onResult: (Boolean) -> Unit) {
        Purchases.sharedInstance.restorePurchases(object : ReceiveCustomerInfoCallback {
            override fun onReceived(customerInfo: CustomerInfo) {
                updateProStatus(customerInfo)
                // 檢查恢復後是否真的有權限
                val hasAccess = customerInfo.entitlements[entitlementId]?.isActive == true
                onResult(hasAccess)
            }

            override fun onError(error: PurchasesError) {
                Log.e("SubscriptionRepo", "Restore failed: ${error.message}")
                onResult(false)
            }
        })
    }
}