package com.sgaf.universidadedoservidor.domain.model

data class Aula(
    val id: Int,
    val titulo: String,
    val conteudo: String,
    val isCompleted: Boolean = false,
    val isFavorite: Boolean = false,
    val quiz: List<QuizPergunta> = emptyList()
)
