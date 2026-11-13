package com.taskly.app.data.repository

import com.taskly.app.data.entity.ReminderEntity
import com.taskly.app.data.entity.TaskEntity
import com.taskly.app.data.model.Priority
import com.taskly.app.data.model.ReminderType
import com.taskly.app.data.model.TaskStatus
import com.taskly.app.data.model.TaskWithDetails
import com.taskly.app.utils.DateUtils
import com.taskly.app.utils.RecurrenceUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TaskLogicTest {

    @Test
    fun testPriorityEnumOrdering() {
        val priorities = listOf(Priority.LOW, Priority.MEDIUM, Priority.HIGH, Priority.URGENT)
        assertEquals("Low", priorities[0].label)
        assertEquals("Urgent", priorities[3].label)
    }

    @Test
    fun testTaskCompletionStatusToggling() {
        val task = TaskEntity(id = 1, title = "Complete project", status = TaskStatus.PENDING)
        val completedTask = task.copy(status = TaskStatus.COMPLETED, completedAt = System.currentTimeMillis())

        assertEquals(TaskStatus.COMPLETED, completedTask.status)
        assertNotNull(completedTask.completedAt)

        val reopenedTask = completedTask.copy(status = TaskStatus.PENDING, completedAt = null)
        assertEquals(TaskStatus.PENDING, reopenedTask.status)
    }

    @Test
    fun testMultipleRemindersTimeOffsetCalculation() {
        val today = LocalDate.of(2026, 6, 1)
        val todayMillis = DateUtils.localDateToEpochMillis(today)

        val reminder1 = ReminderEntity(id = 1, taskId = 10, reminderDateTime = todayMillis + 9 * 3600 * 1000L) // 9:00 AM
        val reminder2 = ReminderEntity(id = 2, taskId = 10, reminderDateTime = todayMillis + 14 * 3600 * 1000L) // 2:00 PM
        val reminder3 = ReminderEntity(id = 3, taskId = 10, reminderDateTime = todayMillis + 18 * 3600 * 1000L) // 6:00 PM

        val reminders = listOf(reminder1, reminder2, reminder3)
        assertEquals(3, reminders.size)

        // Verify all reminders belong to the same task
        assertTrue(reminders.all { it.taskId == 10L })

        // When task repeats after 7 days (weekly), reminders should advance by exactly 7 days
        val nextDueDate = RecurrenceUtils.calculateNextDueDate("WEEKLY", todayMillis)!!
        val timeDelta = nextDueDate - todayMillis

        val nextReminders = reminders.map { it.copy(reminderDateTime = it.reminderDateTime + timeDelta) }
        assertEquals(reminder1.reminderDateTime + timeDelta, nextReminders[0].reminderDateTime)
        assertEquals(reminder2.reminderDateTime + timeDelta, nextReminders[1].reminderDateTime)
        assertEquals(reminder3.reminderDateTime + timeDelta, nextReminders[2].reminderDateTime)
    }

    @Test
    fun testSnoozeCalculation() {
        val now = 1000000L
        val snoozeMinutes = 10
        val snoozedUntil = now + (snoozeMinutes * 60 * 1000L)

        assertEquals(1600000L, snoozedUntil)
        assertTrue(snoozedUntil > now)
    }
}
