package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
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
    val mainTabs = remember { listOf(Screen.TIMETABLE, Screen.DASHBOARD, Screen.TASKS, Screen.COURSES, Screen.MORE) }
    val pagerState = rememberPagerState(initialPage = 1) { mainTabs.size }
    val coroutineScope = rememberCoroutineScope()
    var activeSecondaryScreen by remember { mutableStateOf<Screen?>(null) }
    var showAcademicCalendarModal by remember { mutableStateOf(false) }

    // Back handler management
    if (activeSecondaryScreen != null) {
        BackHandler {
            activeSecondaryScreen = null
        }
    } else if (pagerState.currentPage != 1) { // Default tab is Dashboard (index 1)
        BackHandler {
            coroutineScope.launch {
                pagerState.animateScrollToPage(1)
            }
        }
    }

    val showNotchMode by scheduleViewModel.userPreferencesManager.showNotchMode.collectAsState()

    Scaffold(
        contentWindowInsets = if (showNotchMode) WindowInsets(0, 0, 0, 0) else WindowInsets.systemBars,
        bottomBar = {
            NavigationBar {
                mainTabs.forEachIndexed { index, screen ->
                    val (icon, label) = when (screen) {
                        Screen.TIMETABLE -> Icons.Default.CalendarToday to "Timetable".tr
                        Screen.DASHBOARD -> Icons.Default.Dashboard to "Dashboard".tr
                        Screen.TASKS -> Icons.Default.CheckCircle to "Tasks".tr
                        Screen.COURSES -> Icons.Default.School to "Courses".tr
                        Screen.MORE -> Icons.Default.Widgets to "Tools".tr
                        else -> Icons.Default.Menu to ""
                    }
                    NavigationBarItem(
                        selected = activeSecondaryScreen == null && pagerState.currentPage == index,
                        onClick = {
                            activeSecondaryScreen = null
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            when (activeSecondaryScreen) {
                Screen.IMPORT -> ImportScheduleScreen(
                    importViewModel = importViewModel,
                    scheduleViewModel = scheduleViewModel,
                    onFinishImport = {
                        activeSecondaryScreen = null
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(0) // Go to Timetable
                        }
                    }
                )
                Screen.ABOUT -> AboutScreen(
                    viewModel = scheduleViewModel,
                    onBack = { activeSecondaryScreen = null }
                )
                else -> {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        when (mainTabs[page]) {
                            Screen.TIMETABLE -> TimetableScreen(
                                viewModel = scheduleViewModel,
                                onNavigateToImport = { activeSecondaryScreen = Screen.IMPORT },
                                onNavigateToCourses = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(mainTabs.indexOf(Screen.COURSES))
                                    }
                                },
                                onOpenAcademicCalendar = { showAcademicCalendarModal = true }
                            )
                            Screen.DASHBOARD -> DashboardScreen(
                                viewModel = scheduleViewModel,
                                onNavigateToTimetable = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(mainTabs.indexOf(Screen.TIMETABLE))
                                    }
                                },
                                onNavigateToTasks = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(mainTabs.indexOf(Screen.TASKS))
                                    }
                                },
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
                                onNavigateToAbout = { activeSecondaryScreen = Screen.ABOUT },
                                onNavigateToImport = { activeSecondaryScreen = Screen.IMPORT },
                                onNavigateToTasks = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(mainTabs.indexOf(Screen.TASKS))
                                    }
                                },
                                onNavigateToCourses = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(mainTabs.indexOf(Screen.COURSES))
                                    }
                                }
                            )
                            else -> {}
                        }
                    }
                }
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
