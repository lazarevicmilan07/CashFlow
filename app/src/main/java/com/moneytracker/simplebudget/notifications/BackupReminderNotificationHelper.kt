package com.moneytracker.simplebudget.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.moneytracker.simplebudget.MainActivity
import com.moneytracker.simplebudget.R

object BackupReminderNotificationHelper {

    private const val CHANNEL_ID = "backup_reminder"
    const val NOTIFICATION_ID = 3002
    const val EXTRA_OPEN_SETTINGS = "open_settings"

    private val dailyMessages = listOf(
        "Don't forget to back up your data today \uD83D\uDCBE",
        "Keep your data safe \u2014 create a backup now",
        "Quick reminder: back up your financial data",
        "Secure your records \u2014 run a backup today \uD83D\uDD12"
    )

    private val weeklyMessages = listOf(
        "Time for your weekly data backup \uD83D\uDCBE",
        "Weekly reminder: keep your data safe with a backup",
        "Don't risk losing your records \u2014 back up now",
        "Your weekly backup reminder \uD83D\uDD12"
    )

    private val monthlyMessages = listOf(
        "Monthly backup reminder \u2014 secure your financial data \uD83D\uDCBE",
        "Time for your monthly data backup \uD83D\uDD12",
        "Keep your records safe \u2014 create a monthly backup",
        "Monthly reminder: back up your Money Tracker data"
    )

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Backup Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminds you to back up your data"
                enableVibration(true)
            }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    fun showNotification(context: Context, frequency: ReminderFrequency) {
        createNotificationChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ActivityCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        NotificationManagerCompat.from(context)
            .notify(NOTIFICATION_ID, buildNotification(context, frequency))
    }

    fun cancelNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun buildNotification(context: Context, frequency: ReminderFrequency): Notification {
        val message = when (frequency) {
            ReminderFrequency.DAILY   -> dailyMessages.random()
            ReminderFrequency.WEEKLY  -> weeklyMessages.random()
            ReminderFrequency.MONTHLY -> monthlyMessages.random()
        }
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_SETTINGS, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 1, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Money Tracker")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(0, "Open App", pendingIntent)
            .build()
    }
}
