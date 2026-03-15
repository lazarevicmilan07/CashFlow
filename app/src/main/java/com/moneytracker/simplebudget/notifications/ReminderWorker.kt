package com.moneytracker.simplebudget.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val reminderPreferences: ReminderPreferences,
    private val reminderManager: ReminderManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val settings = reminderPreferences.settings.first()
        if (settings.enabled) {
            ReminderNotificationHelper.showNotification(appContext, settings.frequency)
            reminderManager.scheduleReminder(settings)
        }
        return Result.success()
    }
}
