package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.service.CourseNotificationManager
import com.example.ui.components.BootstartOnboardingDialog
import com.example.ui.components.QuickNoteDialog
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.AcademicCalendarScreen
import com.example.ui.screens.CourseListScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FocusLockScreen
import com.example.ui.screens.HowToUseScreen
import com.example.ui.screens.ImportScheduleScreen
import com.example.ui.screens.MoreScreen
import com.example.ui.screens.NotesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ThemeCustomizationScreen
import com.example.ui.screens.TasksExamsScreen
import com.example.ui.screens.TimetableScreen
import com.example.ui.screens.UsageScreen
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.tr
import com.example.ui.viewmodel.ImportViewModel
import com.example.ui.viewmodel.ScheduleViewModel

sealed class Screen(val route: String, val titleKey: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Timetable : Screen("timetable", "Timetable", Icons.Default.CalendarMonth)
    object TasksExams : Screen("tasks_exams", "Tasks", Icons.Default.TaskAlt)
    object Courses : Screen("courses", "Courses", Icons.Default.School)
    object More : Screen("more", "More", Icons.Default.GridView)
}

class MainActivity : ComponentActivity() {

    private val scheduleViewModel: ScheduleViewModel by viewModels()
    private val importViewModel: ImportViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        try {
            CourseNotificationManager.createNotificationChannels(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContent {
            val themeMode by scheduleViewModel.userPreferencesManager.themeMode.collectAsStateWithLifecycle()
            val accentColor by scheduleViewModel.userPreferencesManager.accentColor.collectAsStateWithLifecycle()
            val appLanguage by scheduleViewModel.userPreferencesManager.appLanguage.collectAsStateWithLifecycle()

            val effectiveLanguage = when (appLanguage) {
                "system", null -> java.util.Locale.getDefault().language
                "zh" -> "zh"
                "en" -> "en"
                else -> appLanguage ?: java.util.Locale.getDefault().language
            }

            CompositionLocalProvider(
                LocalAppLanguage provides effectiveLanguage
            ) {
                MyApplicationTheme(
                    themeMode = themeMode,
                    accentColor = accentColor
                ) {
                    MainApp(
                        scheduleViewModel = scheduleViewModel,
                        importViewModel = importViewModel
                    )
                }
            }
        }
    }
}

@Composable
fun MainApp(
    scheduleViewModel: ScheduleViewModel,
    importViewModel: ImportViewModel
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Timetable.route

    val uiState by scheduleViewModel.uiState.collectAsStateWithLifecycle()
    val hasCompletedOnboarding by scheduleViewModel.userPreferencesManager.hasCompletedOnboarding.collectAsStateWithLifecycle()
    val appLanguage by scheduleViewModel.userPreferencesManager.appLanguage.collectAsStateWithLifecycle()
    val themeMode by scheduleViewModel.userPreferencesManager.themeMode.collectAsStateWithLifecycle()
    val accentColor by scheduleViewModel.userPreferencesManager.accentColor.collectAsStateWithLifecycle()

    var showOnboarding by remember { mutableStateOf(!hasCompletedOnboarding) }

    if (showOnboarding && !hasCompletedOnboarding) {
        BootstartOnboardingDialog(
            initialAppLanguage = appLanguage ?: "en",
            initialThemeMode = themeMode,
            initialAccent = accentColor,
            initialAutoDnd = scheduleViewModel.userPreferencesManager.isAutoDndEnabled.value,
            initial15mReminder = scheduleViewModel.userPreferencesManager.isClassReminder15mEnabled.value,
            onComplete = { lang, mode, accent, dnd, reminder, option, bilingual ->
                scheduleViewModel.userPreferencesManager.setAppLanguage(lang)
                scheduleViewModel.userPreferencesManager.setThemeMode(mode)
                scheduleViewModel.userPreferencesManager.setAccentColor(accent)
                scheduleViewModel.userPreferencesManager.setAutoDndEnabled(dnd)
                scheduleViewModel.userPreferencesManager.setClassReminder15mEnabled(reminder)
                scheduleViewModel.userPreferencesManager.setBilingualPreferred(bilingual)
                scheduleViewModel.userPreferencesManager.setOnboardingCompleted(true)
                showOnboarding = false
                if (option == 0) {
                    // Option 0: Load Mamun Fall 2026 Schedule directly into Room database
                    scheduleViewModel.loadPresetMamunSchedule(bilingual = bilingual, replaceExisting = true)
                } else if (option == 1) {
                    navController.navigate("import_schedule")
                }
            },
            onDismiss = {
                scheduleViewModel.userPreferencesManager.setOnboardingCompleted(true)
                showOnboarding = false
            }
        )
    }

    val navItems = listOf(
        Screen.Timetable,
        Screen.Dashboard,
        Screen.TasksExams,
        Screen.Courses,
        Screen.More
    )

    val bottomBarRoutes = setOf(
        Screen.Timetable.route,
        Screen.Dashboard.route,
        Screen.TasksExams.route,
        Screen.Courses.route,
        Screen.More.route
    )

    val showBottomBar = currentRoute in bottomBarRoutes

    val isFocusLocked = uiState.focusLockEndTimeMillis > System.currentTimeMillis()
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? android.app.Activity

    LaunchedEffect(isFocusLocked) {
        try {
            if (isFocusLocked) {
                activity?.startLockTask()
            } else {
                activity?.stopLockTask()
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "LockTask mode error", e)
        }
    }

    var showQuickNoteDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar(
                        modifier = Modifier.testTag("main_bottom_nav"),
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 3.dp
                    ) {
                        navItems.forEach { screen ->
                            val isSelected = currentRoute == screen.route
                            val labelText = screen.titleKey.tr
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    if (currentRoute != screen.route) {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = screen.icon,
                                        contentDescription = labelText,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = labelText,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                        fontSize = 11.sp,
                                        lineHeight = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                },
                                alwaysShowLabel = true,
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("nav_item_${screen.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Timetable.route,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    state = uiState,
                    viewModel = scheduleViewModel,
                    onNavigateToTimetable = {
                        navController.navigate(Screen.Timetable.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable(Screen.Timetable.route) {
                TimetableScreen(
                    viewModel = scheduleViewModel,
                    onNavigateToImport = {
                        navController.navigate("import_schedule")
                    },
                    onNavigateToChat = {
                        navController.navigate("chat")
                    },
                    onNavigateToCourses = {
                        navController.navigate(Screen.Courses.route)
                    },
                    onNavigateToAbout = {
                        navController.navigate("about")
                    }
                )
            }

            composable(Screen.TasksExams.route) {
                TasksExamsScreen(
                    state = uiState,
                    viewModel = scheduleViewModel
                )
            }

            composable("TASKS_EXAMS") {
                TasksExamsScreen(
                    state = uiState,
                    viewModel = scheduleViewModel
                )
            }

            composable(Screen.Courses.route) {
                CourseListScreen(viewModel = scheduleViewModel)
            }

            composable(Screen.More.route) {
                MoreScreen(
                    onNavigateTo = { destination ->
                        if (destination == "QUICK_NOTE") {
                            showQuickNoteDialog = true
                        } else {
                            val targetRoute = when (destination) {
                                "IMPORT_SCHEDULE" -> "import_schedule"
                                "TASKS_EXAMS" -> Screen.TasksExams.route
                                "CHAT" -> "chat"
                                "NOTES" -> "notes"
                                "USAGE" -> "usage"
                                "ACADEMIC_CALENDAR" -> "academic_calendar"
                                "SETTINGS" -> "settings"
                                "ABOUT" -> "about"
                                "HOW_TO_USE" -> "how_to_use"
                                else -> destination.lowercase()
                            }
                            navController.navigate(targetRoute)
                        }
                    }
                )
            }

            // Sub-destinations reachable from MoreScreen or shortcuts
            composable("import_schedule") {
                ImportScheduleScreen(
                    importViewModel = importViewModel,
                    scheduleViewModel = scheduleViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToTimetable = {
                        navController.navigate(Screen.Timetable.route) {
                            popUpTo(Screen.Dashboard.route)
                        }
                    }
                )
            }

            composable("IMPORT_SCHEDULE") {
                ImportScheduleScreen(
                    importViewModel = importViewModel,
                    scheduleViewModel = scheduleViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToTimetable = {
                        navController.navigate(Screen.Timetable.route) {
                            popUpTo(Screen.Dashboard.route)
                        }
                    }
                )
            }



            composable("notes") {
                NotesScreen(
                    state = uiState,
                    viewModel = scheduleViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("NOTES") {
                NotesScreen(
                    state = uiState,
                    viewModel = scheduleViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("usage") {
                UsageScreen(
                    state = uiState,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("USAGE") {
                UsageScreen(
                    state = uiState,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("academic_calendar") {
                AcademicCalendarScreen(
                    state = uiState,
                    viewModel = scheduleViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("ACADEMIC_CALENDAR") {
                AcademicCalendarScreen(
                    state = uiState,
                    viewModel = scheduleViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("settings") {
                SettingsScreen(
                    viewModel = scheduleViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onOpenThemeCustomization = { navController.navigate("theme_customization") }
                )
            }

            composable("SETTINGS") {
                SettingsScreen(
                    viewModel = scheduleViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onOpenThemeCustomization = { navController.navigate("theme_customization") }
                )
            }

            composable("theme_customization") {
                ThemeCustomizationScreen(
                    viewModel = scheduleViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("THEME_CUSTOMIZATION") {
                ThemeCustomizationScreen(
                    viewModel = scheduleViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("about") {
                AboutScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("ABOUT") {
                AboutScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("how_to_use") {
                HowToUseScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("HOW_TO_USE") {
                HowToUseScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }

        if (isFocusLocked) {
            FocusLockScreen(
                endTimeMillis = uiState.focusLockEndTimeMillis,
                totalDurationSeconds = uiState.focusLockDurationSeconds,
                courseName = uiState.focusLockCourseName,
                whitelistedPackages = uiState.focusWhitelistedPackages,
                onEmergencyUnlock = {
                    scheduleViewModel.stopFocusLock(context)
                }
            )
        }

        if (showQuickNoteDialog) {
            QuickNoteDialog(
                onDismiss = { showQuickNoteDialog = false },
                onSaveNote = { title, content ->
                    scheduleViewModel.addNote(0L, "$title\n$content", "Quick Note")
                },
                onSaveTask = { title, priority ->
                    scheduleViewModel.addTask(
                        title = title,
                        courseName = "General",
                        priority = priority
                    )
                }
            )
        }
    }
}
