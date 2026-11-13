package com.taskly.app.di

import android.content.Context
import com.taskly.app.data.database.TasklyDatabase
import com.taskly.app.data.preferences.UserPreferences
import com.taskly.app.data.repository.CategoryRepository
import com.taskly.app.data.repository.TaskRepository
import com.taskly.app.reminders.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(val context: Context) {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val alarmScheduler: AlarmScheduler by lazy {
        AlarmScheduler(context)
    }

    val userPreferences: UserPreferences by lazy {
        UserPreferences(context)
    }

    val database: TasklyDatabase by lazy {
        TasklyDatabase.getDatabase(context, applicationScope)
    }

    val taskRepository: TaskRepository by lazy {
        TaskRepository(
            taskDao = database.taskDao(),
            reminderDao = database.reminderDao(),
            taskHistoryDao = database.taskHistoryDao(),
            alarmScheduler = alarmScheduler
        )
    }

    val categoryRepository: CategoryRepository by lazy {
        CategoryRepository(database.categoryDao())
    }
}
