package com.taskly.app.ui.tasks

enum class TaskFilter(val label: String) {
    ALL("All"),
    TODAY("Today"),
    UPCOMING("Upcoming"),
    OVERDUE("Overdue"),
    COMPLETED("Completed"),
    ARCHIVED("Archived")
}

enum class TaskSort(val label: String) {
    DUE_DATE("Due Date"),
    PRIORITY("Priority"),
    CREATED_DATE("Newest First"),
    TITLE("Alphabetical")
}

