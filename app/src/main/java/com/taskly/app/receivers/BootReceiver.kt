package com.taskly.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.taskly.app.TasklyApp
import com.taskly.app.data.model.ReminderType
import com.taskly.app.reminders.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_TIME_CHANGED
        ) {
            val app = context.applicationContext as TasklyApp
            val pendingResult = goAsync()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = app.container.database
                    val activeReminders = db.reminderDao().getAllPendingActiveReminders()
                    val scheduler = AlarmScheduler(context)
                    val now = System.currentTimeMillis()

                    for (reminder in activeReminders) {
                        val triggerTime = reminder.snoozedUntil ?: reminder.reminderDateTime
                        if (triggerTime > now) {
                            scheduler.schedule(
                                reminderId = reminder.id,
                                taskId = reminder.taskId,
                                triggerAtMillis = triggerTime,
                                isAlarm = reminder.reminderType == ReminderType.ALARM
                            )
                        }
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}

