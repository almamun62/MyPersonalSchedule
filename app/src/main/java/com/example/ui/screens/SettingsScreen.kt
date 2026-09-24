package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.AppIcons
import com.example.ui.viewmodel.ScheduleViewModel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.AppAccents
import com.example.ui.theme.tr
import com.example.BuildConfig
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ScheduleViewModel,
    onNavigateBack: (() -> Unit)? = null,
    onOpenThemeCustomization: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val allCourses by viewModel.allCourses.collectAsStateWithLifecycle()
    val selectedSemester by viewModel.selectedSemester.collectAsStateWithLifecycle()
    val currentLanguage by viewModel.userPreferencesManager.appLanguage.collectAsStateWithLifecycle()
    val currentThemeMode by viewModel.userPreferencesManager.themeMode.collectAsStateWithLifecycle()

    var showClearDialog by remember { mutableStateOf(false) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<com.example.util.UpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Tools".tr, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back".tr)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Language & Appearance Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "App Language & Appearance".tr,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Text(
                        "App Language".tr,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = currentLanguage == "system" || currentLanguage == null,
                            onClick = { viewModel.userPreferencesManager.setAppLanguage("system") },
                            label = { Text("Follow System".tr, fontSize = 12.sp) },
                            modifier = Modifier.weight(1.2f)
                        )
                        FilterChip(
                            selected = currentLanguage == "en",
                            onClick = { viewModel.userPreferencesManager.setAppLanguage("en") },
                            label = { Text("English", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = currentLanguage == "zh",
                            onClick = { viewModel.userPreferencesManager.setAppLanguage("zh") },
                            label = { Text("中文", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    Text(
                        "Theme Mode".tr,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            selected = currentThemeMode == AppThemeMode.SYSTEM,
                            onClick = { viewModel.userPreferencesManager.setThemeMode(AppThemeMode.SYSTEM) },
                            label = { Text("System".tr, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = currentThemeMode == AppThemeMode.DAY,
                            onClick = { viewModel.userPreferencesManager.setThemeMode(AppThemeMode.DAY) },
                            label = { Text("Day".tr, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = currentThemeMode == AppThemeMode.EYE_CARE,
                            onClick = { viewModel.userPreferencesManager.setThemeMode(AppThemeMode.EYE_CARE) },
                            label = { Text("Eye Care".tr, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = currentThemeMode == AppThemeMode.NIGHT,
                            onClick = { viewModel.userPreferencesManager.setThemeMode(AppThemeMode.NIGHT) },
                            label = { Text("Night".tr, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    val currentAccent by viewModel.userPreferencesManager.accentColor.collectAsStateWithLifecycle()

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    Text(
                        "Accent Color".tr,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AppAccents.ALL.forEach { accent ->
                            FilterChip(
                                selected = currentAccent.id == accent.id,
                                onClick = { viewModel.userPreferencesManager.setAccentColor(accent) },
                                label = { Text(accent.tag, fontSize = 10.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { onOpenThemeCustomization?.invoke() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_theme_customization_button")
                    ) {
                        Icon(AppIcons.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Theme Customization".tr)
                    }
                }
            }

            // Export Section
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Export Timetable".tr,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        "${"Courses".tr}: ${allCourses.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        val csvCopiedMsg = "CSV copied to clipboard!".tr
                        val shareCsvTitle = "Share Schedule CSV".tr
                        Button(
                            onClick = {
                                val csv = viewModel.exportToCsv()
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Course Schedule CSV", csv)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, csvCopiedMsg, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("copy_csv_button")
                        ) {
                            Icon(AppIcons.Copy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy CSV".tr)
                        }

                        OutlinedButton(
                            onClick = {
                                val csv = viewModel.exportToCsv()
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, csv)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, shareCsvTitle)
                                context.startActivity(shareIntent)
                            },
                            modifier = Modifier.testTag("share_csv_button")
                        ) {
                            Icon(AppIcons.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share".tr)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedButton(
                        onClick = {
                            com.example.util.CalendarExportHelper.exportWeeklyScheduleToIcs(context, allCourses.map { it.toEntity() })
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_ics_button")
                    ) {
                        Icon(AppIcons.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export to System Calendar (.ics)".tr)
                    }
                }
            }

            // Import Formats Guide
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(AppIcons.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Supported Import Formats".tr,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Text(
                        "• CSV & TSV: Column headers like Code, Name, Days, Start Time, End Time, Room, Instructor.".tr,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "• Free Text / Portal Paste: Directly copy text from Chinese university portal or Tsinghua / SWPU.".tr,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "• Single / Double Weeks (单双周) & Make-up Days (调休) auto-recognized.".tr,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Check for Updates Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Check for Updates".tr,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    val currentVersionText = "Current Version".tr
                    val checkingText = "Checking for updates...".tr
                    val checkUpdatesText = "Check for Updates".tr
                    val upToDateMsg = "You are on the latest version!".tr
                    val failedMsg = "Check Update Failed".tr

                    Text(
                        "$currentVersionText: ${BuildConfig.VERSION_NAME} (v${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            scope.launch {
                                isCheckingUpdate = true
                                val info = com.example.util.UpdateChecker.checkForUpdates(context, BuildConfig.VERSION_NAME)
                                isCheckingUpdate = false
                                if (info != null) {
                                    updateInfo = info
                                    if (info.isUpdateAvailable) {
                                        showUpdateDialog = true
                                    } else {
                                        Toast.makeText(context, upToDateMsg, Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, failedMsg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = !isCheckingUpdate,
                        modifier = Modifier.testTag("check_update_button")
                    ) {
                        Icon(AppIcons.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isCheckingUpdate) checkingText else checkUpdatesText)
                    }
                }
            }

            // Live Status Notification Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Live Status Notification".tr,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        "Shows remaining duration of current class & name of next upcoming class in status bar.".tr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = {
                            viewModel.postCurrentAndNextClassNotification(context)
                            Toast.makeText(context, "Live notification posted!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(AppIcons.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Show Live Status Notification".tr)
                    }
                }
            }

            // Danger Zone
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Data Management".tr,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    )
                    Text(
                        "Reset timetable database or clear all schedule entries.".tr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = { showClearDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("clear_all_data_button")
                    ) {
                        Icon(AppIcons.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear All Schedule Data".tr)
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All Data?".tr) },
            text = { Text("Are you sure you want to delete all courses and timetable schedules? This action cannot be undone.".tr) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        showClearDialog = false
                        Toast.makeText(context, "All data cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear Everything".tr)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearDialog = false }) {
                    Text("Cancel".tr)
                }
            }
        )
    }

    if (showUpdateDialog && updateInfo != null) {
        val info = updateInfo!!
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text("Update Available".tr) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("A new version v${info.latestVersion} is available on GitHub.")
                    Text("Release Notes:".tr, fontWeight = FontWeight.Bold)
                    Text(info.releaseNotes, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        com.example.util.UpdateChecker.openDownloadPage(context, info.downloadUrl)
                        showUpdateDialog = false
                    }
                ) {
                    Text("Download Update".tr)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showUpdateDialog = false }) {
                    Text("Cancel".tr)
                }
            }
        )
    }
}
