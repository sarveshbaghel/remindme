package com.taskly.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.taskly.app.data.preferences.AppPreferences
import com.taskly.app.data.preferences.ThemeMode
import com.taskly.app.data.preferences.UserPreferences
import com.taskly.app.data.repository.CategoryRepository
import com.taskly.app.data.repository.TaskRepository
import com.taskly.app.utils.BackupUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val userPreferences: UserPreferences,
    private val taskRepository: TaskRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    val preferences: StateFlow<AppPreferences> = userPreferences.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppPreferences())

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            userPreferences.setThemeMode(mode)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setNotificationsEnabled(enabled)
        }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setVibrationEnabled(enabled)
        }
    }

    fun setDefaultSnooze(minutes: Int) {
        viewModelScope.launch {
            userPreferences.setDefaultSnooze(minutes)
        }
    }

    fun setDefaultPriority(priority: String) {
        viewModelScope.launch {
            userPreferences.setDefaultPriority(priority)
        }
    }

    fun exportData(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val tasks = taskRepository.getAllActiveTasks().first() + taskRepository.getAllCompletedTasks().first()
            val categories = categoryRepository.allCategories.first()
            val json = BackupUtils.exportToJson(tasks, categories)
            onResult(json)
        }
    }

    fun importData(jsonString: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = BackupUtils.importFromJson(jsonString, taskRepository, categoryRepository)
            onResult(success)
        }
    }

    fun clearCompletedTasks() {
        viewModelScope.launch {
            taskRepository.clearCompletedTasks()
        }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            taskRepository.deleteAllTasks()
        }
    }

    class Factory(
        private val userPreferences: UserPreferences,
        private val taskRepository: TaskRepository,
        private val categoryRepository: CategoryRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(userPreferences, taskRepository, categoryRepository) as T
        }
    }
}

