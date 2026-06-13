package com.sgaf.universidadedoservidor.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "progresso")
data class ProgressoEntity(
    @PrimaryKey val aulaId: Int,
    val isCompleted: Boolean = false,
    val isFavorite: Boolean = false,
    // Estado persistido do quiz (Item 1.2). quizRespostasJson = Map<Int,Int> serializado
    // (índice da questão -> índice da opção escolhida). quizAcertos alimenta a prova final (Item 4).
    val quizSubmitted: Boolean = false,
    val quizAcertos: Int = 0,
    val quizRespostasJson: String = ""
)
// 100% offline - tracks user completion, bookmarking and quiz state per class/lesson
