package com.moneytracker.simplebudget.notifications

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.reminderDataStore: DataStore<Preferences> by preferencesDataStore(name = "reminder_settings")

enum class ReminderFrequency(val value: Int) {
    DAILY(0), WEEKLY(1), MONTHLY(2);

    companion object {
        fun fromValue(v: Int) = entries.firstOrNull { it.value == v } ?: DAILY
    }
}

enum class MonthlyReminderOption(val value: Int, val label: String) {
    FIRST_DAY(0, "First day"),
    DAY_5(1, "5th"),
    DAY_10(2, "10th"),
    MID_MONTH(3, "Mid-month"),
    DAY_20(4, "20th"),
    DAY_25(5, "25th"),
    LAST_DAY(6, "Last day");

    fun resolveDay(yearMonth: java.time.YearMonth): Int = when (this) {
        FIRST_DAY -> 1
        DAY_5     -> 5
        DAY_10    -> 10
        MID_MONTH -> yearMonth.lengthOfMonth() / 2
        DAY_20    -> 20
        DAY_25    -> 25
        LAST_DAY  -> yearMonth.lengthOfMonth()
    }

    companion object {
        fun fromValue(v: Int) = entries.firstOrNull { it.value == v } ?: FIRST_DAY
    }
}

data class ReminderSettings(
    val enabled: Boolean = false,
    val frequency: ReminderFrequency = ReminderFrequency.DAILY,
    val hour: Int = 20,
    val minute: Int = 0,
    val dayOfWeek: Int = 1,   // 1=Mon .. 7=Sun (java.time.DayOfWeek.value)
    val monthlyOption: MonthlyReminderOption = MonthlyReminderOption.FIRST_DAY
)

@Singleton
class ReminderPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val store = context.reminderDataStore

    val settings: Flow<ReminderSettings> = store.data.map { p ->
        ReminderSettings(
            enabled       = p[ENABLED]        ?: false,
            frequency     = ReminderFrequency.fromValue(p[FREQUENCY] ?: 0),
            hour          = p[HOUR]           ?: 20,
            minute        = p[MINUTE]         ?: 0,
            dayOfWeek     = p[DAY_OF_WEEK]    ?: 1,
            monthlyOption = MonthlyReminderOption.fromValue(p[MONTHLY_OPTION] ?: 0)
        )
    }

    suspend fun save(settings: ReminderSettings) {
        store.edit { p ->
            p[ENABLED]        = settings.enabled
            p[FREQUENCY]      = settings.frequency.value
            p[HOUR]           = settings.hour
            p[MINUTE]         = settings.minute
            p[DAY_OF_WEEK]    = settings.dayOfWeek
            p[MONTHLY_OPTION] = settings.monthlyOption.value
        }
    }

    private companion object {
        val ENABLED        = booleanPreferencesKey("reminder_enabled")
        val FREQUENCY      = intPreferencesKey("reminder_frequency")
        val HOUR           = intPreferencesKey("reminder_hour")
        val MINUTE         = intPreferencesKey("reminder_minute")
        val DAY_OF_WEEK    = intPreferencesKey("reminder_day_of_week")
        val MONTHLY_OPTION = intPreferencesKey("reminder_monthly_option")
    }
}
