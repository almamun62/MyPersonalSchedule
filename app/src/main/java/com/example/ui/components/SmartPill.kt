package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import com.example.ui.theme.tr
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.max

/**
 * A Dynamic Island style punch-hole notification bar, inspired by Xiaomi System / Apple Dynamic Island.
 */
@Composable
fun SmartPill(
    isDndActive: Boolean,
    currentWeek: Int,
    modifier: Modifier = Modifier,
    ongoingCourseName: String? = null
) {
    // Get status bar and display cutout insets to position the island correctly
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val cutoutPadding = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
    val topOffset = maxOf(statusBarPadding, cutoutPadding, 12.dp)

    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .padding(top = topOffset)
            .clip(CircleShape)
            .background(Color.Black)
            .clickable { expanded = !expanded }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = expanded to isDndActive,
            transitionSpec = {
                fadeIn(animationSpec = tween(200)) togetherWith
                        fadeOut(animationSpec = tween(200)) using
                        SizeTransform { initialSize, targetSize ->
                            spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        }
            },
            label = "smart_pill_anim"
        ) { (isExp, isDnd) ->
            if (isExp) {
                // Expanded State
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isDnd) {
                        Icon(
                            imageVector = Icons.Default.NotificationsOff,
                            contentDescription = "DND".tr,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(4.dp)
                        )
                        Text(
                            text = "Do Not Disturb is Active".tr,
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (ongoingCourseName != null) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Class".tr,
                            tint = Color(0xFF34C759),
                            modifier = Modifier.padding(4.dp)
                        )
                        Text(
                            text = "Class in Progress: $ongoingCourseName".tr,
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Week".tr,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(4.dp)
                        )
                        Text(
                            text = "Academic Week $currentWeek",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                // Collapsed State (fits around camera punch hole)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isDnd) {
                        Icon(
                            imageVector = Icons.Default.NotificationsOff,
                            contentDescription = "DND".tr,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(2.dp)
                        )
                    }
                    Text(
                        text = if (isDnd) "DND" else if (ongoingCourseName != null) "In Class" else "Week $currentWeek",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
