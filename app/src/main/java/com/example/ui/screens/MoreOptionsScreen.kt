package com.example.ui.screens

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AcademicCalendarDialog
import com.example.ui.components.HowToUseDialog
import com.example.ui.components.NoteTakingCanvasDialog
import com.example.ui.theme.AppAccentColor
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.tr
import com.example.ui.viewmodel.ScheduleViewModel

data class ToolGridItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreOptionsScreen(
    viewModel: ScheduleViewModel,
    onNavigateToAbout: () -> Unit,
    onNavigateToImport: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToCourses: () -> Unit
) {
    val isAutoDnd by viewModel.userPreferencesManager.isAutoDndEnabled.collectAsStateWithLifecycle()
    val appLang by viewModel.userPreferencesManager.appLanguage.collectAsStateWithLifecycle()
    val currentThemeMode by viewModel.userPreferencesManager.themeMode.collectAsStateWithLifecycle()
    val currentAccentColor by viewModel.userPreferencesManager.accentColor.collectAsStateWithLifecycle()

    var showSettingsModal by remember { mutableStateOf(false) }
    var showHelpModal by remember { mutableStateOf(false) }
    var showFocusModal by remember { mutableStateOf(false) }
    var showCalendarModal by remember { mutableStateOf(false) }
    var showNotesModal by remember { mutableStateOf(false) }
    var showCalculatorModal by remember { mutableStateOf(false) }
    var showAppUpdateModal by remember { mutableStateOf(false) }
    var showDndPermissionDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val notificationManager = remember {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    val notesLabel = "Notes".tr
    val importLabel = "Import".tr
    val tasksLabel = "Tasks".tr
    val settingsLabel = "Settings".tr

    val tools = remember(
        onNavigateToImport,
        onNavigateToTasks,
        notesLabel,
        importLabel,
        tasksLabel,
        settingsLabel
    ) {
        listOf(
            ToolGridItem(
                title = notesLabel,
                subtitle = "Draw & memos",
                icon = Icons.Outlined.EditNote,
                accentColor = Color(0xFF6366F1),
                onClick = { showNotesModal = true }
            ),
            ToolGridItem(
                title = "Calculator",
                subtitle = "Study & Target GPA",
                icon = Icons.Outlined.Calculate,
                accentColor = Color(0xFF3B82F6),
                onClick = { showCalculatorModal = true }
            ),
            ToolGridItem(
                title = importLabel,
                subtitle = "Schedule files",
                icon = Icons.Outlined.CloudUpload,
                accentColor = Color(0xFF0EA5E9),
                onClick = onNavigateToImport
            ),
            ToolGridItem(
                title = tasksLabel,
                subtitle = "To-dos & exams",
                icon = Icons.Outlined.CheckCircleOutline,
                accentColor = Color(0xFF10B981),
                onClick = onNavigateToTasks
            ),
            ToolGridItem(
                title = "Focus",
                subtitle = "Study timer",
                icon = Icons.Outlined.Timer,
                accentColor = Color(0xFFF59E0B),
                onClick = { showFocusModal = true }
            ),
            ToolGridItem(
                title = "Calendar",
                subtitle = "Academic dates",
                icon = Icons.Outlined.CalendarMonth,
                accentColor = Color(0xFFEC4899),
                onClick = { showCalendarModal = true }
            ),
            ToolGridItem(
                title = settingsLabel,
                subtitle = "Theme & display",
                icon = Icons.Outlined.Settings,
                accentColor = Color(0xFF8B5CF6),
                onClick = { showSettingsModal = true }
            )
        )
    }

    Scaffold(
        modifier = Modifier.statusBarsPadding(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                title = {
                    Column {
                        Text(
                            text = "Tools".tr,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Student Hub",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isTablet = maxWidth >= 600.dp
            val horizontalPadding = if (isTablet) 24.dp else 16.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = horizontalPadding, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Clean Semester Header Card (No scary system jargon)
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Fall 2026 Semester",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Offline Student Workspace",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Outlined.School,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                // 2. Simplified Student Tools Grid
                Text(
                    text = "STUDENT TOOLS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )

                val columns = if (isTablet) 3 else 2
                val chunkedTools = tools.chunked(columns)

                chunkedTools.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowItems.forEach { item ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(100.dp)
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable { item.onClick() }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(item.accentColor.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = null,
                                            tint = item.accentColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = item.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = item.subtitle,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        // Fill remaining spaces if row is incomplete
                        if (rowItems.size < columns) {
                            for (i in 0 until (columns - rowItems.size)) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                // 3. Simple Automation: Auto-Silence during Class
                Text(
                    text = "PREFERENCES",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Friendly Auto-Silence Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.VolumeOff,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Auto-Silence during Class",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isAutoDnd) "Mutes notifications while in class" else "Disabled",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = isAutoDnd,
                                onCheckedChange = { enable ->
                                    if (enable) {
                                        val hasPolicyAccess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            notificationManager.isNotificationPolicyAccessGranted
                                        } else true

                                        if (!hasPolicyAccess) {
                                            showDndPermissionDialog = true
                                        } else {
                                            viewModel.toggleAutoDnd()
                                        }
                                    } else {
                                        viewModel.toggleAutoDnd()
                                    }
                                }
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                        // Language Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.Language,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    text = "Language / 语言",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = appLang == "en",
                                    onClick = { viewModel.userPreferencesManager.setAppLanguage("en") },
                                    label = { Text("English", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                )
                                FilterChip(
                                    selected = appLang.startsWith("zh"),
                                    onClick = { viewModel.userPreferencesManager.setAppLanguage("zh") },
                                    label = { Text("中文", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                )
                            }
                        }
                    }
                }

                // 4. Subtle About, Update & Help links at the very bottom
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showHelpModal = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Guide", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { showAppUpdateModal = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.1f)
                    ) {
                        Icon(Icons.Outlined.SystemUpdate, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Update", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }

                    OutlinedButton(
                        onClick = onNavigateToAbout,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("About", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Permission Dialog for DND
    if (showDndPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showDndPermissionDialog = false },
            icon = {
                Icon(
                    Icons.Outlined.NotificationsOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Do Not Disturb Access", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "To automatically silence calls and alerts during class periods, Android requires Do Not Disturb policy permission.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDndPermissionDialog = false
                        viewModel.toggleAutoDnd()
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                ) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDndPermissionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal dialogs
    if (showSettingsModal) {
        AlertDialog(
            onDismissRequest = { showSettingsModal = false },
            title = { Text("Appearance & Theme", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Theme Mode", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val modes = listOf(AppThemeMode.SYSTEM to "System", AppThemeMode.LIGHT to "Light", AppThemeMode.DARK to "Dark")
                        modes.forEachIndexed { idx, (mode, label) ->
                            SegmentedButton(
                                selected = currentThemeMode == mode,
                                onClick = { viewModel.userPreferencesManager.setThemeMode(mode) },
                                shape = SegmentedButtonDefaults.itemShape(index = idx, count = modes.size)
                            ) {
                                Text(label, fontSize = 11.sp)
                            }
                        }
                    }

                    Text("Accent Color Palette", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AppAccentColor.values().forEach { accent ->
                            val isSelected = currentAccentColor == accent
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(accent.primary)
                                    .clickable { viewModel.userPreferencesManager.setAccentColor(accent) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    val isShowNotch by viewModel.userPreferencesManager.showNotchMode.collectAsStateWithLifecycle()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Full Edge-to-Edge", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Render behind device camera cutout", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = isShowNotch,
                            onCheckedChange = { viewModel.userPreferencesManager.setShowNotchMode(it) }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettingsModal = false }) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showFocusModal) {
        com.example.ui.components.StudyFocusDialog(
            viewModel = viewModel,
            onDismiss = { showFocusModal = false }
        )
    }

    if (showNotesModal) {
        NoteTakingCanvasDialog(
            viewModel = viewModel,
            onDismiss = { showNotesModal = false }
        )
    }

    if (showCalculatorModal) {
        com.example.ui.components.OfflineCalculatorDialog(
            viewModel = viewModel,
            onDismiss = { showCalculatorModal = false }
        )
    }

    if (showCalendarModal) {
        AcademicCalendarDialog(
            viewModel = viewModel,
            onDismiss = { showCalendarModal = false }
        )
    }

    if (showHelpModal) {
        HowToUseDialog(
            onDismiss = { showHelpModal = false }
        )
    }

    if (showAppUpdateModal) {
        com.example.ui.components.AppUpdateDialog(
            viewModel = viewModel,
            onDismiss = { showAppUpdateModal = false }
        )
    }
}
