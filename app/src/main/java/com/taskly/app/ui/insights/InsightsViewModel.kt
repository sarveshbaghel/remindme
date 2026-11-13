package com.taskly.app.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.taskly.app.data.dao.TaskHistoryDao
import com.taskly.app.data.entity.CategoryEntity
import com.taskly.app.data.model.TaskWithDetails
import com.taskly.app.data.repository.CategoryRepository
import com.taskly.app.data.repository.TaskRepository
import com.taskly.app.utils.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

data class CategoryStat(
    val categoryName: String,
    val colorHex: Long,
    val taskCount: Int,
    val percentage: Int
)

data class DayCompletionStat(
    val dayName: String, // Mon, Tue, etc.
    val completedCount: Int
)

data class InsightsUiState(
    val isWeeklyView: Boolean = true,
    val completionRate: Int = 0,
    val tasksCreatedCount: Int = 0,
    val tasksCompletedCount: Int = 0,
    val tasksPostponedCount: Int = 0,
    val overdueCount: Int = 0,
    val currentStreakDays: Int = 0,
    val mostProductiveDay: String = "N/A",
    val dayCompletionStats: List<DayCompletionStat> = emptyList(),
    val categoryStats: List<CategoryStat> = emptyList(),
    val isLoading: Boolean = true
)

private data class TasksOverviewData(
    val activeTasks: List<TaskWithDetails>,
    val completedTasks: List<TaskWithDetails>,
    val overdueTasks: List<TaskWithDetails>
)

class InsightsViewModel(
    private val taskRepository: TaskRepository,
    private val categoryRepository: CategoryRepository,
    private val taskHistoryDao: TaskHistoryDao
) : ViewModel() {

    private val _isWeeklyView = MutableStateFlow(true)
    val isWeeklyView: StateFlow<Boolean> = _isWeeklyView

    private val tasksOverviewFlow = combine(
        taskRepository.getAllActiveTasks(),
        taskRepository.getAllCompletedTasks(),
        taskRepository.getOverdueTasks()
    ) { active, completed, overdue ->
        TasksOverviewData(active, completed, overdue)
    }

    val uiState: StateFlow<InsightsUiState> = combine(
        tasksOverviewFlow,
        categoryRepository.allCategories,
        taskHistoryDao.getAllHistory(),
        _isWeeklyView
    ) { overview, categories, historyList, isWeekly ->

        val activeTasks = overview.activeTasks
        val completedTasks = overview.completedTasks
        val overdueTasks = overview.overdueTasks

        val now = System.currentTimeMillis()
        val today = LocalDate.now()
        val daysBack = if (isWeekly) 7L else 30L
        val cutoffEpoch = now - (daysBack * 24 * 60 * 60 * 1000L)

        // Filter items in timeframe
        val timeframeCompleted = completedTasks.filter { (it.task.completedAt ?: 0L) >= cutoffEpoch }
        val timeframeActive = activeTasks.filter { (it.task.createdAt) >= cutoffEpoch }

        val totalTimeframeTasks = timeframeActive.size + timeframeCompleted.size
        val completionRate = if (totalTimeframeTasks > 0) {
            ((timeframeCompleted.size.toFloat() / totalTimeframeTasks) * 100).toInt()
        } else 0

        // Postponed count from history
        val postponedCount = historyList.count { it.actionType == "POSTPONED" }

        // Day of Week completion distribution (last 7 days or all completed)
        val dayCounts = mutableMapOf<DayOfWeek, Int>()
        DayOfWeek.entries.forEach { dayCounts[it] = 0 }

        completedTasks.forEach { taskWithDetails ->
            val compTime = taskWithDetails.task.completedAt
            if (compTime != null) {
                val compDate = Instant.ofEpochMilli(compTime).atZone(DateUtils.zoneId).toLocalDate()
                dayCounts[compDate.dayOfWeek] = (dayCounts[compDate.dayOfWeek] ?: 0) + 1
            }
        }

        val dayCompletionList = listOf(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
        ).map { day ->
            DayCompletionStat(
                dayName = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(3),
                completedCount = dayCounts[day] ?: 0
            )
        }

        val mostProductiveDay = dayCounts.maxByOrNull { it.value }
            ?.let { if (it.value > 0) it.key.getDisplayName(TextStyle.FULL, Locale.getDefault()) else "None" }
            ?: "None"

        // Streak calculation: Consecutive days with at least 1 completed task
        val completionDates = completedTasks.mapNotNull {
            it.task.completedAt?.let { ts ->
                Instant.ofEpochMilli(ts).atZone(DateUtils.zoneId).toLocalDate()
            }
        }.toSet()

        var streak = 0
        var checkDate = today
        // If nothing completed today, check starting from yesterday
        if (!completionDates.contains(today)) {
            checkDate = today.minusDays(1)
        }
        while (completionDates.contains(checkDate)) {
            streak++
            checkDate = checkDate.minusDays(1)
        }

        // Category breakdown
        val totalAllTasks = activeTasks.size + completedTasks.size
        val categoryStats = if (totalAllTasks > 0) {
            val allTasks = activeTasks + completedTasks
            val groupedByCat = allTasks.groupBy { it.task.categoryId }

            categories.map { cat ->
                val count = groupedByCat[cat.id]?.size ?: 0
                val pct = ((count.toFloat() / totalAllTasks) * 100).toInt()
                CategoryStat(
                    categoryName = cat.name,
                    colorHex = cat.colorHex,
                    taskCount = count,
                    percentage = pct
                )
            }.filter { it.taskCount > 0 }
                .sortedByDescending { it.taskCount }
        } else {
            emptyList()
        }

        InsightsUiState(
            isWeeklyView = isWeekly,
            completionRate = completionRate,
            tasksCreatedCount = totalTimeframeTasks,
            tasksCompletedCount = timeframeCompleted.size,
            tasksPostponedCount = postponedCount,
            overdueCount = overdueTasks.size,
            currentStreakDays = streak,
            mostProductiveDay = mostProductiveDay,
            dayCompletionStats = dayCompletionList,
            categoryStats = categoryStats,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InsightsUiState())

    fun toggleView(isWeekly: Boolean) {
        _isWeeklyView.value = isWeekly
    }

    class Factory(
        private val taskRepository: TaskRepository,
        private val categoryRepository: CategoryRepository,
        private val taskHistoryDao: TaskHistoryDao
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return InsightsViewModel(taskRepository, categoryRepository, taskHistoryDao) as T
        }
    }
}
