package com.taskly.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.taskly.app.data.model.ReminderType

@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("taskId"),
        Index("reminderDateTime"),
        Index("isActive")
    ]
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val taskId: Long,
    val reminderDateTime: Long, // Epoch millis
    val reminderType: ReminderType = ReminderType.NOTIFICATION,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val isActive: Boolean = true,
    val isTriggered: Boolean = false,
    val snoozedUntil: Long? = null
)

