package com.taskly.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.taskly.app.TasklyApp
import com.taskly.app.data.model.ReminderType
import com.taskly.app.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_IS_ALARM = "extra_is_alarm"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        val isAlarm = intent.getBooleanExtra(EXTRA_IS_ALARM, false)

        if (reminderId == -1L || taskId == -1L) return

        val pendingResult = goAsync()
        val app = context.applicationContext as TasklyApp

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = app.container.database
                val reminder = db.reminderDao().getReminderById(reminderId)
                val task = db.taskDao().getTaskById(taskId)

                if (task != null && reminder != null && reminder.isActive) {
                    NotificationHelper.showNotification(
                        context = context,
                        reminderId = reminderId,
                        taskId = taskId,
                        title = task.title,
                        description = task.description,
                        isAlarm = reminder.reminderType == ReminderType.ALARM || isAlarm,
                        soundEnabled = reminder.soundEnabled,
                        vibrationEnabled = reminder.vibrationEnabled
                    )

                    // Mark triggered
                    db.reminderDao().updateReminder(
                        reminder.copy(isTriggered = true)
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

