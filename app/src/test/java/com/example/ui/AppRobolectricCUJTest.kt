package com.example.ui

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.Course
import com.example.data.model.Task
import com.example.domain.ScheduleEngine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppRobolectricCUJTest {

    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testCourseDatabaseOperations() = runBlocking {
        val courseDao = db.courseDao()
        val course = Course(
            name = "Test Computer Architecture",
            instructor = "Prof. Zhang",
            time = "09:50 - 12:15",
            location = "Science Hall 101"
        )

        val insertedId = courseDao.insertCourse(course)
        assertTrue(insertedId > 0)

        val courses = courseDao.getCoursesBySemester(1).first()
        assertEquals(1, courses.size)
        assertEquals("Test Computer Architecture", courses[0].name)
    }

    @Test
    fun testTaskDatabaseOperations() = runBlocking {
        val taskDao = db.taskDao()
        val task = Task(
            title = "Complete Homework 1",
            priority = "HIGH",
            dueDate = "2026-10-10"
        )

        val insertedId = taskDao.insertTask(task)
        assertTrue(insertedId > 0)

        val tasks = taskDao.getAllTasks().first()
        assertEquals(1, tasks.size)
        assertEquals("Complete Homework 1", tasks[0].title)
    }

    @Test
    fun testScheduleEngineCalculations() {
        val semStart = java.time.LocalDate.of(2026, 9, 7)
        val today = java.time.LocalDate.of(2026, 10, 4)
        val weekNum = ScheduleEngine.calculateAcademicWeek(semStart, today)
        assertEquals(4, weekNum)
    }
}
