package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BgDark = Color(0xFF0B0F17)
val SurfaceDark = Color(0xFF131B26)
val SurfaceVariantDark = Color(0xFF182230)
val CardBorderDark = Color(0xFF1F2D3F)
val BlueAccent = Color(0xFFA8C7FA)
val OnBlueAccent = Color(0xFF0B1A2D)
val BlueContainer = Color(0xFF162942)
val OnBlueContainer = Color(0xFFD3E3FD)
val SecondaryContainerDark = Color(0xFF1C2E46)
val PurpleBreak = Color(0xFF2C1E38)
val PurpleBreakBorder = Color(0xFF3F2652)
val PurpleBreakText = Color(0xFFE4C7FA)
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFF94A3B8)

@Composable
fun CourseScheduleTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    accentColor: AppAccentColor = AppAccentColor.BLUE,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.SYSTEM -> true
    }

    val darkColorScheme = darkColorScheme(
        primary = BlueAccent,
        onPrimary = OnBlueAccent,
        secondary = BlueAccent.copy(alpha = 0.8f),
        tertiary = PurpleBreakText,
        background = BgDark,
        surface = SurfaceDark,
        surfaceVariant = SurfaceVariantDark,
        primaryContainer = BlueContainer,
        onPrimaryContainer = OnBlueContainer,
        secondaryContainer = SecondaryContainerDark,
        onSecondaryContainer = TextPrimary,
        outline = CardBorderDark,
        outlineVariant = Color(0xFF182536),
        onBackground = TextPrimary,
        onSurface = TextPrimary,
        onSurfaceVariant = TextSecondary
    )

    val lightColorScheme = lightColorScheme(
        primary = Color(0xFF2563EB),
        onPrimary = Color.White,
        secondary = Color(0xFF3B82F6),
        tertiary = Color(0xFF7C3AED),
        background = Color(0xFFF8FAFC),
        surface = Color.White,
        surfaceVariant = Color(0xFFF1F5F9),
        primaryContainer = Color(0xFFDBEAFE),
        onPrimaryContainer = Color(0xFF1E40AF),
        secondaryContainer = Color(0xFFE2E8F0),
        onSecondaryContainer = Color(0xFF1E293B),
        outline = Color(0xFFCBD5E1),
        outlineVariant = Color(0xFFE2E8F0),
        onBackground = Color(0xFF0F172A),
        onSurface = Color(0xFF0F172A),
        onSurfaceVariant = Color(0xFF64748B)
    )

    MaterialTheme(
        colorScheme = if (darkTheme) darkColorScheme else lightColorScheme,
        typography = Typography,
        content = content
    )
}
