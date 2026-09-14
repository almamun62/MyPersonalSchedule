package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.service.CourseNotificationManager
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ScheduleViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: ScheduleViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle possible post-class prompt intent
        handleIntent(intent)

        setContent {
            MyApplicationTheme {
                // Runtime permission request for notifications (Android 13+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val notifPermissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission(),
                        onResult = { /* Handled */ }
                    )
                    LaunchedEffect(Unit) {
                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                MyScheduleApp(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == CourseNotificationManager.ACTION_POST_CLASS_PROMPT) {
            val course = intent.getStringExtra("COURSE_NAME")
            viewModel.setQuickTaskCourse(course)
        }
    }
}

enum class ScheduleScreen(val title: String) {
    DASHBOARD("Dashboard"),
    TIMETABLE("Timetable"),
    TASKS_EXAMS("Tasks & Exams"),
    CHAT("AI Chat"),
    SETTINGS("Settings")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyScheduleApp(viewModel: ScheduleViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val screens = ScheduleScreen.values()
    val pagerState = rememberPagerState(pageCount = { screens.size })
    val coroutineScope = rememberCoroutineScope()
    val currentScreen = screens[pagerState.currentPage]

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "MySchedule",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    // DND status pill
                    if (state.isDndActive) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Text(
                                text = "DND Active",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Text(
                                text = "Week ${state.currentAcademicWeek}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                NavigationBarItem(
                    selected = currentScreen == ScheduleScreen.DASHBOARD,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(ScheduleScreen.DASHBOARD.ordinal) } },
                    icon = {
                        Icon(
                            if (currentScreen == ScheduleScreen.DASHBOARD) Icons.Default.Dashboard else Icons.Outlined.Dashboard,
                            contentDescription = "Dashboard"
                        )
                    },
                    label = { Text("Dashboard") },
                    modifier = Modifier.testTag("nav_dashboard")
                )
                NavigationBarItem(
                    selected = currentScreen == ScheduleScreen.TIMETABLE,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(ScheduleScreen.TIMETABLE.ordinal) } },
                    icon = {
                        Icon(
                            if (currentScreen == ScheduleScreen.TIMETABLE) Icons.Default.CalendarMonth else Icons.Outlined.CalendarMonth,
                            contentDescription = "Timetable"
                        )
                    },
                    label = { Text("Timetable") },
                    modifier = Modifier.testTag("nav_timetable")
                )
                NavigationBarItem(
                    selected = currentScreen == ScheduleScreen.TASKS_EXAMS,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(ScheduleScreen.TASKS_EXAMS.ordinal) } },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (state.pendingTasks.isNotEmpty()) {
                                    Badge { Text("${state.pendingTasks.size}") }
                                }
                            }
                        ) {
                            Icon(
                                if (currentScreen == ScheduleScreen.TASKS_EXAMS) Icons.Default.TaskAlt else Icons.Outlined.TaskAlt,
                                contentDescription = "Tasks"
                            )
                        }
                    },
                    label = { Text("Tasks") },
                    modifier = Modifier.testTag("nav_tasks")
                )
                NavigationBarItem(
                    selected = currentScreen == ScheduleScreen.CHAT,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(ScheduleScreen.CHAT.ordinal) } },
                    icon = {
                        Icon(
                            if (currentScreen == ScheduleScreen.CHAT) Icons.Default.Chat else Icons.Outlined.Chat,
                            contentDescription = "Chat"
                        )
                    },
                    label = { Text("Chat") },
                    modifier = Modifier.testTag("nav_chat")
                )
                NavigationBarItem(
                    selected = currentScreen == ScheduleScreen.SETTINGS,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(ScheduleScreen.SETTINGS.ordinal) } },
                    icon = {
                        Icon(
                            if (currentScreen == ScheduleScreen.SETTINGS) Icons.Default.Tune else Icons.Outlined.Tune,
                            contentDescription = "Settings"
                        )
                    },
                    label = { Text("Settings") },
                    modifier = Modifier.testTag("nav_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = true
            ) { page ->
                when (screens[page]) {
                    ScheduleScreen.DASHBOARD -> DashboardScreen(
                        state = state,
                        viewModel = viewModel,
                        onNavigateToTimetable = { coroutineScope.launch { pagerState.animateScrollToPage(ScheduleScreen.TIMETABLE.ordinal) } }
                    )
                    ScheduleScreen.TIMETABLE -> TimetableScreen(
                        state = state,
                        viewModel = viewModel
                    )
                    ScheduleScreen.TASKS_EXAMS -> TasksExamsScreen(
                        state = state,
                        viewModel = viewModel
                    )
                    ScheduleScreen.CHAT -> ChatScreen()
                    ScheduleScreen.SETTINGS -> SettingsScreen(
                        state = state,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme { Greeting("Android") }
}
