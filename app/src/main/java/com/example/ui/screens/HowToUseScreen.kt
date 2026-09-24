package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.tr

data class FeatureGuide(
    val title: String,
    val icon: ImageVector,
    val description: String,
    val tips: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowToUseScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val guides = listOf(
        FeatureGuide(
            title = "Dashboard & Live Progress".tr,
            icon = Icons.Default.Dashboard,
            description = "Your central command center for today's classes, academic week calculations, and live schedule status.".tr,
            tips = listOf(
                "See current academic week (e.g. Week 3 of 18) and semester completion progress bar.".tr,
                "Hero banner shows your next upcoming class with a live countdown timer and room location.".tr,
                "View conflict resolution results for today and tomorrow to see which class takes priority.".tr,
                "Check the weekly workload visualizer bar chart to plan ahead for busy days.".tr
            )
        ),
        FeatureGuide(
            title = "Weekly Timetable".tr,
            icon = Icons.Default.CalendarMonth,
            description = "A comprehensive 7-day schedule grid with period timing rules and conflict detection.".tr,
            tips = listOf(
                "Filter by day of the week or switch between semesters with the header dropdown.".tr,
                "Tap on any course card to inspect details, room location, instructor, or edit.".tr,
                "Use the floating '+' button to quickly add a new course or custom class slot.".tr
            )
        ),
        FeatureGuide(
            title = "Tasks & Exam Countdown".tr,
            icon = Icons.Default.TaskAlt,
            description = "Stay on top of assignments, homework deadlines, and upcoming exams.".tr,
            tips = listOf(
                "Organize tasks by priority: High (Red), Medium (Amber), or Low (Blue).".tr,
                "Exams feature dynamic countdown chips indicating how many days remain.".tr,
                "Add tasks directly from any course card on your Dashboard or Timetable.".tr
            )
        ),
        FeatureGuide(
            title = "Smart Import & OCR Vision".tr,
            icon = Icons.Default.Download,
            description = "Multiple fast methods to get your full schedule into the app in seconds.".tr,
            tips = listOf(
                "Quick Paste: Paste course list text copied directly from your university portal.".tr,
                "CSV / TSV Import: Load or upload tabular schedule data with automatic column mapping.".tr,
                "AI Camera OCR: Take a photo of your paper syllabus or screenshot to extract courses automatically.".tr
            )
        ),
        FeatureGuide(
            title = "15-Min Reminders & Auto-DND".tr,
            icon = Icons.Default.Alarm,
            description = "Automated alarms and focus mode so you never miss a class or disrupt a lecture.".tr,
            tips = listOf(
                "15-Minute Advance Reminder: Notifies you with classroom location and instructor before class starts.".tr,
                "Auto-DND Mode: Automatically silences your device during class periods and restores volume afterwards.".tr,
                "Grant notification policy access in Settings to enable system-level DND automation.".tr
            )
        ),
        FeatureGuide(
            title = "Class Notes & Study Hub".tr,
            icon = Icons.Default.MenuBook,
            description = "Keep class notes neatly organized by course code.".tr,
            tips = listOf(
                "Take course notes tagged by course code to keep study materials organized.".tr,
                "Access notes anytime during your study sessions.".tr
            )
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("How to Use".tr, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back".tr)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Welcome to Smart Schedule".tr,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Explore the guide below to learn how each feature helps you manage courses, assignments, exams, and daily focus.".tr,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            items(guides) { guide ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = guide.icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = guide.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = guide.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        guide.tips.forEach { tip ->
                            Row(
                                modifier = Modifier.padding(vertical = 3.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "• ",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = tip,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
