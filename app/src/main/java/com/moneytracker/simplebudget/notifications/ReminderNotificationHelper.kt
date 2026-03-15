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

object ReminderNotificationHelper {

    private const val CHANNEL_ID = "transaction_reminder"
    const val NOTIFICATION_ID = 3001

    private val dailyMessages = listOf(
        "Don't forget to log today's transactions \uD83D\uDCB0",
        "Have you recorded your expenses today?",
        "Quick reminder to track your spending!",
        "Stay on top of your budget \u2014 log today's transactions",
        "Keep your finances in check \u2014 add today's entries",
        "Don't let transactions go unrecorded \uD83D\uDCCA",
        "Budget check: have you logged today's expenses?"
    )

    private val weeklyMessages = listOf(
        "Time to log this week's transactions \uD83D\uDCB0",
        "Weekly check: have you recorded all your expenses?",
        "Keep your budget on track \u2014 log this week's spending",
        "Don't forget your weekly expense recap \uD83D\uDCCA"
    )

    private val monthlyMessages = listOf(
        "Monthly budget check \u2014 log any missing transactions \uD83D\uDCB0",
        "Time to review and log this month's expenses \uD83D\uDCCA",
        "Keep your monthly finances in check \u2014 add missing entries",
        "Monthly reminder: record all your transactions"
    )

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Transaction Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminds you to log your transactions"
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
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, openIntent,
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
            .addAction(0, "Add Transaction", pendingIntent)
            .build()
    }
}
