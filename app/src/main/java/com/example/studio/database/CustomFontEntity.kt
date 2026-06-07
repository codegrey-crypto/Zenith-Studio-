package com.example.studio.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_fonts")
data class CustomFontEntity(
    @PrimaryKey val path: String,
    val name: String,
    val category: String
)
