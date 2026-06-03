package com.sgaf.universidadedoservidor.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "aulas")
data class AulaEntity(
    @PrimaryKey val id: Int,
    val moduloId: Int,
    val titulo: String,
    val conteudo: String,
    val quizJson: String
)
