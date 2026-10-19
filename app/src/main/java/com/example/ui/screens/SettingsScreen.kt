package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.service.ClassReminderScheduler
import com.example.ui.theme.AppAccentColor
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.tr
import com.example.ui.viewmodel.ScheduleViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ScheduleViewModel,
    onNavigateToAbout: () -> Unit,
    onNavigateToImport: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val isAutoDnd by viewModel.userPreferencesManager.isAutoDndEnabled.collectAsStateWithLifecycle()
    val isReminder by viewModel.userPreferencesManager.isClassReminder15mEnabled.collectAsStateWithLifecycle()
    val appLang by viewModel.userPreferencesManager.appLanguage.collectAsStateWithLifecycle()
    val currentThemeMode by viewModel.userPreferencesManager.themeMode.collectAsStateWithLifecycle()
    val currentAccentColor by viewModel.userPreferencesManager.accentColor.collectAsStateWithLifecycle()
    val isPinyinEnabled by viewModel.userPreferencesManager.isPinyinEnabled.collectAsStateWithLifecycle()
    val defaultStartupScreen by viewModel.userPreferencesManager.defaultStartupScreen.collectAsStateWithLifecycle()

    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var pendingRestoreContent by remember { mutableStateOf<String?>(null) }
    var restoreReplaceExisting by remember { mutableStateOf(false) }

    // Check battery optimization exemption status
    val pm = remember(context) { context.getSystemService(Context.POWER_SERVICE) as? PowerManager }
    val isIgnoringBatteryOptimizations = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pm?.isIgnoringBatteryOptimizations(context.packageName) == true
        } else true
    }

    // Check exact alarm capability
    val canScheduleExact = remember(context) {
        ClassReminderScheduler.canScheduleExactAlarms(context)
    }

    // Backup export launcher
    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { destUri ->
            coroutineScope.launch {
                try {
                    val jsonContent = viewModel.createBackupJson()
                    context.contentResolver.openOutputStream(destUri)?.use { out ->
                        out.write(jsonContent.toByteArray(Charsets.UTF_8))
                    }
                    Toast.makeText(context, "Backup exported with verified SHA-256 checksum!", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Export failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Restore import launcher
    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { sourceUri ->
            coroutineScope.launch {
                try {
                    val content = context.contentResolver.openInputStream(sourceUri)?.use { input ->
                        input.readBytes().toString(Charsets.UTF_8)
                    } ?: ""
                    if (content.isNotBlank()) {
                        pendingRestoreContent = content
                        showRestoreConfirmDialog = true
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to read backup file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings".tr, fontWeight = FontWeight.Bold) }
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
            // 1. Appearance Settings Card
            Card(
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Appearance & Theme".tr, fontWeight = FontWeight.Bold, fontSize = 15.sp)

                    Text("Theme Mode", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val modes = listOf(AppThemeMode.SYSTEM to "System", AppThemeMode.LIGHT to "Light", AppThemeMode.DARK to "Dark")
                        modes.forEachIndexed { idx, (mode, label) ->
                            SegmentedButton(
                                selected = currentThemeMode == mode,
                                onClick = { viewModel.userPreferencesManager.setThemeMode(mode) },
                                shape = SegmentedButtonDefaults.itemShape(index = idx, count = modes.size)
                            ) {
                                Text(label, fontSize = 12.sp)
                            }
                        }
                    }

                    Text("Accent Color Palette", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AppAccentColor.values().forEach { accent ->
                            val isSelected = currentAccentColor == accent
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(accent.primary)
                                    .clickable { viewModel.userPreferencesManager.setAccentColor(accent) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }

                    val isShowNotch by viewModel.userPreferencesManager.showNotchMode.collectAsStateWithLifecycle()
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Render Under Notch / Cutout", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Auto-fits UI edge-to-edge under camera cutout area", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = isShowNotch,
                            onCheckedChange = { viewModel.userPreferencesManager.setShowNotchMode(it) }
                        )
                    }
                }
            }

            // Chaoxing Timetable & Pronunciation Card
            Card(
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Chaoxing Timetable & Pinyin", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Pinyin Guides (拼音标注)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Displays tone-marked Pinyin above Chinese characters in courses and classrooms", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = isPinyinEnabled,
                            onCheckedChange = { viewModel.userPreferencesManager.setPinyinEnabled(it) }
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    Text("Default Startup Screen", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val screens = listOf("TIMETABLE" to "Chaoxing Grid", "DASHBOARD" to "Dashboard")
                        screens.forEachIndexed { idx, (key, label) ->
                            SegmentedButton(
                                selected = defaultStartupScreen == key,
                                onClick = { viewModel.userPreferencesManager.setDefaultStartupScreen(key) },
                                shape = SegmentedButtonDefaults.itemShape(index = idx, count = screens.size)
                            ) {
                                Text(label, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // 2. Automations & Reminders Card
            Card(
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Automations & Reminders".tr, fontWeight = FontWeight.Bold, fontSize = 15.sp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto Do Not Disturb (DND)".tr, fontWeight = FontWeight.SemiBold)
                            Text("Silences phone during class hours", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = isAutoDnd,
                            onCheckedChange = { viewModel.toggleAutoDnd() }
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("15-Min Class Reminder".tr, fontWeight = FontWeight.SemiBold)
                            Text("Notifies 15 minutes before lecture", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = isReminder,
                            onCheckedChange = { viewModel.userPreferencesManager.setClassReminder15mEnabled(it) }
                        )
                    }
                }
            }

            // 3. Background Reliability & OEM Battery Savers Card (Recommended Feature)
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isIgnoringBatteryOptimizations && canScheduleExact) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        Color(0xFFFEF3C7)
                    }
                ),
                border = BorderStroke(
                    1.dp,
                    if (isIgnoringBatteryOptimizations && canScheduleExact) {
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    } else {
                        Color(0xFFF59E0B)
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.BatteryAlert,
                            contentDescription = null,
                            tint = if (isIgnoringBatteryOptimizations && canScheduleExact) MaterialTheme.colorScheme.primary else Color(0xFFD97706),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Background Alarm Reliability (OEM Battery Savers)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Text(
                        text = "Aggressive battery optimization on Xiaomi (MIUI), Samsung, and Huawei devices can kill background timers and class reminders. Ensure permissions are granted below for 100% reliable alarms:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )

                    // Exact Alarms Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (canScheduleExact) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (canScheduleExact) Color(0xFF10B981) else Color(0xFFF59E0B),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (canScheduleExact) "Exact Alarms Permitted" else "Exact Alarms Restricted",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (!canScheduleExact) {
                            TextButton(
                                onClick = {
                                    try {
                                        context.startActivity(ClassReminderScheduler.getExactAlarmSettingsIntent(context))
                                    } catch (e: Exception) {
                                        context.startActivity(Intent(Settings.ACTION_SETTINGS))
                                    }
                                }
                            ) {
                                Text("Grant Permission", fontSize = 11.sp)
                            }
                        }
                    }

                    // Battery Optimization Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isIgnoringBatteryOptimizations) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isIgnoringBatteryOptimizations) Color(0xFF10B981) else Color(0xFFF59E0B),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isIgnoringBatteryOptimizations) "Battery Saver Exemption Active" else "Battery Saver Active (May delay alarms)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (!isIgnoringBatteryOptimizations && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            TextButton(
                                onClick = {
                                    try {
                                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                                    }
                                }
                            ) {
                                Text("Exempt App", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // 4. Offline Data Backup & Restore Card (Recommended Feature)
            Card(
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("100% Offline Backup & Migration", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Text(
                        text = "Export your complete timetable, notes, tasks, exams, and reminders to a single JSON backup protected with SHA-256 cryptographic checksum. No cloud accounts or internet needed.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.US).format(java.util.Date())
                                exportBackupLauncher.launch("course_schedule_backup_$timestamp.json")
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Outlined.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export Backup", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { restoreBackupLauncher.launch(arrayOf("application/json", "text/plain")) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Outlined.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore Backup", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 5. Language Selection Card
            Card(
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Language / 语言", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(
                            selected = appLang == "en",
                            onClick = { viewModel.userPreferencesManager.setAppLanguage("en") },
                            label = { Text("English 🇺🇸", fontWeight = FontWeight.SemiBold) }
                        )
                        FilterChip(
                            selected = appLang.startsWith("zh"),
                            onClick = { viewModel.userPreferencesManager.setAppLanguage("zh") },
                            label = { Text("中文 🇨🇳", fontWeight = FontWeight.SemiBold) }
                        )
                    }
                }
            }

            // 6. About Application Button
            OutlinedButton(
                onClick = onNavigateToAbout,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Outlined.Info, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("About Application & Offline Mode".tr, fontWeight = FontWeight.Bold)
            }
        }
    }

    // Restore Confirmation Dialog
    if (showRestoreConfirmDialog && pendingRestoreContent != null) {
        AlertDialog(
            onDismissRequest = {
                showRestoreConfirmDialog = false
                pendingRestoreContent = null
            },
            icon = { Icon(Icons.Outlined.Restore, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Restore Offline Backup", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Verify SHA-256 checksum and import data into local Room Database:")
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = !restoreReplaceExisting,
                            onClick = { restoreReplaceExisting = false }
                        )
                        Text("Merge (Preserve existing data)", fontSize = 13.sp)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = restoreReplaceExisting,
                            onClick = { restoreReplaceExisting = true }
                        )
                        Text("Replace (Clear current schedule first)", fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val json = pendingRestoreContent ?: return@Button
                        coroutineScope.launch {
                            val result = viewModel.restoreBackupJson(json, replaceExisting = restoreReplaceExisting)
                            result.onSuccess { summary ->
                                Toast.makeText(
                                    context,
                                    "Restored ${summary.coursesCount} courses, ${summary.notesCount} notes, ${summary.tasksCount} tasks! Checksum verified.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }.onFailure { err ->
                                Toast.makeText(context, "Restore failed: ${err.message}", Toast.LENGTH_LONG).show()
                            }
                            showRestoreConfirmDialog = false
                            pendingRestoreContent = null
                        }
                    }
                ) {
                    Text("Confirm Restore")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirmDialog = false
                        pendingRestoreContent = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
