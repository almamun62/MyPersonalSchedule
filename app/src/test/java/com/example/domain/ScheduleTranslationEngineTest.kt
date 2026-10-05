package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ScheduleTranslationEngineTest {

    @Test
    fun testCourseNameTranslation() {
        assertEquals("Desktop App Design", ScheduleTranslationEngine.translateCourseName("桌面应用程序设计"))
        assertEquals("Computer Architecture", ScheduleTranslationEngine.translateCourseName("计算机组成原理"))
        assertEquals("Data Analysis & ML", ScheduleTranslationEngine.translateCourseName("数据分析与机器学习"))
        assertEquals("Data Science Programming", ScheduleTranslationEngine.translateCourseName("面向数据科学"))
        assertEquals("Neural Networks & AI", ScheduleTranslationEngine.translateCourseName("神经网络"))
        assertEquals("Database Systems", ScheduleTranslationEngine.translateCourseName("数据库原理"))
        assertEquals("Physics Lab", ScheduleTranslationEngine.translateCourseName("Physics Lab"))
    }
}
