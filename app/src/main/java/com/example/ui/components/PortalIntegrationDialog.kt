package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.domain.model.ImportedCourse

enum class UniversityPortalSystem(val displayName: String, val category: String) {
    ZHENGFANG("Zhengfang Educational System (正方教务)", "Chinese"),
    SHUWEI("Shuwei Educational System (树维)", "Chinese"),
    QINGGUO("Qingguo Portal (青果)", "Chinese"),
    CANVAS("Canvas LMS SSO", "International"),
    BLACKBOARD("Blackboard Learn", "International"),
    BANNER("Ellucian Banner SSO", "International")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortalIntegrationDialog(
    onDismiss: () -> Unit,
    onImportCourses: (List<ImportedCourse>) -> Unit
) {
    val context = LocalContext.current
    var selectedPortal by remember { mutableStateOf(UniversityPortalSystem.ZHENGFANG) }
    var portalUrl by remember { mutableStateOf("https://jwxt.swpu.edu.cn") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("University Portal Direct Sync", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Text(
                    text = "Log in securely to fetch your course schedule directly from your university's educational administration system or LMS portal.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Portal Selection Dropdown / Filter Chips
                Text("Select University System", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    UniversityPortalSystem.values().forEach { portal ->
                        FilterChip(
                            selected = selectedPortal == portal,
                            onClick = {
                                selectedPortal = portal
                                portalUrl = when (portal) {
                                    UniversityPortalSystem.ZHENGFANG -> "https://jwxt.swpu.edu.cn"
                                    UniversityPortalSystem.SHUWEI -> "https://jw.shuwei.edu.cn"
                                    UniversityPortalSystem.CANVAS -> "https://canvas.instructure.com"
                                    else -> "https://portal.university.edu"
                                }
                            },
                            label = { Text(portal.displayName, fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                OutlinedTextField(
                    value = portalUrl,
                    onValueChange = { portalUrl = it },
                    label = { Text("Portal Server URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Student ID / Username") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                if (isLoading) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Authenticating & Parsing Portal Schedule...", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Button(
                        onClick = {
                            isLoading = true
                            // Simulate secure SSL portal login and JSON/HTML schedule fetch
                            val fetchedCourses = listOf(
                                ImportedCourse(
                                    name = "Cloud Native Software Engineering",
                                    code = "CS401",
                                    classroom = "Software Bldg Lab 3",
                                    instructor = "Prof. Zhang",
                                    dayOfWeek = 2,
                                    startPeriod = 1,
                                    endPeriod = 2,
                                    startTime = "08:00",
                                    endTime = "09:35",
                                    colorHex = "#2563EB"
                                ),
                                ImportedCourse(
                                    name = "Distributed Database Architecture",
                                    code = "CS402",
                                    classroom = "Science Hall B202",
                                    instructor = "Dr. Liu",
                                    dayOfWeek = 4,
                                    startPeriod = 6,
                                    endPeriod = 8,
                                    startTime = "14:30",
                                    endTime = "17:05",
                                    colorHex = "#10B981"
                                )
                            )
                            isLoading = false
                            onImportCourses(fetchedCourses)
                            Toast.makeText(context, "Direct portal sync successful! Fetched ${fetchedCourses.size} courses.", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Connect & Fetch Schedule", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
