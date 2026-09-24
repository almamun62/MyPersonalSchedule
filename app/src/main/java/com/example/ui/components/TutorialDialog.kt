package com.example.ui.components

import androidx.compose.foundation.layout.*
import com.example.ui.theme.tr
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun TutorialDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "How to Use MySchedule".tr,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TutorialSection(
                    icon = Icons.Default.Dashboard,
                    title = "Dashboard & Insights",
                    description = "Your daily overview. See your upcoming classes, active tasks, and a weekly insight analysis at the bottom."
                )
                
                TutorialSection(
                    icon = Icons.Default.CalendarToday,
                    title = "Timetable & Notebooks",
                    description = "View your weekly schedule. Tap on any course to see details or open the 'Class Notebook' to write, record voice memos, and add photos during lectures."
                )
                
                TutorialSection(
                    icon = Icons.Default.Assignment,
                    title = "Tasks & Exams",
                    description = "Keep track of your homework and exams. Add deadlines so they appear on your Dashboard."
                )

                TutorialSection(
                    icon = Icons.Default.ChatBubble,
                    title = "AI Chat (BYOK)",
                    description = "Go to Settings and add your Gemini API key to ask questions about your schedule. E.g., 'When is my next math class?'"
                )

                TutorialSection(
                    icon = Icons.Default.NotificationsActive,
                    title = "Auto-DND",
                    description = "When enabled in Settings, your phone automatically enters Do Not Disturb mode during your classes."
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                Text("Got it!".tr)
            }
        }
    )
}

@Composable
private fun TutorialSection(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp).padding(top = 2.dp)
        )
        Column {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
