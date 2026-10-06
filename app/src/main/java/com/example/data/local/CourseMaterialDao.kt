package com.example.data.local

import androidx.room.*
import com.example.data.model.CourseMaterialEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseMaterialDao {
    @Query("SELECT * FROM course_materials ORDER BY addedAtMillis DESC")
    fun getAllMaterials(): Flow<List<CourseMaterialEntity>>

    @Query("SELECT * FROM course_materials WHERE courseName = :courseName ORDER BY addedAtMillis DESC")
    fun getMaterialsByCourse(courseName: String): Flow<List<CourseMaterialEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaterial(material: CourseMaterialEntity): Long

    @Delete
    suspend fun deleteMaterial(material: CourseMaterialEntity)
}
