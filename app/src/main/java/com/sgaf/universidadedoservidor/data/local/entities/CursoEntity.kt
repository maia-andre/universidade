package com.sgaf.universidadedoservidor.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cursos")
data class CursoEntity(
    @PrimaryKey val id: Int,
    val titulo: String,
    val descricao: String,
    val isAvailable: Boolean = true
)
