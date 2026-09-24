package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.tr
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalAccentColor

/**
 * System Glassmorphic Card featuring dynamic soft-glow borders that adapt
 * in real-time to the user's selected primary accent color (Pink, Blue, Green, etc.).
 */
@Composable
fun AdaptiveGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    accentColorOverride: Color? = null,
    borderWidth: Dp = 1.2.dp,
    glowRadius: Dp = 16.dp,
    glowAlpha: Float = 0.28f,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val currentAccent = LocalAccentColor.current
    val accent = accentColorOverride ?: currentAccent.primary
    val isDark = isSystemInDarkTheme()

    // Smooth animated transitions when user toggles accent color
    val animatedAccent by animateColorAsState(
        targetValue = accent,
        animationSpec = tween(durationMillis = 350),
        label = "AccentGlowAnim"
    )

    val surfaceColor = if (isDark) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
    }

    val glowBrush = Brush.linearGradient(
        listOf(
            animatedAccent.copy(alpha = if (isDark) 0.65f else 0.50f),
            animatedAccent.copy(alpha = if (isDark) 0.22f else 0.15f),
            if (isDark) Color(0x1AFFFFFF) else Color(0x66FFFFFF)
        )
    )

    Surface(
        shape = shape,
        color = surfaceColor,
        border = BorderStroke(borderWidth, glowBrush),
        tonalElevation = if (isDark) 4.dp else 2.dp,
        shadowElevation = 0.dp,
        modifier = modifier
            .drawBehind {
                // Ambient soft-glow halo under the border
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            animatedAccent.copy(alpha = glowAlpha),
                            Color.Transparent
                        ),
                        radius = size.maxDimension * 0.85f
                    )
                )
            }
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

/**
 * Preview / sample glassmorphism lecture card for Settings & Onboarding
 */
@Composable
fun AdaptiveGlassPreviewCard(
    modifier: Modifier = Modifier,
    accentOverride: Color? = null,
    courseName: String = "CS302 Algorithm Design",
    classroom: String = "Building 3, Rm 204",
    timePeriod: String = "Mon 08:00 - 09:35 (Periods 1-2)",
    lecturer: String = "Prof. Zhang",
    isLive: Boolean = true
) {
    AdaptiveGlassCard(
        modifier = modifier,
        accentColorOverride = accentOverride,
        glowAlpha = 0.35f
    ) {
        Box(modifier = Modifier.padding(bottom = 6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = (accentOverride ?: LocalAccentColor.current.primary).copy(alpha = 0.18f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isLive) {
                            Surface(
                                shape = CircleShape,
                                color = accentOverride ?: LocalAccentColor.current.primary,
                                modifier = Modifier.padding(end = 2.dp)
                            ) {
                                Box(modifier = Modifier.padding(3.dp))
                            }
                        }
                        Text(
                            text = "Soft-Glow System Glass".tr,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentOverride ?: LocalAccentColor.current.primary
                        )
                    }
                }

                Text(
                    text = "Preview".tr,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = courseName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "$timePeriod • $lecturer",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.padding(top = 8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = accentOverride ?: LocalAccentColor.current.primary,
                modifier = Modifier.padding(end = 2.dp)
            )
            Text(
                text = classroom,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
