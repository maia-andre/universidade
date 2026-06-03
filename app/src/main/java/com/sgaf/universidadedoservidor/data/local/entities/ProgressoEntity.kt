package com.sgaf.universidadedoservidor.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "progresso")
data class ProgressoEntity(
    @PrimaryKey val aulaId: Int,
    val isCompleted: Boolean = false,
    val isFavorite: Boolean = false
)
// 100% offline - tracks user completion and bookmarking per class/lesson
