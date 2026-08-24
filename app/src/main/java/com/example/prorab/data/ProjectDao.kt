package com.example.prorab.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    // Получить все проекты (Flow позволяет списку обновляться самому, если что-то изменилось)
    @Query("SELECT * FROM projects ORDER BY dateCreated DESC")
    fun getAllProjects(): Flow<List<Project>>

    // Добавить новый проект
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: Project)

    // Удалить проект
    @Delete
    suspend fun deleteProject(project: Project)
}