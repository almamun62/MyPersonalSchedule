package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SemesterDao {
    @Query("SELECT * FROM semesters ORDER BY id DESC")
    fun getAllSemesters(): Flow<List<SemesterEntity>>

    @Query("SELECT * FROM semesters WHERE isActive = 1 LIMIT 1")
    fun getActiveSemester(): Flow<SemesterEntity?>

    @Query("SELECT * FROM semesters WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveSemesterSync(): SemesterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSemester(semester: SemesterEntity): Long

    @Update
    suspend fun updateSemester(semester: SemesterEntity)

    @Query("UPDATE semesters SET isActive = 0")
    suspend fun deactivateAll()

    @Transaction
    suspend fun setActiveSemester(id: Long) {
        deactivateAll()
        setActiveById(id)
    }

    @Query("UPDATE semesters SET isActive = 1 WHERE id = :id")
    suspend fun setActiveById(id: Long)

    @Delete
    suspend fun deleteSemester(semester: SemesterEntity)
}

@Dao
interface CourseDao {
    @Query("SELECT * FROM courses WHERE semesterId = :semesterId ORDER BY dayOfWeek ASC, startPeriod ASC")
    fun getCoursesBySemester(semesterId: Long): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE semesterId = :semesterId ORDER BY dayOfWeek ASC, startPeriod ASC")
    suspend fun getCoursesBySemesterSync(semesterId: Long): List<CourseEntity>

    @Query("SELECT * FROM courses WHERE id = :id")
    suspend fun getCourseById(id: Long): CourseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: CourseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourses(courses: List<CourseEntity>)

    @Update
    suspend fun updateCourse(course: CourseEntity)

    @Delete
    suspend fun deleteCourse(course: CourseEntity)

    @Query("DELETE FROM courses WHERE semesterId = :semesterId")
    suspend fun deleteCoursesBySemester(semesterId: Long)
}

@Dao
interface ExamDao {
    @Query("SELECT * FROM exams WHERE semesterId = :semesterId ORDER BY examDateMillis ASC")
    fun getExamsBySemester(semesterId: Long): Flow<List<ExamEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: ExamEntity): Long

    @Delete
    suspend fun deleteExam(exam: ExamEntity)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, dueDateMillis ASC, id DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY dueDateMillis ASC, id DESC")
    fun getPendingTasks(): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("UPDATE tasks SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun setTaskCompleted(id: Long, isCompleted: Boolean)

    @Delete
    suspend fun deleteTask(task: TaskEntity)
}

@Dao
interface HolidayOverrideDao {
    @Query("SELECT * FROM holiday_overrides ORDER BY dateString ASC")
    fun getAllHolidayOverrides(): Flow<List<HolidayOverrideEntity>>

    @Query("SELECT * FROM holiday_overrides ORDER BY dateString ASC")
    suspend fun getAllHolidayOverridesSync(): List<HolidayOverrideEntity>

    @Query("SELECT * FROM holiday_overrides WHERE dateString = :dateString LIMIT 1")
    suspend fun getOverrideForDate(dateString: String): HolidayOverrideEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOverride(override: HolidayOverrideEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOverrides(overrides: List<HolidayOverrideEntity>)

    @Delete
    suspend fun deleteOverride(override: HolidayOverrideEntity)
}
