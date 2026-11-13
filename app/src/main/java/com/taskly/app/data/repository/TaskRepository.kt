package com.taskly.app.data.repository

import com.taskly.app.data.dao.ReminderDao
import com.taskly.app.data.dao.TaskDao
import com.taskly.app.data.dao.TaskHistoryDao
import com.taskly.app.data.entity.ReminderEntity
import com.taskly.app.data.entity.TaskEntity
import com.taskly.app.data.entity.TaskHistoryEntity
import com.taskly.app.data.model.ReminderType
import com.taskly.app.data.model.TaskStatus
import com.taskly.app.data.model.TaskWithDetails
import com.taskly.app.reminders.AlarmScheduler
import com.taskly.app.utils.DateUtils
import com.taskly.app.utils.RecurrenceUtils
import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val taskDao: TaskDao,
    private val reminderDao: ReminderDao,
    private val taskHistoryDao: TaskHistoryDao,
    private val alarmScheduler: AlarmScheduler? = null
) {
    fun getAllActiveTasks(): Flow<List<TaskWithDetails>> = taskDao.getAllActiveTasks()

    fun getTodayPendingTasks(): Flow<List<TaskWithDetails>> {
        val start = DateUtils.startOfDayEpoch()
        val end = DateUtils.endOfDayEpoch()
        return taskDao.getTodayPendingTasks(start, end)
    }

    fun getTodayCompletedTasks(): Flow<List<TaskWithDetails>> {
        val start = DateUtils.startOfDayEpoch()
        val end = DateUtils.endOfDayEpoch()
        return taskDao.getTodayCompletedTasks(start, end)
    }

    fun getOverdueTasks(): Flow<List<TaskWithDetails>> {
        val start = DateUtils.startOfDayEpoch()
        return taskDao.getOverdueTasks(start)
    }

    fun getUpcomingTasks(): Flow<List<TaskWithDetails>> {
        val end = DateUtils.endOfDayEpoch()
        return taskDao.getUpcomingTasks(end)
    }

    fun getTasksInDateRange(startDate: Long, endDate: Long): Flow<List<TaskWithDetails>> {
        return taskDao.getTasksInDateRange(startDate, endDate)
    }

    fun searchTasks(query: String): Flow<List<TaskWithDetails>> {
        return taskDao.searchTasks(query)
    }

    fun getArchivedTasks(): Flow<List<TaskWithDetails>> = taskDao.getArchivedTasks()

    fun getAllCompletedTasks(): Flow<List<TaskWithDetails>> = taskDao.getAllCompletedTasks()

    fun getTaskWithDetails(taskId: Long): Flow<TaskWithDetails?> = taskDao.getTaskWithDetails(taskId)

    suspend fun getTaskWithDetailsSync(taskId: Long): TaskWithDetails? = taskDao.getTaskWithDetailsSync(taskId)

    suspend fun insertTask(task: TaskEntity, reminders: List<ReminderEntity> = emptyList()): Long {
        val taskId = taskDao.insertTask(task)
        if (reminders.isNotEmpty()) {
            val remindersWithTaskId = reminders.map { it.copy(taskId = taskId) }
            val insertedIds = reminderDao.insertAll(remindersWithTaskId)

            // Schedule with AlarmManager
            if (alarmScheduler != null) {
                remindersWithTaskId.forEachIndexed { index, reminder ->
                    val id = insertedIds.getOrElse(index) { reminder.id }
                    if (reminder.reminderDateTime > System.currentTimeMillis() && reminder.isActive) {
                        alarmScheduler.schedule(
                            reminderId = id,
                            taskId = taskId,
                            triggerAtMillis = reminder.reminderDateTime,
                            isAlarm = reminder.reminderType == ReminderType.ALARM
                        )
                    }
                }
            }
        }

        taskHistoryDao.insertHistory(
            TaskHistoryEntity(
                taskId = taskId,
                actionType = "CREATED",
                details = "Task created with ${reminders.size} reminder(s)"
            )
        )
        return taskId
    }

    suspend fun updateTask(task: TaskEntity, reminders: List<ReminderEntity>? = null) {
        val updated = task.copy(updatedAt = System.currentTimeMillis())
        taskDao.updateTask(updated)

        if (reminders != null) {
            // Cancel existing alarms
            val existing = reminderDao.getRemindersForTaskSync(task.id)
            existing.forEach { alarmScheduler?.cancel(it.id) }

            reminderDao.deleteRemindersForTask(task.id)

            if (reminders.isNotEmpty()) {
                val remindersWithTaskId = reminders.map { it.copy(taskId = task.id) }
                val insertedIds = reminderDao.insertAll(remindersWithTaskId)

                // Schedule new alarms
                if (alarmScheduler != null) {
                    remindersWithTaskId.forEachIndexed { index, reminder ->
                        val id = insertedIds.getOrElse(index) { reminder.id }
                        val triggerTime = reminder.snoozedUntil ?: reminder.reminderDateTime
                        if (triggerTime > System.currentTimeMillis() && reminder.isActive) {
                            alarmScheduler.schedule(
                                reminderId = id,
                                taskId = task.id,
                                triggerAtMillis = triggerTime,
                                isAlarm = reminder.reminderType == ReminderType.ALARM
                            )
                        }
                    }
                }
            }
        }

        taskHistoryDao.insertHistory(
            TaskHistoryEntity(
                taskId = task.id,
                actionType = "EDITED",
                details = "Task details updated"
            )
        )
    }

    suspend fun toggleTaskCompletion(taskId: Long): Boolean {
        val current = taskDao.getTaskById(taskId) ?: return false
        val newStatus = if (current.status == TaskStatus.PENDING) TaskStatus.COMPLETED else TaskStatus.PENDING
        val completedAt = if (newStatus == TaskStatus.COMPLETED) System.currentTimeMillis() else null

        val updated = current.copy(
            status = newStatus,
            completedAt = completedAt,
            updatedAt = System.currentTimeMillis()
        )
        taskDao.updateTask(updated)

        if (newStatus == TaskStatus.COMPLETED) {
            // Cancel alarms & deactivate in DB
            val reminders = reminderDao.getRemindersForTaskSync(taskId)
            reminders.forEach { alarmScheduler?.cancel(it.id) }
            reminderDao.deactivateRemindersForTask(taskId)

            taskHistoryDao.insertHistory(
                TaskHistoryEntity(
                    taskId = taskId,
                    actionType = "COMPLETED",
                    details = "Task marked completed"
                )
            )

            // Section 8: Recurring Tasks Automation
            if (!current.recurrenceRule.isNullOrBlank()) {
                val nextDueDate = RecurrenceUtils.calculateNextDueDate(current.recurrenceRule, current.dueDate)
                if (nextDueDate != null) {
                    val originalDueDate = current.dueDate ?: DateUtils.todayEpochMillis()
                    val timeDeltaMillis = nextDueDate - originalDueDate

                    val nextTask = current.copy(
                        id = 0,
                        dueDate = nextDueDate,
                        status = TaskStatus.PENDING,
                        completedAt = null,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )

                    // Replicate reminders with adjusted time
                    val nextReminders = reminders.map { r ->
                        r.copy(
                            id = 0,
                            reminderDateTime = r.reminderDateTime + timeDeltaMillis,
                            isActive = true,
                            isTriggered = false,
                            snoozedUntil = null
                        )
                    }

                    val nextTaskId = insertTask(nextTask, nextReminders)
                    taskHistoryDao.insertHistory(
                        TaskHistoryEntity(
                            taskId = nextTaskId,
                            actionType = "RECURRENCE_GENERATED",
                            details = "Next recurrence generated for ${DateUtils.formatDueDate(nextDueDate)}"
                        )
                    )
                }
            }
        } else {
            taskHistoryDao.insertHistory(
                TaskHistoryEntity(
                    taskId = taskId,
                    actionType = "REOPENED",
                    details = "Task reopened"
                )
            )
        }
        return newStatus == TaskStatus.COMPLETED
    }

    suspend fun postponeTask(taskId: Long, daysToPostpone: Long = 1) {
        val task = taskDao.getTaskById(taskId) ?: return
        val currentDueDate = task.dueDate ?: DateUtils.todayEpochMillis()
        val currentLocalDate = DateUtils.epochMillisToLocalDate(currentDueDate)
        val newLocalDate = currentLocalDate.plusDays(daysToPostpone)
        val newDueDate = DateUtils.localDateToEpochMillis(newLocalDate)

        val updated = task.copy(
            dueDate = newDueDate,
            updatedAt = System.currentTimeMillis()
        )
        taskDao.updateTask(updated)

        // Shift active reminders forward
        val shiftMillis = daysToPostpone * 24 * 60 * 60 * 1000L
        val reminders = reminderDao.getRemindersForTaskSync(taskId)
        for (reminder in reminders) {
            alarmScheduler?.cancel(reminder.id)
            val newTime = reminder.reminderDateTime + shiftMillis
            val updatedReminder = reminder.copy(
                reminderDateTime = newTime,
                isTriggered = false,
                isActive = true,
                snoozedUntil = null
            )
            reminderDao.updateReminder(updatedReminder)
            if (newTime > System.currentTimeMillis()) {
                alarmScheduler?.schedule(
                    reminderId = reminder.id,
                    taskId = taskId,
                    triggerAtMillis = newTime,
                    isAlarm = reminder.reminderType == ReminderType.ALARM
                )
            }
        }

        taskHistoryDao.insertHistory(
            TaskHistoryEntity(
                taskId = taskId,
                actionType = "POSTPONED",
                details = "Postponed by $daysToPostpone day(s) to $newLocalDate"
            )
        )
    }

    suspend fun duplicateTask(taskId: Long): Long? {
        val original = taskDao.getTaskWithDetailsSync(taskId) ?: return null
        val newTask = original.task.copy(
            id = 0,
            title = "${original.task.title} (Copy)",
            createdAt = System.currentTimeMillis(),
            completedAt = null,
            updatedAt = System.currentTimeMillis(),
            status = TaskStatus.PENDING
        )
        val reminders = original.reminders.map { it.copy(id = 0, isTriggered = false, isActive = true) }
        return insertTask(newTask, reminders)
    }

    suspend fun archiveTask(taskId: Long) {
        val task = taskDao.getTaskById(taskId) ?: return
        val updated = task.copy(
            archived = true,
            updatedAt = System.currentTimeMillis()
        )
        taskDao.updateTask(updated)

        val reminders = reminderDao.getRemindersForTaskSync(taskId)
        reminders.forEach { alarmScheduler?.cancel(it.id) }
        reminderDao.deactivateRemindersForTask(taskId)

        taskHistoryDao.insertHistory(
            TaskHistoryEntity(
                taskId = taskId,
                actionType = "ARCHIVED",
                details = "Task archived"
            )
        )
    }

    suspend fun deleteTask(taskId: Long) {
        val task = taskDao.getTaskById(taskId)
        task?.voiceNotePath?.let { path ->
            val file = java.io.File(path)
            if (file.exists()) file.delete()
        }
        val reminders = reminderDao.getRemindersForTaskSync(taskId)
        reminders.forEach { alarmScheduler?.cancel(it.id) }
        taskDao.deleteTaskById(taskId)
    }

    suspend fun clearCompletedTasks() {
        taskDao.clearCompletedTasks()
    }

    suspend fun deleteAllTasks() {
        taskDao.deleteAllTasks()
    }
}
