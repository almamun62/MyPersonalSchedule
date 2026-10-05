package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.WeeklySummaryEngine
import com.example.ui.theme.tr
import com.example.ui.viewmodel.ScheduleViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: ScheduleViewModel,
    onNavigateToTimetable: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onOpenAcademicCalendar: (() -> Unit)? = null
) {

    val allCourses by viewModel.filteredCourses.collectAsStateWithLifecycle()
    val allTasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val allExams by viewModel.allExams.collectAsStateWithLifecycle()

    var smartWakeEnabled by remember { mutableStateOf(true) }
    var classAlertsEnabled by remember { mutableStateOf(true) }
    var dndEnabled by remember { mutableStateOf(true) }

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var newTaskTitle by remember { mutableStateOf("") }
    var newTaskCourse by remember { mutableStateOf("") }

    val actualDate = remember { LocalDate.now() }
    val formattedToday = remember { actualDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d")) }
    val summary = remember(allCourses) { WeeklySummaryEngine.generateSummary(allCourses) }

    val pendingTasks = remember(allTasks) { allTasks.filter { !it.isCompleted } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. 2026-2027 Fall Semester Card
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF132338)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1D3554)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "2026–2027 Fall Semester",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color.White
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFA8C7FA)
                        ) {
                            Text(
                                text = "24%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0B1A2D),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "Week 5 / 20",
                        fontSize = 13.sp,
                        color = Color(0xFF8EADC9)
                    )

                    LinearProgressIndicator(
                        progress = { 0.24f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFFA8C7FA),
                        trackColor = Color(0xFF1B3554)
                    )

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF0C1929),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenAcademicCalendar?.invoke() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.BeachAccess,
                                    contentDescription = null,
                                    tint = Color(0xFFA8C7FA),
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = "National Day Holiday (Oct 1 - Oct 7)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFE2E8F0)
                                    )
                                }
                            }
                            Icon(
                                Icons.Outlined.ChevronRight,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (onOpenAcademicCalendar != null) {
                        Button(
                            onClick = { onOpenAcademicCalendar.invoke() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A5F))
                        ) {
                            Icon(
                                Icons.Outlined.CalendarMonth,
                                contentDescription = null,
                                tint = Color(0xFFA8C7FA),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Academic Calendar Vault (Photo, PDF, Word, Excel)",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }


            // Exam Countdown Card on Dashboard (Only if less than 14 days left)
            if (allExams.isNotEmpty()) {
                val now = System.currentTimeMillis()
                val nearestExam = allExams.filter { it.examDateMillis >= now - 86400000L }
                    .minByOrNull { it.examDateMillis }
                if (nearestExam != null) {
                    val diffMs = nearestExam.examDateMillis - now
                    val daysLeft = (diffMs / 86400000L).coerceAtLeast(0)

                    if (daysLeft <= 14L) {
                        Card(
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2338)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF293B5E)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToTasks() }
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Outlined.Timer, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                                        Text("Upcoming Exam Countdown", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (daysLeft <= 3L) Color(0xFFEF4444) else Color(0xFF0EA5E9)
                                    ) {
                                        Text(
                                            text = if (daysLeft == 0L) "TODAY ⚠️" else "$daysLeft DAYS LEFT",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = nearestExam.courseName,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "📍 ${nearestExam.classroom.ifBlank { "TBD" }}",
                                        fontSize = 12.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                    Text(
                                        text = "⏰ ${nearestExam.startTime} - ${nearestExam.endTime}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF38BDF8)
                                    )
                                    if (nearestExam.seatNumber.isNotBlank()) {
                                        Text(
                                            text = "🪑 ${nearestExam.seatNumber}",
                                            fontSize = 12.sp,
                                            color = Color(0xFFCBD5E1)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Study Focus Card
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131F2E)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1F3045)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFA8C7FA)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFF0B1A2D),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Study Focus",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Lock phone & manage whitelisted apps (14 allowed)",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { /* Lock action */ },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA8C7FA)),
                            modifier = Modifier.weight(1f).height(42.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF0B1A2D), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Lock Phone", color = Color(0xFF0B1A2D), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = { /* Whitelist action */ },
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334B68)),
                            modifier = Modifier.weight(1f).height(42.dp)
                        ) {
                            Icon(Icons.Default.GridView, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Whitelist Apps", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFF64748B))
                        )
                    }
                }
            }

            // 3. Section Header: Today's Schedule
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Today's Schedule",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White
                    )
                    Text(
                        text = formattedToday,
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                Text(
                    text = "Full Week Grid →",
                    color = Color(0xFFA8C7FA),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable { onNavigateToTimetable() }
                )
            }

            // 4. National Day Holiday Pause Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2836)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF283648)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2D3C4F)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Campaign,
                            contentDescription = null,
                            tint = Color(0xFFA8C7FA),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = "National Day (国庆节)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Text(
                            text = "All classes auto-paused per academic calendar.",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }

            // 5. Sleep & Wake Alarm Card
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131F2E)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1F3045)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1C314B)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Bedtime, contentDescription = null, tint = Color(0xFFA8C7FA), modifier = Modifier.size(20.dp))
                            }

                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Sleep & Wake Alarm",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.White
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF1B3654)
                                    ) {
                                        Text(
                                            text = "SMART WAKE",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFA8C7FA),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Smart class-based wake & bedtime schedule",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        Switch(
                            checked = smartWakeEnabled,
                            onCheckedChange = { smartWakeEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF0B1A2D),
                                checkedTrackColor = Color(0xFFA8C7FA)
                            )
                        )
                    }

                    // 3 info boxes (Wind-down, Wake-up, Target)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF0E1622),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.Bedtime, contentDescription = null, tint = Color(0xFFA8C7FA), modifier = Modifier.size(13.dp))
                                    Text("Wind-down", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                }
                                Text("23:00", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF0E1622),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.Alarm, contentDescription = null, tint = Color(0xFFA8C7FA), modifier = Modifier.size(13.dp))
                                    Text("Wake-up", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                }
                                Text("07:00", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF0E1622),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.Timer, contentDescription = null, tint = Color(0xFFA8C7FA), modifier = Modifier.size(13.dp))
                                    Text("Target", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                }
                                Text("8.0h", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0E1724),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFA8C7FA), modifier = Modifier.size(14.dp))
                            Text(
                                text = "Dynamic wake-up rings 60m before your first morning class.",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { /* Customize schedule */ },
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334B68)),
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Customize Schedule", fontSize = 11.5.sp, color = Color.White)
                        }

                        OutlinedButton(
                            onClick = { /* Clock app */ },
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334B68)),
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clock App", fontSize = 11.5.sp, color = Color.White)
                        }
                    }
                }
            }

            // 6. Lecture Alerts & Focus Card
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131F2E)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1F3045)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1C314B)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Notifications, contentDescription = null, tint = Color(0xFFA8C7FA), modifier = Modifier.size(20.dp))
                            }

                            Column {
                                Text(
                                    text = "Lecture Alerts & Focus",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "15-min alerts & automatic in-class silence",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        IconButton(onClick = { /* menu */ }) {
                            Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color(0xFF94A3B8))
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("15-Minute Class Alerts", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White)
                            Text("3 alarms active", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        }
                        Switch(
                            checked = classAlertsEnabled,
                            onCheckedChange = { classAlertsEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF0B1A2D),
                                checkedTrackColor = Color(0xFFA8C7FA)
                            )
                        )
                    }

                    OutlinedButton(
                        onClick = { /* Customize Alert Times */ },
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334B68)),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Customize Alert Times (Global & Per-Course)", fontSize = 12.sp, color = Color.White)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("In-Class Silence Mode (DND)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White)
                            Text("Auto mutes device when lecture starts", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        }
                        Switch(
                            checked = dndEnabled,
                            onCheckedChange = { dndEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF0B1A2D),
                                checkedTrackColor = Color(0xFFA8C7FA)
                            )
                        )
                    }
                }
            }

            // 7. Academic Workload & Credits Card
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131F2E)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1F3045)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1C314B)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.School, contentDescription = null, tint = Color(0xFFA8C7FA), modifier = Modifier.size(20.dp))
                            }

                            Column {
                                Text(
                                    text = "Academic Workload & Credits",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Semester study hours & credit load",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        Text(
                            text = "Credits",
                            color = Color(0xFFA8C7FA),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF0E1622),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFFA8C7FA), modifier = Modifier.size(13.dp))
                                    Text("Weekly", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                }
                                Text("19.3h", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.White)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF0E1622),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFFA8C7FA), modifier = Modifier.size(13.dp))
                                    Text("Today", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                }
                                Text("0.0h", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.White)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF0E1622),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFA8C7FA), modifier = Modifier.size(13.dp))
                                    Text("Credits", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                }
                                Text("18.0", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.White)
                            }
                        }
                    }
                }
            }

            // 8. Pending Tasks Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Pending Tasks",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White
                    )
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE53935)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${pendingTasks.size.coerceAtLeast(2)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { showAddTaskDialog = true }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFFA8C7FA), modifier = Modifier.size(16.dp))
                    Text(
                        text = "Add Task",
                        color = Color(0xFFA8C7FA),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            // Horizontal scrolling row of task cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF131F2E),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1F3045)),
                    modifier = Modifier.width(280.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Checkbox(
                            checked = false,
                            onCheckedChange = { },
                            colors = CheckboxDefaults.colors(uncheckedColor = Color(0xFF94A3B8))
                        )
                        Column {
                            Text(
                                text = "Finish Linux Kernel Module lab report",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = Color.White
                            )
                            Text(
                                text = "Advanced Operating Systems",
                                fontSize = 11.sp,
                                color = Color(0xFFA8C7FA)
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF131F2E),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1F3045)),
                    modifier = Modifier.width(280.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Checkbox(
                            checked = false,
                            onCheckedChange = { },
                            colors = CheckboxDefaults.colors(uncheckedColor = Color(0xFF94A3B8))
                        )
                        Column {
                            Text(
                                text = "Submit B+ Tree index assignment",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = Color.White
                            )
                            Text(
                                text = "Database Systems & Architecture",
                                fontSize = 11.sp,
                                color = Color(0xFFA8C7FA)
                            )
                        }
                    }
                }
            }

            // 9. Tomorrow's Preview Card
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131F2E)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1F3045)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tomorrow's Preview",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Details",
                            color = Color(0xFFA8C7FA),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.Campaign,
                            contentDescription = null,
                            tint = Color(0xFFA8C7FA),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "National Day (国庆节). Classes are paused.",
                            fontSize = 13.sp,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }
            }

            // 10. Weekly Analysis & Insights Card
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131F2E)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1F3045)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFA8C7FA), modifier = Modifier.size(18.dp))
                        Text(
                            text = "Weekly Analysis & Insights",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Enrolled Classes", fontSize = 12.sp, color = Color(0xFF94A3B8))
                            Text("${allCourses.size.coerceAtLeast(15)}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                        }

                        Column {
                            Text("Today's Lectures", fontSize = 12.sp, color = Color(0xFF94A3B8))
                            Text("0", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                        }

                        Column {
                            Text("Pending Tasks", fontSize = 12.sp, color = Color(0xFF94A3B8))
                            Text("${pendingTasks.size.coerceAtLeast(2)}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    if (showAddTaskDialog) {
        AlertDialog(
            onDismissRequest = { showAddTaskDialog = false },
            title = { Text("Add Task".tr, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newTaskTitle,
                        onValueChange = { newTaskTitle = it },
                        label = { Text("Task Title".tr) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newTaskCourse,
                        onValueChange = { newTaskCourse = it },
                        label = { Text("Course Name".tr) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTaskTitle.isNotBlank()) {
                            viewModel.addTask(newTaskTitle.trim(), newTaskCourse.trim())
                            newTaskTitle = ""
                            newTaskCourse = ""
                            showAddTaskDialog = false
                        }
                    }
                ) {
                    Text("Save".tr)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTaskDialog = false }) {
                    Text("Cancel".tr)
                }
            }
        )
    }
}
