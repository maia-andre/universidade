package com.sgaf.universidadedoservidor.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Resultado da prova final por curso (v5, migração v6->v7).
 * As PERGUNTAS da prova vêm dos assets em runtime; aqui guardamos só o estado do aluno.
 */
@Entity(tableName = "prova_final_resultado")
data class ProvaFinalResultadoEntity(
    @PrimaryKey val cursoId: Int,
    // Map<Int,Int> serializado: índice da questão -> índice da opção escolhida.
    val respostasJson: String = "",
    val acertos: Int = 0,
    val totalQuestoes: Int = 0,
    val aprovado: Boolean = false,
    val tentativas: Int = 0,
    val atualizadoEm: Long = 0
)
