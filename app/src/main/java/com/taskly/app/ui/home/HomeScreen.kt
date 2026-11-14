package com.taskly.app.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskly.app.data.model.TaskWithDetails
import com.taskly.app.ui.components.EmptyState
import com.taskly.app.ui.components.TaskCard
import com.taskly.app.ui.components.TaskCardPlaceholder
import com.taskly.app.ui.tasks.TaskDetailSheet
import com.taskly.app.ui.theme.Error
import com.taskly.app.ui.theme.Success
import com.taskly.app.ui.theme.Warning
import com.taskly.app.utils.DateUtils
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onSettingsClick: () -> Unit = {},
    onAddTaskClick: () -> Unit = {},
    onEditTaskClick: (Long) -> Unit = {},
    onSearchClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var selectedTaskForDetails by remember { mutableStateOf<TaskWithDetails?>(null) }
    val detailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showOverdueExpanded by remember { mutableStateOf(true) }
    var showCompletedExpanded by remember { mutableStateOf(false) }

    val greeting = remember { DateUtils.getGreeting() }
    val todayFormatted = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM"))
    }

    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 600

    val hasAnyTasks = state.totalToday > 0 || state.overdue.isNotEmpty() || state.upcoming.isNotEmpty()

    val showUndoSnackbar: (String, () -> Unit) -> Unit = { message, onUndo ->
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                onUndo()
            }
        }
    }

    // Show error message if one occurs
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (isWideScreen) {
            // Tablet 2-pane layout
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Left Pane: Overview & Today's Tasks
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        HomeHeader(
                            greeting = greeting,
                            todayFormatted = todayFormatted,
                            onSearchClick = onSearchClick,
                            onSettingsClick = onSettingsClick
                        )
                    }

                    item {
                        ProductivityOverviewCard(state = state)
                    }

                    item {
                        Text(
                            text = "Today's Tasks (${state.pendingToday.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    if (state.isLoading) {
                        items(3) {
                            TaskCardPlaceholder()
                        }
                    } else if (state.pendingToday.isEmpty()) {
                        item {
                            TodayEmptyCard(hasCompletedToday = state.completedCount > 0)
                        }
                    } else {
                        items(state.pendingToday, key = { it.task.id }) { item ->
                            TaskCard(
                                taskWithDetails = item,
                                onToggleCompletion = { viewModel.toggleTaskCompletion(item.task.id) },
                                onClick = { selectedTaskForDetails = item },
                                onEdit = { onEditTaskClick(item.task.id) },
                                onPostpone = { viewModel.postponeTask(item.task.id, 1) },
                                onDuplicate = { viewModel.duplicateTask(item.task.id) },
                                onArchive = {
                                    viewModel.archiveTaskWithUndo(item, showUndoSnackbar)
                                },
                                onDelete = {
                                    viewModel.deleteTaskWithUndo(item, showUndoSnackbar)
                                }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }

                // Right Pane: Overdue, Upcoming, Completed Today
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Overdue Section
                    if (state.overdue.isNotEmpty()) {
                        item {
                            SectionHeaderWithBadge(
                                title = "Overdue",
                                count = state.overdue.size,
                                badgeColor = Error,
                                isExpanded = showOverdueExpanded,
                                onToggle = { showOverdueExpanded = !showOverdueExpanded },
                                leadingIcon = Icons.Outlined.WarningAmber
                            )
                        }

                        if (showOverdueExpanded) {
                            items(state.overdue, key = { it.task.id }) { item ->
                                TaskCard(
                                    taskWithDetails = item,
                                    onToggleCompletion = { viewModel.toggleTaskCompletion(item.task.id) },
                                    onClick = { selectedTaskForDetails = item },
                                    onEdit = { onEditTaskClick(item.task.id) },
                                    onPostpone = { viewModel.postponeTask(item.task.id, 1) },
                                    onDuplicate = { viewModel.duplicateTask(item.task.id) },
                                    onArchive = {
                                        viewModel.archiveTaskWithUndo(item, showUndoSnackbar)
                                    },
                                    onDelete = {
                                        viewModel.deleteTaskWithUndo(item, showUndoSnackbar)
                                    }
                                )
                            }
                        }
                    }

                    // Upcoming Section
                    if (state.upcoming.isNotEmpty()) {
                        item {
                            Text(
                                text = "Upcoming (${state.upcoming.size})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        items(state.upcoming.take(8), key = { it.task.id }) { item ->
                            TaskCard(
                                taskWithDetails = item,
                                onToggleCompletion = { viewModel.toggleTaskCompletion(item.task.id) },
                                onClick = { selectedTaskForDetails = item },
                                onEdit = { onEditTaskClick(item.task.id) },
                                onPostpone = { viewModel.postponeTask(item.task.id, 1) },
                                onDuplicate = { viewModel.duplicateTask(item.task.id) },
                                onArchive = {
                                    viewModel.archiveTaskWithUndo(item, showUndoSnackbar)
                                },
                                onDelete = {
                                    viewModel.deleteTaskWithUndo(item, showUndoSnackbar)
                                }
                            )
                        }
                    }

                    // Completed Today Section
                    if (state.completedToday.isNotEmpty()) {
                        item {
                            SectionHeaderWithBadge(
                                title = "Completed Today",
                                count = state.completedToday.size,
                                badgeColor = Success,
                                isExpanded = showCompletedExpanded,
                                onToggle = { showCompletedExpanded = !showCompletedExpanded },
                                leadingIcon = Icons.Outlined.CheckCircle
                            )
                        }

                        if (showCompletedExpanded) {
                            items(state.completedToday, key = { it.task.id }) { item ->
                                TaskCard(
                                    taskWithDetails = item,
                                    onToggleCompletion = { viewModel.toggleTaskCompletion(item.task.id) },
                                    onClick = { selectedTaskForDetails = item },
                                    onEdit = { onEditTaskClick(item.task.id) },
                                    onPostpone = { viewModel.postponeTask(item.task.id, 1) },
                                    onDuplicate = { viewModel.duplicateTask(item.task.id) },
                                    onArchive = {
                                        viewModel.archiveTaskWithUndo(item, showUndoSnackbar)
                                    },
                                    onDelete = {
                                        viewModel.deleteTaskWithUndo(item, showUndoSnackbar)
                                    }
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        } else {
            // Phone single-column layout
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Header Row
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    HomeHeader(
                        greeting = greeting,
                        todayFormatted = todayFormatted,
                        onSearchClick = onSearchClick,
                        onSettingsClick = onSettingsClick
                    )
                }

                // 2. Productivity Overview Card
                item {
                    ProductivityOverviewCard(state = state)
                }

                // Shimmer loading placeholder
                if (state.isLoading) {
                    items(3) {
                        TaskCardPlaceholder()
                    }
                }

                // 3. Overdue Tasks Section
                if (state.overdue.isNotEmpty()) {
                    item {
                        SectionHeaderWithBadge(
                            title = "Overdue",
                            count = state.overdue.size,
                            badgeColor = Error,
                            isExpanded = showOverdueExpanded,
                            onToggle = { showOverdueExpanded = !showOverdueExpanded },
                            leadingIcon = Icons.Outlined.WarningAmber
                        )
                    }

                    if (showOverdueExpanded) {
                        items(state.overdue, key = { it.task.id }) { item ->
                            TaskCard(
                                taskWithDetails = item,
                                onToggleCompletion = { viewModel.toggleTaskCompletion(item.task.id) },
                                onClick = { selectedTaskForDetails = item },
                                onEdit = { onEditTaskClick(item.task.id) },
                                onPostpone = { viewModel.postponeTask(item.task.id, 1) },
                                onDuplicate = { viewModel.duplicateTask(item.task.id) },
                                onArchive = {
                                    viewModel.archiveTaskWithUndo(item, showUndoSnackbar)
                                },
                                onDelete = {
                                    viewModel.deleteTaskWithUndo(item, showUndoSnackbar)
                                }
                            )
                        }
                    }
                }

                // 4. Today's Pending Tasks Section
                item {
                    Text(
                        text = "Today's Tasks (${state.pendingToday.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (!state.isLoading && state.pendingToday.isEmpty()) {
                    item {
                        TodayEmptyCard(hasCompletedToday = state.completedCount > 0)
                    }
                } else if (!state.isLoading) {
                    items(state.pendingToday, key = { it.task.id }) { item ->
                        TaskCard(
                            taskWithDetails = item,
                            onToggleCompletion = { viewModel.toggleTaskCompletion(item.task.id) },
                            onClick = { selectedTaskForDetails = item },
                            onEdit = { onEditTaskClick(item.task.id) },
                            onPostpone = { viewModel.postponeTask(item.task.id, 1) },
                            onDuplicate = { viewModel.duplicateTask(item.task.id) },
                            onArchive = {
                                viewModel.archiveTaskWithUndo(item, showUndoSnackbar)
                            },
                            onDelete = {
                                viewModel.deleteTaskWithUndo(item, showUndoSnackbar)
                            }
                        )
                    }
                }

                // 5. Upcoming Tasks Preview
                if (state.upcoming.isNotEmpty()) {
                    item {
                        Text(
                            text = "Upcoming (${state.upcoming.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(state.upcoming.take(5), key = { it.task.id }) { item ->
                        TaskCard(
                            taskWithDetails = item,
                            onToggleCompletion = { viewModel.toggleTaskCompletion(item.task.id) },
                            onClick = { selectedTaskForDetails = item },
                            onEdit = { onEditTaskClick(item.task.id) },
                            onPostpone = { viewModel.postponeTask(item.task.id, 1) },
                            onDuplicate = { viewModel.duplicateTask(item.task.id) },
                            onArchive = {
                                viewModel.archiveTaskWithUndo(item, showUndoSnackbar)
                            },
                            onDelete = {
                                viewModel.deleteTaskWithUndo(item, showUndoSnackbar)
                            }
                        )
                    }
                }

                // 6. Completed Today Section
                if (state.completedToday.isNotEmpty()) {
                    item {
                        SectionHeaderWithBadge(
                            title = "Completed Today",
                            count = state.completedToday.size,
                            badgeColor = Success,
                            isExpanded = showCompletedExpanded,
                            onToggle = { showCompletedExpanded = !showCompletedExpanded },
                            leadingIcon = Icons.Outlined.CheckCircle
                        )
                    }

                    if (showCompletedExpanded) {
                        items(state.completedToday, key = { it.task.id }) { item ->
                            TaskCard(
                                taskWithDetails = item,
                                onToggleCompletion = { viewModel.toggleTaskCompletion(item.task.id) },
                                onClick = { selectedTaskForDetails = item },
                                onEdit = { onEditTaskClick(item.task.id) },
                                onPostpone = { viewModel.postponeTask(item.task.id, 1) },
                                onDuplicate = { viewModel.duplicateTask(item.task.id) },
                                onArchive = {
                                    viewModel.archiveTaskWithUndo(item, showUndoSnackbar)
                                },
                                onDelete = {
                                    viewModel.deleteTaskWithUndo(item, showUndoSnackbar)
                                }
                            )
                        }
                    }
                }

                // Global Empty State
                if (!hasAnyTasks && !state.isLoading) {
                    item {
                        EmptyState(
                            title = "Welcome to Taskly",
                            message = "You don't have any tasks right now. Create your first task to start organizing your day!",
                            actionLabel = "Add a Task",
                            onActionClick = onAddTaskClick
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    // Task Details Modal Sheet
    if (selectedTaskForDetails != null) {
        TaskDetailSheet(
            taskWithDetails = selectedTaskForDetails,
            sheetState = detailSheetState,
            onDismiss = { selectedTaskForDetails = null },
            onToggleCompletion = {
                selectedTaskForDetails?.let { viewModel.toggleTaskCompletion(it.task.id) }
            },
            onEdit = {
                selectedTaskForDetails?.let { onEditTaskClick(it.task.id) }
            },
            onPostpone = { days ->
                selectedTaskForDetails?.let { viewModel.postponeTask(it.task.id, days) }
            },
            onDuplicate = {
                selectedTaskForDetails?.let { viewModel.duplicateTask(it.task.id) }
            },
            onArchive = {
                selectedTaskForDetails?.let { item ->
                    selectedTaskForDetails = null
                    viewModel.archiveTaskWithUndo(item, showUndoSnackbar)
                }
            },
            onDelete = {
                selectedTaskForDetails?.let { item ->
                    selectedTaskForDetails = null
                    viewModel.deleteTaskWithUndo(item, showUndoSnackbar)
                }
            }
        )
    }
}

@Composable
private fun HomeHeader(
    greeting: String,
    todayFormatted: String,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column {
            Text(
                text = greeting,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = todayFormatted,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row {
            IconButton(
                onClick = onSearchClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = "Search Tasks",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun TodayEmptyCard(hasCompletedToday: Boolean) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = Success,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (hasCompletedToday) "All today's tasks completed!" else "No tasks scheduled for today",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ProductivityOverviewCard(state: HomeUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's Progress",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )

                Text(
                    text = "${state.completionPercentage}%",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { (state.completionPercentage / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatItem(label = "Pending", value = "${state.pendingCount}")
                StatItem(label = "Done", value = "${state.completedCount}", valueColor = Success)
                StatItem(label = "Overdue", value = "${state.overdueCount}", valueColor = if (state.overdueCount > 0) Error else MaterialTheme.colorScheme.onSurface)
                StatItem(label = "Total", value = "${state.totalToday}")
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SectionHeaderWithBadge(
    title: String,
    count: Int,
    badgeColor: androidx.compose.ui.graphics.Color,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onToggle)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = badgeColor
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = CircleShape,
                color = badgeColor.copy(alpha = 0.15f),
                modifier = Modifier.size(22.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = badgeColor
                    )
                }
            }
        }

        Icon(
            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = if (isExpanded) "Collapse" else "Expand",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
