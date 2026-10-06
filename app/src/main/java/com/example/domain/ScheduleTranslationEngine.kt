package com.example.domain

object ScheduleTranslationEngine {
    fun cleanText(text: String, isEnglishMode: Boolean = true): String {
        if (text.isBlank()) return ""
        var cleaned = text

        // Normalize common Course & Location names
        if (cleaned.contains("计算机组成原理")) cleaned = cleaned.replace("计算机组成原理", "Computer Architecture")
        if (cleaned.contains("桌面应用程序设计")) cleaned = cleaned.replace("桌面应用程序设计", "Desktop App Design")
        if (cleaned.contains("数据分析与机器学习")) cleaned = cleaned.replace("数据分析与机器学习", "Data Analysis & ML")
        if (cleaned.contains("面向数据科学")) cleaned = cleaned.replace("面向数据科学", "Data Science Programming")
        if (cleaned.contains("神经网络")) cleaned = cleaned.replace("神经网络与深度学习", "Neural Networks & AI").replace("神经网络", "Neural Networks & AI")
        if (cleaned.contains("数据库原理")) cleaned = cleaned.replace("数据库原理及应用", "Database Systems").replace("数据库原理", "Database Systems")
        if (cleaned.contains("明理楼")) cleaned = cleaned.replace("明理楼", "Mingli Hall ")
        if (cleaned.contains("明德楼")) cleaned = cleaned.replace("明德楼", "Mingde Hall ")
        if (cleaned.contains("软件机房")) cleaned = cleaned.replace("软件机房", "Software Lab ")
        if (cleaned.contains("计算机学院")) cleaned = cleaned.replace("计算机学院", "School of Computer Science")

        if (isEnglishMode) {
            // Strip out Chinese CJK ideographs and empty leftover brackets
            cleaned = cleaned.replace(Regex("[\\u4e00-\\u9fa5]+"), "")
            cleaned = cleaned.replace("()", "").replace("（）", "").replace("  ", " ").trim()
        }
        return cleaned.ifBlank { text }
    }

    fun translateCourseName(rawName: String): String {
        return cleanText(rawName)
    }
}
