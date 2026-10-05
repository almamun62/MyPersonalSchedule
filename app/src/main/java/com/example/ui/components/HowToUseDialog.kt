package com.example.ui.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.tr

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowToUseDialog(
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.HelpOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text("How to Use App".tr, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                            Text("Easy Step-by-Step Guide", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Scrollable Category Chips
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 0.dp,
                    divider = {},
                    indicator = {}
                ) {
                    val tabs = listOf(
                        "🚀 Quick Start",
                        "📅 Timetable",
                        "⚡ Automations",
                        "💡 FAQs & Tips"
                    )
                    tabs.forEachIndexed { index, title ->
                        FilterChip(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            label = { Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                }

                // Tab Content Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (selectedTab) {
                        0 -> QuickStartSection()
                        1 -> TimetableGesturesSection()
                        2 -> AutomationsSection()
                        3 -> FaqSection()
                    }
                }

                // Bottom Action Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Got It, Let's Start!".tr, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun QuickStartSection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GuideCard(
            stepNumber = "1",
            title = "View Your Weekly Schedule",
            description = "Tap the 'Timetable' tab on the bottom menu to see your 12-period daily class schedule, including lunch break timings and classroom numbers.",
            icon = Icons.Outlined.CalendarToday,
            badge = "Timetable Tab",
            color = Color(0xFF3B82F6)
        )

        GuideCard(
            stepNumber = "2",
            title = "Import or Add Classes",
            description = "Go to 'More Options' -> 'Import Schedule' to instantly load your SWPU Fall 2026 courses or import custom CSV files.",
            icon = Icons.Outlined.CloudUpload,
            badge = "Import Tool",
            color = Color(0xFF10B981)
        )

        GuideCard(
            stepNumber = "3",
            title = "Add Home Screen Widget",
            description = "Long-press your phone's home screen, tap Widgets, and select 'SWPU Class Schedule' to track your next class directly from your home screen!",
            icon = Icons.Outlined.Widgets,
            badge = "Glance Widget",
            color = Color(0xFF8B5CF6)
        )
    }
}

@Composable
private fun TimetableGesturesSection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GuideCard(
            stepNumber = "👉",
            title = "Tap Any Course Card",
            description = "Tapping an active class card opens the Quick Modify dialog so you can immediately edit room numbers, instructor names, or class periods.",
            icon = Icons.Outlined.Edit,
            badge = "Quick Edit",
            color = Color(0xFFF59E0B)
        )

        GuideCard(
            stepNumber = "➕",
            title = "Tap Any Empty Time Slot",
            description = "Tap an empty period slot on the schedule grid to quickly add a new class or lecture into that exact time slot.",
            icon = Icons.Outlined.AddCircleOutline,
            badge = "Instant Add",
            color = Color(0xFF0EA5E9)
        )

        GuideCard(
            stepNumber = "🗓️",
            title = "Switch Days & Weeks",
            description = "Tap the Monday-Sunday chips at the top to view schedules for specific days, or change academic weeks using the top week selector.",
            icon = Icons.Outlined.DateRange,
            badge = "Filter Days",
            color = Color(0xFFEC4899)
        )
    }
}

@Composable
private fun AutomationsSection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GuideCard(
            stepNumber = "🔕",
            title = "Auto Do Not Disturb (DND)",
            description = "When enabled, the app automatically silences your phone's ringers during class hours so your lectures are never interrupted by phone calls.",
            icon = Icons.Outlined.DoNotDisturbOn,
            badge = "Auto Silence",
            color = Color(0xFFEF4444)
        )

        GuideCard(
            stepNumber = "⏰",
            title = "15-Minute Class Reminders",
            description = "Get a gentle notification 15 minutes before every lecture begins so you always have time to head to your classroom.",
            icon = Icons.Outlined.NotificationsActive,
            badge = "Reminders",
            color = Color(0xFF3B82F6)
        )

        GuideCard(
            stepNumber = "⏱️",
            title = "Study Focus Lock Mode",
            description = "Activate 25-minute Pomodoro study lock sessions from 'More Options' -> 'Focus & Study Timer' to block distractions during study time.",
            icon = Icons.Outlined.Timer,
            badge = "Pomodoro",
            color = Color(0xFF10B981)
        )
    }
}

@Composable
private fun FaqSection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GuideCard(
            stepNumber = "🔒",
            title = "Does this app work 100% offline?",
            description = "Yes! All course data, tasks, notes, and schedules are stored safely inside your phone's local database. No cloud account or internet needed.",
            icon = Icons.Outlined.Security,
            badge = "100% Offline",
            color = Color(0xFF14B8A6)
        )

        GuideCard(
            stepNumber = "🌐",
            title = "How do I change app language or theme?",
            description = "Go to 'More Options' -> 'Settings & Theme' to toggle between English and Chinese or switch primary accent colors.",
            icon = Icons.Outlined.Palette,
            badge = "Customization",
            color = Color(0xFF6366F1)
        )
    }
}

@Composable
private fun GuideCard(
    stepNumber: String,
    title: String,
    description: String,
    icon: ImageVector,
    badge: String,
    color: Color
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = color.copy(alpha = 0.25f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = color.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = badge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = color,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp
                )
            }
        }
    }
}
