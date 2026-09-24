package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.GridItemSpan
import com.example.ui.theme.tr

data class MoreMenuItem(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
fun MoreScreen(
    onNavigateTo: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        MoreMenuItem("Quick Note".tr, Icons.Default.NoteAdd) { onNavigateTo("QUICK_NOTE") },
        MoreMenuItem("Import Schedule".tr, Icons.Default.CloudUpload) { onNavigateTo("IMPORT_SCHEDULE") },
        MoreMenuItem("Tasks & Exams".tr, Icons.Default.TaskAlt) { onNavigateTo("TASKS_EXAMS") },
        MoreMenuItem("Notes".tr, Icons.Default.MenuBook) { onNavigateTo("NOTES") },
        MoreMenuItem("Screen Usage".tr, Icons.Default.Timeline) { onNavigateTo("USAGE") },
        MoreMenuItem("Academic Calendar".tr, Icons.Default.DateRange) { onNavigateTo("ACADEMIC_CALENDAR") },
        MoreMenuItem("Settings".tr, Icons.Default.Settings) { onNavigateTo("SETTINGS") },
        MoreMenuItem("About App".tr, Icons.Default.Info) { onNavigateTo("ABOUT") },
        MoreMenuItem("How to use".tr, Icons.Default.Help) { onNavigateTo("HOW_TO_USE") }
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(2) }) {
            Text(
                text = "More Options".tr,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp, top = 8.dp)
            )
        }
        items(items) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = item.onClick)
                    .height(100.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}
