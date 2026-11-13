package com.taskly.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.taskly.app.MainActivity
import com.taskly.app.R
import com.taskly.app.receivers.ReminderActionReceiver

object NotificationHelper {
    const val CHANNEL_REMINDERS_ID = "taskly_reminders_channel"
    const val CHANNEL_ALARMS_ID = "taskly_alarms_channel"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 1. Standard Reminders Channel
            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS_ID,
                "Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Standard notifications for scheduled task reminders"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(reminderChannel)

            // 2. Urgent / Alarm-style Channel
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val alarmChannel = NotificationChannel(
                CHANNEL_ALARMS_ID,
                "Task Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority alarm-style notifications for urgent tasks"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 500, 500, 500, 500)
                setSound(alarmSound, audioAttributes)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(alarmChannel)
        }
    }

    fun showNotification(
        context: Context,
        reminderId: Long,
        taskId: Long,
        title: String,
        description: String = "",
        isAlarm: Boolean = false,
        soundEnabled: Boolean = true,
        vibrationEnabled: Boolean = true
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = if (isAlarm) CHANNEL_ALARMS_ID else CHANNEL_REMINDERS_ID

        // 1. Open App Intent
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_TASK_ID", taskId)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            taskId.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Complete Action Intent
        val completeIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderActionReceiver.ACTION_COMPLETE
            putExtra(ReminderActionReceiver.EXTRA_TASK_ID, taskId)
            putExtra(ReminderActionReceiver.EXTRA_REMINDER_ID, reminderId)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            (reminderId * 10 + 1).toInt(),
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Snooze Action Intent (Default 10 minutes)
        val snoozeIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderActionReceiver.ACTION_SNOOZE
            putExtra(ReminderActionReceiver.EXTRA_TASK_ID, taskId)
            putExtra(ReminderActionReceiver.EXTRA_REMINDER_ID, reminderId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (reminderId * 10 + 2).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 4. Dismiss Action Intent
        val dismissIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderActionReceiver.ACTION_DISMISS
            putExtra(ReminderActionReceiver.EXTRA_REMINDER_ID, reminderId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            (reminderId * 10 + 3).toInt(),
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(description.ifBlank { "Task reminder" })
            .setStyle(if (description.isNotBlank()) NotificationCompat.BigTextStyle().bigText(description) else null)
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)
            .setPriority(if (isAlarm) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH)
            .setCategory(if (isAlarm) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .addAction(0, "✓ Complete", completePendingIntent)
            .addAction(0, "⏰ Snooze 10m", snoozePendingIntent)

        if (isAlarm) {
            builder.setFullScreenIntent(openPendingIntent, true)
            builder.addAction(0, "Dismiss", dismissPendingIntent)
        }

        if (!vibrationEnabled) {
            builder.setVibrate(longArrayOf(0))
        }

        if (!soundEnabled) {
            builder.setSilent(true)
        }

        notificationManager.notify(reminderId.toInt(), builder.build())
    }

    fun cancelNotification(context: Context, reminderId: Long) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(reminderId.toInt())
    }
}

