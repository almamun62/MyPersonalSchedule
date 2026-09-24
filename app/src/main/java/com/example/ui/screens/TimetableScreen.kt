package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CourseEntity
import com.example.data.model.SectionTiming
import com.example.domain.model.Course
import com.example.ui.components.SectionTimingsDialog
import com.example.ui.components.SpreadsheetImportDialog
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.tr
import com.example.ui.viewmodel.ScheduleViewModel
import com.example.util.IcsExporter
import com.example.util.ScheduleImportHelper
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val DAYS_CHINESE = listOf(
    Pair(1, "一"),
    Pair(2, "二"),
    Pair(3, "三"),
    Pair(4, "四"),
    Pair(5, "五"),
    Pair(6, "六"),
    Pair(7, "日")
)

private val DAYS_ENGLISH = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    viewModel: ScheduleViewModel,
    onNavigateToImport: () -> Unit,
    onNavigateToChat: (() -> Unit)? = null,
    onNavigateToCourses: (() -> Unit)? = null,
    onNavigateToAbout: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val selectedDay by viewModel.selectedDay.collectAsStateWithLifecycle()
    val selectedWeek by viewModel.selectedWeek.collectAsStateWithLifecycle()
    val selectedSemester by viewModel.selectedSemester.collectAsStateWithLifecycle()
    val allCourses by viewModel.filteredCourses.collectAsStateWithLifecycle()
    val isFullWeekView by viewModel.isFullWeekView.collectAsStateWithLifecycle()
    val isCompactMode by viewModel.isCompactMode.collectAsStateWithLifecycle()
    val timings by viewModel.userPreferencesManager.sectionTimings.collectAsStateWithLifecycle()
    val isReminderEnabled by viewModel.userPreferencesManager.isClassReminder15mEnabled.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Dialog & menu states
    var showAddCourseDialog by remember { mutableStateOf(false) }
    var initialAddDay by remember { mutableIntStateOf(1) }
    var initialAddPeriod by remember { mutableIntStateOf(1) }
    var courseToEdit by remember { mutableStateOf<Course?>(null) }
    var courseDetailToShow by remember { mutableStateOf<Course?>(null) }

    var showImportMenu by remember { mutableStateOf(false) }
    var showShareMenu by remember { mutableStateOf(false) }
    var showMoreSheet by remember { mutableStateOf(false) }
    var showSectionTimingsDialog by remember { mutableStateOf(false) }
    var showQuickPasteDialog by remember { mutableStateOf(false) }
    var showTimetableSettingsDialog by remember { mutableStateOf(false) }
    var showWidgetsDialog by remember { mutableStateOf(false) }
    var showSpreadsheetImportDialog by remember { mutableStateOf(false) }
    var showGlobalSearch by remember { mutableStateOf(false) }

    // File picker launcher for Excel/CSV
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onNavigateToImport()
        }
    }

    // Dynamic dates for current week
    val today = remember { LocalDate.now() }
    val mondayThisWeek = remember(today) {
        today.minusDays((today.dayOfWeek.value - 1).toLong())
    }
    val currentMonth = remember(today) { today.monthValue }
    val selectedDayChinese = DAYS_CHINESE.firstOrNull { it.first == selectedDay }?.second ?: "三"

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SmallFloatingActionButton(
                    onClick = { showSpreadsheetImportDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.testTag("timetable_excel_pdf_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.UploadFile,
                        contentDescription = "Excel & PDF Import",
                        modifier = Modifier.size(20.dp)
                    )
                }

                ExtendedFloatingActionButton(
                    onClick = {
                        initialAddDay = selectedDay
                        initialAddPeriod = 1
                        showAddCourseDialog = true
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null
                        )
                    },
                    text = {
                        Text(
                            text = "Add Course".tr,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("timetable_add_course_fab")
                )
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    // Left Header: "第4周 周三" + "9/23/26" (Screenshot 1)
                    Column(
                        modifier = Modifier
                            .clickable { showMoreSheet = true }
                            .padding(vertical = 4.dp)
                    ) {
                        val currentLang = LocalAppLanguage.current
                        val isEnglish = currentLang == "en" || !currentLang.startsWith("zh")
                        val dayEnglishName = DAYS_ENGLISH.getOrNull(selectedDay - 1) ?: "Wed"
                        val headerText = if (isEnglish) "Week $selectedWeek, $dayEnglishName" else "第$selectedWeek 周 周$selectedDayChinese"
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = headerText,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                            )
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Switch Week",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = today.format(DateTimeFormatter.ofPattern("M/d/yy")),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                },
                actions = {
                    // Global Search Button
                    IconButton(
                        onClick = { showGlobalSearch = true },
                        modifier = Modifier.testTag("global_search_button")
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Global Search",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // 2. Add Course '+' (Screenshot 1)
                    IconButton(
                        onClick = {
                            initialAddDay = selectedDay
                            initialAddPeriod = 1
                            showAddCourseDialog = true
                        },
                        modifier = Modifier.testTag("timetable_add_button")
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add Course",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // 3. Import Button (Screenshot 1 -> opens Screenshot 3 menu)
                    Box {
                        IconButton(
                            onClick = { showImportMenu = true },
                            modifier = Modifier.testTag("timetable_import_button")
                        ) {
                            Icon(
                                Icons.Outlined.FileDownload,
                                contentDescription = "Import Schedule",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Screenshot 3: Import Dropdown Menu
                        DropdownMenu(
                            expanded = showImportMenu,
                            onDismissRequest = { showImportMenu = false },
                            modifier = Modifier.width(220.dp)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("从教务导入", fontWeight = FontWeight.Bold)
                                        Text("1-Click Fall 2026 Schedule", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                onClick = {
                                    showImportMenu = false
                                    viewModel.loadPresetMamunSchedule(bilingual = true, replaceExisting = false)
                                    Toast.makeText(context, "🎓 成功从教务系统导入 2026 秋季课表！", Toast.LENGTH_SHORT).show()
                                }
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("手动添加课程 (Add Course)", fontWeight = FontWeight.Bold)
                                        Text("Create a course slot manually", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                leadingIcon = { Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    showImportMenu = false
                                    initialAddDay = selectedDay
                                    initialAddPeriod = 1
                                    showAddCourseDialog = true
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("Excel & PDF 导入 (Excel / PDF)", fontWeight = FontWeight.Bold)
                                        Text("Import from .xlsx, .pdf, or .csv", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                leadingIcon = { Icon(Icons.Default.TableChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    showImportMenu = false
                                    showSpreadsheetImportDialog = true
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("分享口令导入 (Text Code)") },
                                leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                                onClick = {
                                    showImportMenu = false
                                    showQuickPasteDialog = true
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("HTML导入 (Web Portal)") },
                                leadingIcon = { Icon(Icons.Default.Code, contentDescription = null) },
                                onClick = {
                                    showImportMenu = false
                                    onNavigateToImport()
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("从备份导入 (Presets)") },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                                onClick = {
                                    showImportMenu = false
                                    onNavigateToImport()
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("拍照 / 扫码导入 (OCR)") },
                                leadingIcon = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                                onClick = {
                                    showImportMenu = false
                                    onNavigateToImport()
                                }
                            )
                        }
                    }

                    // 4. Share Button (Screenshot 1 -> opens Screenshot 4 menu)
                    Box {
                        IconButton(
                            onClick = { showShareMenu = true },
                            modifier = Modifier.testTag("timetable_share_button")
                        ) {
                            Icon(
                                Icons.Outlined.Share,
                                contentDescription = "Share",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Screenshot 4: Share Dropdown Menu
                        DropdownMenu(
                            expanded = showShareMenu,
                            onDismissRequest = { showShareMenu = false },
                            modifier = Modifier.width(220.dp)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("导出为日历文件", fontWeight = FontWeight.Bold)
                                        Text("Export to .ics file", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                onClick = {
                                    showShareMenu = false
                                    IcsExporter.shareIcs(context, allCourses, selectedSemester)
                                }
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            DropdownMenuItem(
                                text = { Text("分享 App (Share App)") },
                                leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null) },
                                onClick = {
                                    showShareMenu = false
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, "Check out my university timetable app with instant schedule import!")
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share App"))
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("在线分享课表 (Copy Code)") },
                                leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                                onClick = {
                                    showShareMenu = false
                                    IcsExporter.copyShareCode(context, allCourses, selectedSemester)
                                }
                            )
                        }
                    }

                    // 5. More Button '...' (Screenshot 1 -> opens Screenshot 5 bottom sheet)
                    IconButton(
                        onClick = { showMoreSheet = true },
                        modifier = Modifier.testTag("timetable_more_button")
                    ) {
                        Icon(
                            Icons.Default.MoreHoriz,
                            contentDescription = "More Settings",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            // Horizontal Weekday Strip (Screenshot 1: "9月" + "23 三", "24 四"...)
            WeekdayDateStrip(
                month = currentMonth,
                mondayDate = mondayThisWeek,
                selectedDay = selectedDay,
                onSelectDay = { viewModel.setSelectedDay(it) },
                courses = allCourses
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                thickness = 0.5.dp
            )

            // Timetable View Mode (Daily Period Grid vs 7-Day Grid)
            if (isFullWeekView) {
                FullWeekTimetableGrid(
                    allCourses = allCourses,
                    timings = timings,
                    onCourseClick = { courseDetailToShow = it },
                    onEmptySlotClick = { day, period ->
                        initialAddDay = day
                        initialAddPeriod = period
                        showAddCourseDialog = true
                    }
                )
            } else {
                DailyTimetableGrid(
                    selectedDay = selectedDay,
                    courses = allCourses.filter { it.dayOfWeek == selectedDay },
                    timings = timings,
                    isCompact = isCompactMode,
                    onCourseClick = { courseDetailToShow = it },
                    onEmptySlotClick = { period ->
                        initialAddDay = selectedDay
                        initialAddPeriod = period
                        showAddCourseDialog = true
                    }
                )
            }
        }
    }

    // Modal: Add / Edit Course Dialog (Matches Screenshot 2!)
    if (showAddCourseDialog) {
        AddEditCourseDialog(
            defaultSemester = selectedSemester,
            initialDay = initialAddDay,
            initialStartPeriod = initialAddPeriod,
            onDismiss = { showAddCourseDialog = false },
            onSave = {
                viewModel.addCourse(it)
                showAddCourseDialog = false
                Toast.makeText(context, "已添加课程：${it.name}", Toast.LENGTH_SHORT).show()
            },
            onSaveMultiple = { courses ->
                courses.forEach { viewModel.addCourse(it) }
                showAddCourseDialog = false
                Toast.makeText(context, "已添加 ${courses.size} 节课程", Toast.LENGTH_SHORT).show()
            }
        )
    }

    courseToEdit?.let { course ->
        AddEditCourseDialog(
            initialCourse = course,
            defaultSemester = course.semester,
            onDismiss = { courseToEdit = null },
            onSave = {
                viewModel.updateCourse(it)
                courseToEdit = null
                Toast.makeText(context, "课程已更新", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showGlobalSearch) {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val allDomainCourses by viewModel.filteredCourses.collectAsStateWithLifecycle()
        com.example.ui.components.GlobalSearchDialog(
            state = state,
            viewModel = viewModel,
            onDismiss = { showGlobalSearch = false },
            onCourseClick = { entity ->
                courseDetailToShow = allDomainCourses.find { it.id == entity.id } ?: com.example.domain.model.Course(
                    id = entity.id,
                    name = entity.name,
                    code = entity.code,
                    instructor = entity.instructor,
                    classroom = entity.classroom,
                    dayOfWeek = entity.dayOfWeek,
                    startTime = entity.startTime,
                    endTime = entity.endTime,
                    colorHex = "#4F46E5",
                    notes = entity.notes
                )
            }
        )
    }

    // Course Quick Detail Bottom Sheet
    courseDetailToShow?.let { course ->
        CourseDetailBottomSheet(
            course = course,
            onDismiss = { courseDetailToShow = null },
            onEdit = {
                courseDetailToShow = null
                courseToEdit = course
            },
            onDelete = {
                viewModel.deleteCourse(course)
                courseDetailToShow = null
                Toast.makeText(context, "已删除：${course.name}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showSpreadsheetImportDialog) {
        SpreadsheetImportDialog(
            onDismiss = { showSpreadsheetImportDialog = false },
            viewModel = viewModel
        )
    }

    // Screenshot 5: Timetable Settings & Tools Bottom Sheet
    if (showMoreSheet) {
        TimetableMoreBottomSheet(
            currentWeek = selectedWeek,
            onWeekChange = { viewModel.setSelectedWeek(it) },
            selectedSemester = selectedSemester,
            isFullWeek = isFullWeekView,
            isReminderEnabled = isReminderEnabled,
            onToggleFullWeek = { viewModel.toggleFullWeekView() },
            onToggleNightMode = { viewModel.toggleThemeMode() },
            onToggleReminder = { viewModel.toggleClassReminder15m() },
            onToggleCompactMode = { viewModel.toggleCompactMode() },
            onOpenClassTimes = {
                showMoreSheet = false
                showSectionTimingsDialog = true
            },
            onOpenSettings = {
                showMoreSheet = false
                showTimetableSettingsDialog = true
            },
            onOpenCourses = {
                showMoreSheet = false
                onNavigateToCourses?.invoke()
            },
            onOpenWidgets = {
                showMoreSheet = false
                showWidgetsDialog = true
            },
            onOpenContact = {
                showMoreSheet = false
                onNavigateToAbout?.invoke()
            },
            onDismiss = { showMoreSheet = false }
        )
    }

    // Section Timings Dialog (作息时间表)
    if (showSectionTimingsDialog) {
        SectionTimingsDialog(
            currentTimings = timings,
            onDismiss = { showSectionTimingsDialog = false },
            onSave = { updated ->
                viewModel.userPreferencesManager.setSectionTimings(updated)
                showSectionTimingsDialog = false
                Toast.makeText(context, "作息时间已保存", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Quick Paste Text / Share Code Dialog
    if (showQuickPasteDialog) {
        QuickPasteCodeDialog(
            onDismiss = { showQuickPasteDialog = false },
            onImport = { rawText ->
                val parsed = ScheduleImportHelper.parseTextToCourses(rawText, semesterId = 1L, autoTranslateToEnglish = false, bilingual = true)
                if (parsed.isNotEmpty()) {
                    viewModel.importCourseEntities(parsed, replaceExisting = false)
                    Toast.makeText(context, "成功导入 ${parsed.size} 门课程！", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "未能解析出课程，请检查格式或直接从教务导入", Toast.LENGTH_LONG).show()
                }
                showQuickPasteDialog = false
            }
        )
    }

    // Desktop Widget Guide Dialog
    if (showWidgetsDialog) {
        AlertDialog(
            onDismissRequest = { showWidgetsDialog = false },
            title = { Text("桌面课表小组件", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("您可以在手机主屏幕添加课表微件：")
                    Text("1. 在手机桌面空白处长按", fontSize = 13.5.sp)
                    Text("2. 选择「微件 (Widgets)」并搜索课表", fontSize = 13.5.sp)
                    Text("3. 将「今日课程」或「周课表」拖入主屏幕", fontSize = 13.5.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("💡 课表小组件将自动同步并展示当前节次与教室！", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(onClick = { showWidgetsDialog = false }) {
                    Text("知道了")
                }
            }
        )
    }

    // Quick Timetable Settings Dialog
    if (showTimetableSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showTimetableSettingsDialog = false },
            title = { Text("课表视图设置", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("全周 7 天视图 (Full 7-Day Grid)")
                        Switch(
                            checked = isFullWeekView,
                            onCheckedChange = { viewModel.toggleFullWeekView() }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("简洁紧凑模式 (Compact Rows)")
                        Switch(
                            checked = isCompactMode,
                            onCheckedChange = { viewModel.toggleCompactMode() }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("15 分钟上课提醒 (Class Reminder)")
                        Switch(
                            checked = isReminderEnabled,
                            onCheckedChange = { viewModel.toggleClassReminder15m() }
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showTimetableSettingsDialog = false }) {
                    Text("完成")
                }
            }
        )
    }
}

/**
 * Weekday and Date Bar (Screenshot 1: "9月" on left, followed by 7 day pills)
 */
@Composable
fun WeekdayDateStrip(
    month: Int,
    mondayDate: LocalDate,
    selectedDay: Int,
    onSelectDay: (Int) -> Unit,
    courses: List<com.example.domain.model.Course> = emptyList()
) {
    val today = LocalDate.now()
    val currentLang = LocalAppLanguage.current
    val isEnglish = currentLang == "en" || !currentLang.startsWith("zh")

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Month Badge on the left
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier
                    .width(44.dp)
                    .height(52.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(4.dp)
                ) {
                    Text(
                        text = if (isEnglish) java.time.Month.of(month).name.take(3).lowercase().replaceFirstChar { it.uppercase() } else "${month}月",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "📅",
                        fontSize = 11.sp
                    )
                }
            }

            // 7 Day Pills
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (d in 1..7) {
                    val dayDate = mondayDate.plusDays((d - 1).toLong())
                    val dayNumber = dayDate.dayOfMonth
                    val isSelected = selectedDay == d
                    val isToday = dayDate == today
                    val dayLabel = if (isEnglish) DAYS_ENGLISH[d - 1] else DAYS_CHINESE[d - 1].second
                    val hasCoursesOnDay = courses.any { it.dayOfWeek == d }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = when {
                            isSelected -> MaterialTheme.colorScheme.primary
                            isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        },
                        shadowElevation = if (isSelected) 2.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 2.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectDay(d) }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp)
                        ) {
                            Text(
                                text = dayLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = when {
                                    isSelected -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                                    isToday -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = dayNumber.toString(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                    isToday -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isSelected -> MaterialTheme.colorScheme.onPrimary
                                            hasCoursesOnDay -> MaterialTheme.colorScheme.primary
                                            else -> Color.Transparent
                                        }
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getBreakMinutes(period: Int, timings: List<SectionTiming>): Int {
    if (period < 1 || period >= timings.size) return 0
    val currTiming = timings.getOrNull(period - 1) ?: return 0
    val nextTiming = timings.getOrNull(period) ?: return 0
    return try {
        val (h1, m1) = currTiming.endTime.split(":").map { it.trim().toInt() }
        val (h2, m2) = nextTiming.startTime.split(":").map { it.trim().toInt() }
        val diff = (h2 * 60 + m2) - (h1 * 60 + m1)
        if (diff > 0) diff else 0
    } catch (e: Exception) {
        0
    }
}

@Composable
private fun BreakTimeBanner(
    breakMinutes: Int,
    startTime: String,
    endTime: String,
    height: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val breakType = when {
        breakMinutes >= 80 && (startTime.startsWith("11") || startTime.startsWith("12")) -> "Lunch Break".tr
        breakMinutes >= 45 && (startTime.startsWith("17") || startTime.startsWith("18")) -> "Dinner Break".tr
        else -> "Break Time".tr
    }

    val formattedDuration = if (breakMinutes >= 60) {
        val h = breakMinutes / 60
        val m = breakMinutes % 60
        if (m == 0) "${h}h" else "${h}h ${m}m"
    } else {
        "${breakMinutes}m"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .padding(vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FreeBreakfast,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "$breakType • $formattedDuration",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }

                if (startTime.isNotBlank() && endTime.isNotBlank()) {
                    Text(
                        text = "$startTime - $endTime",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.5.sp,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

/**
 * Daily Period Timetable Grid (Screenshot 1: Period 1..12 along left sidebar)
 */
@Composable
fun DailyTimetableGrid(
    selectedDay: Int,
    courses: List<Course>,
    timings: List<SectionTiming>,
    isCompact: Boolean,
    onCourseClick: (Course) -> Unit,
    onEmptySlotClick: (Int) -> Unit
) {
    val periodHeight = if (isCompact) 56.dp else 68.dp
    val breakRowHeight = 28.dp

    // Precalculate course positions
    val periodCourseMap = remember(courses, timings) {
        val map = mutableMapOf<Int, Course>()
        val covered = mutableSetOf<Int>()

        courses.sortedBy { it.startTime }.forEach { course ->
            val startP = course.getStartPeriod(timings)
            val endP = course.getEndPeriod(timings).coerceAtLeast(startP)
            map[startP] = course
            for (p in (startP + 1)..endP) {
                covered.add(p)
            }
        }
        Pair(map, covered)
    }

    val startingCourses = periodCourseMap.first
    val coveredPeriods = periodCourseMap.second

    Row(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 4.dp)
    ) {
        // Left Column: Periods 1 to 12 with Times (Screenshot 1)
        Column(
            modifier = Modifier
                .width(54.dp)
                .padding(start = 6.dp, end = 2.dp)
        ) {
            for (period in 1..12) {
                val timing = timings.getOrNull(period - 1)
                val sTime = timing?.startTime ?: "08:00"
                val eTime = timing?.endTime ?: "08:45"

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(periodHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$period",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = sTime,
                            fontSize = 8.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = eTime,
                            fontSize = 8.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                // Show break time if gap to next period > 10 min
                if (period < 12) {
                    val breakMin = getBreakMinutes(period, timings)
                    if (breakMin > 10) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(breakRowHeight),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.75f),
                                modifier = Modifier.padding(horizontal = 2.dp)
                            ) {
                                Text(
                                    text = "${breakMin}m",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Right Column: Course Slots
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 10.dp, start = 4.dp)
        ) {
            var currentPeriod = 1
            while (currentPeriod <= 12) {
                val course = startingCourses[currentPeriod]
                if (course != null) {
                    val startP = course.getStartPeriod(timings)
                    val endP = course.getEndPeriod(timings).coerceAtLeast(startP)
                    val span = (endP - startP + 1).coerceAtLeast(1)

                    val internalBreakHeight = (startP until endP).sumOf { p ->
                        if (getBreakMinutes(p, timings) > 10) 28 else 0
                    }
                    val cardHeight = (periodHeight * span) + internalBreakHeight.dp - 6.dp

                    // Beautiful Pastel Course Card matching Screenshot 1!
                    val cardColor = try {
                        Color(android.graphics.Color.parseColor(course.colorHex))
                    } catch (e: Exception) {
                        Color(0xFF5B9BF3)
                    }

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(cardHeight)
                            .padding(vertical = 3.dp)
                            .clickable { onCourseClick(course) }
                            .testTag("course_card_${course.id}")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = course.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp,
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (course.classroom.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Outlined.LocationOn,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.85f),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = course.classroom,
                                            fontSize = 11.5.sp,
                                            color = Color.White.copy(alpha = 0.9f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (course.instructor.isNotBlank()) {
                                    Text(
                                        text = course.instructor,
                                        fontSize = 10.5.sp,
                                        color = Color.White.copy(alpha = 0.85f),
                                        maxLines = 1
                                    )
                                } else {
                                    Spacer(modifier = Modifier.width(1.dp))
                                }

                                Text(
                                    text = "第$startP-$endP 节",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    // Show break time banner after course finishes if gap > 10 min
                    if (endP < 12) {
                        val breakMin = getBreakMinutes(endP, timings)
                        if (breakMin > 10) {
                            val currT = timings.getOrNull(endP - 1)
                            val nextT = timings.getOrNull(endP)
                            BreakTimeBanner(
                                breakMinutes = breakMin,
                                startTime = currT?.endTime ?: "",
                                endTime = nextT?.startTime ?: "",
                                height = breakRowHeight
                            )
                        }
                    }

                    currentPeriod = endP + 1
                } else if (coveredPeriods.contains(currentPeriod)) {
                    currentPeriod++
                } else {
                    // Empty Slot: subtle dashed/dotted line + clickable to add class
                    val emptyPeriod = currentPeriod
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(periodHeight)
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = 0.8.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onEmptySlotClick(emptyPeriod) },
                        contentAlignment = Alignment.Center
                    ) {
                        val currentLang = LocalAppLanguage.current
                        val isEnglish = currentLang == "en" || !currentLang.startsWith("zh")
                        val emptySlotText = if (isEnglish) "+ Period $emptyPeriod" else "+ 第 $emptyPeriod 节"
                        Text(
                            text = emptySlotText,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                    }

                    // Show break time banner after empty period if gap > 10 min
                    if (emptyPeriod < 12) {
                        val breakMin = getBreakMinutes(emptyPeriod, timings)
                        if (breakMin > 10) {
                            val currT = timings.getOrNull(emptyPeriod - 1)
                            val nextT = timings.getOrNull(emptyPeriod)
                            BreakTimeBanner(
                                breakMinutes = breakMin,
                                startTime = currT?.endTime ?: "",
                                endTime = nextT?.startTime ?: "",
                                height = breakRowHeight
                            )
                        }
                    }

                    currentPeriod++
                }
            }
        }
    }
}

/**
 * Full 7-Day Timetable Grid
 */
@Composable
fun FullWeekTimetableGrid(
    allCourses: List<Course>,
    timings: List<SectionTiming>,
    onCourseClick: (Course) -> Unit,
    onEmptySlotClick: (Int, Int) -> Unit
) {
    val scrollState = rememberScrollState()
    val cellHeight = 52.dp

    Row(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        // Periods on Left
        Column(modifier = Modifier.width(36.dp)) {
            for (p in 1..12) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(cellHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "$p",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (p < 12) {
                    val breakMin = getBreakMinutes(p, timings)
                    if (breakMin > 10) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("☕", fontSize = 9.sp)
                        }
                    }
                }
            }
        }

        // 7 Day Columns
        for (day in 1..7) {
            val dayCourses = allCourses.filter { it.dayOfWeek == day }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 1.dp)
            ) {
                for (p in 1..12) {
                    val course = dayCourses.firstOrNull { c ->
                        val start = c.getStartPeriod(timings)
                        val end = c.getEndPeriod(timings)
                        p in start..end
                    }

                    if (course != null) {
                        val isStart = course.getStartPeriod(timings) == p
                        val cardColor = try {
                            Color(android.graphics.Color.parseColor(course.colorHex))
                        } catch (e: Exception) {
                            Color(0xFF5B9BF3)
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(cellHeight)
                                .padding(1.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(cardColor)
                                .clickable { onCourseClick(course) }
                                .padding(2.dp)
                        ) {
                            if (isStart) {
                                Text(
                                    text = course.name,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(cellHeight)
                                .padding(1.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                .clickable { onEmptySlotClick(day, p) }
                        )
                    }

                    if (p < 12) {
                        val breakMin = getBreakMinutes(p, timings)
                        if (breakMin > 10) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(18.dp)
                                    .padding(horizontal = 1.dp, vertical = 2.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (day == 1) {
                                    Text(
                                        text = "${breakMin}m",
                                        fontSize = 7.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Screenshot 5: Timetable Settings, Week Slider & Quick Tools Bottom Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableMoreBottomSheet(
    currentWeek: Int,
    onWeekChange: (Int) -> Unit,
    selectedSemester: String,
    isFullWeek: Boolean,
    isReminderEnabled: Boolean,
    onToggleFullWeek: () -> Unit,
    onToggleNightMode: () -> Unit,
    onToggleReminder: () -> Unit,
    onToggleCompactMode: () -> Unit,
    onOpenClassTimes: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCourses: () -> Unit,
    onOpenWidgets: () -> Unit,
    onOpenContact: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card 1: Week Slider & Active Timetable (Screenshot 5)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Week row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("周数", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    "第 $currentWeek 周",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Text(
                            text = "修改当前周",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Slider(
                        value = currentWeek.toFloat(),
                        onValueChange = { onWeekChange(it.toInt()) },
                        valueRange = 1f..20f,
                        steps = 18,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFF3B30),
                            activeTrackColor = Color(0xFFFF3B30).copy(alpha = 0.7f)
                        ),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // Timetable Switcher Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("课表", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                "新建课表",
                                fontSize = 12.5.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "管理",
                                fontSize = 12.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Active Timetable Pill
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Fall 2026 (Mamun)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("7 Courses", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Card 2: 8 Quick Tools Grid (Screenshot 5)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Row 1: 上课时间, 课表设置, 已添课程, 小组件
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        QuickToolItem(
                            icon = Icons.Outlined.Schedule,
                            label = "上课时间",
                            subLabel = "Class Times",
                            onClick = onOpenClassTimes
                        )
                        QuickToolItem(
                            icon = Icons.Outlined.Settings,
                            label = "课表设置",
                            subLabel = "Settings",
                            onClick = onOpenSettings
                        )
                        QuickToolItem(
                            icon = Icons.Outlined.Inventory2,
                            label = "已添课程",
                            subLabel = "Courses",
                            onClick = onOpenCourses
                        )
                        QuickToolItem(
                            icon = Icons.Outlined.Widgets,
                            label = "小组件",
                            subLabel = "Widgets",
                            onClick = onOpenWidgets
                        )
                    }

                    // Row 2: 联系我们, 上课提醒, 夜间模式, 简洁模式
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        QuickToolItem(
                            icon = Icons.Outlined.ChatBubbleOutline,
                            label = "联系我们",
                            subLabel = "Contact",
                            onClick = onOpenContact
                        )
                        QuickToolItem(
                            icon = if (isReminderEnabled) Icons.Default.NotificationsActive else Icons.Outlined.Notifications,
                            label = "上课提醒",
                            subLabel = if (isReminderEnabled) "已开启" else "关闭",
                            tint = if (isReminderEnabled) Color(0xFF10B981) else null,
                            onClick = onToggleReminder
                        )
                        QuickToolItem(
                            icon = Icons.Outlined.DarkMode,
                            label = "夜间模式",
                            subLabel = "Dark Mode",
                            onClick = onToggleNightMode
                        )
                        QuickToolItem(
                            icon = Icons.Outlined.CropSquare,
                            label = "简洁模式",
                            subLabel = "Compact",
                            onClick = onToggleCompactMode
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun QuickToolItem(
    icon: ImageVector,
    label: String,
    subLabel: String,
    tint: Color? = null,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier.size(46.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = tint ?: MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
        Text(
            text = subLabel,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Course Quick Detail & Actions Bottom Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailBottomSheet(
    course: Course,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val cardColor = try {
        Color(android.graphics.Color.parseColor(course.colorHex))
    } catch (e: Exception) {
        Color(0xFF5B9BF3)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = cardColor
                ) {
                    Text(
                        text = course.code.ifEmpty { "COURSE" },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("编辑")
                    }
                    Button(
                        onClick = onDelete,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("删除")
                    }
                }
            }

            Text(
                text = course.name,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            HorizontalDivider()

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("时间: ${course.dayName} ${course.startTime} - ${course.endTime}", fontSize = 14.sp)
            }

            if (course.classroom.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("教室: ${course.classroom}", fontSize = 14.sp)
                }
            }

            if (course.instructor.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("老师: ${course.instructor}", fontSize = 14.sp)
                }
            }

            if (course.notes.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("备注: ${course.notes}", fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Quick Paste / Share Code Import Dialog
 */
@Composable
fun QuickPasteCodeDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Unit
) {
    var rawText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("分享口令 / 文本导入", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("粘贴教务课表文本或朋友分享的课表口令：", fontSize = 13.sp)
                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    placeholder = { Text("例如：高等数学 周一 1-2节 明理楼B105 张教授...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                )
                Button(
                    onClick = {
                        rawText = "高等数学\tMATH-101\t明理楼B105\t张教授\t1\t1\t2\t08:00\t09:35\tALL\t\t#5B9BF3\n" +
                                "大学物理\tPHYS-102\t明理楼A201\t李老师\t2\t3\t4\t09:50\t11:25\tALL\t\t#10B981"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("填充示例数据 (Sample)")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onImport(rawText) },
                enabled = rawText.isNotBlank()
            ) {
                Text("立即导入")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

// Extension helper to map Course start & end time to Section period numbers
fun Course.getStartPeriod(timings: List<SectionTiming>): Int {
    val cleanStart = startTime.trim()
    val match = timings.indexOfFirst { it.startTime <= cleanStart && cleanStart <= it.endTime }
    if (match != -1) return match + 1
    val byStart = timings.indexOfFirst { it.startTime >= cleanStart }
    if (byStart != -1) return byStart + 1
    return 1
}

fun Course.getEndPeriod(timings: List<SectionTiming>): Int {
    val cleanEnd = endTime.trim()
    val match = timings.indexOfLast { it.startTime <= cleanEnd && cleanEnd <= it.endTime }
    if (match != -1) return match + 1
    val byEnd = timings.indexOfLast { it.endTime <= cleanEnd }
    if (byEnd != -1) return (byEnd + 1).coerceAtLeast(getStartPeriod(timings))
    return getStartPeriod(timings).coerceAtLeast(1)
}
