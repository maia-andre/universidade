package com.sgaf.universidadedoservidor.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "modulos")
data class ModuloEntity(
    @PrimaryKey val id: Int,
    val cursoId: Int,
    val titulo: String,
    val descricao: String
)
