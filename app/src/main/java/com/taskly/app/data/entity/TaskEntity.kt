package com.taskly.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.taskly.app.data.model.Priority
import com.taskly.app.data.model.TaskStatus

@Entity(
    tableName = "tasks",
    indices = [
        Index("dueDate"),
        Index("status"),
        Index("categoryId"),
        Index("archived")
    ]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val categoryId: Long? = null,
    val priority: Priority = Priority.MEDIUM,
    val dueDate: Long? = null, // Epoch millis at start of day (00:00:00 UTC/Local)
    val dueTime: Int? = null, // Minutes from midnight (e.g. 14:30 -> 870)
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val status: TaskStatus = TaskStatus.PENDING,
    val recurrenceRule: String? = null,
    val archived: Boolean = false,
    val voiceNotePath: String? = null,
    val voiceNoteDurationMs: Long? = null
)

