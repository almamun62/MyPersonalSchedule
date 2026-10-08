package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.AppUpdateManager
import com.example.domain.UpdateCheckResult
import com.example.ui.viewmodel.ScheduleViewModel
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUpdateDialog(
    viewModel: ScheduleViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repoPref by viewModel.userPreferencesManager.customGithubRepo.collectAsStateWithLifecycle()

    var repoInput by remember { mutableStateOf(repoPref) }
    var isChecking by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableIntStateOf(0) }
    var updateResult by remember { mutableStateOf<UpdateCheckResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var downloadedApkFile by remember { mutableStateOf<File?>(null) }
    var showRepoConfig by remember { mutableStateOf(false) }

    val currentVersion = remember { AppUpdateManager.getCurrentVersion(context) }

    fun checkUpdates() {
        isChecking = true
        errorMessage = null
        updateResult = null
        downloadedApkFile = null
        coroutineScope.launch {
            val res = AppUpdateManager.checkForUpdates(context, repoInput)
            isChecking = false
            res.onSuccess {
                updateResult = it
            }.onFailure {
                errorMessage = it.localizedMessage ?: "Failed to check GitHub releases."
            }
        }
    }

    // Auto-check on dialog open
    LaunchedEffect(Unit) {
        checkUpdates()
    }

    fun startDownload(apkUrl: String, apkName: String) {
        isDownloading = true
        downloadProgress = 0
        errorMessage = null
        coroutineScope.launch {
            val res = AppUpdateManager.downloadApk(context, apkUrl, apkName) { progress ->
                downloadProgress = progress
            }
            isDownloading = false
            res.onSuccess { file ->
                downloadedApkFile = file
                Toast.makeText(context, "Downloaded! Opening installer...", Toast.LENGTH_SHORT).show()
                AppUpdateManager.installApk(context, file)
            }.onFailure {
                errorMessage = "Download error: ${it.localizedMessage}"
            }
        }
    }

    Dialog(
        onDismissRequest = {
            if (!isDownloading) onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Outlined.SystemUpdate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Software Update",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Installed: v$currentVersion",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = !isDownloading
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Repository Source Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Outlined.Code, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "Source: ${repoInput.trim()}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    TextButton(
                        onClick = { showRepoConfig = !showRepoConfig },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(if (showRepoConfig) "Done" else "Change Repo", fontSize = 11.sp)
                    }
                }

                // Repo Configuration Box
                AnimatedVisibility(visible = showRepoConfig) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Set GitHub Repository (owner/repo):", fontSize = 11.sp)
                            OutlinedTextField(
                                value = repoInput,
                                onValueChange = { repoInput = it },
                                placeholder = { Text("username/repository") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                            Button(
                                onClick = {
                                    viewModel.userPreferencesManager.setCustomGithubRepo(repoInput.trim())
                                    showRepoConfig = false
                                    checkUpdates()
                                },
                                modifier = Modifier.fillMaxWidth().height(36.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Save & Check", fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Status & Loading State
                if (isChecking) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                            Text("Checking GitHub Releases API...", fontSize = 13.sp)
                        }
                    }
                }

                // Error State
                if (errorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Check Failed", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                                Text(errorMessage!!, fontSize = 11.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }

                // Update Result Details
                if (updateResult != null && !isChecking) {
                    val res = updateResult!!

                    if (res.hasUpdate) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF10B981)
                                    ) {
                                        Text(
                                            text = "NEW VERSION AVAILABLE",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.Black,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "Version ${res.latestVersion}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color.White
                                )

                                Text(
                                    text = "What's New:",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.9f)
                                )

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.Black.copy(alpha = 0.25f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = res.releaseNotes,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp,
                                        color = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }

                                if (res.apkDownloadUrl != null) {
                                    val sizeMb = if (res.apkSize > 0) String.format("%.1f MB", res.apkSize / (1024.0 * 1024.0)) else ""
                                    Text(
                                        text = "📦 Package: ${res.apkName} ${if (sizeMb.isNotBlank()) "($sizeMb)" else ""}",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }

                        // Download Progress bar
                        if (isDownloading) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Downloading APK update...", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text("$downloadProgress%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                LinearProgressIndicator(
                                    progress = { downloadProgress / 100f },
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                                )
                            }
                        }

                        // Action Buttons for New Update
                        if (downloadedApkFile != null) {
                            Button(
                                onClick = { AppUpdateManager.installApk(context, downloadedApkFile!!) },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.InstallMobile, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Install Downloaded Update", fontWeight = FontWeight.Bold)
                            }
                        } else if (res.apkDownloadUrl != null) {
                            Button(
                                onClick = { startDownload(res.apkDownloadUrl, res.apkName ?: "update.apk") },
                                enabled = !isDownloading,
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (isDownloading) "Downloading... ($downloadProgress%)" else "Download & Install Update", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            // Release exists but no APK asset attached
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(res.releaseUrl))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.fillMaxWidth().height(46.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Outlined.OpenInBrowser, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("View Release on GitHub")
                            }
                        }
                    } else {
                        // Up to Date
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "You're on the Latest Version!",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Current: v$currentVersion • Latest: ${res.latestVersion}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Bottom Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { checkUpdates() },
                        enabled = !isChecking && !isDownloading,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Recheck", fontSize = 12.sp)
                    }

                    if (updateResult?.releaseUrl != null) {
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateResult!!.releaseUrl))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Outlined.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Releases", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
