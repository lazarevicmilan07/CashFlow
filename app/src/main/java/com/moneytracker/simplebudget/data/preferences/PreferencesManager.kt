package com.moneytracker.simplebudget.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class ThemeMode(val value: Int) {
    SYSTEM(0), LIGHT(1), DARK(2);

    companion object {
        fun fromValue(value: Int) = entries.firstOrNull { it.value == value } ?: SYSTEM
    }
}

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    val userPreferences: Flow<UserPreferences> = dataStore.data.map { preferences ->
        UserPreferences(
            themeMode = resolveThemeMode(preferences),
            currency = preferences[PreferencesKeys.CURRENCY] ?: "USD",
            isPremium = preferences[PreferencesKeys.IS_PREMIUM] ?: false,
            currencySymbolAfter = preferences[PreferencesKeys.CURRENCY_SYMBOL_AFTER] ?: true
        )
    }

    val themeMode: Flow<ThemeMode> = dataStore.data.map { preferences ->
        resolveThemeMode(preferences)
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

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.value
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

    private fun resolveThemeMode(preferences: Preferences): ThemeMode {
        val stored = preferences[PreferencesKeys.THEME_MODE]
        if (stored != null) return ThemeMode.fromValue(stored)
        // Migrate from old boolean: dark=true→DARK, dark=false→LIGHT
        return if (preferences[PreferencesKeys.DARK_MODE] == false) ThemeMode.LIGHT else ThemeMode.DARK
    }

    private object PreferencesKeys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val THEME_MODE = intPreferencesKey("theme_mode")
        val CURRENCY = stringPreferencesKey("currency")
        val IS_PREMIUM = booleanPreferencesKey("is_premium")
        val CURRENCY_SYMBOL_AFTER = booleanPreferencesKey("currency_symbol_after")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }
}

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val currency: String = "USD",
    val isPremium: Boolean = false,
    val currencySymbolAfter: Boolean = true
)
