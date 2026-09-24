package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.service.FocusLockService
import com.example.ui.components.InstalledAppItem
import com.example.ui.theme.tr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun FocusLockScreen(
    endTimeMillis: Long,
    totalDurationSeconds: Long,
    courseName: String,
    whitelistedPackages: Set<String>,
    onEmergencyUnlock: () -> Unit
) {
    val context = LocalContext.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        com.example.util.HapticHelper.vibrateDevice(context, 120L)
        com.example.util.HapticHelper.performClick(haptic)
    }

    var remainingMillis by remember { mutableLongStateOf((endTimeMillis - System.currentTimeMillis()).coerceAtLeast(0L)) }
    var holdProgress by remember { mutableFloatStateOf(0f) }
    var isHoldingEmergency by remember { mutableStateOf(false) }
    var showEmergencyConfirmDialog by remember { mutableStateOf(false) }

    // Whitelisted apps list with icons
    var allowedApps by remember { mutableStateOf<List<InstalledAppItem>>(emptyList()) }

    LaunchedEffect(whitelistedPackages) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolves = pm.queryIntentActivities(intent, 0)
            val list = resolves.filter { whitelistedPackages.contains(it.activityInfo.packageName) }
                .map { ri ->
                    InstalledAppItem(
                        packageName = ri.activityInfo.packageName,
                        appName = ri.loadLabel(pm).toString(),
                        iconDrawable = try { ri.loadIcon(pm) } catch (_: Exception) { null }
                    )
                }.distinctBy { it.packageName }
            allowedApps = list
        }
    }

    // 1-second countdown ticker
    LaunchedEffect(endTimeMillis) {
        while (true) {
            val diff = (endTimeMillis - System.currentTimeMillis()).coerceAtLeast(0L)
            remainingMillis = diff
            if (diff <= 0) break
            delay(500L)
        }
    }

    // Emergency Hold Handler: 5 seconds hold to unlock
    LaunchedEffect(isHoldingEmergency) {
        if (isHoldingEmergency) {
            val startHold = System.currentTimeMillis()
            while (isHoldingEmergency && holdProgress < 1f) {
                val elapsed = System.currentTimeMillis() - startHold
                holdProgress = (elapsed / 4000f).coerceIn(0f, 1f)
                if (holdProgress >= 1f) {
                    showEmergencyConfirmDialog = true
                    isHoldingEmergency = false
                    break
                }
                delay(50L)
            }
        } else {
            holdProgress = 0f
        }
    }

    val totalMillis = (totalDurationSeconds * 1000L).coerceAtLeast(1L)
    val progress = ((totalMillis - remainingMillis).toFloat() / totalMillis.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(400), label = "focus_progress")

    val hours = (remainingMillis / 3600000)
    val minutes = (remainingMillis % 3600000) / 60000
    val seconds = (remainingMillis % 60000) / 1000
    val timeFormatted = String.format("%02d:%02d:%02d", hours, minutes, seconds)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Header: Study status
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "DEEP STUDY LOCK ACTIVE".tr,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                }

                if (courseName.isNotBlank()) {
                    Text(
                        text = courseName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Text(
                    text = "Distracting apps are blocked. Stay focused on your goals.".tr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Center: Big Radial Progress & XX:XX:XX Countdown
            Box(
                modifier = Modifier.size(280.dp),
                contentAlignment = Alignment.Center
            ) {
                val primaryColor = MaterialTheme.colorScheme.primary
                val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 14.dp.toPx()
                    // Background track
                    drawCircle(
                        color = trackColor,
                        style = Stroke(width = strokeWidth)
                    )
                    // Animated progress arc
                    drawArc(
                        color = primaryColor,
                        startAngle = -90f,
                        sweepAngle = 360f * (1f - animatedProgress),
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.HourglassTop,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )

                    Text(
                        text = timeFormatted,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Text(
                        text = "${((1f - animatedProgress) * 100).toInt()}% Remaining",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Bottom Section: Allowed Apps Dock + Emergency Unlock
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Allowed Apps Dock
                if (allowedApps.isNotEmpty()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "ALLOWED STUDY TOOLS".tr,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.5.sp
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            items(allowedApps, key = { it.packageName }) { app ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            try {
                                                val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                                                if (launchIntent != null) {
                                                    context.startActivity(launchIntent)
                                                }
                                            } catch (_: Exception) {}
                                        }
                                        .padding(6.dp)
                                ) {
                                    if (app.iconDrawable != null) {
                                        val bmp = remember(app.packageName) {
                                            try { app.iconDrawable.toBitmap(80, 80).asImageBitmap() } catch (_: Exception) { null }
                                        }
                                        if (bmp != null) {
                                            Image(
                                                bitmap = bmp,
                                                contentDescription = app.appName,
                                                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp))
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = app.appName,
                                        fontSize = 10.5.sp,
                                        maxLines = 1,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // Emergency Unlock (5s hold)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    isHoldingEmergency = true
                                    tryAwaitRelease()
                                    isHoldingEmergency = false
                                }
                            )
                        },
                    contentAlignment = Alignment.CenterStart
                ) {
                    // Fill bar as user holds
                    if (holdProgress > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(holdProgress)
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = if (isHoldingEmergency) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHoldingEmergency) "Keep holding to unlock...".tr else "Hold 5s to Emergency Unlock".tr,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isHoldingEmergency) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showEmergencyConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showEmergencyConfirmDialog = false },
            title = { Text("Emergency Unlock".tr, fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to end your study lock early? Maintaining focus builds strong study habits.".tr)
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEmergencyConfirmDialog = false
                        onEmergencyUnlock()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Unlock Phone".tr)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmergencyConfirmDialog = false }) {
                    Text("Stay Focused".tr)
                }
            }
        )
    }
}
