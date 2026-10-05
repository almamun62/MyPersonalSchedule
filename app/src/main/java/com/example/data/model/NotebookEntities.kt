package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notebooks")
data class NotebookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val courseId: Long = 0,
    val title: String,
    val description: String = "",
    val paperStyle: String = "GRID",
    val coverColorHex: String = "#5B9BF3",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val notebookId: Long = 0,
    val courseId: Long = 0,
    val title: String,
    val content: String = "",
    val audioPath: String? = null,
    val imagePath: String? = null,
    val drawingDataJson: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)
