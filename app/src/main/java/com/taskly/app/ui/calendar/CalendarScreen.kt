package com.taskly.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskly.app.data.model.TaskWithDetails
import com.taskly.app.ui.components.TaskCard
import com.taskly.app.ui.tasks.TaskDetailSheet
import com.taskly.app.utils.DateUtils
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    onEditTask: (Long) -> Unit = {},
    onAddTaskForDate: (Long) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTaskForDetails by remember { mutableStateOf<TaskWithDetails?>(null) }
    val detailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy") }
    val selectedDateFormatted = remember(state.selectedDate) {
        state.selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM"))
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(14.dp))

            // Calendar Navigation Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = state.currentMonth.format(monthFormatter),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = { viewModel.goToToday() },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Today", style = MaterialTheme.typography.labelMedium)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = { viewModel.previousMonth() }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Month")
                    }

                    IconButton(
                        onClick = { viewModel.nextMonth() }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Month")
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Month Grid Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    // Day of Week Header Row (Mon, Tue, Wed, Thu, Fri, Sat, Sun)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        val daysOfWeek = listOf(
                            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
                        )
                        daysOfWeek.forEach { day ->
                            Text(
                                text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(3),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(38.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Month Grid Calculations
                    val firstDayOfMonth = state.currentMonth.atDay(1)
                    val daysInMonth = state.currentMonth.lengthOfMonth()
                    // Monday is 1, Sunday is 7. Offset = dayOfWeek.value - 1
                    val startOffset = firstDayOfMonth.dayOfWeek.value - 1

                    val totalCells = ((startOffset + daysInMonth + 6) / 7) * 7
                    val today = LocalDate.now()

                    for (week in 0 until (totalCells / 7)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            for (dayIndex in 0..6) {
                                val cellNumber = week * 7 + dayIndex
                                val dayOfMonth = cellNumber - startOffset + 1

                                if (dayOfMonth in 1..daysInMonth) {
                                    val date = state.currentMonth.atDay(dayOfMonth)
                                    val isSelected = date == state.selectedDate
                                    val isToday = date == today
                                    val tasksOnDate = state.tasksByDate[date] ?: emptyList()

                                    CalendarDayCell(
                                        date = date,
                                        isSelected = isSelected,
                                        isToday = isToday,
                                        taskCount = tasksOnDate.size,
                                        tasks = tasksOnDate,
                                        onClick = { viewModel.selectDate(date) }
                                    )
                                } else {
                                    // Empty Cell placeholder
                                    Box(modifier = Modifier.size(38.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            Spacer(modifier = Modifier.height(14.dp))

            // Agenda Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = selectedDateFormatted,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${state.tasksForSelectedDate.size} ${if (state.tasksForSelectedDate.size == 1) "task" else "tasks"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilledTonalButton(
                    onClick = {
                        val epochMillis = DateUtils.localDateToEpochMillis(state.selectedDate)
                        onAddTaskForDate(epochMillis)
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Task", style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Agenda Items or Empty State
        if (state.tasksForSelectedDate.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No tasks for this date",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(state.tasksForSelectedDate, key = { it.task.id }) { item ->
                TaskCard(
                    taskWithDetails = item,
                    onToggleCompletion = { viewModel.toggleTaskCompletion(item.task.id) },
                    onClick = { selectedTaskForDetails = item },
                    onEdit = { onEditTask(item.task.id) },
                    onPostpone = { viewModel.postponeTask(item.task.id, 1) },
                    onDuplicate = { viewModel.duplicateTask(item.task.id) },
                    onArchive = { viewModel.archiveTask(item.task.id) },
                    onDelete = { viewModel.deleteTask(item.task.id) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Detail Bottom Sheet
    if (selectedTaskForDetails != null) {
        TaskDetailSheet(
            taskWithDetails = selectedTaskForDetails,
            sheetState = detailSheetState,
            onDismiss = { selectedTaskForDetails = null },
            onToggleCompletion = {
                selectedTaskForDetails?.let { viewModel.toggleTaskCompletion(it.task.id) }
            },
            onEdit = {
                selectedTaskForDetails?.let { onEditTask(it.task.id) }
            },
            onPostpone = { days ->
                selectedTaskForDetails?.let { viewModel.postponeTask(it.task.id, days) }
            },
            onDuplicate = {
                selectedTaskForDetails?.let { viewModel.duplicateTask(it.task.id) }
            },
            onArchive = {
                selectedTaskForDetails?.let { viewModel.archiveTask(it.task.id) }
            },
            onDelete = {
                selectedTaskForDetails?.let { viewModel.deleteTask(it.task.id) }
            }
        )
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    taskCount: Int,
    tasks: List<TaskWithDetails>,
    onClick: () -> Unit
) {
    val bgColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primaryContainer
        else -> Color.Transparent
    }

    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "${date.dayOfMonth}",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 13.sp
            ),
            color = textColor
        )

        // Dots representing tasks (up to 3 dots)
        if (taskCount > 0) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 1.dp)
            ) {
                val dotsToShow = tasks.take(3)
                dotsToShow.forEach { taskWithDetails ->
                    val dotColor = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        taskWithDetails.task.priority.color
                    }
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }
            }
        }
    }
}
