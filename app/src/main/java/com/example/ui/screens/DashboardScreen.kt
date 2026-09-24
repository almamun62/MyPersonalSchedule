package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.data.model.CourseEntity
import com.example.data.model.HolidayOverrideEntity
import com.example.data.model.HolidayOverrideType
import com.example.data.model.TaskEntity
import com.example.domain.CourseClassStatus
import com.example.domain.CourseWithStatus
import com.example.domain.DayResolutionResult
import com.example.service.DndManager
import com.example.service.FocusLockService
import com.example.service.SleepAlarmScheduler
import com.example.ui.components.AddTaskDialog
import com.example.ui.components.CourseAlertSettingsDialog
import com.example.ui.components.ConflictResolutionCard
import com.example.ui.components.FocusLockSetupDialog
import com.example.ui.components.FocusAppWhitelistDialog
import com.example.ui.components.SleepAlarmDialog
import com.example.ui.theme.tr
import com.example.ui.viewmodel.ScheduleUiState
import com.example.ui.viewmodel.ScheduleViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun DashboardScreen(
    state: ScheduleUiState,
    viewModel: ScheduleViewModel,
    onNavigateToTimetable: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var taskDialogInitialCourse by remember { mutableStateOf("") }
    var showSleepAlarmDialog by remember { mutableStateOf(false) }
    var creditDialogCourseName by remember { mutableStateOf<String?>(null) }
    var showCourseAlertSettingsDialog by remember { mutableStateOf(false) }
    var showFocusSetupDialog by remember { mutableStateOf(false) }
    var showFocusWhitelistDialog by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshDndPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (showAddTaskDialog) {
        AddTaskDialog(
            initialCourseName = taskDialogInitialCourse,
            courses = state.courses,
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { title, course, priority ->
                viewModel.addTask(title, course, priority)
                showAddTaskDialog = false
            }
        )
    }

    if (showSleepAlarmDialog) {
        SleepAlarmDialog(
            isEnabled = state.isSleepAlarmEnabled,
            initialBedtime = state.sleepBedtime,
            initialWakeTime = state.sleepWakeTime,
            initialSmartWake = state.sleepSmartWakeEnabled,
            initialAdvanceMin = state.sleepWakeAdvanceMinutes,
            initialTargetHours = state.sleepTargetHours,
            initialVibrate = state.sleepAlarmVibrate,
            initialSound = state.sleepAlarmSound,
            onDismiss = { showSleepAlarmDialog = false },
            onSave = { enabled, bedtime, wakeTime, smartWake, advanceMin, targetHours, vibrate, sound ->
                viewModel.updateSleepSchedule(
                    context = context,
                    enabled = enabled,
                    bedtime = bedtime,
                    wakeTime = wakeTime,
                    smartWake = smartWake,
                    advanceMin = advanceMin,
                    targetHours = targetHours,
                    vibrate = vibrate,
                    sound = sound
                )
                showSleepAlarmDialog = false
            },
            onTestAlarm = {
                viewModel.testSleepAlarm(context)
            },
            onSyncSystemClock = { hour, minute, message ->
                viewModel.setAlarmInClockApp(context, hour, minute, message)
            }
        )
    }

    if (creditDialogCourseName != null) {
        val courseName = creditDialogCourseName!!
        val currentCredit = state.courseCreditsMap[courseName] ?: 3.0f
        CourseCreditDialog(
            courseName = courseName,
            initialCredit = currentCredit,
            onDismiss = { creditDialogCourseName = null },
            onSave = { cr ->
                viewModel.setCourseCredits(courseName, cr)
                creditDialogCourseName = null
            }
        )
    }

    if (showCourseAlertSettingsDialog) {
        CourseAlertSettingsDialog(
            globalAdvanceMinutes = state.classReminderMinutes,
            courses = state.courses,
            courseSpecificAlerts = state.courseReminderMinutes,
            onDismiss = { showCourseAlertSettingsDialog = false },
            onSaveGlobalMinutes = { mins ->
                viewModel.setClassReminderMinutes(mins)
            },
            onSetCourseAlert = { courseName, mins ->
                viewModel.setCourseReminderMinutes(courseName, mins)
            }
        )
    }

    var showUsageAccessPermissionDialog by remember { mutableStateOf(false) }

    if (showUsageAccessPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showUsageAccessPermissionDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Usage Access Permission Required".tr, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = "To strictly block distracting apps while your phone is locked, Study Focus needs 'Usage Access' permission. Please grant this permission in the system settings screen that opens next.".tr,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(onClick = {
                    showUsageAccessPermissionDialog = false
                    FocusLockService.requestUsageStatsPermission(context)
                }) {
                    Text("Open Settings".tr)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUsageAccessPermissionDialog = false }) {
                    Text("Cancel".tr)
                }
            }
        )
    }

    if (showFocusSetupDialog) {
        FocusLockSetupDialog(
            availableCourses = state.courses.map { it.name }.distinct(),
            whitelistedAppsCount = state.focusWhitelistedPackages.size,
            onOpenWhitelistPicker = { showFocusWhitelistDialog = true },
            onDismiss = { showFocusSetupDialog = false },
            onStartLock = { durationSeconds, courseName ->
                showFocusSetupDialog = false
                if (!FocusLockService.hasUsageStatsPermission(context)) {
                    showUsageAccessPermissionDialog = true
                } else {
                    viewModel.startFocusLock(context, durationSeconds, courseName)
                }
            }
        )
    }

    if (showFocusWhitelistDialog) {
        FocusAppWhitelistDialog(
            currentWhitelist = state.focusWhitelistedPackages,
            onDismiss = { showFocusWhitelistDialog = false },
            onSaveWhitelist = { newWhitelist ->
                viewModel.setFocusWhitelistedPackages(newWhitelist)
                showFocusWhitelistDialog = false
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp)
    ) {
        // 1. SEMESTER PROGRESS HEADER
        item {
            SemesterProgressCard(
                semesterName = state.activeSemester?.name ?: "Academic Term",
                currentWeek = state.currentAcademicWeek,
                totalWeeks = state.activeSemester?.totalWeeks ?: 16,
                progressPercent = state.semesterProgressPercent,
                upcomingHoliday = state.upcomingHoliday
            )
        }

        // 2. STUDY FOCUS CARD (Prominent at top)
        item {
            StudyFocusLockCard(
                whitelistedCount = state.focusWhitelistedPackages.size,
                onStartLock = { showFocusSetupDialog = true },
                onManageWhitelist = { showFocusWhitelistDialog = true }
            )
        }

        // 2. TODAY'S CONFLICT RESOLUTION (if overlapping classes today)
        val todayDayOfWeek = LocalDate.now().dayOfWeek.value
        val todayConflicts = state.selectedWeekConflicts.filter { it.dayOfWeek == todayDayOfWeek }
        if (todayConflicts.isNotEmpty()) {
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    todayConflicts.forEach { conflict ->
                        ConflictResolutionCard(
                            conflict = conflict,
                            currentWeek = state.currentAcademicWeek,
                            sessionPriorities = state.sessionCoursePriorities,
                            timeBlockPreferences = state.timeBlockPermanentPreferences,
                            preferredConflictCourseIds = state.preferredConflictCourseIds,
                            hideAudited = state.hideAuditedInConflict,
                            onPrioritizeSession = { conf, prioritized, other ->
                                viewModel.prioritizeCourseForCurrentSession(
                                    state.currentAcademicWeek,
                                    conf.dayOfWeek,
                                    conf.overlapStartPeriod,
                                    prioritized,
                                    other
                                )
                            },
                            onSetPermanentPreference = { conf, prioritized, other ->
                                viewModel.setTimeBlockPermanentPreference(
                                    conf.dayOfWeek,
                                    conf.overlapStartPeriod,
                                    conf.overlapEndPeriod,
                                    prioritized,
                                    other
                                )
                            },
                            onClearPreference = { conf ->
                                viewModel.clearConflictPreference(conf, state.currentAcademicWeek)
                            },
                            onToggleHideAudited = { hide ->
                                viewModel.setHideAuditedInConflict(hide)
                            },
                            onOpenAdvancedOptions = { _ -> onNavigateToTimetable() }
                        )
                    }
                }
            }
        }

        // 3. TODAY'S TIMELINE (Primary focus: classes today with live countdown!)
        item {
            TodayTimelineSection(
                resolution = state.todayResolution,
                coursesWithStatus = state.todayCoursesWithStatus,
                onAddCourseTask = { courseName ->
                    taskDialogInitialCourse = courseName
                    showAddTaskDialog = true
                },
                onViewFullTimetable = onNavigateToTimetable
            )
        }

        // 4. SLEEP & WAKE-UP ALARM SYSTEM (User can configure / toggle as desired)
        item {
            val tomorrowMorningClass = state.tomorrowResolution?.activeCourses?.minByOrNull { it.startTime }
            val nextClassWithStatus = if (tomorrowMorningClass != null) {
                CourseWithStatus(
                    course = tomorrowMorningClass,
                    status = CourseClassStatus.UPCOMING,
                    minutesUntilStart = 0,
                    minutesRemaining = 0
                )
            } else null

            SleepAlarmCard(
                isEnabled = state.isSleepAlarmEnabled,
                bedtime = state.sleepBedtime,
                wakeTime = state.sleepWakeTime,
                isSmartWake = state.sleepSmartWakeEnabled,
                advanceMinutes = state.sleepWakeAdvanceMinutes,
                targetHours = state.sleepTargetHours,
                nextMorningClass = nextClassWithStatus,
                onToggle = { enabled ->
                    viewModel.toggleSleepAlarm(context, enabled)
                },
                onConfigure = {
                    showSleepAlarmDialog = true
                },
                onOpenClockApp = {
                    SleepAlarmScheduler.openClockApp(context)
                }
            )
        }

        // 5. LECTURE ALERTS & IN-CLASS FOCUS (Consolidated 15m alert + auto DND)
        item {
            val nextUpcoming = state.todayCoursesWithStatus.find {
                it.status == CourseClassStatus.NEXT_UP || it.status == CourseClassStatus.UPCOMING
            }
            LectureAlertsAndFocusCard(
                is15mEnabled = state.isClassReminder15mEnabled,
                scheduledCount = state.scheduledRemindersCount,
                nextUpcomingCourse = nextUpcoming,
                isDndPermissionGranted = state.isDndPermissionGranted,
                isDndActive = state.isDndActive,
                isAutoDndEnabled = state.isAutoDndEnabled,
                onToggle15m = { viewModel.toggleClassReminder15m(it) },
                onRequestDndPermission = { viewModel.requestDndPermission(context) },
                onToggleAutoDnd = { isEnabled ->
                    if (isEnabled && !DndManager.checkDndPermission(context)) {
                        viewModel.requestDndPermission(context)
                    } else {
                        viewModel.toggleAutoDnd(isEnabled)
                    }
                },
                onTest15mAlert = {
                    viewModel.test15MinuteReminder(context) { courseName ->
                        android.widget.Toast.makeText(
                            context,
                            "15m Reminder notification sent for: $courseName",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onRescheduleAlarms = {
                    viewModel.rescheduleClassReminders(context) { count ->
                        android.widget.Toast.makeText(
                            context,
                            "Synced $count class alarms from database",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onTestSticky = { viewModel.testStickyNotification(context) },
                onTestNag = { viewModel.testNagNotification(context) },
                onOpenAlertSettings = { showCourseAlertSettingsDialog = true }
            )
        }

        // 6. ACADEMIC WORKLOAD & CREDITS OVERVIEW
        item {
            AcademicWorkloadCard(
                weeklyHours = state.weeklyClassHours,
                todayHours = state.todayClassHours,
                totalCredits = state.totalCredits,
                courses = state.courses,
                creditsMap = state.courseCreditsMap,
                onOpenCreditDialog = { name -> creditDialogCourseName = name }
            )
        }

        // 7. PENDING ASSIGNMENTS & TASKS
        item {
            PendingTasksSection(
                pendingTasks = state.pendingTasks,
                onCompleteTask = { id -> viewModel.toggleTaskCompleted(id, true) },
                onAddNewTask = {
                    taskDialogInitialCourse = ""
                    showAddTaskDialog = true
                }
            )
        }

        // 8. TOMORROW'S PREVIEW (Schedule & Holiday alert)
        item {
            TomorrowPreviewCard(
                resolution = state.tomorrowResolution,
                onViewSchedule = onNavigateToTimetable
            )
        }

        // 9. DASHBOARD INSIGHTS & SUMMARY
        item {
            DashboardInsightsAnalysis(state = state)
        }
    }
}

@Composable
private fun SleepAlarmCard(
    isEnabled: Boolean,
    bedtime: String,
    wakeTime: String,
    isSmartWake: Boolean,
    advanceMinutes: Int,
    targetHours: Float,
    nextMorningClass: CourseWithStatus?,
    onToggle: (Boolean) -> Unit,
    onConfigure: () -> Unit,
    onOpenClockApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("sleep_alarm_dashboard_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = null,
                            tint = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Sleep & Wake Alarm".tr,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (isEnabled && isSmartWake) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "SMART WAKE".tr,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        Text(
                            text = if (isEnabled) "Smart class-based wake & bedtime schedule".tr else "Optional sleep & class wake-up assistant".tr,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier.testTag("sleep_alarm_switch")
                )
            }

            if (isEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.NightsStay, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                Text("Wind-down".tr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(bedtime, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                Text("Wake-up".tr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(wakeTime, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                Text("Target".tr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(String.format("%.1fh", targetHours), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (isSmartWake) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (nextMorningClass != null) {
                                    "First class tomorrow starts at ${nextMorningClass.course.startTime}. Wake alarm rings ${advanceMinutes}m ahead.".tr
                                } else {
                                    "Dynamic wake-up rings ${advanceMinutes}m before your first morning class.".tr
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = onConfigure,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("configure_sleep_alarm_button"),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Customize Schedule".tr, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = onOpenClockApp,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.AccessAlarm, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clock App".tr, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sync wake alarms with class schedules & get wind-down reminders.".tr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    TextButton(onClick = onConfigure) {
                        Text("Setup".tr, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun LectureAlertsAndFocusCard(
    is15mEnabled: Boolean,
    scheduledCount: Int,
    nextUpcomingCourse: CourseWithStatus?,
    isDndPermissionGranted: Boolean,
    isDndActive: Boolean,
    isAutoDndEnabled: Boolean,
    onToggle15m: (Boolean) -> Unit,
    onRequestDndPermission: () -> Unit,
    onToggleAutoDnd: (Boolean) -> Unit,
    onTest15mAlert: () -> Unit,
    onRescheduleAlarms: () -> Unit,
    onTestSticky: () -> Unit,
    onTestNag: () -> Unit,
    onOpenAlertSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAdvancedTools by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDndActive) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        ),
        modifier = modifier.fillMaxWidth().testTag("lecture_alerts_focus_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isDndActive) Icons.Default.DoNotDisturbOn else Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = if (isDndActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Lecture Alerts & Focus".tr,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "15-min alerts & automatic in-class silence".tr,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = { showAdvancedTools = !showAdvancedTools },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (showAdvancedTools) Icons.Default.ExpandLess else Icons.Default.MoreVert,
                        contentDescription = "Tools",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (nextUpcomingCourse != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Next: ${nextUpcomingCourse.course.name} (${nextUpcomingCourse.course.classroom.ifBlank { "TBA" }}) • In ${nextUpcomingCourse.minutesUntilStart}m",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // 15-Minute Class Alerts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "15-Minute Class Alerts".tr,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (is15mEnabled) {
                            if (scheduledCount > 0) "$scheduledCount alarms active" else "Armed and monitoring class schedule".tr
                        } else {
                            "Disabled".tr
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = is15mEnabled,
                    onCheckedChange = onToggle15m,
                    modifier = Modifier.testTag("reminder_15m_switch")
                )
            }

            OutlinedButton(
                onClick = onOpenAlertSettings,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Customize Alert Times (Global & Per-Course)".tr, fontSize = 13.sp)
            }

            // In-Class DND Mode
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "In-Class Silence Mode (DND)".tr,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (!isDndPermissionGranted) {
                            "Needs permission to mute phone in class".tr
                        } else if (isDndActive) {
                            "Active now during lecture".tr
                        } else if (isAutoDndEnabled) {
                            "Auto mutes device when lecture starts".tr
                        } else {
                            "Disabled".tr
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDndActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!isDndPermissionGranted) {
                    FilledTonalButton(
                        onClick = onRequestDndPermission,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("grant_dnd_button")
                    ) {
                        Text("Grant".tr, fontSize = 12.sp)
                    }
                } else {
                    Switch(
                        checked = isAutoDndEnabled,
                        onCheckedChange = onToggleAutoDnd,
                        modifier = Modifier.testTag("auto_dnd_switch")
                    )
                }
            }

            // Collapsible Testing Tools
            AnimatedVisibility(visible = showAdvancedTools) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Testing & Diagnostics".tr, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = onTest15mAlert,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text("Test 15m".tr, fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = onRescheduleAlarms,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text("Resync".tr, fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = onTestSticky,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text("Live Banner".tr, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AcademicWorkloadCard(
    weeklyHours: Float,
    todayHours: Float,
    totalCredits: Float,
    courses: List<CourseEntity>,
    creditsMap: Map<String, Float>,
    onOpenCreditDialog: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val distinctCourses = remember(courses) {
        courses.distinctBy { it.name }.filter { it.name.isNotBlank() }
    }
    var showCoursePickerForCredit by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        ),
        modifier = modifier.fillMaxWidth().testTag("academic_workload_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Academic Workload & Credits".tr,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Semester study hours & credit load".tr,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (distinctCourses.isNotEmpty()) {
                    TextButton(
                        onClick = { showCoursePickerForCredit = !showCoursePickerForCredit },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(if (showCoursePickerForCredit) "Hide".tr else "Credits".tr, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.weight(1f),
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                            Text("Weekly".tr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(String.format("%.1fh", weeklyHours), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.weight(1f),
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Today, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
                            Text("Today".tr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(String.format("%.1fh", todayHours), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.weight(1f),
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Grade, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(14.dp))
                            Text("Credits".tr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(String.format("%.1f", totalCredits), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }

            AnimatedVisibility(visible = showCoursePickerForCredit) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Tap a course to adjust its credit value:".tr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    distinctCourses.forEach { c ->
                        val cr = creditsMap[c.name] ?: 3.0f
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenCreditDialog(c.name) }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(c.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = String.format("%.1f cr", cr),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SemesterProgressCard(
    semesterName: String,
    currentWeek: Int,
    totalWeeks: Int,
    progressPercent: Float,
    upcomingHoliday: HolidayOverrideEntity?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("semester_progress_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = semesterName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "${"Week".tr} $currentWeek / $totalWeeks",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Text(
                        text = "${(progressPercent * 100).toInt()}%",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            val animatedProgress by animateFloatAsState(
                targetValue = progressPercent,
                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                label = "semesterProgress"
            )
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )

            if (upcomingHoliday != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (upcomingHoliday.type == HolidayOverrideType.HOLIDAY) Icons.Default.BeachAccess else Icons.Default.EventAvailable,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "${upcomingHoliday.name} (${upcomingHoliday.dateString})",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingTasksSection(
    pendingTasks: List<TaskEntity>,
    onCompleteTask: (Long) -> Unit,
    onAddNewTask: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Pending Tasks".tr,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (pendingTasks.isNotEmpty()) {
                    Badge { Text("${pendingTasks.size}") }
                }
            }

            TextButton(
                onClick = onAddNewTask,
                modifier = Modifier.testTag("add_task_header_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Task".tr)
            }
        }

        if (pendingTasks.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.CheckCircleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("All tasks completed! Ready for upcoming lectures.".tr, style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(pendingTasks) { task ->
                    TaskChip(task = task, onComplete = { onCompleteTask(task.id) })
                }
            }
        }
    }
}

@Composable
private fun TaskChip(
    task: TaskEntity,
    onComplete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .widthIn(min = 160.dp, max = 240.dp)
            .clickable { onComplete() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Checkbox(
                checked = false,
                onCheckedChange = { onComplete() },
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (task.courseName.isNotEmpty()) {
                    Text(
                        text = task.courseName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun TodayTimelineSection(
    resolution: DayResolutionResult?,
    coursesWithStatus: List<CourseWithStatus>,
    onAddCourseTask: (String) -> Unit,
    onViewFullTimetable: () -> Unit
) {
    val todayFormatted = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d"))

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Today's Schedule".tr,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = todayFormatted,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TextButton(onClick = onViewFullTimetable) {
                Text("Full Week Grid".tr)
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }

        if (resolution?.isHoliday == true) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Celebration,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            text = resolution.holidayName ?: "Official Holiday",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "All classes auto-paused per academic calendar.".tr,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        } else if (resolution?.isMakeUpDay == true) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    Text(
                        text = resolution.makeUpDescription ?: "Make-up Schedule",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }

        if (coursesWithStatus.isEmpty() && resolution?.isHoliday != true) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("No scheduled classes today! Perfect time for self-study or lab prep.".tr)
                }
            }
        } else {
            coursesWithStatus.forEach { item ->
                TodayCourseTimelineItem(
                    item = item,
                    onAddPostClassTask = { onAddCourseTask(item.course.name) }
                )
            }
        }
    }
}

@Composable
private fun TodayCourseTimelineItem(
    item: CourseWithStatus,
    onAddPostClassTask: () -> Unit
) {
    val isCompleted = item.status == CourseClassStatus.COMPLETED
    val isActive = item.status == CourseClassStatus.ACTIVE
    val isNextUp = item.status == CourseClassStatus.NEXT_UP

    val cardBg = when {
        isActive -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        isCompleted -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        isNextUp -> MaterialTheme.colorScheme.surface
        else -> MaterialTheme.colorScheme.surface
    }

    val contentAlpha = if (isCompleted) 0.45f else 1f
    val courseColor = try {
        Color(item.course.colorHex)
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = if (isActive || isNextUp) {
            androidx.compose.foundation.BorderStroke(1.5.dp, if (isActive) MaterialTheme.colorScheme.primary else courseColor)
        } else {
            androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("course_item_${item.course.id}")
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isCompleted) Color.Gray else courseColor)
                    )
                    Text(
                        text = "${item.course.startTime} - ${item.course.endTime}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                    )
                }

                when {
                    isActive -> {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                                Text(
                                    text = "LIVE: ${item.minutesRemaining}m left",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    isNextUp -> {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Text(
                                text = "Next Up: in ${item.minutesUntilStart}m",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    isCompleted -> {
                        Text(
                            text = "Completed".tr,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    else -> {
                        Text(
                            text = "In ${item.minutesUntilStart / 60}h ${item.minutesUntilStart % 60}m",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.course.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Room: ${item.course.classroom}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
                        )
                        if (item.course.instructor.isNotEmpty()) {
                            Text(
                                text = "• ${item.course.instructor}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onAddPostClassTask,
                    modifier = Modifier.testTag("post_class_task_${item.course.id}")
                ) {
                    Icon(
                        Icons.Default.PostAdd,
                        contentDescription = "Add Homework Task".tr,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = contentAlpha)
                    )
                }
            }
        }
    }
}

@Composable
private fun TomorrowPreviewCard(
    resolution: DayResolutionResult?,
    onViewSchedule: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tomorrow's Preview".tr,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onViewSchedule) {
                    Text("Details".tr, fontSize = 12.sp)
                }
            }

            if (resolution?.isHoliday == true) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Celebration, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Text(
                        text = "${resolution.holidayName}. Classes are paused.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                val count = resolution?.activeCourses?.size ?: 0
                if (count == 0) {
                    Text("No scheduled classes tomorrow. Enjoy your free time!".tr, style = MaterialTheme.typography.bodySmall)
                } else {
                    Text(
                        text = "$count course${if (count > 1) "s" else ""} scheduled tomorrow.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    resolution?.activeCourses?.take(3)?.forEach { c ->
                        Text(
                            text = "• ${c.startTime} ${c.name} (${c.classroom})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardInsightsAnalysis(state: ScheduleUiState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dashboard_insights_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Insights, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = "Weekly Analysis & Insights".tr,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            val totalClasses = state.courses.size
            val pendingTasks = state.pendingTasks.size
            val todayClasses = state.todayCoursesWithStatus.size

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Enrolled Classes".tr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$totalClasses", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Today's Lectures".tr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$todayClasses", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Pending Tasks".tr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$pendingTasks", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CourseCreditDialog(
    courseName: String,
    initialCredit: Float,
    onDismiss: () -> Unit,
    onSave: (Float) -> Unit
) {
    var selectedCredit by remember { mutableFloatStateOf(initialCredit) }
    var customText by remember { mutableStateOf(initialCredit.toString()) }
    val presets = listOf(1.0f, 1.5f, 2.0f, 2.5f, 3.0f, 3.5f, 4.0f, 5.0f)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "${"Course Credit".tr}: $courseName",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Select or enter course credits:".tr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presets.take(4).forEach { cr ->
                            FilterChip(
                                selected = selectedCredit == cr,
                                onClick = {
                                    selectedCredit = cr
                                    customText = cr.toString()
                                },
                                label = { Text("${cr} cr", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presets.drop(4).forEach { cr ->
                            FilterChip(
                                selected = selectedCredit == cr,
                                onClick = {
                                    selectedCredit = cr
                                    customText = cr.toString()
                                },
                                label = { Text("${cr} cr", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = customText,
                    onValueChange = { input ->
                        customText = input
                        val parsed = input.toFloatOrNull()
                        if (parsed != null && parsed in 0.1f..20.0f) {
                            selectedCredit = parsed
                        }
                    },
                    label = { Text("Credit Hours (e.g. 3.0)".tr) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalCr = customText.toFloatOrNull() ?: selectedCredit
                    onSave(finalCr.coerceIn(0.5f, 20.0f))
                }
            ) {
                Text("Save".tr)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel".tr)
            }
        }
    )
}

@Composable
private fun StudyFocusLockCard(
    whitelistedCount: Int,
    onStartLock: () -> Unit,
    onManageWhitelist: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        ),
        modifier = modifier.fillMaxWidth().testTag("study_focus_lock_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Study Focus".tr,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Lock phone & manage whitelisted apps ($whitelistedCount allowed)".tr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onStartLock,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Lock Phone".tr, fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = onManageWhitelist,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Apps, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Whitelist Apps".tr, fontSize = 13.sp)
                }
            }
        }
    }
}
