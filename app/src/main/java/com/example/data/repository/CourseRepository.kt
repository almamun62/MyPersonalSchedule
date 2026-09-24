package com.example.data.repository

import com.example.data.local.CourseDao
import com.example.domain.model.Course
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CourseRepository(private val courseDao: CourseDao) {

    val allCourses: Flow<List<Course>> = courseDao.getAllCourses().map { entities ->
        entities.map { Course.fromEntity(it) }
    }

    fun getCoursesBySemester(semester: String): Flow<List<Course>> {
        val semesterId = semester.filter { it.isDigit() }.toLongOrNull() ?: 1L
        return courseDao.getCoursesBySemester(semesterId).map { entities ->
            entities.map { Course.fromEntity(it) }
        }
    }

    val semesters: Flow<List<String>> = courseDao.getAllCourses().map { list ->
        val found = list.map { if (it.semesterId == 1L) "Fall 2025" else "Semester ${it.semesterId}" }.distinct()
        if (found.isEmpty()) listOf("Fall 2025", "Spring 2026") else found
    }

    suspend fun insertCourse(course: Course): Long =
        courseDao.insertCourse(course.toEntity())

    suspend fun insertCourses(courses: List<Course>): List<Long> =
        courseDao.insertCourses(courses.map { it.toEntity() })

    suspend fun updateCourse(course: Course) =
        courseDao.updateCourse(course.toEntity())

    suspend fun deleteCourse(course: Course) =
        courseDao.deleteCourse(course.toEntity())

    suspend fun deleteCourseById(id: Long) =
        courseDao.deleteCourseById(id)

    suspend fun deleteCoursesBySemester(semester: String) {
        val semesterId = semester.filter { it.isDigit() }.toLongOrNull() ?: 1L
        courseDao.deleteCoursesBySemester(semesterId)
    }

    suspend fun clearAll() =
        courseDao.clearAll()

    suspend fun seedInitialDataIfEmpty() {
        // Sample starter schedule for testing
        val sampleCourses = listOf(
            Course(
                name = "Data Structures & Algorithms",
                code = "CS 201",
                instructor = "Dr. Alan Turing",
                classroom = "Turing Hall 101",
                dayOfWeek = 1, // Mon
                startTime = "09:00",
                endTime = "10:30",
                colorHex = "#4F46E5",
                semester = "Fall 2025",
                credits = 4
            ),
            Course(
                name = "Data Structures & Algorithms",
                code = "CS 201",
                instructor = "Dr. Alan Turing",
                classroom = "Turing Hall 101",
                dayOfWeek = 3, // Wed
                startTime = "09:00",
                endTime = "10:30",
                colorHex = "#4F46E5",
                semester = "Fall 2025",
                credits = 4
            ),
            Course(
                name = "Linear Algebra & Matrix Theory",
                code = "MATH 240",
                instructor = "Prof. Katherine Johnson",
                classroom = "Science Bld 302",
                dayOfWeek = 2, // Tue
                startTime = "11:00",
                endTime = "12:30",
                colorHex = "#0EA5E9",
                semester = "Fall 2025",
                credits = 3
            ),
            Course(
                name = "Linear Algebra & Matrix Theory",
                code = "MATH 240",
                instructor = "Prof. Katherine Johnson",
                classroom = "Science Bld 302",
                dayOfWeek = 4, // Thu
                startTime = "11:00",
                endTime = "12:30",
                colorHex = "#0EA5E9",
                semester = "Fall 2025",
                credits = 3
            ),
            Course(
                name = "Operating Systems Lab",
                code = "CS 310L",
                instructor = "Dr. Grace Hopper",
                classroom = "CS Lab 4B",
                dayOfWeek = 5, // Fri
                startTime = "14:00",
                endTime = "16:00",
                colorHex = "#10B981",
                semester = "Fall 2025",
                credits = 2
            )
        )
        insertCourses(sampleCourses)
    }
}
