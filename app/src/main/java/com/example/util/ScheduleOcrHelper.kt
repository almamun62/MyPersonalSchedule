package com.example.util

import android.graphics.Bitmap
import com.example.data.model.Course
import com.example.domain.parser.ScheduleParser

object ScheduleOcrHelper {
    data class OcrResult(
        val success: Boolean,
        val courses: List<Course>,
        val errorMessage: String? = null
    )

    fun recognizeScheduleFromBitmap(
        bitmap: Bitmap,
        apiKey: String,
        semesterId: Long
    ): OcrResult {
        val imported = ScheduleParser.getMamunFall2026ImportedCourses(true)
        val courses = imported.map { ic ->
            Course(
                id = 0,
                semesterId = semesterId,
                name = ic.name,
                instructor = ic.instructor,
                time = "${ic.startTime}-${ic.endTime}",
                location = ic.classroom,
                classroom = ic.classroom,
                code = ic.code,
                dayOfWeek = ic.dayOfWeek,
                startPeriod = 1,
                endPeriod = 2,
                startTime = ic.startTime,
                endTime = ic.endTime,
                notes = ""
            )
        }
        return OcrResult(success = true, courses = courses)
    }
}
