package com.taskly.app.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.taskly.app.data.entity.CategoryEntity
import com.taskly.app.data.model.Priority
import com.taskly.app.data.model.TaskStatus
import com.taskly.app.data.model.TaskWithDetails
import com.taskly.app.data.repository.CategoryRepository
import com.taskly.app.data.repository.TaskRepository
import com.taskly.app.utils.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TasksScreenUiState(
    val tasks: List<TaskWithDetails> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val currentFilter: TaskFilter = TaskFilter.ALL,
    val currentSort: TaskSort = TaskSort.DUE_DATE,
    val selectedCategoryId: Long? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

private data class TasksFilterParams(
    val filter: TaskFilter,
    val sort: TaskSort,
    val categoryId: Long?,
    val query: String
)

class TasksViewModel(
    private val taskRepository: TaskRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _currentFilter = MutableStateFlow(TaskFilter.ALL)
    val currentFilter: StateFlow<TaskFilter> = _currentFilter

    private val _currentSort = MutableStateFlow(TaskSort.DUE_DATE)
    val currentSort: StateFlow<TaskSort> = _currentSort

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val filterParamsFlow = combine(
        _currentFilter,
        _currentSort,
        _selectedCategoryId,
        _searchQuery
    ) { filter, sort, categoryId, query ->
        TasksFilterParams(filter, sort, categoryId, query)
    }

    val uiState: StateFlow<TasksScreenUiState> = combine(
        taskRepository.getAllActiveTasks(),
        taskRepository.getArchivedTasks(),
        categoryRepository.allCategories,
        filterParamsFlow
    ) { activeTasks, archivedTasks, categories, params ->

        val filter = params.filter
        val sort = params.sort
        val categoryId = params.categoryId
        val query = params.query

        val baseList = if (filter == TaskFilter.ARCHIVED) archivedTasks else activeTasks
        val todayStart = DateUtils.startOfDayEpoch()
        val todayEnd = DateUtils.endOfDayEpoch()

        // 1. Filter by status / date category
        val filteredByTab = when (filter) {
            TaskFilter.ALL -> baseList
            TaskFilter.TODAY -> baseList.filter {
                val due = it.task.dueDate
                due != null && due in todayStart..todayEnd
            }
            TaskFilter.UPCOMING -> baseList.filter {
                val due = it.task.dueDate
                it.task.status == TaskStatus.PENDING && (due != null && due > todayEnd)
            }
            TaskFilter.OVERDUE -> baseList.filter {
                val due = it.task.dueDate
                it.task.status == TaskStatus.PENDING && (due != null && due < todayStart)
            }
            TaskFilter.COMPLETED -> baseList.filter {
                it.task.status == TaskStatus.COMPLETED
            }
            TaskFilter.ARCHIVED -> baseList
        }

        // 2. Filter by category
        val filteredByCategory = if (categoryId != null) {
            filteredByTab.filter { it.task.categoryId == categoryId }
        } else {
            filteredByTab
        }

        // 3. Search query
        val filteredByQuery = if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            filteredByCategory.filter {
                it.task.title.lowercase().contains(q) || it.task.description.lowercase().contains(q)
            }
        } else {
            filteredByCategory
        }

        // 4. Sort
        val sortedList = when (sort) {
            TaskSort.DUE_DATE -> filteredByQuery.sortedWith(
                compareBy<TaskWithDetails> { it.task.status == TaskStatus.COMPLETED }
                    .thenBy { it.task.dueDate == null }
                    .thenBy { it.task.dueDate }
                    .thenBy { it.task.dueTime ?: Int.MAX_VALUE }
            )
            TaskSort.PRIORITY -> filteredByQuery.sortedWith(
                compareBy<TaskWithDetails> { it.task.status == TaskStatus.COMPLETED }
                    .thenByDescending {
                        when (it.task.priority) {
                            Priority.URGENT -> 4
                            Priority.HIGH -> 3
                            Priority.MEDIUM -> 2
                            Priority.LOW -> 1
                        }
                    }
                    .thenBy { it.task.dueDate ?: Long.MAX_VALUE }
            )
            TaskSort.CREATED_DATE -> filteredByQuery.sortedByDescending { it.task.createdAt }
            TaskSort.TITLE -> filteredByQuery.sortedBy { it.task.title.lowercase() }
        }

        TasksScreenUiState(
            tasks = sortedList,
            categories = categories,
            currentFilter = filter,
            currentSort = sort,
            selectedCategoryId = categoryId,
            searchQuery = query,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TasksScreenUiState())

    fun setFilter(filter: TaskFilter) {
        _currentFilter.value = filter
    }

    fun setSort(sort: TaskSort) {
        _currentSort.value = sort
    }

    fun setCategoryFilter(categoryId: Long?) {
        _selectedCategoryId.value = categoryId
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

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

    class Factory(
        private val taskRepository: TaskRepository,
        private val categoryRepository: CategoryRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TasksViewModel(taskRepository, categoryRepository) as T
        }
    }
}
