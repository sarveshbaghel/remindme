package com.taskly.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.taskly.app.TasklyApp
import com.taskly.app.ui.calendar.CalendarScreen
import com.taskly.app.ui.calendar.CalendarViewModel
import com.taskly.app.ui.home.HomeScreen
import com.taskly.app.ui.home.HomeViewModel
import com.taskly.app.ui.insights.InsightsScreen
import com.taskly.app.ui.insights.InsightsViewModel
import com.taskly.app.ui.settings.SettingsScreen
import com.taskly.app.ui.settings.SettingsViewModel
import com.taskly.app.ui.tasks.AddEditTaskScreen
import com.taskly.app.ui.tasks.AddEditTaskViewModel
import com.taskly.app.ui.tasks.TasksScreen
import com.taskly.app.ui.tasks.TasksViewModel

enum class Tab(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
) {
    Home("home", "Home", Icons.Outlined.Home, Icons.Filled.Home),
    Tasks("tasks", "Tasks", Icons.Outlined.CheckCircle, Icons.Filled.CheckCircle),
    Calendar("calendar", "Calendar", Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth),
    Insights("insights", "Insights", Icons.Outlined.BarChart, Icons.Filled.BarChart),
}

@Composable
fun TasklyNavigation() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val isTabRoute = Tab.entries.any { it.route == currentRoute }

    val context = LocalContext.current
    val app = context.applicationContext as TasklyApp

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (isTabRoute) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                ) {
                    Tab.entries.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.icon,
                                    contentDescription = tab.label,
                                )
                            },
                            label = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (isTabRoute) {
                FloatingActionButton(
                    onClick = { navController.navigate("add_task") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Task")
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Tab.Home.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Tab.Home.route) {
                val homeViewModel: HomeViewModel = viewModel(
                    factory = HomeViewModel.Factory(app.container.taskRepository)
                )
                HomeScreen(
                    viewModel = homeViewModel,
                    onSettingsClick = { navController.navigate("settings") },
                    onAddTaskClick = { navController.navigate("add_task") },
                    onEditTaskClick = { taskId -> navController.navigate("edit_task/$taskId") },
                    onSearchClick = { navController.navigate(Tab.Tasks.route) }
                )
            }

            composable(Tab.Tasks.route) {
                val tasksViewModel: TasksViewModel = viewModel(
                    factory = TasksViewModel.Factory(
                        taskRepository = app.container.taskRepository,
                        categoryRepository = app.container.categoryRepository
                    )
                )
                TasksScreen(
                    viewModel = tasksViewModel,
                    onEditTask = { taskId -> navController.navigate("edit_task/$taskId") },
                    onAddTask = { navController.navigate("add_task") }
                )
            }

            composable(Tab.Calendar.route) {
                val calendarViewModel: CalendarViewModel = viewModel(
                    factory = CalendarViewModel.Factory(app.container.taskRepository)
                )
                CalendarScreen(
                    viewModel = calendarViewModel,
                    onEditTask = { taskId -> navController.navigate("edit_task/$taskId") },
                    onAddTaskForDate = { dateMillis -> navController.navigate("add_task?date=$dateMillis") }
                )
            }

            composable(Tab.Insights.route) {
                val insightsViewModel: InsightsViewModel = viewModel(
                    factory = InsightsViewModel.Factory(
                        taskRepository = app.container.taskRepository,
                        categoryRepository = app.container.categoryRepository,
                        taskHistoryDao = app.container.database.taskHistoryDao()
                    )
                )
                InsightsScreen(
                    viewModel = insightsViewModel
                )
            }

            composable("settings") {
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.Factory(
                        userPreferences = app.container.userPreferences,
                        taskRepository = app.container.taskRepository,
                        categoryRepository = app.container.categoryRepository
                    )
                )
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "add_task?date={date}",
                arguments = listOf(
                    navArgument("date") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val dateMillis = backStackEntry.arguments?.getString("date")?.toLongOrNull()
                val addViewModel: AddEditTaskViewModel = viewModel(
                    factory = AddEditTaskViewModel.Factory(
                        taskRepository = app.container.taskRepository,
                        categoryRepository = app.container.categoryRepository,
                        taskId = -1L,
                        dateMillis = dateMillis
                    )
                )
                AddEditTaskScreen(
                    viewModel = addViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "edit_task/{taskId}",
                arguments = listOf(
                    navArgument("taskId") {
                        type = NavType.LongType
                    }
                )
            ) { backStackEntry ->
                val taskId = backStackEntry.arguments?.getLong("taskId") ?: -1L
                val editViewModel: AddEditTaskViewModel = viewModel(
                    factory = AddEditTaskViewModel.Factory(
                        taskRepository = app.container.taskRepository,
                        categoryRepository = app.container.categoryRepository,
                        taskId = taskId
                    )
                )
                AddEditTaskScreen(
                    viewModel = editViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
