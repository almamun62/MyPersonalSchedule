package com.example.domain.model

data class CourseConflict(
    val course1: Course,
    val course2: Course,
    val overlapWeeksDescription: String,
    val timeSlotDescription: String
)
