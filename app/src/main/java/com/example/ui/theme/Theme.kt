package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BgDark = Color(0xFF0B0F17)
val SurfaceDark = Color(0xFF131B26)
val SurfaceVariantDark = Color(0xFF182230)
val CardBorderDark = Color(0xFF1F2D3F)
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFF94A3B8)

@Composable
fun CourseScheduleTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    accentColor: AppAccentColor = AppAccentColor.BLUE,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()
    val isDarkTheme = when (themeMode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.SYSTEM -> systemInDark
    }

    // Base accent primary color from selected AppAccentColor
    val primaryColor = accentColor.primary

    // High contrast primary variant for dark theme
    val primaryDark = when (accentColor) {
        AppAccentColor.BLUE -> Color(0xFFA8C7FA)
        AppAccentColor.EMERALD -> Color(0xFF6EE7B7)
        AppAccentColor.AMBER -> Color(0xFFFCD34D)
        AppAccentColor.PURPLE -> Color(0xFFC084FC)
        AppAccentColor.ROSE -> Color(0xFFFDA4AF)
    }

    val activePrimary = if (isDarkTheme) primaryDark else primaryColor

    val darkColorScheme = darkColorScheme(
        primary = activePrimary,
        onPrimary = Color(0xFF0B1A2D),
        secondary = activePrimary.copy(alpha = 0.85f),
        onSecondary = Color.White,
        tertiary = activePrimary.copy(alpha = 0.7f),
        background = BgDark,
        surface = SurfaceDark,
        surfaceVariant = SurfaceVariantDark,
        primaryContainer = activePrimary.copy(alpha = 0.2f),
        onPrimaryContainer = activePrimary,
        secondaryContainer = Color(0xFF1C2E46),
        onSecondaryContainer = TextPrimary,
        outline = CardBorderDark,
        outlineVariant = Color(0xFF182536),
        onBackground = TextPrimary,
        onSurface = TextPrimary,
        onSurfaceVariant = TextSecondary
    )

    val lightColorScheme = lightColorScheme(
        primary = activePrimary,
        onPrimary = Color.White,
        secondary = activePrimary.copy(alpha = 0.85f),
        onSecondary = Color.White,
        tertiary = activePrimary.copy(alpha = 0.7f),
        background = Color(0xFFF8FAFC),
        surface = Color.White,
        surfaceVariant = Color(0xFFF1F5F9),
        primaryContainer = activePrimary.copy(alpha = 0.15f),
        onPrimaryContainer = activePrimary,
        secondaryContainer = Color(0xFFE2E8F0),
        onSecondaryContainer = Color(0xFF1E293B),
        outline = Color(0xFFCBD5E1),
        outlineVariant = Color(0xFFE2E8F0),
        onBackground = Color(0xFF0F172A),
        onSurface = Color(0xFF0F172A),
        onSurfaceVariant = Color(0xFF64748B)
    )

    MaterialTheme(
        colorScheme = if (isDarkTheme) darkColorScheme else lightColorScheme,
        typography = Typography,
        content = content
    )
}
