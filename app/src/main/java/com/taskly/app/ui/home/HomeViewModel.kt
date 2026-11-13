package com.taskly.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.taskly.app.data.model.TaskWithDetails
import com.taskly.app.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val pendingToday: List<TaskWithDetails> = emptyList(),
    val completedToday: List<TaskWithDetails> = emptyList(),
    val overdue: List<TaskWithDetails> = emptyList(),
    val upcoming: List<TaskWithDetails> = emptyList(),
    val totalToday: Int = 0,
    val completedCount: Int = 0,
    val pendingCount: Int = 0,
    val overdueCount: Int = 0,
    val completionPercentage: Int = 0,
    val isLoading: Boolean = true
)

class HomeViewModel(
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val uiState: StateFlow<HomeUiState> = combine(
        taskRepository.getTodayPendingTasks(),
        taskRepository.getTodayCompletedTasks(),
        taskRepository.getOverdueTasks(),
        taskRepository.getUpcomingTasks()
    ) { pending, completed, overdue, upcoming ->
        val totalToday = pending.size + completed.size
        val completionPct = if (totalToday > 0) ((completed.size.toFloat() / totalToday) * 100).toInt() else 0

        HomeUiState(
            pendingToday = pending,
            completedToday = completed,
            overdue = overdue,
            upcoming = upcoming,
            totalToday = totalToday,
            completedCount = completed.size,
            pendingCount = pending.size,
            overdueCount = overdue.size,
            completionPercentage = completionPct,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun toggleTaskCompletion(taskId: Long) {
        viewModelScope.launch {
            taskRepository.toggleTaskCompletion(taskId)
        }
    }

    fun postponeTask(taskId: Long, days: Long = 1) {
        viewModelScope.launch {
            taskRepository.postponeTask(taskId, days)
        }
    }

    fun duplicateTask(taskId: Long) {
        viewModelScope.launch {
            taskRepository.duplicateTask(taskId)
        }
    }

    fun archiveTask(taskId: Long) {
        viewModelScope.launch {
            taskRepository.archiveTask(taskId)
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            taskRepository.deleteTask(taskId)
        }
    }

    class Factory(private val taskRepository: TaskRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(taskRepository) as T
        }
    }
}

