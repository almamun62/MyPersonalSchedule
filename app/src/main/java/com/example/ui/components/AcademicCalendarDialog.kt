package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.data.model.AcademicCalendarFileEntity
import com.example.ui.viewmodel.ScheduleViewModel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademicCalendarDialog(
    viewModel: ScheduleViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val calendarFiles by viewModel.allCalendarFiles.collectAsState()

    var previewImageFile by remember { mutableStateOf<AcademicCalendarFileEntity?>(null) }
    var fileToDelete by remember { mutableStateOf<AcademicCalendarFileEntity?>(null) }
    var noteInputDialogFile by remember { mutableStateOf<AcademicCalendarFileEntity?>(null) }
    var noteText by remember { mutableStateOf("") }

    // File picker launcher supporting Photo, PDF, Word, Excel, and general documents
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            saveCalendarFileFromUri(context, viewModel, selectedUri)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(24.dp))
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF38BDF8),
                            Color(0xFF818CF8),
                            Color(0xFFC084FC)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                ),
            color = Color(0xFF0F172A)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF3B82F6), Color(0xFF8B5CF6))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarMonth,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Academic Calendar Store",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 19.sp,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "Offline File Vault (Photo, PDF, Word, Excel)",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Format Type Legend Badges
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E293B))
                        .padding(vertical = 10.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FormatTypeBadge(emoji = "🖼️", label = "Photo", color = Color(0xFFA855F7))
                    FormatTypeBadge(emoji = "📄", label = "PDF", color = Color(0xFFEF4444))
                    FormatTypeBadge(emoji = "📝", label = "WORD", color = Color(0xFF3B82F6))
                    FormatTypeBadge(emoji = "📊", label = "EXCEL", color = Color(0xFF10B981))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Bar: Upload File & Add Sample File
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            try {
                                filePickerLauncher.launch(
                                    arrayOf(
                                        "image/*",
                                        "application/pdf",
                                        "application/msword",
                                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                        "application/vnd.ms-excel",
                                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                        "*/*"
                                    )
                                )
                            } catch (e: Exception) {
                                Toast.makeText(context, "Opening file picker...", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2563EB)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.UploadFile,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Upload File",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            createSampleCalendarFile(context, viewModel)
                        },
                        modifier = Modifier.height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFF38BDF8), Color(0xFF818CF8))
                            )
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF38BDF8)
                        )
                    ) {

                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Sample Cal", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Content List
                if (calendarFiles.isEmpty()) {
                    // Empty state with pre-loaded default schedule details
                    EmptyCalendarState(
                        onUploadClick = {
                            filePickerLauncher.launch(arrayOf("*/*"))
                        },
                        onSampleClick = {
                            createSampleCalendarFile(context, viewModel)
                        }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(calendarFiles, key = { it.id }) { item ->
                            CalendarFileCard(
                                fileItem = item,
                                onViewClick = {
                                    if (item.fileType == "PHOTO") {
                                        previewImageFile = item
                                    } else {
                                        openCalendarFile(context, item)
                                    }
                                },
                                onShareClick = {
                                    shareCalendarFile(context, item)
                                },
                                onDeleteClick = {
                                    fileToDelete = item
                                },
                                onAddNoteClick = {
                                    noteInputDialogFile = item
                                    noteText = item.note
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Photo Preview Fullscreen Dialog
    previewImageFile?.let { photoFile ->
        PhotoCalendarPreviewDialog(
            fileItem = photoFile,
            onDismiss = { previewImageFile = null },
            onShare = { shareCalendarFile(context, photoFile) }
        )
    }

    // Delete Confirmation Dialog
    fileToDelete?.let { file ->
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            containerColor = Color(0xFF1E293B),
            titleContentColor = Color.White,
            textContentColor = Color(0xFF94A3B8),
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Delete Academic Calendar File?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to remove '${file.fileName}' from your offline storage?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCalendarFile(file)
                        fileToDelete = null
                        Toast.makeText(context, "File deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    // Note Input Dialog
    noteInputDialogFile?.let { file ->
        AlertDialog(
            onDismissRequest = { noteInputDialogFile = null },
            containerColor = Color(0xFF1E293B),
            titleContentColor = Color.White,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.EditNote,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Add Note / Description", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    placeholder = { Text("e.g. Fall 2026 Official Exam & Vacation Dates", color = Color(0xFF64748B)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF334155)
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addCalendarFile(file.copy(note = noteText))
                        noteInputDialogFile = null
                        Toast.makeText(context, "Note saved", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text("Save Note", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { noteInputDialogFile = null }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }
}

@Composable
fun FormatTypeBadge(emoji: String, label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = emoji, fontSize = 13.sp)
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun CalendarFileCard(
    fileItem: AcademicCalendarFileEntity,
    onViewClick: () -> Unit,
    onShareClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onAddNoteClick: () -> Unit
) {
    val (typeColor, typeIcon, typeLabel) = when (fileItem.fileType) {
        "PHOTO" -> Triple(Color(0xFFA855F7), Icons.Outlined.Image, "PHOTO")
        "PDF" -> Triple(Color(0xFFEF4444), Icons.Outlined.Article, "PDF")
        "WORD" -> Triple(Color(0xFF3B82F6), Icons.Outlined.Description, "WORD")
        "EXCEL" -> Triple(Color(0xFF10B981), Icons.Outlined.TableChart, "EXCEL")
        else -> Triple(Color(0xFF0EA5E9), Icons.Outlined.InsertDriveFile, "DOCUMENT")
    }

    val formattedDate = remember(fileItem.addedAtMillis) {
        SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault()).format(Date(fileItem.addedAtMillis))
    }

    val formattedSize = remember(fileItem.fileSizeBytes) {
        if (fileItem.fileSizeBytes < 1024) "${fileItem.fileSizeBytes} B"
        else if (fileItem.fileSizeBytes < 1024 * 1024) "${fileItem.fileSizeBytes / 1024} KB"
        else String.format(Locale.getDefault(), "%.1f MB", fileItem.fileSizeBytes / (1024f * 1024f))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onViewClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                colors = listOf(typeColor.copy(alpha = 0.5f), Color(0xFF334155))
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Type Badge & Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(typeColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = typeIcon,
                            contentDescription = null,
                            tint = typeColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = fileItem.fileName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = typeColor.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = typeLabel,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = typeColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = "$formattedSize · $formattedDate",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                // Delete Button
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Note section if present
            if (fileItem.note.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0F172A),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.StickyNote2,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = fileItem.note,
                            fontSize = 12.sp,
                            color = Color(0xFFCBD5E1),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // View / Open Button
                Button(
                    onClick = onViewClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = typeColor)
                ) {
                    Icon(
                        imageVector = if (fileItem.fileType == "PHOTO") Icons.Outlined.Visibility else Icons.Outlined.OpenInNew,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (fileItem.fileType == "PHOTO") "View Photo" else "Open File",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Add Note Button
                OutlinedButton(
                    onClick = onAddNoteClick,
                    modifier = Modifier.height(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.EditNote,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Note", fontSize = 12.sp)
                }

                // Share Button
                IconButton(
                    onClick = onShareClick,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF334155))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Share",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyCalendarState(
    onUploadClick: () -> Unit,
    onSampleClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E293B))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.FolderZip,
            contentDescription = null,
            tint = Color(0xFF38BDF8),
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "No Saved Calendar Files Yet",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Store your official Academic Calendar photos, PDFs, Word docs, or Excel sheets for 100% offline access anytime.",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                fontSize = 12.sp
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Preset Information Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.linearGradient(listOf(Color(0xFF38BDF8), Color(0xFF818CF8)))
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "🏫 Fall 2026 Academic Calendar Overview",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFF38BDF8)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "• Term: Sept 7, 2026 – Jan 15, 2027 (16 Weeks)", fontSize = 11.sp, color = Color.White)
                Text(text = "• Midterm Exams: Week 8 (Oct 26 – Oct 30)", fontSize = 11.sp, color = Color.White)
                Text(text = "• National Day Holiday: Oct 1 – Oct 7", fontSize = 11.sp, color = Color.White)
                Text(text = "• Final Exams: Week 16 (Jan 11 – Jan 15)", fontSize = 11.sp, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onUploadClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Upload Document", fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = onSampleClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8))
            ) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Sample Cal", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun PhotoCalendarPreviewDialog(
    fileItem: AcademicCalendarFileEntity,
    onDismiss: () -> Unit,
    onShare: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(20.dp)),
            color = Color(0xFF0F172A)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = fileItem.fileName,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row {
                        IconButton(onClick = onShare) {
                            Icon(Icons.Outlined.Share, contentDescription = "Share", tint = Color.White)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Image Viewer Container
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    val imgFile = remember(fileItem.localPath) { File(fileItem.localPath) }
                    if (imgFile.exists()) {
                        AsyncImage(
                            model = imgFile,
                            contentDescription = fileItem.fileName,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.BrokenImage,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Image file not found on local storage.", color = Color(0xFF94A3B8), fontSize = 13.sp)
                        }
                    }
                }

                if (fileItem.note.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Note: ${fileItem.note}",
                        color = Color(0xFF38BDF8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// Helper: Save calendar file selected from Uri into internal storage
fun saveCalendarFileFromUri(
    context: Context,
    viewModel: ScheduleViewModel,
    uri: Uri
) {
    try {
        var fileName = "academic_calendar"
        var fileSize = 0L

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex != -1) fileName = cursor.getString(nameIndex)
                if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
            }
        }

        val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
        val lowerName = fileName.lowercase()

        val fileType = when {
            lowerName.endsWith(".png") || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".webp") || mimeType.startsWith("image/") -> "PHOTO"
            lowerName.endsWith(".pdf") || mimeType.contains("pdf") -> "PDF"
            lowerName.endsWith(".doc") || lowerName.endsWith(".docx") || mimeType.contains("word") -> "WORD"
            lowerName.endsWith(".xls") || lowerName.endsWith(".xlsx") || lowerName.endsWith(".csv") || mimeType.contains("excel") || mimeType.contains("spreadsheet") -> "EXCEL"
            else -> "OTHER"
        }

        // Copy file stream to app internal storage directory
        val calDir = File(context.filesDir, "academic_calendars")
        if (!calDir.exists()) calDir.mkdirs()

        val targetFile = File(calDir, "cal_${System.currentTimeMillis()}_${fileName.replace(" ", "_")}")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        }

        if (fileSize <= 0) {
            fileSize = targetFile.length()
        }

        val entity = AcademicCalendarFileEntity(
            fileName = fileName,
            fileType = fileType,
            mimeType = mimeType,
            localPath = targetFile.absolutePath,
            fileSizeBytes = fileSize,
            addedAtMillis = System.currentTimeMillis(),
            note = "Uploaded from device"
        )

        viewModel.addCalendarFile(entity)
        Toast.makeText(context, "Saved '$fileName' to Offline Calendar Store!", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error saving calendar file: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

// Helper: Create a sample pre-loaded academic calendar file
fun createSampleCalendarFile(
    context: Context,
    viewModel: ScheduleViewModel
) {
    try {
        val calDir = File(context.filesDir, "academic_calendars")
        if (!calDir.exists()) calDir.mkdirs()

        val sampleFile = File(calDir, "University_Academic_Calendar_Fall_2026.pdf")
        if (!sampleFile.exists()) {
            sampleFile.writeText(
                "UNIVERSITY ACADEMIC CALENDAR FALL 2026\n\n" +
                        "1. Term Duration: September 7, 2026 – January 15, 2027 (16 Weeks)\n" +
                        "2. Midterm Examinations: Week 8 (October 26 – October 30)\n" +
                        "3. National Day Recess: October 1 – October 7\n" +
                        "4. Final Examinations: Week 16 (January 11 – January 15, 2027)\n" +
                        "5. Winter Vacation: January 18, 2027 onwards"
            )
        }

        val entity = AcademicCalendarFileEntity(
            fileName = "University_Academic_Calendar_Fall_2026.pdf",
            fileType = "PDF",
            mimeType = "application/pdf",
            localPath = sampleFile.absolutePath,
            fileSizeBytes = sampleFile.length(),
            addedAtMillis = System.currentTimeMillis(),
            note = "Official Fall 2026 Schedule & Exam Dates"
        )

        viewModel.addCalendarFile(entity)
        Toast.makeText(context, "Sample Calendar file saved!", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// Helper: Open file via system intent using FileProvider
fun openCalendarFile(context: Context, item: AcademicCalendarFileEntity) {
    try {
        val file = File(item.localPath)
        if (!file.exists()) {
            Toast.makeText(context, "File not found at local storage", Toast.LENGTH_SHORT).show()
            return
        }

        val contentUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, item.mimeType.ifEmpty { "*/*" })
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(
            context,
            "No viewer app found for ${item.fileType} (${item.fileName}). Please install a PDF/Document viewer.",
            Toast.LENGTH_LONG
        ).show()
    }
}

// Helper: Share calendar file
fun shareCalendarFile(context: Context, item: AcademicCalendarFileEntity) {
    try {
        val file = File(item.localPath)
        if (!file.exists()) {
            Toast.makeText(context, "File does not exist", Toast.LENGTH_SHORT).show()
            return
        }

        val contentUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = item.mimeType.ifEmpty { "*/*" }
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, "Academic Calendar: ${item.fileName}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Share Academic Calendar"))
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to share file: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
