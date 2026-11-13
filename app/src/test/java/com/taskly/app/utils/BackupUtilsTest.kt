package com.taskly.app.utils

import com.taskly.app.data.entity.CategoryEntity
import com.taskly.app.data.entity.ReminderEntity
import com.taskly.app.data.entity.TaskEntity
import com.taskly.app.data.model.Priority
import com.taskly.app.data.model.ReminderType
import com.taskly.app.data.model.TaskStatus
import com.taskly.app.data.model.TaskWithDetails
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupUtilsTest {

    @Test
    fun testExportToJsonStructure() {
        val category = CategoryEntity(id = 1, name = "Work", iconName = "Work", colorHex = 0xFF4A90E2)
        val task = TaskEntity(
            id = 10,
            title = "Prepare presentation",
            description = "Slides for Monday",
            categoryId = 1,
            priority = Priority.HIGH,
            dueDate = 1770000000000L,
            status = TaskStatus.PENDING,
            voiceNotePath = "/data/user/0/com.taskly.app/files/voice_notes/vn_1.m4a",
            voiceNoteDurationMs = 15000L
        )
        val reminder = ReminderEntity(
            id = 100,
            taskId = 10,
            reminderDateTime = 1770000000000L,
            reminderType = ReminderType.ALARM
        )
        val taskWithDetails = TaskWithDetails(task = task, category = category, reminders = listOf(reminder))

        val jsonString = BackupUtils.exportToJson(listOf(taskWithDetails), listOf(category))

        val json = JSONObject(jsonString)
        assertEquals("Taskly", json.getString("app"))
        assertEquals(1, json.getInt("version"))
        assertTrue(json.has("categories"))
        assertTrue(json.has("tasks"))

        val tasksArray = json.getJSONArray("tasks")
        assertEquals(1, tasksArray.length())
        val taskObj = tasksArray.getJSONObject(0)
        assertEquals("Prepare presentation", taskObj.getString("title"))
        assertEquals("HIGH", taskObj.getString("priority"))
        assertEquals("/data/user/0/com.taskly.app/files/voice_notes/vn_1.m4a", taskObj.getString("voiceNotePath"))
        assertEquals(15000L, taskObj.getLong("voiceNoteDurationMs"))

        val remArray = taskObj.getJSONArray("reminders")
        assertEquals(1, remArray.length())
        assertEquals("ALARM", remArray.getJSONObject(0).getString("reminderType"))
    }
}
