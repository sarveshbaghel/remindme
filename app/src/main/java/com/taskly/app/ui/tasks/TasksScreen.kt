package com.taskly.app.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskly.app.data.model.TaskWithDetails
import com.taskly.app.ui.components.CategoryIconHelper
import com.taskly.app.ui.components.EmptyState
import com.taskly.app.ui.components.TaskCard
import com.taskly.app.ui.components.TaskCardPlaceholder
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    viewModel: TasksViewModel,
    onEditTask: (Long) -> Unit = {},
    onAddTask: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var selectedTaskForDetails by remember { mutableStateOf<TaskWithDetails?>(null) }
    val detailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showSortMenu by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 600

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

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = if (isWideScreen) 24.dp else 20.dp)
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            // Title and Sort Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tasks",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )

                Box {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.clickable { showSortMenu = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Sort Tasks",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = state.currentSort.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        TaskSort.entries.forEach { sort ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = sort.label,
                                        fontWeight = if (state.currentSort == sort) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    viewModel.setSort(sort)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search tasks...") },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = "Search icon",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (state.searchQuery.isNotBlank()) {
                        IconButton(
                            onClick = { viewModel.setSearchQuery("") },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search query")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Chips (All, Today, Upcoming, Overdue, Completed, Archived)
            val filterScrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(filterScrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TaskFilter.entries.forEach { filter ->
                    val isSelected = state.currentFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setFilter(filter) },
                        label = { Text(filter.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Category Filter Chips
            if (state.categories.isNotEmpty()) {
                val categoryScrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(categoryScrollState),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = state.selectedCategoryId == null,
                        onClick = { viewModel.setCategoryFilter(null) },
                        label = { Text("All Categories") }
                    )

                    state.categories.forEach { cat ->
                        val isSelected = state.selectedCategoryId == cat.id
                        val catColor = Color(cat.colorHex)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                viewModel.setCategoryFilter(if (isSelected) null else cat.id)
                            },
                            label = { Text(cat.name) },
                            leadingIcon = {
                                Icon(
                                    imageVector = CategoryIconHelper.getIcon(cat.iconName),
                                    contentDescription = cat.name,
                                    modifier = Modifier.size(13.dp),
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else catColor
                                )
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tasks Count Subtitle
            Text(
                text = "${state.tasks.size} ${if (state.tasks.size == 1) "task" else "tasks"}",
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Loading state: Shimmer placeholders
            if (state.isLoading) {
                if (isWideScreen) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 340.dp),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(6) {
                            TaskCardPlaceholder()
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(4) {
                            TaskCardPlaceholder()
                        }
                    }
                }
            } else if (state.tasks.isEmpty()) {
                // Empty state
                val emptyMessage = when (state.currentFilter) {
                    TaskFilter.OVERDUE -> "No overdue tasks! You're completely up to date."
                    TaskFilter.COMPLETED -> "No completed tasks yet."
                    TaskFilter.TODAY -> "No tasks scheduled for today."
                    TaskFilter.UPCOMING -> "No upcoming tasks found."
                    TaskFilter.ARCHIVED -> "No archived tasks."
                    else -> if (state.searchQuery.isNotBlank()) "No tasks match '${state.searchQuery}'"
                    else "No tasks yet. Tap + to add your first task!"
                }

                EmptyState(
                    title = "No tasks found",
                    message = emptyMessage,
                    actionLabel = if (state.currentFilter != TaskFilter.COMPLETED && state.currentFilter != TaskFilter.ARCHIVED) "Add Task" else null,
                    onActionClick = onAddTask
                )
            } else {
                // Task list: Adaptive 2-pane grid on tablet, single column on phone
                if (isWideScreen) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 340.dp),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(state.tasks, key = { it.task.id }) { item ->
                            TaskCard(
                                taskWithDetails = item,
                                onToggleCompletion = { viewModel.toggleTaskCompletion(item.task.id) },
                                onClick = { selectedTaskForDetails = item },
                                onEdit = { onEditTask(item.task.id) },
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
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(state.tasks, key = { it.task.id }) { item ->
                            TaskCard(
                                taskWithDetails = item,
                                onToggleCompletion = { viewModel.toggleTaskCompletion(item.task.id) },
                                onClick = { selectedTaskForDetails = item },
                                onEdit = { onEditTask(item.task.id) },
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
            }
        }
    }

    // Detail Sheet
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
