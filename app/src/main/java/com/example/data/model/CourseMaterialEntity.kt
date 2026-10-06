package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "course_materials")
data class CourseMaterialEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val courseId: Long = 0,
    val courseName: String = "",
    val title: String,
    val fileName: String,
    val fileType: String = "TEXT", // "TEXT", "PDF", "DOCUMENT", "NOTES"
    val content: String = "",
    val localPath: String = "",
    val fileSizeBytes: Long = 0,
    val addedAtMillis: Long = System.currentTimeMillis()
)
