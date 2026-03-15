package com.moneytracker.simplebudget.notifications

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val WORK_NAME = "transaction_reminder"
    }

    fun scheduleReminder(settings: ReminderSettings) {
        val delay = when (settings.frequency) {
            ReminderFrequency.DAILY   -> dailyDelay(settings.hour, settings.minute)
            ReminderFrequency.WEEKLY  -> weeklyDelay(settings.dayOfWeek, settings.hour, settings.minute)
            ReminderFrequency.MONTHLY -> monthlyDelay(settings.monthlyOption, settings.hour, settings.minute)
        }
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag(WORK_NAME)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancelReminder() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    private fun dailyDelay(hour: Int, minute: Int): Long {
        val now = LocalDateTime.now()
        var target = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!target.isAfter(now)) target = target.plusDays(1)
        return ChronoUnit.MILLIS.between(now, target)
    }

    private fun weeklyDelay(dayOfWeek: Int, hour: Int, minute: Int): Long {
        val now = LocalDateTime.now()
        val dow = DayOfWeek.of(dayOfWeek)
        var target = now.with(TemporalAdjusters.nextOrSame(dow))
            .withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!target.isAfter(now)) target = target.plusWeeks(1)
        return ChronoUnit.MILLIS.between(now, target)
    }

    private fun monthlyDelay(option: MonthlyReminderOption, hour: Int, minute: Int): Long {
        val now = LocalDateTime.now()
        val thisMonth = YearMonth.from(now)
        var target = now.toLocalDate()
            .withDayOfMonth(option.resolveDay(thisMonth))
            .atTime(hour, minute, 0)
        if (!target.isAfter(now)) {
            val nextMonth = thisMonth.plusMonths(1)
            target = nextMonth.atDay(option.resolveDay(nextMonth)).atTime(hour, minute, 0)
        }
        return ChronoUnit.MILLIS.between(now, target)
    }
}
