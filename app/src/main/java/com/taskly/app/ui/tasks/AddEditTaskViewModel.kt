package com.taskly.app.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.taskly.app.data.entity.CategoryEntity
import com.taskly.app.data.entity.ReminderEntity
import com.taskly.app.data.entity.TaskEntity
import com.taskly.app.data.model.Priority
import com.taskly.app.data.model.ReminderType
import com.taskly.app.data.repository.CategoryRepository
import com.taskly.app.data.repository.TaskRepository
import com.taskly.app.utils.DateUtils
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

data class ReminderDraft(
    val id: Long = 0,
    val reminderDateTime: Long,
    val reminderType: ReminderType = ReminderType.NOTIFICATION
)

class AddEditTaskViewModel(
    private val taskRepository: TaskRepository,
    private val categoryRepository: CategoryRepository,
    private val initialTaskId: Long = -1L,
    private val initialDateMillis: Long? = null
) : ViewModel() {

    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var title = androidx.compose.runtime.mutableStateOf("")
    var description = androidx.compose.runtime.mutableStateOf("")
    var selectedCategoryId = androidx.compose.runtime.mutableStateOf<Long?>(null)
    var selectedPriority = androidx.compose.runtime.mutableStateOf(Priority.MEDIUM)
    var selectedDueDate = androidx.compose.runtime.mutableStateOf<Long?>(initialDateMillis ?: DateUtils.todayEpochMillis())
    var selectedDueTime = androidx.compose.runtime.mutableStateOf<Int?>(DateUtils.timeToMinutes(LocalTime.of(9, 0)))
    var selectedRecurrence = androidx.compose.runtime.mutableStateOf<String?>(null)
    var reminderDrafts = androidx.compose.runtime.mutableStateListOf<ReminderDraft>()

    val isEditMode = initialTaskId > 0L
    var isSaving = androidx.compose.runtime.mutableStateOf(false)

    init {
        if (isEditMode) {
            viewModelScope.launch {
                val taskWithDetails = taskRepository.getTaskWithDetailsSync(initialTaskId)
                if (taskWithDetails != null) {
                    val task = taskWithDetails.task
                    title.value = task.title
                    description.value = task.description
                    selectedCategoryId.value = task.categoryId
                    selectedPriority.value = task.priority
                    selectedDueDate.value = task.dueDate
                    selectedDueTime.value = task.dueTime
                    selectedRecurrence.value = task.recurrenceRule

                    reminderDrafts.clear()
                    reminderDrafts.addAll(
                        taskWithDetails.reminders.map {
                            ReminderDraft(
                                id = it.id,
                                reminderDateTime = it.reminderDateTime,
                                reminderType = it.reminderType
                            )
                        }
                    )
                }
            }
        }
    }

    fun addReminder(dateTimeMillis: Long, type: ReminderType = ReminderType.NOTIFICATION) {
        reminderDrafts.add(ReminderDraft(reminderDateTime = dateTimeMillis, reminderType = type))
    }

    fun removeReminder(index: Int) {
        if (index in reminderDrafts.indices) {
            reminderDrafts.removeAt(index)
        }
    }

    fun addCustomCategory(name: String, icon: String = "Folder", colorHex: Long = 0xFF635BFF) {
        viewModelScope.launch {
            val newId = categoryRepository.insertCategory(
                CategoryEntity(name = name, iconName = icon, colorHex = colorHex)
            )
            selectedCategoryId.value = newId
        }
    }

    fun saveTask(onSaved: () -> Unit) {
        val trimmedTitle = title.value.trim()
        if (trimmedTitle.isBlank() || isSaving.value) return

        isSaving.value = true
        viewModelScope.launch {
            val task = TaskEntity(
                id = if (isEditMode) initialTaskId else 0,
                title = trimmedTitle,
                description = description.value.trim(),
                categoryId = selectedCategoryId.value,
                priority = selectedPriority.value,
                dueDate = selectedDueDate.value,
                dueTime = selectedDueTime.value,
                recurrenceRule = selectedRecurrence.value
            )

            val reminderEntities = reminderDrafts.map { draft ->
                ReminderEntity(
                    id = draft.id,
                    taskId = task.id,
                    reminderDateTime = draft.reminderDateTime,
                    reminderType = draft.reminderType
                )
            }

            if (isEditMode) {
                taskRepository.updateTask(task, reminderEntities)
            } else {
                taskRepository.insertTask(task, reminderEntities)
            }

            isSaving.value = false
            onSaved()
        }
    }

    class Factory(
        private val taskRepository: TaskRepository,
        private val categoryRepository: CategoryRepository,
        private val taskId: Long = -1L,
        private val dateMillis: Long? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddEditTaskViewModel(taskRepository, categoryRepository, taskId, dateMillis) as T
        }
    }
}
