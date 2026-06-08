package com.example.studio.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val width: Float,
    val height: Float,
    val timestamp: Long,
    val layersJson: String,
    val dpi: Int = 300
)
