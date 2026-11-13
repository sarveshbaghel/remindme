package com.taskly.app.data.model

import androidx.compose.ui.graphics.Color
import com.taskly.app.ui.theme.Error
import com.taskly.app.ui.theme.Success
import com.taskly.app.ui.theme.Warning

enum class Priority(val label: String, val color: Color) {
    LOW("Low", Success),
    MEDIUM("Medium", Color(0xFF4A90E2)),
    HIGH("High", Warning),
    URGENT("Urgent", Error)
}

enum class TaskStatus {
    PENDING,
    COMPLETED
}

enum class ReminderType {
    NOTIFICATION,
    ALARM
}

