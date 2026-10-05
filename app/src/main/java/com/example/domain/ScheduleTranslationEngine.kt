package com.example.domain

import com.example.domain.model.Course

object ScheduleTranslationEngine {
    fun translateCourseName(rawName: String): String {
        return when {
            rawName.contains("桌面应用程序设计") -> "Desktop Application Design"
            rawName.contains("计算机组成原理") -> "Computer Architecture"
            rawName.contains("数据分析与机器学习") -> "Data Analysis & Machine Learning"
            rawName.contains("面向数据科学") -> "Programming for Data Science"
            rawName.contains("神经网络") -> "Neural Networks & Deep Learning"
            rawName.contains("数据库原理") -> "Database Systems & Application"
            rawName.contains("高等数学") -> "Advanced Mathematics"
            rawName.contains("大学物理") -> "University Physics"
            else -> rawName
        }
    }
}
