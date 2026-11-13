package com.taskly.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.taskly.app.TasklyApp
import com.taskly.app.notifications.NotificationHelper
import com.taskly.app.reminders.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_COMPLETE = "com.taskly.app.ACTION_COMPLETE"
        const val ACTION_SNOOZE = "com.taskly.app.ACTION_SNOOZE"
        const val ACTION_DISMISS = "com.taskly.app.ACTION_DISMISS"

        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)

        if (reminderId != -1L) {
            NotificationHelper.cancelNotification(context, reminderId)
        }

        val app = context.applicationContext as TasklyApp
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_COMPLETE -> {
                        if (taskId != -1L) {
                            // 1. Complete the task in repo
                            app.container.taskRepository.toggleTaskCompletion(taskId)

                            // 2. Cancel all scheduled alarms for this task
                            val remainingReminders = app.container.database.reminderDao().getRemindersForTaskSync(taskId)
                            val scheduler = AlarmScheduler(context)
                            remainingReminders.forEach { scheduler.cancel(it.id) }
                        }
                    }

                    ACTION_SNOOZE -> {
                        if (reminderId != -1L && taskId != -1L) {
                            val db = app.container.database
                            val reminder = db.reminderDao().getReminderById(reminderId)
                            if (reminder != null) {
                                val snoozeDurationMillis = 10 * 60 * 1000L // 10 minutes
                                val snoozedTime = System.currentTimeMillis() + snoozeDurationMillis

                                val updatedReminder = reminder.copy(
                                    snoozedUntil = snoozedTime,
                                    isTriggered = false,
                                    isActive = true
                                )
                                db.reminderDao().updateReminder(updatedReminder)

                                // Schedule the new alert
                                AlarmScheduler(context).schedule(
                                    reminderId = reminder.id,
                                    taskId = taskId,
                                    triggerAtMillis = snoozedTime,
                                    isAlarm = reminder.reminderType == com.taskly.app.data.model.ReminderType.ALARM
                                )
                            }
                        }
                    }

                    ACTION_DISMISS -> {
                        // Just notification cancellation, which happened at start of onReceive
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

