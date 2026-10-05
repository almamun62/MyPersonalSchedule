package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.*
import com.example.ui.theme.CourseScheduleTheme
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.tr
import com.example.ui.viewmodel.ImportViewModel
import com.example.ui.viewmodel.ScheduleViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val scheduleViewModel: ScheduleViewModel = viewModel()
            val importViewModel: ImportViewModel = viewModel()

            val appLang by scheduleViewModel.userPreferencesManager.appLanguage.collectAsState()
            val themeMode by scheduleViewModel.userPreferencesManager.themeMode.collectAsState()
            val accentColor by scheduleViewModel.userPreferencesManager.accentColor.collectAsState()

            CompositionLocalProvider(LocalAppLanguage provides appLang) {
                CourseScheduleTheme(
                    themeMode = themeMode,
                    accentColor = accentColor
                ) {
                    MainAppScaffold(
                        scheduleViewModel = scheduleViewModel,
                        importViewModel = importViewModel
                    )
                }
            }
        }
    }
}

enum class Screen {
    TIMETABLE,
    DASHBOARD,
    TASKS,
    COURSES,
    MORE,
    IMPORT,
    ABOUT
}

@Composable
fun MainAppScaffold(
    scheduleViewModel: ScheduleViewModel,
    importViewModel: ImportViewModel
) {
    var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }
    var showAcademicCalendarModal by remember { mutableStateOf(false) }

    if (currentScreen != Screen.DASHBOARD) {
        BackHandler {
            currentScreen = Screen.DASHBOARD
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentScreen == Screen.TIMETABLE,
                    onClick = { currentScreen = Screen.TIMETABLE },
                    icon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                    label = { Text("Timetable".tr) }
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.DASHBOARD,
                    onClick = { currentScreen = Screen.DASHBOARD },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                    label = { Text("Dashboard".tr) }
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.TASKS,
                    onClick = { currentScreen = Screen.TASKS },
                    icon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                    label = { Text("Tasks".tr) }
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.COURSES,
                    onClick = { currentScreen = Screen.COURSES },
                    icon = { Icon(Icons.Default.School, contentDescription = null) },
                    label = { Text("Courses".tr) }
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.MORE,
                    onClick = { currentScreen = Screen.MORE },
                    icon = { Icon(Icons.Default.GridView, contentDescription = null) },
                    label = { Text("More".tr) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.TIMETABLE -> TimetableScreen(
                    viewModel = scheduleViewModel,
                    onNavigateToImport = { currentScreen = Screen.IMPORT },
                    onNavigateToCourses = { currentScreen = Screen.COURSES },
                    onOpenAcademicCalendar = { showAcademicCalendarModal = true }
                )
                Screen.DASHBOARD -> DashboardScreen(
                    viewModel = scheduleViewModel,
                    onNavigateToTimetable = { currentScreen = Screen.TIMETABLE },
                    onNavigateToTasks = { currentScreen = Screen.TASKS },
                    onOpenAcademicCalendar = { showAcademicCalendarModal = true }
                )
                Screen.TASKS -> TasksExamsScreen(
                    viewModel = scheduleViewModel
                )
                Screen.COURSES -> CourseListScreen(
                    viewModel = scheduleViewModel
                )
                Screen.MORE -> MoreOptionsScreen(
                    viewModel = scheduleViewModel,
                    onNavigateToAbout = { currentScreen = Screen.ABOUT },
                    onNavigateToImport = { currentScreen = Screen.IMPORT },
                    onNavigateToTasks = { currentScreen = Screen.TASKS },
                    onNavigateToCourses = { currentScreen = Screen.COURSES }
                )
                Screen.IMPORT -> ImportScheduleScreen(
                    importViewModel = importViewModel,
                    scheduleViewModel = scheduleViewModel,
                    onFinishImport = { currentScreen = Screen.TIMETABLE }
                )
                Screen.ABOUT -> AboutScreen()
            }

            if (showAcademicCalendarModal) {
                com.example.ui.components.AcademicCalendarDialog(
                    viewModel = scheduleViewModel,
                    onDismiss = { showAcademicCalendarModal = false }
                )
            }
        }
    }
}

