package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.AcademicCalendarFileEntity
import com.example.data.model.Course
import com.example.data.model.CourseEntity
import com.example.data.model.ExamEntity
import com.example.data.model.NoteEntity
import com.example.data.model.Task
import kotlinx.coroutines.flow.Flow

class ScheduleRepository(val database: AppDatabase) {
    fun getAllCourses(): Flow<List<Course>> = database.courseDao().getAllCourses()
    fun getCoursesBySemester(semId: Long): Flow<List<Course>> = database.courseDao().getCoursesBySemester(semId)
    
    suspend fun insertCourse(course: CourseEntity): Long = database.courseDao().insertCourse(course)
    suspend fun insertCourses(courses: List<CourseEntity>) = database.courseDao().insertCourses(courses)
    suspend fun updateCourse(course: CourseEntity) = database.courseDao().updateCourse(course)
    suspend fun deleteCourse(course: CourseEntity) = database.courseDao().deleteCourse(course)
    suspend fun deleteAllCourses() = database.courseDao().deleteAllCourses()

    fun getAllExams(): Flow<List<ExamEntity>> = database.examDao().getAllExams()
    suspend fun insertExam(exam: ExamEntity): Long = database.examDao().insertExam(exam)
    suspend fun deleteExam(exam: ExamEntity) = database.examDao().deleteExam(exam)

    fun getAllTasks(): Flow<List<Task>> = database.taskDao().getAllTasks()
    suspend fun insertTask(task: Task): Long = database.taskDao().insertTask(task)
    suspend fun updateTask(task: Task) = database.taskDao().updateTask(task)
    suspend fun deleteTask(task: Task) = database.taskDao().deleteTask(task)

    fun getAllCalendarFiles(): Flow<List<AcademicCalendarFileEntity>> = database.academicCalendarFileDao().getAllCalendarFiles()
    suspend fun insertCalendarFile(file: AcademicCalendarFileEntity): Long = database.academicCalendarFileDao().insertCalendarFile(file)
    suspend fun deleteCalendarFile(file: AcademicCalendarFileEntity) = database.academicCalendarFileDao().deleteCalendarFile(file)

    fun getAllNotes(): Flow<List<NoteEntity>> = database.noteDao().getAllNotes()
    suspend fun insertNote(note: NoteEntity): Long = database.noteDao().insertNote(note)
    suspend fun deleteNote(note: NoteEntity) = database.noteDao().deleteNote(note)

    fun getAllCourseMaterials(): Flow<List<com.example.data.model.CourseMaterialEntity>> = database.courseMaterialDao().getAllMaterials()
    suspend fun insertCourseMaterial(material: com.example.data.model.CourseMaterialEntity): Long = database.courseMaterialDao().insertMaterial(material)
    suspend fun deleteCourseMaterial(material: com.example.data.model.CourseMaterialEntity) = database.courseMaterialDao().deleteMaterial(material)

    fun getAllReminders(): Flow<List<com.example.data.model.ReminderEntity>> = database.reminderDao().getAllReminders()
    suspend fun insertReminder(reminder: com.example.data.model.ReminderEntity): Long = database.reminderDao().insertReminder(reminder)
    suspend fun updateReminder(reminder: com.example.data.model.ReminderEntity) = database.reminderDao().updateReminder(reminder)
    suspend fun deleteReminder(reminder: com.example.data.model.ReminderEntity) = database.reminderDao().deleteReminder(reminder)
}
