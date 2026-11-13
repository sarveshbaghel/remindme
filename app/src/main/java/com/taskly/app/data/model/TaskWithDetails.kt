package com.taskly.app.data.model

import androidx.room.Embedded
import androidx.room.Relation
import com.taskly.app.data.entity.CategoryEntity
import com.taskly.app.data.entity.ReminderEntity
import com.taskly.app.data.entity.TaskEntity

data class TaskWithDetails(
    @Embedded
    val task: TaskEntity,

    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: CategoryEntity? = null,

    @Relation(
        parentColumn = "id",
        entityColumn = "taskId"
    )
    val reminders: List<ReminderEntity> = emptyList()
)

