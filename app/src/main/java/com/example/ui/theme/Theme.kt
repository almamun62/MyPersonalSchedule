package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF60A5FA),
    onPrimary = Color(0xFF0F172A),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFF38BDF8),
    onSecondary = Color(0xFF0F172A),
    background = SurfaceDark,
    surface = SurfaceCardDark,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    outline = SurfaceBorderDark
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E40AF),
    secondary = Color(0xFF0284C7),
    onSecondary = Color.White,
    background = SurfaceLight,
    surface = SurfaceCardLight,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    outline = SurfaceBorderLight
)

private val EyeCareColorScheme = lightColorScheme(
    primary = Color(0xFFB45309), // Warm amber
    onPrimary = Color(0xFFFEF3C7),
    primaryContainer = Color(0xFFFDE68A),
    onPrimaryContainer = Color(0xFF78350F),
    secondary = Color(0xFF92400E),
    onSecondary = Color(0xFFFEF3C7),
    background = Color(0xFFFBF7EE), // Warm cream paper background
    surface = Color(0xFFF5EFE6), // Warm sepia card surface
    onBackground = Color(0xFF43302B),
    onSurface = Color(0xFF43302B),
    outline = Color(0xFFD4C5B9)
)

@Composable
fun CourseScheduleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    accentColor: AppAccentColor = AppAccents.BLUE,
    darkTheme: Boolean = when (themeMode) {
        AppThemeMode.DAY -> false
        AppThemeMode.EYE_CARE -> false
        AppThemeMode.NIGHT -> true
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    },
    dynamicColor: Boolean = true, // Enabled by default for OS-wise Material You dynamic wallpaper colors on Android 12+
    content: @Composable () -> Unit
) {
    val baseColorScheme = when {
        themeMode == AppThemeMode.EYE_CARE -> EyeCareColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val colorScheme = if (themeMode == AppThemeMode.EYE_CARE || (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)) {
        baseColorScheme
    } else {
        baseColorScheme.copy(
            primary = accentColor.primary,
            onPrimary = accentColor.onPrimary,
            primaryContainer = accentColor.primaryContainer,
            onPrimaryContainer = accentColor.onPrimaryContainer
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) = CourseScheduleTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
