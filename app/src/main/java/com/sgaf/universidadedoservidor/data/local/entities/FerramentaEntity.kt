package com.sgaf.universidadedoservidor.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Instância preenchida de uma ferramenta prática (v4 Item 2), ex.: uma Matriz SWOT
 * ou um 5W2H. [camposJson] guarda um Map<String,String> serializado (chave do campo → texto).
 */
@Entity(tableName = "ferramentas")
data class FerramentaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tipo: String,
    val titulo: String,
    val camposJson: String,
    val criadoEm: Long
)
