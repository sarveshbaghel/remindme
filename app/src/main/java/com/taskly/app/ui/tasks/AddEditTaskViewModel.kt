package com.taskly.app.ui.tasks

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.taskly.app.audio.AudioRecorder
import com.taskly.app.data.entity.CategoryEntity
import com.taskly.app.data.entity.ReminderEntity
import com.taskly.app.data.entity.TaskEntity
import com.taskly.app.data.model.Priority
import com.taskly.app.data.model.ReminderType
import com.taskly.app.data.repository.CategoryRepository
import com.taskly.app.data.repository.TaskRepository
import com.taskly.app.utils.DateUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalTime

data class ReminderDraft(
    val id: Long = 0,
    val reminderDateTime: Long,
    val reminderType: ReminderType = ReminderType.NOTIFICATION
)

class AddEditTaskViewModel(
    private val context: Context,
    private val taskRepository: TaskRepository,
    private val categoryRepository: CategoryRepository,
    private val initialTaskId: Long = -1L,
    private val initialDateMillis: Long? = null
) : ViewModel() {

    private val audioRecorder = AudioRecorder(context)
    private var recordingTimerJob: Job? = null
    private var activeRecordingFile: File? = null

    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var title = mutableStateOf("")
    var description = mutableStateOf("")
    var selectedCategoryId = mutableStateOf<Long?>(null)
    var selectedPriority = mutableStateOf(Priority.MEDIUM)
    var selectedDueDate = mutableStateOf<Long?>(initialDateMillis ?: DateUtils.todayEpochMillis())
    var selectedDueTime = mutableStateOf<Int?>(DateUtils.timeToMinutes(LocalTime.of(9, 0)))
    var selectedRecurrence = mutableStateOf<String?>(null)
    var reminderDrafts = mutableStateListOf<ReminderDraft>()

    // Voice note attachment state
    var voiceNotePath = mutableStateOf<String?>(null)
    var voiceNoteDurationMs = mutableStateOf<Long?>(null)

    // Active recording state
    var isRecording = mutableStateOf(false)
    var recordingDurationSeconds = mutableStateOf(0)

    val isEditMode = initialTaskId > 0L
    var isSaving = mutableStateOf(false)

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
                    voiceNotePath.value = task.voiceNotePath
                    voiceNoteDurationMs.value = task.voiceNoteDurationMs

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

    fun startVoiceRecording(): Boolean {
        if (isRecording.value) return false
        val voiceNotesDir = File(context.filesDir, "voice_notes")
        if (!voiceNotesDir.exists()) voiceNotesDir.mkdirs()

        val file = File(voiceNotesDir, "vn_${System.currentTimeMillis()}.m4a")
        activeRecordingFile = file

        val started = audioRecorder.start(file)
        if (started) {
            isRecording.value = true
            recordingDurationSeconds.value = 0
            startRecordingTimer()
        }
        return started
    }

    fun stopVoiceRecording() {
        if (!isRecording.value) return
        stopRecordingTimer()
        val duration = audioRecorder.stop()
        isRecording.value = false

        activeRecordingFile?.let { file ->
            if (file.exists() && file.length() > 0) {
                // Delete previous voice note if replacing
                voiceNotePath.value?.let { oldPath ->
                    if (oldPath != file.absolutePath) {
                        val oldFile = File(oldPath)
                        if (oldFile.exists()) oldFile.delete()
                    }
                }
                voiceNotePath.value = file.absolutePath
                voiceNoteDurationMs.value = duration
            }
        }
        activeRecordingFile = null
    }

    fun cancelVoiceRecording() {
        if (!isRecording.value) return
        stopRecordingTimer()
        audioRecorder.cancel(activeRecordingFile)
        isRecording.value = false
        recordingDurationSeconds.value = 0
        activeRecordingFile = null
    }

    fun removeVoiceNote() {
        voiceNotePath.value?.let { path ->
            val file = File(path)
            if (file.exists()) file.delete()
        }
        voiceNotePath.value = null
        voiceNoteDurationMs.value = null
    }

    private fun startRecordingTimer() {
        recordingTimerJob?.cancel()
        recordingTimerJob = viewModelScope.launch {
            while (isActive && isRecording.value) {
                delay(1000)
                recordingDurationSeconds.value++
            }
        }
    }

    private fun stopRecordingTimer() {
        recordingTimerJob?.cancel()
        recordingTimerJob = null
    }

    fun appendDictatedText(transcribedText: String) {
        if (transcribedText.isBlank()) return
        val current = description.value.trim()
        description.value = if (current.isBlank()) {
            transcribedText.trim()
        } else {
            "$current\n${transcribedText.trim()}"
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
                recurrenceRule = selectedRecurrence.value,
                voiceNotePath = voiceNotePath.value,
                voiceNoteDurationMs = voiceNoteDurationMs.value
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

    override fun onCleared() {
        super.onCleared()
        if (isRecording.value) {
            cancelVoiceRecording()
        }
    }

    class Factory(
        private val context: Context,
        private val taskRepository: TaskRepository,
        private val categoryRepository: CategoryRepository,
        private val taskId: Long = -1L,
        private val dateMillis: Long? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddEditTaskViewModel(context, taskRepository, categoryRepository, taskId, dateMillis) as T
        }
    }
}
