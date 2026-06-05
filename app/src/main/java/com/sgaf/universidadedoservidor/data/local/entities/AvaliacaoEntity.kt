package com.sgaf.universidadedoservidor.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "avaliacoes")
data class AvaliacaoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cursoId: Int,
    val pergunta1: Int, // Likert scale 1-5
    val pergunta2: Int,
    val pergunta3: Int,
    val pergunta4: Int,
    val pergunta5: Int,
    val oQueMaisGostou: String,
    val sugestoes: String
)
