package com.example

import com.example.data.model.Course
import com.example.data.model.CourseEntity
import com.example.data.model.Task
import com.example.data.model.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseAndTaskEntityTest {

    @Test
    fun testCourseEntityCreationWithRequiredFields() {
        // Course entity storing name, instructor, time, and location
        val course = Course(
            name = "CS302 Algorithm Design",
            instructor = "Prof. Liu",
            time = "08:00 - 09:35",
            location = "Science Bldg 4, Rm 302"
        )

        assertEquals("CS302 Algorithm Design", course.name)
        assertEquals("Prof. Liu", course.instructor)
        assertEquals("08:00 - 09:35", course.time)
        assertEquals("08:00", course.startTime)
        assertEquals("09:35", course.endTime)
        assertEquals("Science Bldg 4, Rm 302", course.classroom)
        assertEquals("Science Bldg 4, Rm 302", course.location)
    }

    @Test
    fun testTaskEntityCreationForAssignments() {
        val task = Task(
            courseId = 1L,
            title = "Assignment 1: Dynamic Programming Analysis",
            dueDate = "2026-10-15",
            priority = "HIGH",
            isCompleted = false
        )

        assertEquals(1L, task.courseId)
        assertEquals("Assignment 1: Dynamic Programming Analysis", task.title)
        assertEquals("2026-10-15", task.dueDate)
        assertEquals("HIGH", task.priority)
        assertFalse(task.isCompleted)
    }

    @Test
    fun testTypealiasCompatibility() {
        val legacyCourse: CourseEntity = Course(
            name = "Linear Algebra",
            instructor = "Dr. Wang",
            time = "10:00 - 11:35",
            location = "Math Hall 101"
        )
        assertNotNull(legacyCourse)
        assertEquals("Linear Algebra", legacyCourse.name)

        val legacyTask: TaskEntity = Task(
            courseId = legacyCourse.id,
            title = "Problem Set 3",
            dueDate = "2026-10-20"
        )
        assertNotNull(legacyTask)
        assertEquals("Problem Set 3", legacyTask.title)
    }
}
