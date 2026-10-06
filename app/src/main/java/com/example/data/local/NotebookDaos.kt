package com.example.data.local

import androidx.room.*
import com.example.data.model.NoteEntity
import com.example.data.model.NotebookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotebookDao {
    @Query("SELECT * FROM notebooks ORDER BY updatedAtMillis DESC")
    fun getAllNotebooks(): Flow<List<NotebookEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotebook(notebook: NotebookEntity): Long

    @Delete
    suspend fun deleteNotebook(notebook: NotebookEntity)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY updatedAtMillis DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE notebookId = :notebookId ORDER BY updatedAtMillis DESC")
    fun getNotesByNotebook(notebookId: Long): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE courseId = :courseId ORDER BY updatedAtMillis DESC")
    fun getNotesForCourse(courseId: Long): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Delete
    suspend fun deleteNote(note: NoteEntity)
}
