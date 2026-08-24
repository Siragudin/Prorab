package com.example.prorab.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "records",
    foreignKeys = [ForeignKey(
        entity = Project::class,
        parentColumns = ["id"],
        childColumns = ["projectId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Record(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val projectId: Int,
    val type: Int,           // 0 = Работа, 1 = Расход
    val title: String,

    // --- НОВЫЕ ПОЛЯ (По умолчанию 0.0, если не ввели) ---
    val quantity: Double = 0.0,  // Количество
    val unit: String = "",
    val unitPrice: Double = 0.0, // Цена за единицу

    val amount: Double,      // Итоговая сумма (Главная цифра)

    val date: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)