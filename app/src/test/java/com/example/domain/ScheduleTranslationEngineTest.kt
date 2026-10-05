package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ScheduleTranslationEngineTest {

    @Test
    fun testCourseNameTranslation() {
        assertEquals("Desktop Application Design", ScheduleTranslationEngine.translateCourseName("桌面应用程序设计"))
        assertEquals("Computer Architecture", ScheduleTranslationEngine.translateCourseName("计算机组成原理"))
        assertEquals("Data Analysis & Machine Learning", ScheduleTranslationEngine.translateCourseName("数据分析与机器学习"))
        assertEquals("Programming for Data Science", ScheduleTranslationEngine.translateCourseName("面向数据科学"))
        assertEquals("Neural Networks & Deep Learning", ScheduleTranslationEngine.translateCourseName("神经网络"))
        assertEquals("Database Systems & Application", ScheduleTranslationEngine.translateCourseName("数据库原理"))
        assertEquals("Advanced Mathematics", ScheduleTranslationEngine.translateCourseName("高等数学"))
        assertEquals("University Physics", ScheduleTranslationEngine.translateCourseName("大学物理"))
        assertEquals("Physics Lab", ScheduleTranslationEngine.translateCourseName("Physics Lab"))
    }
}
