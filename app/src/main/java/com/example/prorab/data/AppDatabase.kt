package com.example.prorab.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Миграция с версии 1 на 2: Добавляем колонку unit
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Добавляем поле unit (текст, по умолчанию пустая строка) в таблицу records
        db.execSQL("ALTER TABLE records ADD COLUMN unit TEXT NOT NULL DEFAULT ''")
    }
}

@Database(entities = [Project::class, Record::class], version = 2)
abstract class AppDatabase : RoomDatabase() {

    abstract fun projectDao(): ProjectDao
    abstract fun recordDao(): RecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "prorab_database"
                )
                    .addMigrations(MIGRATION_1_2) // Подключаем нашу миграцию
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}