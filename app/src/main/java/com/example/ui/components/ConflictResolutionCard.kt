package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CourseEntity
import com.example.domain.CourseConflict
import com.example.ui.theme.tr

/**
 * Conflict Resolution Card: Appears prominently in the UI when two classes overlap.
 * Allows the student to select which course to prioritize for the current session (this week only)
 * or set a permanent preference for that specific time block (across all semester weeks).
 */
@Composable
fun ConflictResolutionCard(
    conflict: CourseConflict,
    currentWeek: Int,
    sessionPriorities: Map<String, Long>,
    timeBlockPreferences: Map<String, Long>,
    preferredConflictCourseIds: Set<Long>,
    hideAudited: Boolean,
    modifier: Modifier = Modifier,
    conflictIndex: Int = 0,
    totalConflicts: Int = 1,
    onPreviousConflict: (() -> Unit)? = null,
    onNextConflict: (() -> Unit)? = null,
    onPrioritizeSession: (conflict: CourseConflict, prioritizedCourse: CourseEntity, otherCourse: CourseEntity) -> Unit,
    onSetPermanentPreference: (conflict: CourseConflict, prioritizedCourse: CourseEntity, otherCourse: CourseEntity) -> Unit,
    onClearPreference: (conflict: CourseConflict) -> Unit,
    onToggleHideAudited: (Boolean) -> Unit,
    onOpenAdvancedOptions: ((conflict: CourseConflict) -> Unit)? = null,
    onDismiss: (() -> Unit)? = null
) {
    val isDark = isSystemInDarkTheme()

    // Determine current resolution status
    val sessionKey = "w${currentWeek}_d${conflict.dayOfWeek}_p${conflict.overlapStartPeriod}"
    val sessionCourseId = sessionPriorities[sessionKey]

    val blockKey = "d${conflict.dayOfWeek}_p${conflict.overlapStartPeriod}"
    val blockCourseId = timeBlockPreferences[blockKey]

    val isC1SessionPrioritized = sessionCourseId == conflict.course1.id
    val isC2SessionPrioritized = sessionCourseId == conflict.course2.id

    val isC1BlockPermanent = blockCourseId == conflict.course1.id ||
            (sessionCourseId == null && blockCourseId == null && preferredConflictCourseIds.contains(conflict.course1.id) && !preferredConflictCourseIds.contains(conflict.course2.id))
    val isC2BlockPermanent = blockCourseId == conflict.course2.id ||
            (sessionCourseId == null && blockCourseId == null && preferredConflictCourseIds.contains(conflict.course2.id) && !preferredConflictCourseIds.contains(conflict.course1.id))

    val isC1Preferred = isC1SessionPrioritized || (sessionCourseId == null && isC1BlockPermanent)
    val isC2Preferred = isC2SessionPrioritized || (sessionCourseId == null && isC2BlockPermanent)
    val isResolved = isC1Preferred || isC2Preferred

    // Card background gradient
    val backgroundBrush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF261C14),
                Color(0xFF1C1612)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFFBF6),
                Color(0xFFFFF4E8)
            )
        )
    }

    val cardBorderColor = if (isResolved) {
        Color(0xFFEAB308).copy(alpha = 0.6f)
    } else {
        Color(0xFFFF9500).copy(alpha = 0.8f)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("conflict_resolution_card"),
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, cardBorderColor),
        shadowElevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .background(backgroundBrush)
                .padding(14.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Header Bar: Title, Time Block, Multi-Conflict Controls, and Dismiss
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFF9500).copy(alpha = 0.2f),
                            border = BorderStroke(0.5.dp, Color(0xFFFF9500))
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = "Conflict".tr,
                                tint = Color(0xFFFF9500),
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(16.dp)
                            )
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Conflict Resolution".tr,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (isDark) Color(0xFFFFCC80) else Color(0xFFC05621)
                                )

                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFFF9500).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "Week $currentWeek".tr,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = Color(0xFFFF9500)
                                    )
                                }
                            }

                            Text(
                                text = "${conflict.dayName} • ${conflict.overlapStartTime} - ${conflict.overlapEndTime} (Sec ${conflict.overlapStartPeriod}-${conflict.overlapEndPeriod})",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Multi-conflict navigator (if more than 1 conflict in this week)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (totalConflicts > 1) {
                            IconButton(
                                onClick = { onPreviousConflict?.invoke() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Previous Conflict".tr,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "${conflictIndex + 1}/$totalConflicts",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            IconButton(
                                onClick = { onNextConflict?.invoke() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Next Conflict".tr,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (onDismiss != null) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("dismiss_conflict_card_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss".tr,
                                    modifier = Modifier.size(15.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 2. Active Resolution Alert Strip (if chosen)
                if (isResolved) {
                    val preferredCourse = if (isC1Preferred) conflict.course1 else conflict.course2
                    val isSession = if (isC1Preferred) isC1SessionPrioritized else isC2SessionPrioritized

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSession) Color(0x203B82F6) else Color(0x20EAB308),
                        border = BorderStroke(0.5.dp, if (isSession) Color(0xFF3B82F6) else Color(0xFFEAB308)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (isSession) Icons.Default.Today else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (isSession) Color(0xFF3B82F6) else Color(0xFFEAB308),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (isSession) {
                                        "Session Priority: Attending ${preferredCourse.name} (Week $currentWeek)".tr
                                    } else {
                                        "Permanent Preference: Always attending ${preferredCourse.name}".tr
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSession) Color(0xFF3B82F6) else Color(0xFFEAB308),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            TextButton(
                                onClick = { onClearPreference(conflict) },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                modifier = Modifier
                                    .height(26.dp)
                                    .testTag("reset_conflict_preference_btn")
                            ) {
                                Text(
                                    text = "Reset".tr,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    // Helper guidance prompt
                    Text(
                        text = "Choose which course you plan to attend for this session or lock in as your permanent preference:".tr,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 3. Two Overlapping Courses Comparison Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Course 1 Card
                    Box(modifier = Modifier.weight(1f)) {
                        CourseResolutionItem(
                            course = conflict.course1,
                            otherCourse = conflict.course2,
                            conflict = conflict,
                            currentWeek = currentWeek,
                            isSessionPrioritized = isC1SessionPrioritized,
                            isPermanentPreference = isC1BlockPermanent,
                            isOtherPreferred = isC2Preferred,
                            onPrioritizeSession = {
                                onPrioritizeSession(conflict, conflict.course1, conflict.course2)
                            },
                            onSetPermanentPreference = {
                                onSetPermanentPreference(conflict, conflict.course1, conflict.course2)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Course 2 Card
                    Box(modifier = Modifier.weight(1f)) {
                        CourseResolutionItem(
                            course = conflict.course2,
                            otherCourse = conflict.course1,
                            conflict = conflict,
                            currentWeek = currentWeek,
                            isSessionPrioritized = isC2SessionPrioritized,
                            isPermanentPreference = isC2BlockPermanent,
                            isOtherPreferred = isC1Preferred,
                            onPrioritizeSession = {
                                onPrioritizeSession(conflict, conflict.course2, conflict.course1)
                            },
                            onSetPermanentPreference = {
                                onSetPermanentPreference(conflict, conflict.course2, conflict.course1)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // 4. Bottom Toolbar: Hide Audited Toggle + Advanced Options Link
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Toggle Hide Audited In Grid
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onToggleHideAudited(!hideAudited) }
                            .padding(vertical = 2.dp, horizontal = 4.dp)
                            .testTag("hide_audited_toggle")
                    ) {
                        Checkbox(
                            checked = hideAudited,
                            onCheckedChange = { onToggleHideAudited(it) },
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "Hide Audited in Grid".tr,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Advanced Dialog button (Odd/Even alternating, audit move, etc.)
                    if (onOpenAdvancedOptions != null) {
                        TextButton(
                            onClick = { onOpenAdvancedOptions(conflict) },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(
                                text = "More Options...".tr,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Single Course Item inside the Conflict Resolution Card with distinct one-tap actions
 * for session-level priority and permanent time-block preference.
 */
@Composable
private fun CourseResolutionItem(
    course: CourseEntity,
    otherCourse: CourseEntity,
    conflict: CourseConflict,
    currentWeek: Int,
    isSessionPrioritized: Boolean,
    isPermanentPreference: Boolean,
    isOtherPreferred: Boolean,
    onPrioritizeSession: () -> Unit,
    onSetPermanentPreference: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val courseColor = Color(course.colorHex)

    val isAttending = isSessionPrioritized || isPermanentPreference
    val isAuditing = isOtherPreferred && !isAttending

    val itemBorder = when {
        isPermanentPreference -> BorderStroke(1.5.dp, Color(0xFFEAB308))
        isSessionPrioritized -> BorderStroke(1.5.dp, Color(0xFF3B82F6))
        isAuditing -> BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        else -> BorderStroke(0.5.dp, courseColor.copy(alpha = 0.5f))
    }

    val itemBg = when {
        isPermanentPreference -> if (isDark) Color(0x35EAB308) else Color(0x25FEF08A)
        isSessionPrioritized -> if (isDark) Color(0x353B82F6) else Color(0x20BFDBFE)
        isAuditing -> if (isDark) Color(0x201E1E1E) else Color(0x20E5E7EB)
        else -> if (isDark) Color(0x401E1A16) else Color(0xFFFFFFFF)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = itemBg,
        border = itemBorder
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Course Header with colored stripe & status pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 4.dp, height = 14.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(courseColor)
                    )

                    Text(
                        text = course.name,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isAuditing) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                    )
                }

                // Status Badge
                when {
                    isPermanentPreference -> {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFEAB308),
                            modifier = Modifier.padding(start = 2.dp)
                        ) {
                            Text(
                                text = "⭐ Permanent".tr,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.Black
                            )
                        }
                    }
                    isSessionPrioritized -> {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF3B82F6),
                            modifier = Modifier.padding(start = 2.dp)
                        ) {
                            Text(
                                text = "⚡ Week $currentWeek".tr,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White
                            )
                        }
                    }
                    isAuditing -> {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(start = 2.dp)
                        ) {
                            Text(
                                text = "Auditing".tr,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Normal
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Location & Instructor details
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (course.classroom.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            modifier = Modifier.size(11.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Text(
                            text = course.classroom,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (course.instructor.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(11.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Text(
                            text = course.instructor,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Action 1: Prioritize Current Session
            Button(
                onClick = onPrioritizeSession,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .testTag("prioritize_session_${course.id}"),
                colors = if (isSessionPrioritized) {
                    ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                } else {
                    ButtonDefaults.filledTonalButtonColors()
                },
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Today,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isSessionPrioritized) "Attending Session ✓".tr else "Prioritize Session".tr,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
            }

            // Action 2: Set Permanent Preference for this time block
            OutlinedButton(
                onClick = onSetPermanentPreference,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .testTag("permanent_preference_${course.id}"),
                colors = if (isPermanentPreference) {
                    ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFCA8A04)
                    )
                } else {
                    ButtonDefaults.outlinedButtonColors()
                },
                border = BorderStroke(
                    1.dp,
                    if (isPermanentPreference) Color(0xFFEAB308) else MaterialTheme.colorScheme.outlineVariant
                ),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isPermanentPreference) "Permanent Choice ✓".tr else "Permanent Block".tr,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1
                )
            }
        }
    }
}
