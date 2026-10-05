package com.example.ui.theme

import androidx.compose.ui.graphics.Color

val BluePrimary = Color(0xFF2563EB)
val BlueSecondary = Color(0xFF3B82F6)
val BlueTertiary = Color(0xFF60A5FA)

val EmeraldPrimary = Color(0xFF059669)
val AmberPrimary = Color(0xFFD97706)
val PurplePrimary = Color(0xFF7C3AED)
val RosePrimary = Color(0xFFE11D48)

enum class AppThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

enum class AppAccentColor(val id: String, val displayName: String, val primary: Color) {
    BLUE("blue", "Royal Blue", BluePrimary),
    EMERALD("emerald", "Emerald Green", EmeraldPrimary),
    AMBER("amber", "Amber Orange", AmberPrimary),
    PURPLE("purple", "Deep Purple", PurplePrimary),
    ROSE("rose", "Rose Pink", RosePrimary)
}
