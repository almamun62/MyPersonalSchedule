package com.example.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Supported Theme display modes: Day Mode (Light), Eye Care Mode (Warm Sepia), Night Mode (Dark OLED), or System.
 */
enum class AppThemeMode(val title: String, val subtitle: String) {
    DAY("Day Mode", "Crisp high-contrast light theme"),
    EYE_CARE("Eye Care Mode", "Warm sepia & amber tones to reduce eye strain"),
    NIGHT("Night Mode", "OLED deep black System dark theme"),
    SYSTEM("System Default", "Follows Android OS setting")
}

/**
 * Primary accent color definition for dynamic System soft-glow borders and cards.
 */
data class AppAccentColor(
    val id: String,
    val name: String,
    val tag: String,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val glowColor: Color,
    val softGlowGradient: List<Color>
)

object AppAccents {
    // 1. System Sakura / Blossom Pink Accent
    val PINK = AppAccentColor(
        id = "pink",
        name = "Pink Accent",
        tag = "🌸 Blossom",
        primary = Color(0xFFFF2D55),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFE4EC),
        onPrimaryContainer = Color(0xFF6B001D),
        glowColor = Color(0xFFFF2D75),
        softGlowGradient = listOf(Color(0xFFFF2D75), Color(0xFFFF8DA1), Color(0xFFFFE4EC))
    )

    // 2. System Classic Vivid Blue
    val BLUE = AppAccentColor(
        id = "blue",
        name = "System Blue",
        tag = "⚡ Vivid",
        primary = Color(0xFF337DFF),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFDCE8FF),
        onPrimaryContainer = Color(0xFF002980),
        glowColor = Color(0xFF337DFF),
        softGlowGradient = listOf(Color(0xFF337DFF), Color(0xFF60A5FA), Color(0xFFDBEAFE))
    )

    // 3. System Mint / Emerald Green
    val GREEN = AppAccentColor(
        id = "green",
        name = "Emerald Green",
        tag = "🍃 Fresh",
        primary = Color(0xFF10B981),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFD1FAE5),
        onPrimaryContainer = Color(0xFF064E3B),
        glowColor = Color(0xFF10B981),
        softGlowGradient = listOf(Color(0xFF10B981), Color(0xFF34D399), Color(0xFFD1FAE5))
    )

    // 4. System Nebula Violet / Purple
    val PURPLE = AppAccentColor(
        id = "purple",
        name = "Violet Purple",
        tag = "🔮 Nebula",
        primary = Color(0xFF8B5CF6),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFEDE9FE),
        onPrimaryContainer = Color(0xFF3B0764),
        glowColor = Color(0xFF8B5CF6),
        softGlowGradient = listOf(Color(0xFF8B5CF6), Color(0xFFA78BFA), Color(0xFFEDE9FE))
    )

    // 5. System Sunrise Amber / Orange
    val ORANGE = AppAccentColor(
        id = "orange",
        name = "Sunrise Orange",
        tag = "🌅 Amber",
        primary = Color(0xFFFF9500),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFEED9),
        onPrimaryContainer = Color(0xFF6E3600),
        glowColor = Color(0xFFFF9500),
        softGlowGradient = listOf(Color(0xFFFF9500), Color(0xFFFBBF24), Color(0xFFFFEED9))
    )

    val ALL = listOf(PINK, BLUE, GREEN, PURPLE, ORANGE)

    fun fromId(id: String?): AppAccentColor {
        return ALL.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: PINK
    }
}

val LocalAccentColor = staticCompositionLocalOf { AppAccents.PINK }
val LocalThemeMode = staticCompositionLocalOf { AppThemeMode.SYSTEM }
