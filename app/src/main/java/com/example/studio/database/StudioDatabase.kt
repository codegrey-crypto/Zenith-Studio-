package com.example.studio.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ProjectEntity::class, CustomFontEntity::class], version = 3, exportSchema = false)
abstract class StudioDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun customFontDao(): CustomFontDao

    companion object {
        @Volatile
        private var INSTANCE: StudioDatabase? = null

        fun getDatabase(context: Context): StudioDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StudioDatabase::class.java,
                    "studio_database"
                )
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
