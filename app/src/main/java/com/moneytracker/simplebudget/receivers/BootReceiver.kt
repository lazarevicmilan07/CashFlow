package com.moneytracker.simplebudget.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.moneytracker.simplebudget.notifications.ReminderManager
import com.moneytracker.simplebudget.notifications.ReminderPreferences
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var reminderPreferences: ReminderPreferences
    @Inject lateinit var reminderManager: ReminderManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON") return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = reminderPreferences.settings.first()
                if (settings.enabled) reminderManager.scheduleReminder(settings)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
