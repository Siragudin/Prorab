package com.example.prorab.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Update // <--- НОВЫЙ ИМПОРТ
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordDao {
    @Query("SELECT * FROM records WHERE projectId = :projectId ORDER BY date ASC")
    fun getRecordsByProject(projectId: Int): Flow<List<Record>>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: Record)

    // --- НОВЫЙ МЕТОД ---
    @Update
    suspend fun updateRecord(record: Record)
    // -------------------

    @Delete
    suspend fun deleteRecord(record: Record)

    @Query("SELECT SUM(amount) FROM records WHERE projectId = :projectId AND type = 1")
    fun getTotalExpenses(projectId: Int): Flow<Double?>

    @Query("SELECT SUM(amount) FROM records WHERE projectId = :projectId AND type = 0")
    fun getTotalWork(projectId: Int): Flow<Double?>
}