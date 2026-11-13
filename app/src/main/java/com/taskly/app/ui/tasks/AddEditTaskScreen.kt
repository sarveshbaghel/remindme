package com.taskly.app.ui.tasks

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.taskly.app.data.model.Priority
import com.taskly.app.data.model.ReminderType
import com.taskly.app.ui.components.CategoryIconHelper
import com.taskly.app.ui.components.VoiceNotePlayer
import com.taskly.app.ui.components.VoiceNoteRecorderBar
import com.taskly.app.utils.DateUtils
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditTaskScreen(
    viewModel: AddEditTaskViewModel,
    onBack: () -> Unit
) {
    val categories by viewModel.categories.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }

    var pendingAudioAction by remember { mutableStateOf<String?>(null) } // "DICTATE" or "RECORD"

    // Speech to Text launcher
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                viewModel.appendDictatedText(spokenText)
            }
        }
    }

    val launchSpeechRecognition: () -> Unit = {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to dictate description...")
            }
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Speech recognition is not supported on this device", Toast.LENGTH_SHORT).show()
        }
    }

    // Microphone permission launcher
    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            when (pendingAudioAction) {
                "DICTATE" -> launchSpeechRecognition()
                "RECORD" -> {
                    val started = viewModel.startVoiceRecording()
                    if (!started) {
                        Toast.makeText(context, "Could not start audio recorder", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            Toast.makeText(context, "Microphone permission is required for voice features", Toast.LENGTH_SHORT).show()
        }
        pendingAudioAction = null
    }

    val checkPermissionAndExecute: (String) -> Unit = { action ->
        pendingAudioAction = action
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            when (action) {
                "DICTATE" -> launchSpeechRecognition()
                "RECORD" -> {
                    val started = viewModel.startVoiceRecording()
                    if (!started) {
                        Toast.makeText(context, "Could not start audio recorder", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            pendingAudioAction = null
        } else {
            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = viewModel.selectedDueDate.value ?: System.currentTimeMillis()
    )

    val currentTime = viewModel.selectedDueTime.value?.let { DateUtils.minutesToTime(it) } ?: LocalTime.of(9, 0)
    val timePickerState = rememberTimePickerState(
        initialHour = currentTime.hour,
        initialMinute = currentTime.minute,
        is24Hour = false
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (viewModel.isEditMode) "Edit Task" else "New Task",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.saveTask(onSaved = onBack) },
                        enabled = viewModel.title.value.isNotBlank() && !viewModel.isSaving.value,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text("Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Task Title
            OutlinedTextField(
                value = viewModel.title.value,
                onValueChange = { viewModel.title.value = it },
                label = { Text("Task Title") },
                placeholder = { Text("What needs to be done?") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 3,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Task Description Header with Dictate and Voice Note actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Description",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Voice to Text (Dictate)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.clickable { checkPermissionAndExecute("DICTATE") }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Mic,
                                contentDescription = "Dictate",
                                modifier = Modifier.size(15.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Dictate",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Voice Note (Record Audio)
                    if (!viewModel.isRecording.value && viewModel.voiceNotePath.value == null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.clickable { checkPermissionAndExecute("RECORD") }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.GraphicEq,
                                    contentDescription = "Voice Note",
                                    modifier = Modifier.size(15.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Voice Note",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Task Description TextField
            OutlinedTextField(
                value = viewModel.description.value,
                onValueChange = { viewModel.description.value = it },
                placeholder = { Text("Add notes, subtasks, or links...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6,
                shape = RoundedCornerShape(12.dp)
            )

            // Voice Note Recorder Bar (Active recording)
            if (viewModel.isRecording.value) {
                Spacer(modifier = Modifier.height(8.dp))
                VoiceNoteRecorderBar(
                    durationSeconds = viewModel.recordingDurationSeconds.value,
                    onCancel = { viewModel.cancelVoiceRecording() },
                    onDone = { viewModel.stopVoiceRecording() }
                )
            }

            // Voice Note Player (Attached voice note)
            viewModel.voiceNotePath.value?.let { path ->
                Spacer(modifier = Modifier.height(8.dp))
                VoiceNotePlayer(
                    filePath = path,
                    totalDurationMs = viewModel.voiceNoteDurationMs.value,
                    onDelete = { viewModel.removeVoiceNote() }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Due Date & Time Section
            Text(
                "Date & Time",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Date Button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showDatePicker = true }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = DateUtils.formatDueDate(viewModel.selectedDueDate.value),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Time Button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showTimePicker = true }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (viewModel.selectedDueTime.value != null)
                                DateUtils.formatDueTime(viewModel.selectedDueTime.value)
                            else "Set Time",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Priority Section
            Text(
                "Priority",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Priority.entries.forEach { priority ->
                    val isSelected = viewModel.selectedPriority.value == priority
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectedPriority.value = priority },
                        label = { Text(priority.label) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(priority.color)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = priority.color.copy(alpha = 0.15f),
                            selectedLabelColor = priority.color
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Category Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Category",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = { showCategoryDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New")
                }
            }
            Spacer(modifier = Modifier.height(6.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // "No Category" chip
                FilterChip(
                    selected = viewModel.selectedCategoryId.value == null,
                    onClick = { viewModel.selectedCategoryId.value = null },
                    label = { Text("None") }
                )

                categories.forEach { cat ->
                    val isSelected = viewModel.selectedCategoryId.value == cat.id
                    val catColor = Color(cat.colorHex)
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectedCategoryId.value = cat.id },
                        label = { Text(cat.name) },
                        leadingIcon = {
                            Icon(
                                imageVector = CategoryIconHelper.getIcon(cat.iconName),
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = catColor
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = catColor.copy(alpha = 0.15f),
                            selectedLabelColor = catColor
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Reminders Section (Smart Multiple Reminders)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Reminders",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = { showReminderDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Reminder")
                }
            }

            if (viewModel.reminderDrafts.isEmpty()) {
                Text(
                    "No reminders set. Add reminders to get alerted before the due date.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                viewModel.reminderDrafts.forEachIndexed { index, draft ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (draft.reminderType == ReminderType.ALARM)
                                        Icons.Outlined.Alarm else Icons.Outlined.Notifications,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = DateUtils.formatDateTime(draft.reminderDateTime),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "(${draft.reminderType.name.lowercase().replaceFirstChar { it.uppercase() }})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { viewModel.removeReminder(index) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Recurrence Section
            Text(
                "Repeat",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            val recurrenceOptions = listOf(
                null to "Does not repeat",
                "DAILY" to "Every day",
                "WEEKDAYS" to "Weekdays (Mon-Fri)",
                "WEEKLY" to "Every week",
                "MONTHLY" to "Every month"
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                recurrenceOptions.forEach { (rule, label) ->
                    val isSelected = viewModel.selectedRecurrence.value == rule
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectedRecurrence.value = rule },
                        label = { Text(label) },
                        leadingIcon = if (rule != null) {
                            {
                                Icon(
                                    Icons.Outlined.Repeat,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        } else null
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // Material 3 DatePickerDialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedUtc = datePickerState.selectedDateMillis
                        if (selectedUtc != null) {
                            // Convert UTC date to local start of day
                            val localDate = Instant.ofEpochMilli(selectedUtc)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            viewModel.selectedDueDate.value = DateUtils.localDateToEpochMillis(localDate)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // TimePicker Dialog
    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val minutes = timePickerState.hour * 60 + timePickerState.minute
                        viewModel.selectedDueTime.value = minutes
                        showTimePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TimePicker(state = timePickerState)
                }
            }
        )
    }

    // Quick Add Reminder Dialog
    if (showReminderDialog) {
        var reminderType by remember { mutableStateOf(ReminderType.NOTIFICATION) }

        val dueDate = viewModel.selectedDueDate.value ?: DateUtils.todayEpochMillis()
        val dueTime = viewModel.selectedDueTime.value ?: (9 * 60)
        val taskDateTime = DateUtils.combineDateAndTime(dueDate, dueTime)

        AlertDialog(
            onDismissRequest = { showReminderDialog = false },
            title = { Text("Add Reminder") },
            text = {
                Column {
                    Text("Choose when to be reminded:")
                    Spacer(modifier = Modifier.height(12.dp))

                    val quickTimes = listOf(
                        "At time of event" to taskDateTime,
                        "10 minutes before" to (taskDateTime - 10 * 60 * 1000L),
                        "30 minutes before" to (taskDateTime - 30 * 60 * 1000L),
                        "1 hour before" to (taskDateTime - 60 * 60 * 1000L),
                        "1 day before (9:00 AM)" to (DateUtils.startOfDayEpoch(taskDateTime) - 24 * 60 * 60 * 1000L + 9 * 60 * 60 * 1000L)
                    )

                    quickTimes.forEach { (label, timeMillis) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    viewModel.addReminder(timeMillis, reminderType)
                                    showReminderDialog = false
                                }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Reminder Type:", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = reminderType == ReminderType.NOTIFICATION,
                            onClick = { reminderType = ReminderType.NOTIFICATION },
                            label = { Text("Notification") }
                        )
                        FilterChip(
                            selected = reminderType == ReminderType.ALARM,
                            onClick = { reminderType = ReminderType.ALARM },
                            label = { Text("Alarm") }
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showReminderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // New Category Dialog
    if (showCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text("Create Category") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("Category Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            viewModel.addCustomCategory(newCategoryName.trim())
                            newCategoryName = ""
                            showCategoryDialog = false
                        }
                    },
                    enabled = newCategoryName.isNotBlank()
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCategoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

