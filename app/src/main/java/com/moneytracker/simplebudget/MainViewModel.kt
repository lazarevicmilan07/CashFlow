package com.moneytracker.simplebudget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneytracker.simplebudget.billing.BillingManager
import com.moneytracker.simplebudget.data.preferences.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val billingManager: BillingManager,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    init {
        viewModelScope.launch {
            billingManager.purchaseState.collect { state ->
                if (state is BillingManager.PurchaseState.Success) {
                    preferencesManager.setPremium(true)
                }
            }
        }
    }
}
