import re

with open('app/src/main/java/com/example/ui/screens/TimetableScreen.kt', 'r') as f:
    content = f.read()

# We need to replace the grid block.
# Let's find the start string.
start_str = "            // 3. Timetable Grid: 7 Day Columns + 12 Period Rows"
# The end of the TimetableScreen function is before computeCourseLanes
# Let's split content
parts = content.split(start_str)

if len(parts) == 2:
    prefix = parts[0]
    suffix_part = parts[1]
    
    # find the end of the TimetableScreen composable block which precedes computeCourseLanes
    # It ends with:
    #                 }
    #             }
    #         }
    #     }
    # }
    # /**
    #  * Computes side-by-side lanes
    end_marker = "/**\n * Computes side-by-side lanes"
    suffix_parts = suffix_part.split(end_marker)
    
    if len(suffix_parts) == 2:
        new_layout = """            // 3. HyperOS Card-Based Timetable Layout
            var selectedDay by remember { mutableStateOf(LocalDate.now().dayOfWeek.value) }
            val currentDay = LocalDate.now().dayOfWeek.value

            // Day Selector
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(7) { index ->
                    val dayNum = index + 1
                    val dayName = daysOfWeek[index]
                    val isSelected = selectedDay == dayNum
                    val isToday = currentDay == dayNum

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = when {
                            isSelected -> MaterialTheme.colorScheme.primary
                            isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        },
                        contentColor = when {
                            isSelected -> MaterialTheme.colorScheme.onPrimary
                            isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        shadowElevation = if (isSelected) 4.dp else 0.dp,
                        border = BorderStroke(
                            0.5.dp, 
                            if (isSelected) Color(0x44FFFFFF) else Color(0x11FFFFFF)
                        ),
                        modifier = Modifier
                            .clickable { selectedDay = dayNum }
                            .widthIn(min = 60.dp)
                            .height(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = dayName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Cards for the selected day
            val dayCourses = activeCoursesInWeek.filter { it.dayOfWeek == selectedDay }.sortedBy { it.startPeriod }

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(Color(0x33FFFFFF), Color(0x11FFFFFF), Color.Transparent)
                        ),
                        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                    ),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                tonalElevation = 0.dp
            ) {
                if (dayCourses.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Coffee,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp).padding(bottom = 12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "No classes today".tr,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "Take a break and relax!".tr,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(dayCourses) { course ->
                            val isConflicted = activeWeekConflictedCourseNames.contains(course.name)
                            
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isConflicted) Color(0xFFFF9500).copy(alpha = 0.5f)
                                    else Brush.linearGradient(
                                        listOf(Color(0x55FFFFFF), Color(0x11FFFFFF))
                                    )
                                ),
                                shadowElevation = 8.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedCourseForDetails = course }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Time Indicator Column
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.width(60.dp)
                                    ) {
                                        Text(
                                            text = "P${course.startPeriod}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Box(
                                            modifier = Modifier
                                                .padding(vertical = 4.dp)
                                                .width(2.dp)
                                                .height(24.dp)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape)
                                        )
                                        Text(
                                            text = "P${course.endPeriod}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    // Content Column
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (isConflicted) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier
                                                    .background(Color(0xFFFF9500).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.WarningAmber,
                                                    contentDescription = null,
                                                    tint = Color(0xFFFF9500),
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = "Conflict".tr,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFFF9500)
                                                )
                                            }
                                        }

                                        Text(
                                            text = course.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            if (course.classroom.isNotBlank()) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Icon(
                                                        imageVector = Icons.Default.LocationOn,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Text(
                                                        text = course.classroom,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            if (course.instructor.isNotBlank()) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Icon(
                                                        imageVector = Icons.Default.Person,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Text(
                                                        text = course.instructor,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }

                                        // Start/End Time Label
                                        if (course.startTime.isNotBlank() && course.endTime.isNotBlank()) {
                                            Text(
                                                text = "${course.startTime} - ${course.endTime}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                    
                                    // Accent color bar
                                    Box(
                                        modifier = Modifier
                                            .width(6.dp)
                                            .height(48.dp)
                                            .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                                            .background(Color(course.colorHex))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
\n""" + end_marker + suffix_parts[1]
        
        with open('app/src/main/java/com/example/ui/screens/TimetableScreen.kt', 'w') as f:
            f.write(prefix + new_layout)
        print("Success")
    else:
        print("Failed to split suffix part by end_marker")
else:
    print("Failed to split by start_str")

