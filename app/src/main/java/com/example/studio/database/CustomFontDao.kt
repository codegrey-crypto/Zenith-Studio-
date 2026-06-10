package com.example.studio.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CustomFontDao {
    @Query("SELECT * FROM custom_fonts ORDER BY name ASC")
    suspend fun getAllCustomFonts(): List<CustomFontEntity>

    @Query("""
        SELECT * FROM custom_fonts 
        WHERE (:category = 'All' OR :category = 'Imported' OR category = :category) 
          AND (:query = '' OR name LIKE '%' || :query || '%') 
        ORDER BY name ASC 
        LIMIT :limit
    """)
    suspend fun searchCustomFonts(category: String, query: String, limit: Int): List<CustomFontEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomFont(font: CustomFontEntity)

    @Query("DELETE FROM custom_fonts WHERE path = :path")
    suspend fun deleteCustomFont(path: String)
}
