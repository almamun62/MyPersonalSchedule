package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.AppAccentColor
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.tr
import com.example.ui.viewmodel.ScheduleViewModel

data class MoreOptionItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val badge: String? = null,
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
    val isReminder by viewModel.userPreferencesManager.isClassReminder15mEnabled.collectAsStateWithLifecycle()
    val appLang by viewModel.userPreferencesManager.appLanguage.collectAsStateWithLifecycle()
    val currentThemeMode by viewModel.userPreferencesManager.themeMode.collectAsStateWithLifecycle()
    val currentAccentColor by viewModel.userPreferencesManager.accentColor.collectAsStateWithLifecycle()

    var showSettingsModal by remember { mutableStateOf(false) }
    var showHelpModal by remember { mutableStateOf(false) }
    var showFocusModal by remember { mutableStateOf(false) }
    var showCalendarModal by remember { mutableStateOf(false) }
    var showNotesModal by remember { mutableStateOf(false) }

    val options = remember(
        onNavigateToAbout,
        onNavigateToImport,
        onNavigateToTasks,
        onNavigateToCourses
    ) {
        listOf(
            MoreOptionItem(
                title = "Course Notes Archive",
                subtitle = "Digital Notebooks & Memos",
                icon = Icons.Outlined.MenuBook,
                badge = "PDF / Text",
                accentColor = Color(0xFF6366F1),
                onClick = { showNotesModal = true }
            ),
            MoreOptionItem(
                title = "Import Schedule",
                subtitle = "CSV & SWPU Fall 2026 Sync",
                icon = Icons.Outlined.CloudUpload,
                badge = "AI Parser",
                accentColor = Color(0xFF0EA5E9),
                onClick = onNavigateToImport
            ),
            MoreOptionItem(
                title = "Tasks & Exams",
                subtitle = "Deadlines, Priority & Countdown",
                icon = Icons.Outlined.CheckCircleOutline,
                badge = "Active",
                accentColor = Color(0xFF10B981),
                onClick = onNavigateToTasks
            ),
            MoreOptionItem(
                title = "Focus & Study Timer",
                subtitle = "Pomodoro & App Shielding",
                icon = Icons.Outlined.Timer,
                badge = "25 Min",
                accentColor = Color(0xFFF59E0B),
                onClick = { showFocusModal = true }
            ),
            MoreOptionItem(
                title = "Academic Calendar",
                subtitle = "Holidays & Term Milestones",
                icon = Icons.Outlined.CalendarMonth,
                badge = "Fall 2026",
                accentColor = Color(0xFFEC4899),
                onClick = { showCalendarModal = true }
            ),
            MoreOptionItem(
                title = "Settings & Theme",
                subtitle = "Colors, DND & Languages",
                icon = Icons.Outlined.Settings,
                badge = "Custom",
                accentColor = Color(0xFF8B5CF6),
                onClick = { showSettingsModal = true }
            ),
            MoreOptionItem(
                title = "About Application",
                subtitle = "Offline Shield & Build Logs",
                icon = Icons.Outlined.Info,
                badge = "v2.4",
                accentColor = Color(0xFF64748B),
                onClick = onNavigateToAbout
            ),
            MoreOptionItem(
                title = "User Guide & Tips",
                subtitle = "Gestures & Quick Setup",
                icon = Icons.Outlined.HelpOutline,
                badge = "FAQ",
                accentColor = Color(0xFF14B8A6),
                onClick = { showHelpModal = true }
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("More Options".tr, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                        Text("Academic Control Center • 100% Offline", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Futuristic Hero Banner Card
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color.Unspecified,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
                            )
                        ),
                        shape = RoundedCornerShape(22.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                )
                            ),
                            shape = RoundedCornerShape(22.dp)
                        )
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = "🟢 SYSTEM SHIELD ACTIVE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                text = "SWPU Fall '26",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = "Smart Student Workspace",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )

                        Text(
                            text = "Manage course archives, system automations, import tools, and focus lock mode directly from one central hub.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // 2. Futuristic 2-Column Grid
            Text(
                text = "SYSTEM HUB & UTILITIES",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )

            // Grid items rendered in rows of 2
            options.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { item ->
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier
                                .weight(1f)
                                .height(115.dp)
                                .border(
                                    width = 1.dp,
                                    color = item.accentColor.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .clickable { item.onClick() }
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(14.dp)
                                    .fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(item.accentColor.copy(alpha = 0.18f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = null,
                                            tint = item.accentColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    item.badge?.let { badgeText ->
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = item.accentColor.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = badgeText,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = item.accentColor,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = item.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = item.subtitle,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    // Fill row if odd items
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // 3. Futuristic Live Automation & Health Control Widget (Filling space!)
            Text(
                text = "AUTOMATION & SYSTEM HEALTH",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 8.dp)
            )

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // DND Quick Toggle Row
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
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.DoNotDisturbOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text("Auto Do Not Disturb (DND)".tr, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    if (isAutoDnd) "Active • Silences phone during lectures" else "Disabled • Tap to enable auto silence",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = isAutoDnd,
                            onCheckedChange = { viewModel.userPreferencesManager.setAutoDndEnabled(it) }
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Language Selector Row
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
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.Language,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text("Language / 语言", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("App interface language", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = appLang == "en",
                                onClick = { viewModel.userPreferencesManager.setAppLanguage("en") },
                                label = { Text("English 🇺🇸", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            )
                            FilterChip(
                                selected = appLang.startsWith("zh"),
                                onClick = { viewModel.userPreferencesManager.setAppLanguage("zh") },
                                label = { Text("中文 🇨🇳", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Room DB Storage Metrics Row
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
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.Storage,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text("Local SQLite / Room Engine", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Encrypted local DB • 100% Private", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "1.2 MB",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Modal dialogs
    if (showSettingsModal) {
        AlertDialog(
            onDismissRequest = { showSettingsModal = false },
            title = { Text("Appearance & Theme Settings", fontWeight = FontWeight.Bold) },
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
        AlertDialog(
            onDismissRequest = { showFocusModal = false },
            icon = { Icon(Icons.Outlined.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp)) },
            title = { Text("Study Focus Lock", fontWeight = FontWeight.Bold) },
            text = {
                Text("Launch a 25-minute Pomodoro study session. The app will mute notifications and activate class shield focus mode.")
            },
            confirmButton = {
                Button(onClick = { showFocusModal = false }) {
                    Text("Start 25m Focus")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFocusModal = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showNotesModal) {
        AlertDialog(
            onDismissRequest = { showNotesModal = false },
            icon = { Icon(Icons.Outlined.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp)) },
            title = { Text("Course Notes Archive", fontWeight = FontWeight.Bold) },
            text = {
                Text("You have 4 lecture notebooks saved locally in Room SQLite database. Access notes directly from any course item on the Timetable screen.")
            },
            confirmButton = {
                TextButton(onClick = { showNotesModal = false }) {
                    Text("OK")
                }
            }
        )
    }

    if (showCalendarModal) {
        com.example.ui.components.AcademicCalendarDialog(
            viewModel = viewModel,
            onDismiss = { showCalendarModal = false }
        )
    }


    if (showHelpModal) {
        com.example.ui.components.HowToUseDialog(
            onDismiss = { showHelpModal = false }
        )
    }
}
