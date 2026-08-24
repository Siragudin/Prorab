package com.example.prorab.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects") // Название таблицы в SQL
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, // У каждого объекта свой уникальный номер
    val name: String, // Название (Дача, Квартира)
    val dateCreated: Long = System.currentTimeMillis() // Когда создали
)