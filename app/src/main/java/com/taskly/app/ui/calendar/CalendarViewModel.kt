package com.taskly.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.taskly.app.data.model.TaskWithDetails
import com.taskly.app.data.repository.TaskRepository
import com.taskly.app.utils.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class CalendarUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val tasksByDate: Map<LocalDate, List<TaskWithDetails>> = emptyMap(),
    val tasksForSelectedDate: List<TaskWithDetails> = emptyList(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    val uiState: StateFlow<CalendarUiState> = _currentMonth.flatMapLatest { month ->
        val startOfMonth = month.atDay(1)
        val endOfMonth = month.atEndOfMonth()
        val startEpoch = DateUtils.localDateToEpochMillis(startOfMonth)
        val endEpoch = DateUtils.endOfDayEpoch(DateUtils.localDateToEpochMillis(endOfMonth))

        combine(
            taskRepository.getTasksInDateRange(startEpoch, endEpoch),
            _selectedDate
        ) { tasks, selected ->
            val grouped = tasks.groupBy { taskItem ->
                taskItem.task.dueDate?.let { DateUtils.epochMillisToLocalDate(it) } ?: LocalDate.MIN
            }

            CalendarUiState(
                currentMonth = month,
                selectedDate = selected,
                tasksByDate = grouped,
                tasksForSelectedDate = grouped[selected] ?: emptyList(),
                isLoading = false
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CalendarUiState())

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        if (YearMonth.from(date) != _currentMonth.value) {
            _currentMonth.value = YearMonth.from(date)
        }
    }

    fun previousMonth() {
        _currentMonth.value = _currentMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        _currentMonth.value = _currentMonth.value.plusMonths(1)
    }

    fun goToToday() {
        val today = LocalDate.now()
        _selectedDate.value = today
        _currentMonth.value = YearMonth.from(today)
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

    class Factory(private val taskRepository: TaskRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CalendarViewModel(taskRepository) as T
        }
    }
}

