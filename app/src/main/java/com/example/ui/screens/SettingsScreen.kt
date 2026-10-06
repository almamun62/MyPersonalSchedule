package com.example.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.AppAccentColor
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.tr
import com.example.ui.viewmodel.ScheduleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ScheduleViewModel,
    onNavigateToAbout: () -> Unit,
    onNavigateToImport: (() -> Unit)? = null
) {
    val isAutoDnd by viewModel.userPreferencesManager.isAutoDndEnabled.collectAsStateWithLifecycle()
    val isReminder by viewModel.userPreferencesManager.isClassReminder15mEnabled.collectAsStateWithLifecycle()
    val appLang by viewModel.userPreferencesManager.appLanguage.collectAsStateWithLifecycle()
    val currentThemeMode by viewModel.userPreferencesManager.themeMode.collectAsStateWithLifecycle()
    val currentAccentColor by viewModel.userPreferencesManager.accentColor.collectAsStateWithLifecycle()

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
                        Column {
                            Text("Auto Do Not Disturb (DND)".tr, fontWeight = FontWeight.SemiBold)
                            Text("Silences phone during class hours", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = isAutoDnd,
                            onCheckedChange = { viewModel.toggleAutoDnd() }
                        )

                    }

                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
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

            // 3. Language Selection Card
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

            // 4. About Application Button
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
}
