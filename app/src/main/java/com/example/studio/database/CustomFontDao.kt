package com.example.studio.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CustomFontDao {
    @Query("SELECT * FROM custom_fonts ORDER BY name ASC")
    suspend fun getAllCustomFonts(): List<CustomFontEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomFont(font: CustomFontEntity)

    @Query("DELETE FROM custom_fonts WHERE path = :path")
    suspend fun deleteCustomFont(path: String)
}
