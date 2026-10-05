package com.example.data.repository

import com.example.data.local.CourseDao
import com.example.domain.model.Course
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CourseRepository(private val courseDao: CourseDao) {
    fun getAllCourses(): Flow<List<Course>> = courseDao.getAllCourses().map { list ->
        list.map { Course.fromEntity(it) }
    }

    suspend fun insertCourse(course: Course): Long =
        courseDao.insertCourse(course.toEntity())

    suspend fun updateCourse(course: Course) =
        courseDao.updateCourse(course.toEntity())

    suspend fun deleteCourse(course: Course) =
        courseDao.deleteCourse(course.toEntity())
}
