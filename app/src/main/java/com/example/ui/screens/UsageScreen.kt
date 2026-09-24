package com.example.ui.screens

import android.app.AppOpsManager
import com.example.ui.theme.tr
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.ScheduleUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

import androidx.compose.material.icons.automirrored.filled.ArrowBack

data class AppUsageItem(
    val packageName: String,
    val appName: String,
    val usageTimeMs: Long
)

@Composable
fun UsageScreen(
    state: ScheduleUiState,
    onNavigateBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var hasUsagePermission by remember { mutableStateOf(checkUsageStatsPermission(context)) }

    var topApps by remember { mutableStateOf<List<AppUsageItem>>(emptyList()) }
    var totalPhoneUsageMs by remember { mutableStateOf(0L) }
    var isLoading by remember { mutableStateOf(false) }

    // Derived stats from the offline schedule
    val totalClassTimeMinutes = remember(state.todayCoursesWithStatus) {
        state.todayCoursesWithStatus.sumOf { (it.course.endPeriod - it.course.startPeriod + 1) * 45L }
    }
    val totalStudyTimeMinutes = remember(state.pendingTasks) {
        state.pendingTasks.size * 30L // Estimate: 30 min per pending task
    }

    LaunchedEffect(hasUsagePermission) {
        if (hasUsagePermission) {
            isLoading = true
            withContext(Dispatchers.IO) {
                val stats = getUsageStats(context)
                totalPhoneUsageMs = stats.sumOf { it.usageTimeMs }
                topApps = stats.sortedByDescending { it.usageTimeMs }.take(6)
            }
            isLoading = false
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                if (onNavigateBack != null) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back".tr
                        )
                    }
                }
                Text(
                    text = "Screen Time & Focus".tr,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (!hasUsagePermission) {
            item {
                PermissionRequestCard(
                    onRequest = {
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    },
                    onRefresh = {
                        hasUsagePermission = checkUsageStatsPermission(context)
                    }
                )
            }
        } else {
            item {
                UsageOverviewRow(
                    phoneUsageMs = totalPhoneUsageMs,
                    classTimeMinutes = totalClassTimeMinutes,
                    studyTimeMinutes = totalStudyTimeMinutes
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Most Used Applications".tr,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (isLoading) {
                item {
                    CircularProgressIndicator(modifier = Modifier.padding(32.dp))
                }
            } else {
                items(topApps) { app ->
                    AppUsageCard(app = app)
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "App Limitations & Rules".tr,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                AppLimitCard(
                    title = "Block Distracting Apps",
                    description = "When active, non-essential apps (social media, games) are restricted during your scheduled Class Time and Study Time.",
                    icon = Icons.Default.Block
                )
            }
        }
    }
}

@Composable
fun UsageOverviewRow(phoneUsageMs: Long, classTimeMinutes: Long, studyTimeMinutes: Long) {
    val phoneMinutes = phoneUsageMs / (1000 * 60)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            title = "Mobile Usage",
            value = formatMinutes(phoneMinutes),
            icon = Icons.Default.Smartphone,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
        StatCard(
            modifier = Modifier.weight(1f),
            title = "Class Time",
            value = formatMinutes(classTimeMinutes),
            icon = Icons.Default.School,
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
        StatCard(
            modifier = Modifier.weight(1f),
            title = "Study Time",
            value = formatMinutes(studyTimeMinutes),
            icon = Icons.Default.AutoStories,
            color = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    contentColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = contentColor)
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = contentColor)
            Text(text = title, style = MaterialTheme.typography.bodySmall, color = contentColor.copy(alpha = 0.8f))
        }
    }
}

@Composable
fun AppUsageCard(app: AppUsageItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Apps, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = app.appName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(text = app.packageName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = formatMinutes(app.usageTimeMs / (1000 * 60)),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun AppLimitCard(title: String, description: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    var isEnabled by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f))
            }
            Switch(checked = isEnabled, onCheckedChange = { isEnabled = it })
        }
    }
}

@Composable
fun PermissionRequestCard(onRequest: () -> Unit, onRefresh: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Text(
                text = "Usage Access Required".tr,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = "To track your phone usage and block distracting apps during class, you must grant Usage Access permission in your device settings.".tr,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRequest, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)) {
                    Text("Open Settings".tr)
                }
                OutlinedButton(onClick = onRefresh) {
                    Text("I've Granted It".tr)
                }
            }
        }
    }
}

private fun formatMinutes(totalMinutes: Long): String {
    if (totalMinutes <= 0) return "0h 0m"
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

private fun checkUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
    } else {
        appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
    }
    return mode == AppOpsManager.MODE_ALLOWED
}

private fun getUsageStats(context: Context): List<AppUsageItem> {
    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val pm = context.packageManager
    
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    val startOfDay = calendar.timeInMillis
    val endOfDay = System.currentTimeMillis()

    val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startOfDay, endOfDay)
    if (stats.isNullOrEmpty()) return emptyList()

    val usageMap = mutableMapOf<String, Long>()
    for (stat in stats) {
        if (stat.totalTimeInForeground > 0) {
            usageMap[stat.packageName] = usageMap.getOrDefault(stat.packageName, 0L) + stat.totalTimeInForeground
        }
    }

    return usageMap.mapNotNull { (packageName, timeMs) ->
        // Filter out system packages or launchers if needed
        if (packageName == context.packageName || packageName.contains("launcher") || packageName.contains("systemui")) return@mapNotNull null
        
        val appName = try {
            val ai = pm.getApplicationInfo(packageName, 0)
            if ((ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0) return@mapNotNull null // Skip system apps
            pm.getApplicationLabel(ai).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
        AppUsageItem(packageName, appName, timeMs)
    }
}
