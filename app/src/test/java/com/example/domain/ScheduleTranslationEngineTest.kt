package com.example.domain

import com.example.data.model.CourseEntity
import com.example.data.model.WeekRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleTranslationEngineTest {

    @Test
    fun testChineseDetection() {
        assertTrue(ScheduleTranslationEngine.containsChinese("计算机组成原理"))
        assertTrue(ScheduleTranslationEngine.containsChinese("明理楼B105"))
        assertTrue(ScheduleTranslationEngine.containsChinese("Course (国际学生)"))
        assertFalse(ScheduleTranslationEngine.containsChinese("Computer Organization"))
        assertFalse(ScheduleTranslationEngine.containsChinese("Science Hall 401"))
        assertFalse(ScheduleTranslationEngine.containsChinese("1619304040"))
    }

    @Test
    fun testCourseNameTranslation() {
        assertEquals(
            "Computer Organization and Architecture (Intl)",
            ScheduleTranslationEngine.translateCourseName("计算机组成原理(国际学生)")
        )
        assertEquals(
            "Desktop Application Design (Intl)",
            ScheduleTranslationEngine.translateCourseName("桌面应用程序设计(国际学生)")
        )
        assertEquals(
            "Programming Languages for Data Science (Intl)",
            ScheduleTranslationEngine.translateCourseName("面向数据科学的编程语言(国际学生)")
        )
        assertEquals(
            "Database Principles & Applications (Intl)",
            ScheduleTranslationEngine.translateCourseName("数据库原理及应用(国际学生)")
        )
        assertEquals(
            "Introduction to Neural Networks & Deep Learning (Intl)",
            ScheduleTranslationEngine.translateCourseName("神经网络与深度学习导论(国际学生)")
        )
        assertEquals(
            "Data Analysis & Machine Learning (Intl)",
            ScheduleTranslationEngine.translateCourseName("数据分析与机器学习(国际学生)")
        )
        assertEquals(
            "Production Internship & Engineering Practice (Intl)",
            ScheduleTranslationEngine.translateCourseName("生产实习及工程实践(国际学生)")
        )
        assertEquals(
            "Computer Systems & Programming Contest (Intl)",
            ScheduleTranslationEngine.translateCourseName("计算机系统与程序设计竞赛（国际学生）")
        )
    }

    @Test
    fun testClassroomTranslation() {
        assertEquals(
            "Mingli Hall B105",
            ScheduleTranslationEngine.translateClassroom("明理楼B105")
        )
        assertEquals(
            "Mingli Hall Software Lab 1",
            ScheduleTranslationEngine.translateClassroom("明理楼软件机房1")
        )
        assertEquals(
            "Mingde Hall A304",
            ScheduleTranslationEngine.translateClassroom("明德楼A304")
        )
        assertEquals(
            "Mingde Hall B301",
            ScheduleTranslationEngine.translateClassroom("明德楼B301")
        )
        assertEquals(
            "TBD",
            ScheduleTranslationEngine.translateClassroom("待定")
        )
    }

    @Test
    fun testInstructorTranslation() {
        assertEquals(
            "School of Computer Science",
            ScheduleTranslationEngine.translateInstructor("计算机学院")
        )
        assertEquals(
            "School of Artificial Intelligence",
            ScheduleTranslationEngine.translateInstructor("人工智能学院")
        )
        assertEquals(
            "School of Data Science",
            ScheduleTranslationEngine.translateInstructor("数据科学学院")
        )
        assertEquals(
            "Zhang Jian, Gu Xinyu",
            ScheduleTranslationEngine.translateInstructor("张剑, 古新宇")
        )
    }

    @Test
    fun testBilingualMode() {
        val course = CourseEntity(
            id = 1,
            semesterId = 1,
            name = "计算机组成原理(国际学生)",
            code = "1619304040",
            classroom = "明理楼B105",
            instructor = "计算机学院",
            dayOfWeek = 1,
            startPeriod = 3,
            endPeriod = 5,
            startTime = "09:50",
            endTime = "12:15",
            weekRule = WeekRule.CUSTOM,
            customWeeks = "1-10",
            colorHex = 0xFF337DFF
        )

        val bilingual = ScheduleTranslationEngine.translateCourse(
            course,
            ScheduleTranslationEngine.TranslationMode.BILINGUAL
        )

        assertTrue(bilingual.name.contains("Computer Organization and Architecture (Intl)"))
        assertTrue(bilingual.name.contains("计算机组成原理"))
        assertTrue(bilingual.classroom.contains("Mingli Hall B105"))
        assertTrue(bilingual.classroom.contains("明理楼B105"))
        assertEquals("School of Computer Science", bilingual.instructor)
    }
}
