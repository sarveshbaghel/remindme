package com.taskly.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "task_history",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("taskId"), Index("timestamp")]
)
data class TaskHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val taskId: Long,
    val actionType: String, // CREATED, COMPLETED, REOPENED, POSTPONED, EDITED, DELETED
    val timestamp: Long = System.currentTimeMillis(),
    val details: String = ""
)

