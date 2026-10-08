package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.service.FocusLockService
import com.example.ui.viewmodel.ScheduleViewModel
import kotlinx.coroutines.delay

data class FocusPreset(
    val name: String,
    val minutes: Int,
    val icon: String
)

private val FOCUS_PRESETS = listOf(
    FocusPreset("Pomodoro", 25, "🍅"),
    FocusPreset("Deep Work", 45, "🧠"),
    FocusPreset("Exam Prep", 50, "📚"),
    FocusPreset("Quick Review", 15, "⚡"),
    FocusPreset("Short Break", 5, "☕")
)

data class AllowedAppItem(
    val id: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val isBuiltIn: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyFocusDialog(
    viewModel: ScheduleViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val allCourses by viewModel.filteredCourses.collectAsStateWithLifecycle()
    val allowedAppsPrefs by viewModel.userPreferencesManager.focusAllowedApps.collectAsStateWithLifecycle()

    val prefs = viewModel.userPreferencesManager
    val focusActivePref by prefs.focusActive.collectAsStateWithLifecycle()
    val focusEndTimestampPref by prefs.focusEndTimestamp.collectAsStateWithLifecycle()
    val focusTotalMinutesPref by prefs.focusTotalMinutes.collectAsStateWithLifecycle()
    val focusCourseNamePref by prefs.focusCourseName.collectAsStateWithLifecycle()

    var selectedPresetMinutes by remember { mutableIntStateOf(25) }
    var selectedCourseName by remember { mutableStateOf("General Study") }
    var isRunning by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var targetEndTimeMillis by remember { mutableLongStateOf(0L) }
    var remainingSeconds by remember { mutableIntStateOf(25 * 60) }
    var completedSessionsToday by remember { mutableIntStateOf(2) }

    val hasUsageStats = remember(context) {
        try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? android.app.AppOpsManager
            val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                appOps?.unsafeCheckOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
            } else {
                appOps?.checkOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
            }
            mode == android.app.AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    // Lock Task / Screen Pinning & Screen-On Lock
    fun lockScreenAndPinApp() {
        activity?.let { act ->
            try {
                act.startLockTask()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            try {
                act.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                val controller = WindowCompat.getInsetsController(act.window, act.window.decorView)
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun unlockScreenAndUnpinApp() {
        activity?.let { act ->
            try {
                act.stopLockTask()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            try {
                act.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                val controller = WindowCompat.getInsetsController(act.window, act.window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Restore running focus session if process was restarted
    LaunchedEffect(focusActivePref, focusEndTimestampPref) {
        if (focusActivePref && focusEndTimestampPref > System.currentTimeMillis()) {
            targetEndTimeMillis = focusEndTimestampPref
            selectedPresetMinutes = focusTotalMinutesPref
            selectedCourseName = focusCourseNamePref
            val remSec = ((focusEndTimestampPref - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L).toInt()
            remainingSeconds = remSec
            isRunning = true
            isPaused = false
            lockScreenAndPinApp()
        }
    }

    var showEmergencyExitDialog by remember { mutableStateOf(false) }
    var showConfigureAppsDialog by remember { mutableStateOf(false) }
    var showLibraryReader by remember { mutableStateOf(false) }
    var showNoteCanvasDialog by remember { mutableStateOf(false) }
    var showCalculatorDialog by remember { mutableStateOf(false) }

    // Map allowed app IDs to labels and icons (including offline built-in tools)
    val resolvedAllowedApps = remember(allowedAppsPrefs) {
        val pm = context.packageManager
        // If empty or defaults, ensure 3 standard tools
        val currentPrefs = if (allowedAppsPrefs.isEmpty()) {
            listOf("internal_calculator", "internal_notes", "internal_materials")
        } else {
            allowedAppsPrefs.take(3)
        }

        currentPrefs.map { toolId ->
            when (toolId) {
                "internal_calculator" -> AllowedAppItem("internal_calculator", "Calculator", Icons.Outlined.Calculate, isBuiltIn = true)
                "internal_notes" -> AllowedAppItem("internal_notes", "Take Notes", Icons.Outlined.Draw, isBuiltIn = true)
                "internal_materials" -> AllowedAppItem("internal_materials", "Materials", Icons.Outlined.MenuBook, isBuiltIn = true)
                else -> {
                    val label = try {
                        val appInfo = pm.getApplicationInfo(toolId, 0)
                        pm.getApplicationLabel(appInfo).toString()
                    } catch (e: Exception) {
                        if (toolId.contains("calc", ignoreCase = true)) "Calculator"
                        else if (toolId.contains("chrome") || toolId.contains("browser")) "Browser"
                        else if (toolId.contains("note") || toolId.contains("keep")) "Notes"
                        else "Study Tool"
                    }
                    val icon = if (toolId.contains("calc", ignoreCase = true)) Icons.Outlined.Calculate
                    else if (toolId.contains("chrome") || toolId.contains("browser")) Icons.Outlined.Language
                    else if (toolId.contains("note") || toolId.contains("keep")) Icons.Outlined.EditNote
                    else Icons.Outlined.Apps
                    AllowedAppItem(toolId, label, icon, isBuiltIn = false)
                }
            }
        }
    }

    // Intercept back button when active: User cannot close app during Focus Mode
    BackHandler(enabled = isRunning) {
        showEmergencyExitDialog = true
    }

    // Countdown Coroutine driven by targetEndTimeMillis
    LaunchedEffect(isRunning, isPaused, targetEndTimeMillis) {
        while (isRunning && !isPaused) {
            val now = System.currentTimeMillis()
            val remSec = ((targetEndTimeMillis - now) / 1000L).coerceAtLeast(0L).toInt()
            remainingSeconds = remSec
            if (remSec <= 0) {
                isRunning = false
                completedSessionsToday++
                unlockScreenAndUnpinApp()
                prefs.clearFocusSession()
                FocusLockService.stopFocus(context)
                break
            }
            delay(1000L)
        }
    }

    fun startTimer() {
        val durationMin = selectedPresetMinutes
        val endMs = System.currentTimeMillis() + (durationMin * 60 * 1000L)
        targetEndTimeMillis = endMs
        remainingSeconds = durationMin * 60
        isRunning = true
        isPaused = false
        prefs.setFocusSession(active = true, endTimestamp = endMs, totalMinutes = durationMin, courseName = selectedCourseName)
        lockScreenAndPinApp()
        FocusLockService.startFocus(context, durationMin, selectedCourseName, endMs)
    }

    fun pauseTimer() {
        isPaused = true
    }

    fun resumeTimer() {
        if (targetEndTimeMillis > System.currentTimeMillis()) {
            isPaused = false
        } else {
            startTimer()
        }
    }

    val totalSec = (selectedPresetMinutes * 60).coerceAtLeast(1)
    val progress = (1f - (remainingSeconds.toFloat() / totalSec.toFloat())).coerceIn(0f, 1f)
    val minutesDisplay = remainingSeconds / 60
    val secondsDisplay = remainingSeconds % 60
    val timeFormatted = String.format("%02d:%02d", minutesDisplay, secondsDisplay)

    Dialog(
        onDismissRequest = {
            if (!isRunning) onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isRunning,
            dismissOnClickOutside = !isRunning
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                modifier = Modifier.statusBarsPadding(),
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isRunning) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFEF4444)
                                    ) {
                                        Text(
                                            text = "🔒 PINNED LOCKED MODE",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                } else {
                                    Column {
                                        Text("Study Focus Mode", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                        Text("Pinned Kiosk Mode • Max 3 Allowed Apps", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        },
                        navigationIcon = {
                            if (!isRunning) {
                                IconButton(onClick = onDismiss) {
                                    Icon(Icons.Default.Close, contentDescription = "Close")
                                }
                            }
                        },
                        actions = {
                            if (!isRunning) {
                                IconButton(onClick = { showConfigureAppsDialog = true }) {
                                    Icon(Icons.Outlined.Apps, contentDescription = "Allowed Apps (Max 3)")
                                }
                            } else {
                                TextButton(onClick = { showEmergencyExitDialog = true }) {
                                    Text("Emergency Unlock", color = MaterialTheme.colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    )
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Pinned Kiosk Alert Banner
                    if (isRunning) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF7F1D1D),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                Text(
                                    text = "App is pinned to screen. Exit, home, and unauthorized apps are locked until session completes.",
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    lineHeight = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // 1. Preset Selector (if idle)
                    if (!isRunning) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "FOCUS DURATION",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(FOCUS_PRESETS) { preset ->
                                    val isSelected = selectedPresetMinutes == preset.minutes
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.clickable {
                                            selectedPresetMinutes = preset.minutes
                                            remainingSeconds = preset.minutes * 60
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(preset.icon, fontSize = 14.sp)
                                            Text(
                                                text = "${preset.minutes}m",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "LINKED COURSE / SUBJECT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                            val courseOptions = remember(allCourses) {
                                listOf("General Study") + allCourses.map { it.name }.distinct()
                            }
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(courseOptions) { cName ->
                                    FilterChip(
                                        selected = selectedCourseName == cName,
                                        onClick = { selectedCourseName = cName },
                                        label = { Text(cName, fontSize = 12.sp, fontWeight = FontWeight.Medium) }
                                    )
                                }
                            }
                        }
                    }

                    // 2. Giant Circular Countdown Timer
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(230.dp)
                            .padding(8.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { if (isRunning) progress else 0f },
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 10.dp,
                            color = if (isRunning) (if (isPaused) MaterialTheme.colorScheme.outline else Color(0xFF10B981)) else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = timeFormatted,
                                fontSize = 44.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isRunning) Color(0xFF064E3B) else MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = if (isRunning) (if (isPaused) "PAUSED" else "STUDY LOCKED") else "READY",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isRunning) Color(0xFF10B981) else MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                            Text(
                                text = selectedCourseName,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }

                    // 3. Quick Action Bar: Read Course Material & Take Notes Canvas
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showLibraryReader = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(
                                Icons.Outlined.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Course Material",
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = { showNoteCanvasDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Draw,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Note Canvas",
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // 4. Maximum 3 Allowed Apps Bar
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "PERMITTED TOOLS (MAX 3 APPS)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.sp
                                )
                                TextButton(onClick = { showConfigureAppsDialog = true }) {
                                    Text("Customize (3)", fontSize = 11.sp)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                resolvedAllowedApps.forEach { appItem ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(52.dp)
                                            .clickable {
                                                when (appItem.id) {
                                                    "internal_calculator" -> {
                                                        showCalculatorDialog = true
                                                    }
                                                    "internal_notes" -> {
                                                        showNoteCanvasDialog = true
                                                    }
                                                    "internal_materials" -> {
                                                        showLibraryReader = true
                                                    }
                                                    else -> {
                                                        // External app launch
                                                        try {
                                                            val launchIntent = context.packageManager.getLaunchIntentForPackage(appItem.id)
                                                            if (launchIntent != null) {
                                                                context.startActivity(launchIntent)
                                                            } else if (appItem.id.contains("calc", ignoreCase = true)) {
                                                                // Open our built-in offline calculator directly!
                                                                showCalculatorDialog = true
                                                            } else if (appItem.id.contains("note") || appItem.id.contains("keep")) {
                                                                showNoteCanvasDialog = true
                                                            }
                                                        } catch (e: Exception) {
                                                            if (appItem.id.contains("calc", ignoreCase = true)) {
                                                                showCalculatorDialog = true
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(appItem.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(appItem.label, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 5. Start / Control Buttons
                    if (!isRunning) {
                        Button(
                            onClick = { startTimer() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Turn On Pinned Focus Mode", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (isPaused) {
                                Button(
                                    onClick = { resumeTimer() },
                                    modifier = Modifier.weight(1f).height(50.dp),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Resume", fontWeight = FontWeight.Bold)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { pauseTimer() },
                                    modifier = Modifier.weight(1f).height(50.dp),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Default.Pause, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Pause")
                                }
                            }

                            Button(
                                onClick = { showEmergencyExitDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.LockOpen, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("End Session")
                            }
                        }
                    }

                    // Completed Stats
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Sessions completed today: $completedSessionsToday • ${completedSessionsToday * 25}m focused",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Emergency Unlock Confirmation Dialog
    if (showEmergencyExitDialog) {
        var confirmHoldingSeconds by remember { mutableIntStateOf(5) }
        var isHolding by remember { mutableStateOf(false) }

        LaunchedEffect(isHolding) {
            while (isHolding && confirmHoldingSeconds > 0) {
                delay(1000L)
                confirmHoldingSeconds--
            }
            if (confirmHoldingSeconds == 0 && isHolding) {
                isRunning = false
                isPaused = false
                remainingSeconds = selectedPresetMinutes * 60
                unlockScreenAndUnpinApp()
                FocusLockService.stopFocus(context)
                showEmergencyExitDialog = false
            }
        }

        AlertDialog(
            onDismissRequest = {
                if (!isRunning) showEmergencyExitDialog = false
            },
            icon = {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(36.dp))
            },
            title = {
                Text("Emergency Unlock", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Study lock prevents interruptions. To unpin the app and end your focus session before the timer ends, hold the unlock button for 5 seconds.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    if (isHolding) {
                        LinearProgressIndicator(
                            progress = { (5 - confirmHoldingSeconds) / 5f },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isHolding = !isHolding
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isHolding) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(if (isHolding) "Holding ($confirmHoldingSeconds s)..." else "Hold to Unlock")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    isHolding = false
                    confirmHoldingSeconds = 5
                    showEmergencyExitDialog = false
                }) {
                    Text("Stay in Focus")
                }
            }
        )
    }

    // Configure 3 Allowed Apps Dialog
    if (showConfigureAppsDialog) {
        var selectedList by remember {
            val initial = if (allowedAppsPrefs.isEmpty()) {
                listOf("internal_calculator", "internal_notes", "internal_materials")
            } else {
                allowedAppsPrefs.take(3)
            }
            mutableStateOf(initial.toMutableList())
        }

        val availableTools = listOf(
            "internal_calculator" to "Scientific Calculator (Built-in Offline)",
            "internal_notes" to "Drawing & Note Taking Canvas (Built-in)",
            "internal_materials" to "Course Material Library Reader (Built-in)",
            "com.google.android.calculator" to "Google Calculator",
            "com.android.calculator2" to "System Calculator",
            "com.android.chrome" to "Chrome Browser",
            "com.google.android.keep" to "Google Keep",
            "com.google.android.apps.translate" to "Google Translate"
        )

        AlertDialog(
            onDismissRequest = { showConfigureAppsDialog = false },
            title = { Text("Configure Allowed Apps (Max 3)", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select up to 3 tools accessible during pinned Focus Mode:", fontSize = 12.sp)

                    availableTools.forEach { (toolId, name) ->
                        val isChecked = selectedList.contains(toolId)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val updated = selectedList.toMutableList()
                                    if (isChecked) {
                                        updated.remove(toolId)
                                    } else if (updated.size < 3) {
                                        updated.add(toolId)
                                    }
                                    selectedList = updated
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { check ->
                                    val updated = selectedList.toMutableList()
                                    if (!check) {
                                        updated.remove(toolId)
                                    } else if (updated.size < 3) {
                                        updated.add(toolId)
                                    }
                                    selectedList = updated
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(name, fontSize = 12.sp)
                        }
                    }

                    Text("Selected: ${selectedList.size}/3 apps", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.userPreferencesManager.setFocusAllowedApps(selectedList.take(3))
                        showConfigureAppsDialog = false
                    }
                ) {
                    Text("Save App List")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfigureAppsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Built-in Offline Calculator
    if (showCalculatorDialog) {
        OfflineCalculatorDialog(
            viewModel = viewModel,
            isExamLockedMode = isRunning,
            onDismiss = { showCalculatorDialog = false }
        )
    }

    // In-App Note Taking & Drawing Canvas
    if (showNoteCanvasDialog) {
        val courseId = remember(selectedCourseName, allCourses) {
            allCourses.find { it.name.equals(selectedCourseName, ignoreCase = true) }?.id ?: 0L
        }
        NoteTakingCanvasDialog(
            viewModel = viewModel,
            initialCourseId = courseId,
            onDismiss = { showNoteCanvasDialog = false }
        )
    }

    // In-App Course Library & Reader
    if (showLibraryReader) {
        CourseMaterialLibraryDialog(
            viewModel = viewModel,
            initialCourseName = selectedCourseName,
            onDismiss = { showLibraryReader = false }
        )
    }
}
