package com.example.ui.components

import android.app.NotificationManager
import com.example.ui.theme.tr
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.AppAccentColor
import com.example.ui.theme.AppAccents
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.MyApplicationTheme

/**
 * First-Launch Personalized Bootstart Onboarding Dialog.
 * Walks new users through:
 * 1. Theme Preference (Day, Night, Pink Accent, System themes) with a live soft-glow glass preview card.
 * 2. Course Schedule Setup tailored for Chinese University students (1-12 periods, bilingual translations, presets).
 * 3. DND Mode Setup (Auto silence during lectures, 15m advance notification).
 */
@Composable
fun BootstartOnboardingDialog(
    initialAppLanguage: String,
    initialThemeMode: AppThemeMode,
    initialAccent: AppAccentColor,
    initialAutoDnd: Boolean,
    initial15mReminder: Boolean,
    onComplete: (
        appLanguage: String,
        themeMode: AppThemeMode,
        accent: AppAccentColor,
        autoDnd: Boolean,
        reminder15m: Boolean,
        scheduleOption: Int, // 0 = Mamun Fall 2026, 1 = Open Import Dialog, 2 = Clean Start
        bilingual: Boolean
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember { mutableIntStateOf(1) }

    // Step 1 State: Language, Theme & Accent
    var selectedAppLanguage by remember { mutableStateOf(initialAppLanguage) }
    
    val effectiveSelectedLang = if (selectedAppLanguage == "system") {
        java.util.Locale.getDefault().language
    } else {
        selectedAppLanguage
    }
    androidx.compose.runtime.CompositionLocalProvider(
        com.example.ui.theme.LocalAppLanguage provides effectiveSelectedLang
    ) {
    var selectedThemeMode by remember { mutableStateOf(initialThemeMode) }
    var selectedAccent by remember { mutableStateOf(initialAccent) }

    // Step 2 State: Schedule Setup for Chinese Universities
    var selectedScheduleOption by remember { mutableIntStateOf(0) } // 0 = Mamun Fall 2026, 1 = Import, 2 = Blank
    var isBilingual by remember { mutableStateOf(true) }

    // Step 3 State: DND & Reminders
    var autoDndEnabled by remember { mutableStateOf(initialAutoDnd) }
    var reminder15mEnabled by remember { mutableStateOf(initial15mReminder) }

    // Check system DND permission
    val notificationManager = remember {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    }
    var isDndPermissionGranted by remember {
        mutableStateOf(notificationManager?.isNotificationPolicyAccessGranted == true)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, notificationManager) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isDndPermissionGranted = notificationManager?.isNotificationPolicyAccessGranted == true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Dialog(
        onDismissRequest = { /* Prevent accidental cancel during setup */ },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            com.example.ui.theme.LocalAppLanguage provides selectedAppLanguage
        ) {
            MyApplicationTheme(
                themeMode = selectedThemeMode,
                accentColor = selectedAccent
            ) {
            Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            border = BorderStroke(
                1.5.dp,
                Brush.linearGradient(
                    listOf(
                        selectedAccent.primary.copy(alpha = 0.6f),
                        selectedAccent.primary.copy(alpha = 0.15f),
                        Color.Transparent
                    )
                )
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header & Step Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = selectedAccent.primary.copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = selectedAccent.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Personalize MySchedule".tr,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Step $currentStep of 3".tr,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Step Dots
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (i in 1..3) {
                            Box(
                                modifier = Modifier
                                    .size(if (i == currentStep) 24.dp else 8.dp, 8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (i == currentStep) selectedAccent.primary
                                        else if (i < currentStep) selectedAccent.primary.copy(alpha = 0.4f)
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                // Step Content Area (Scrollable)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    AnimatedContent(
                        targetState = currentStep,
                        transitionSpec = {
                            if (targetState > initialState) {
                                (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                            } else {
                                (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                            }
                        },
                        label = "StepTransition"
                    ) { step ->
                        when (step) {
                            1 -> OnboardingStepLanguageAndTheme(
                                selectedLanguage = selectedAppLanguage,
                                selectedMode = selectedThemeMode,
                                selectedAccent = selectedAccent,
                                onSelectLanguage = { selectedAppLanguage = it },
                                onSelectMode = { selectedThemeMode = it },
                                onSelectAccent = { selectedAccent = it }
                            )
                            2 -> OnboardingStepSchedule(
                                selectedOption = selectedScheduleOption,
                                onSelectOption = { selectedScheduleOption = it },
                                isBilingual = isBilingual,
                                onToggleBilingual = { isBilingual = it },
                                accent = selectedAccent
                            )
                            3 -> OnboardingStepDnd(
                                autoDnd = autoDndEnabled,
                                onToggleAutoDnd = { autoDndEnabled = it },
                                reminder15m = reminder15mEnabled,
                                onToggleReminder15m = { reminder15mEnabled = it },
                                isDndGranted = isDndPermissionGranted,
                                onRequestDndPermission = {
                                    try {
                                        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                },
                                accent = selectedAccent
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 1) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Back".tr)
                        }
                    } else {
                        TextButton(
                            onClick = {
                                // Skip directly to defaults
                                onComplete(
                                    selectedAppLanguage,
                                    selectedThemeMode,
                                    selectedAccent,
                                    autoDndEnabled,
                                    reminder15mEnabled,
                                    selectedScheduleOption,
                                    isBilingual
                                )
                            }
                        ) {
                            Text("Skip".tr, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Button(
                        onClick = {
                            if (currentStep < 3) {
                                currentStep++
                            } else {
                                onComplete(
                                    selectedAppLanguage,
                                    selectedThemeMode,
                                    selectedAccent,
                                    autoDndEnabled,
                                    reminder15mEnabled,
                                    selectedScheduleOption,
                                    isBilingual
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = selectedAccent.primary)
                    ) {
                        Text(
                            text = if (currentStep == 3) "Get Started".tr else "Continue".tr,
                            fontWeight = FontWeight.Bold,
                            color = selectedAccent.onPrimary
                        )
                        if (currentStep < 3) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = selectedAccent.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
}
    } // End CompositionLocalProvider
} // End BootstartOnboardingDialog
// -------------------------------------------------------------------------------------------------
// -------------------------------------------------------------------------------------------------
// STEP 1: THEME PREFERENCE & LIVE SOFT-GLOW PREVIEW
// -------------------------------------------------------------------------------------------------
@Composable
private fun OnboardingStepLanguageAndTheme(
    selectedLanguage: String,
    selectedMode: AppThemeMode,
    selectedAccent: AppAccentColor,
    onSelectLanguage: (String) -> Unit,
    onSelectMode: (AppThemeMode) -> Unit,
    onSelectAccent: (AppAccentColor) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column {
            Text(
                text = "Appearance".tr,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Choose your look and language.".tr,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Language Switcher
        Text(
            text = "APP LANGUAGE".tr,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ModeOptionCard(
                title = "System".tr,
                subtitle = "跟随系统",
                icon = Icons.Default.SettingsSuggest,
                isSelected = selectedLanguage == "system",
                accent = selectedAccent,
                onClick = { onSelectLanguage("system") },
                modifier = Modifier.weight(1f)
            )
            ModeOptionCard(
                title = "English",
                subtitle = "English",
                icon = Icons.Default.Language,
                isSelected = selectedLanguage == "en",
                accent = selectedAccent,
                onClick = { onSelectLanguage("en") },
                modifier = Modifier.weight(1f)
            )
            ModeOptionCard(
                title = "Chinese",
                subtitle = "中文",
                icon = Icons.Default.Language,
                isSelected = selectedLanguage == "zh",
                accent = selectedAccent,
                onClick = { onSelectLanguage("zh") },
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(2.dp))

        // Live Preview Card
        Text(
            text = "LIVE CARD PREVIEW".tr,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = selectedAccent.primary
        )

        AdaptiveGlassPreviewCard(
            accentOverride = selectedAccent.primary,
            courseName = if (selectedLanguage == "zh") "CS302 算法设计与分析" else "CS302 Algorithm Design & Analysis",
            classroom = if (selectedLanguage == "zh") "理科楼4号楼 302教室" else "Science Bldg 4, Rm 302",
            timePeriod = if (selectedLanguage == "zh") "周一 08:00 - 09:35 (1-2节)" else "Mon 08:00 - 09:35 (Periods 1-2)",
            lecturer = if (selectedLanguage == "zh") "刘教授" else "Prof. Liu"
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Display Mode Switcher (Day vs Night vs System)
        Text(
            text = "DISPLAY MODE".tr,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ModeOptionCard(
                title = "Day Mode",
                subtitle = "Light Canvas",
                icon = Icons.Default.LightMode,
                isSelected = selectedMode == AppThemeMode.DAY,
                accent = selectedAccent,
                onClick = { onSelectMode(AppThemeMode.DAY) },
                modifier = Modifier.weight(1f)
            )

            ModeOptionCard(
                title = "Night Mode",
                subtitle = "System OLED",
                icon = Icons.Default.DarkMode,
                isSelected = selectedMode == AppThemeMode.NIGHT,
                accent = selectedAccent,
                onClick = { onSelectMode(AppThemeMode.NIGHT) },
                modifier = Modifier.weight(1f)
            )

            ModeOptionCard(
                title = "System",
                subtitle = "Auto Follow",
                icon = Icons.Default.BrightnessAuto,
                isSelected = selectedMode == AppThemeMode.SYSTEM,
                accent = selectedAccent,
                onClick = { onSelectMode(AppThemeMode.SYSTEM) },
                modifier = Modifier.weight(1f)
            )
        }

        // Primary Accent Color Picker
        Text(
            text = "PRIMARY ACCENT COLOR (SOFT-GLOW BORDER)".tr,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppAccents.ALL.forEach { accent ->
                    val isSelected = accent.id == selectedAccent.id
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectAccent(accent) }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .border(
                                    width = if (isSelected) 3.dp else 1.5.dp,
                                    color = if (isSelected) accent.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                                .padding(3.dp)
                                .clip(CircleShape)
                                .background(accent.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when (accent.id) {
                                "pink" -> "Pink".tr
                                "blue" -> "Blue".tr
                                "green" -> "Emerald".tr
                                "purple" -> "Violet".tr
                                "orange" -> "Sunrise".tr
                                else -> accent.name.tr
                            },
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) accent.primary else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ModeOptionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    accent: AppAccentColor,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) accent.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) accent.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) accent.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title.tr,
                fontSize = 11.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) accent.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle.tr,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// -------------------------------------------------------------------------------------------------
// STEP 2: COURSE SCHEDULE SETUP (FOR CHINESE UNIVERSITY STUDENTS)
// -------------------------------------------------------------------------------------------------
@Composable
private fun OnboardingStepSchedule(
    selectedOption: Int,
    onSelectOption: (Int) -> Unit,
    isBilingual: Boolean,
    onToggleBilingual: (Boolean) -> Unit,
    accent: AppAccentColor
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column {
            Text(
                text = "Timetable Setup".tr,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Start fresh or load a standard semester schedule.".tr,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Feature Highlights Badge Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = accent.primary.copy(alpha = 0.12f),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("1-12 Periods".tr, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = accent.primary)
                    Text("Morning to Night".tr, fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = accent.primary.copy(alpha = 0.12f),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("单双周 Parity".tr, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = accent.primary)
                    Text("Week Alternation".tr, fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = accent.primary.copy(alpha = 0.12f),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("调休 Make-up".tr, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = accent.primary)
                    Text("Weekend Shift".tr, fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Text(
            text = "HOW WOULD YOU LIKE TO START?".tr,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Option 0: Mamun Fall 2026 Preset Schedule (Recommended)
        ScheduleOptionCard(
            title = "Load Mamun Fall 2026 Schedule",
            subtitle = "Recommended • 7 courses, 14 class slots, algorithms, operating systems, and lab sessions ready to explore.",
            badge = "RECOMMENDED",
            isSelected = selectedOption == 0,
            icon = Icons.Default.School,
            accent = accent,
            onClick = { onSelectOption(0) }
        )

        // Option 1: Import from Spreadsheet / CSV / Paste
        ScheduleOptionCard(
            title = "Import via Excel / CSV / Copy-Paste",
            subtitle = "Paste your school's timetable grid or upload a spreadsheet. Supports Tsinghua, Peking, and Chinese university formats.",
            badge = "IMPORT",
            isSelected = selectedOption == 1,
            icon = Icons.Default.UploadFile,
            accent = accent,
            onClick = { onSelectOption(1) }
        )

        // Option 2: Blank Schedule
        ScheduleOptionCard(
            title = "Start with a Blank Schedule",
            subtitle = "Empty timetable. You can manually enter individual courses later using the + button.",
            badge = "CLEAN",
            isSelected = selectedOption == 2,
            icon = Icons.Default.EditCalendar,
            accent = accent,
            onClick = { onSelectOption(2) }
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Language translation preference
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text("Bilingual Course Names".tr, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    Text(
                        "Shows course names in English + Chinese (e.g., Computer Systems 计算机系统) for easy navigation.".tr,
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 13.5.sp
                    )
                }
                Switch(
                    checked = isBilingual,
                    onCheckedChange = onToggleBilingual,
                    colors = SwitchDefaults.colors(checkedThumbColor = accent.primary)
                )
            }
        }
    }
}

@Composable
private fun ScheduleOptionCard(
    title: String,
    subtitle: String,
    badge: String,
    isSelected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: AppAccentColor,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) accent.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) accent.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) accent.primary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) accent.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title.tr,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        modifier = Modifier.weight(1f).padding(end = 6.dp),
                        maxLines = 1
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isSelected) accent.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    ) {
                        Text(
                            text = badge.tr,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = subtitle.tr,
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 13.5.sp,
                    maxLines = 2
                )
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// STEP 3: DND MODE & CLASS REMINDER SETUP
// -------------------------------------------------------------------------------------------------
@Composable
private fun OnboardingStepDnd(
    autoDnd: Boolean,
    onToggleAutoDnd: (Boolean) -> Unit,
    reminder15m: Boolean,
    onToggleReminder15m: (Boolean) -> Unit,
    isDndGranted: Boolean,
    onRequestDndPermission: () -> Unit,
    accent: AppAccentColor
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column {
            Text(
                text = "Focus & Alerts".tr,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Automate silence during lectures and get timely reminders.".tr,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // DND Toggle Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = accent.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.DoNotDisturbOn,
                                    contentDescription = null,
                                    tint = accent.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column {
                            Text("Automatic DND During Class".tr, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                            Text("Silences notifications during scheduled lectures".tr, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Switch(
                        checked = autoDnd,
                        onCheckedChange = onToggleAutoDnd,
                        colors = SwitchDefaults.colors(checkedThumbColor = accent.primary)
                    )
                }

                // Permission indicator
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDndGranted) Color(0xFF10B981).copy(alpha = 0.12f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                if (isDndGranted) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = if (isDndGranted) Color(0xFF10B981) else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (isDndGranted) "System DND Policy Access Granted" else "Policy Permission Needed for DND",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isDndGranted) Color(0xFF065F46) else MaterialTheme.colorScheme.error
                            )
                        }

                        if (!isDndGranted) {
                            TextButton(
                                onClick = onRequestDndPermission,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Grant".tr, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 15-Minute Advance Reminder Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = accent.primary.copy(alpha = 0.15f),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = accent.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column {
                        Text("15-Minute Advance Alerts".tr, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                        Text(
                            "Delivers alerts with classroom building & room code 15m prior".tr,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = reminder15m,
                    onCheckedChange = onToggleReminder15m,
                    colors = SwitchDefaults.colors(checkedThumbColor = accent.primary)
                )
            }
        }

        // Privacy Guarantee badge
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "100% Offline & Private: No schedule or personal data ever leaves your device.".tr,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
