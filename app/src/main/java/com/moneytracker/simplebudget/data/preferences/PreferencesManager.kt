package com.moneytracker.simplebudget.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    val userPreferences: Flow<UserPreferences> = dataStore.data.map { preferences ->
        UserPreferences(
            isDarkMode = preferences[PreferencesKeys.DARK_MODE] ?: true,
            currency = preferences[PreferencesKeys.CURRENCY] ?: "USD",
            isPremium = preferences[PreferencesKeys.IS_PREMIUM] ?: false,
            currencySymbolAfter = preferences[PreferencesKeys.CURRENCY_SYMBOL_AFTER] ?: true
        )
    }

    val isDarkMode: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DARK_MODE] ?: true
    }

    val currency: Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.CURRENCY] ?: "USD"
    }

    val isPremium: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.IS_PREMIUM] ?: false
    }

    val currencySymbolAfter: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.CURRENCY_SYMBOL_AFTER] ?: true
    }

    val onboardingCompleted: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false
    }

    suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DARK_MODE] = enabled
        }
    }

    suspend fun setCurrency(currency: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.CURRENCY] = currency
        }
    }

    suspend fun setCurrencySymbolAfter(after: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.CURRENCY_SYMBOL_AFTER] = after
        }
    }

    suspend fun setPremium(isPremium: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_PREMIUM] = isPremium
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = completed
        }
    }

    private object PreferencesKeys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val CURRENCY = stringPreferencesKey("currency")
        val IS_PREMIUM = booleanPreferencesKey("is_premium")
        val CURRENCY_SYMBOL_AFTER = booleanPreferencesKey("currency_symbol_after")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }
}

data class UserPreferences(
    val isDarkMode: Boolean = true,
    val currency: String = "USD",
    val isPremium: Boolean = false,
    val currencySymbolAfter: Boolean = true
)
