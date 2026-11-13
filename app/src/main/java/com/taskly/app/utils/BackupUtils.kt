package com.taskly.app.utils

import com.taskly.app.data.entity.CategoryEntity
import com.taskly.app.data.entity.ReminderEntity
import com.taskly.app.data.entity.TaskEntity
import com.taskly.app.data.model.Priority
import com.taskly.app.data.model.ReminderType
import com.taskly.app.data.model.TaskStatus
import com.taskly.app.data.model.TaskWithDetails
import com.taskly.app.data.repository.CategoryRepository
import com.taskly.app.data.repository.TaskRepository
import org.json.JSONArray
import org.json.JSONObject

object BackupUtils {

    fun exportToJson(
        tasks: List<TaskWithDetails>,
        categories: List<CategoryEntity>
    ): String {
        val root = JSONObject()
        root.put("app", "Taskly")
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        // Categories Array
        val catArray = JSONArray()
        categories.forEach { cat ->
            val obj = JSONObject()
            obj.put("id", cat.id)
            obj.put("name", cat.name)
            obj.put("iconName", cat.iconName)
            obj.put("colorHex", cat.colorHex)
            catArray.put(obj)
        }
        root.put("categories", catArray)

        // Tasks Array
        val taskArray = JSONArray()
        tasks.forEach { item ->
            val t = item.task
            val taskObj = JSONObject()
            taskObj.put("title", t.title)
            taskObj.put("description", t.description)
            if (t.categoryId != null) taskObj.put("categoryId", t.categoryId)
            taskObj.put("priority", t.priority.name)
            if (t.dueDate != null) taskObj.put("dueDate", t.dueDate)
            if (t.dueTime != null) taskObj.put("dueTime", t.dueTime)
            taskObj.put("status", t.status.name)
            if (t.recurrenceRule != null) taskObj.put("recurrenceRule", t.recurrenceRule)
            taskObj.put("createdAt", t.createdAt)
            if (t.completedAt != null) taskObj.put("completedAt", t.completedAt)
            if (t.voiceNotePath != null) taskObj.put("voiceNotePath", t.voiceNotePath)
            if (t.voiceNoteDurationMs != null) taskObj.put("voiceNoteDurationMs", t.voiceNoteDurationMs)

            // Reminders for this task
            val remArray = JSONArray()
            item.reminders.forEach { r ->
                val remObj = JSONObject()
                remObj.put("reminderDateTime", r.reminderDateTime)
                remObj.put("reminderType", r.reminderType.name)
                remObj.put("soundEnabled", r.soundEnabled)
                remObj.put("vibrationEnabled", r.vibrationEnabled)
                remArray.put(remObj)
            }
            taskObj.put("reminders", remArray)

            taskArray.put(taskObj)
        }
        root.put("tasks", taskArray)

        return root.toString(2)
    }

    suspend fun importFromJson(
        jsonString: String,
        taskRepository: TaskRepository,
        categoryRepository: CategoryRepository
    ): Boolean {
        return try {
            val root = JSONObject(jsonString)
            if (!root.has("tasks")) return false

            // 1. Process Categories
            val categoryIdMap = mutableMapOf<Long, Long>() // oldId -> newId
            if (root.has("categories")) {
                val catArray = root.getJSONArray("categories")
                for (i in 0 until catArray.length()) {
                    val catObj = catArray.getJSONObject(i)
                    val oldId = catObj.getLong("id")
                    val name = catObj.getString("name")
                    val iconName = catObj.optString("iconName", "Folder")
                    val colorHex = catObj.optLong("colorHex", 0xFF635BFF)

                    val newId = categoryRepository.insertCategory(
                        CategoryEntity(name = name, iconName = iconName, colorHex = colorHex)
                    )
                    categoryIdMap[oldId] = newId
                }
            }

            // 2. Process Tasks
            val taskArray = root.getJSONArray("tasks")
            for (i in 0 until taskArray.length()) {
                val taskObj = taskArray.getJSONObject(i)
                val title = taskObj.getString("title")
                val description = taskObj.optString("description", "")
                val oldCatId = if (taskObj.has("categoryId")) taskObj.getLong("categoryId") else null
                val newCatId = if (oldCatId != null) (categoryIdMap[oldCatId] ?: oldCatId) else null

                val priorityStr = taskObj.optString("priority", "MEDIUM")
                val priority = try { Priority.valueOf(priorityStr) } catch (e: Exception) { Priority.MEDIUM }

                val dueDate = if (taskObj.has("dueDate")) taskObj.getLong("dueDate") else null
                val dueTime = if (taskObj.has("dueTime")) taskObj.getInt("dueTime") else null

                val statusStr = taskObj.optString("status", "PENDING")
                val status = try { TaskStatus.valueOf(statusStr) } catch (e: Exception) { TaskStatus.PENDING }

                val recurrenceRule = if (taskObj.has("recurrenceRule")) taskObj.getString("recurrenceRule") else null
                val createdAt = taskObj.optLong("createdAt", System.currentTimeMillis())
                val completedAt = if (taskObj.has("completedAt")) taskObj.getLong("completedAt") else null
                val voiceNotePath = if (taskObj.has("voiceNotePath")) taskObj.getString("voiceNotePath") else null
                val voiceNoteDurationMs = if (taskObj.has("voiceNoteDurationMs")) taskObj.getLong("voiceNoteDurationMs") else null

                val taskEntity = TaskEntity(
                    title = title,
                    description = description,
                    categoryId = newCatId,
                    priority = priority,
                    dueDate = dueDate,
                    dueTime = dueTime,
                    status = status,
                    recurrenceRule = recurrenceRule,
                    createdAt = createdAt,
                    completedAt = completedAt,
                    voiceNotePath = voiceNotePath,
                    voiceNoteDurationMs = voiceNoteDurationMs
                )

                val reminderEntities = mutableListOf<ReminderEntity>()
                if (taskObj.has("reminders")) {
                    val remArray = taskObj.getJSONArray("reminders")
                    for (j in 0 until remArray.length()) {
                        val remObj = remArray.getJSONObject(j)
                        val rTime = remObj.getLong("reminderDateTime")
                        val rTypeStr = remObj.optString("reminderType", "NOTIFICATION")
                        val rType = try { ReminderType.valueOf(rTypeStr) } catch (e: Exception) { ReminderType.NOTIFICATION }
                        val sound = remObj.optBoolean("soundEnabled", true)
                        val vibration = remObj.optBoolean("vibrationEnabled", true)

                        reminderEntities.add(
                            ReminderEntity(
                                taskId = 0,
                                reminderDateTime = rTime,
                                reminderType = rType,
                                soundEnabled = sound,
                                vibrationEnabled = vibration
                            )
                        )
                    }
                }

                taskRepository.insertTask(taskEntity, reminderEntities)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}

