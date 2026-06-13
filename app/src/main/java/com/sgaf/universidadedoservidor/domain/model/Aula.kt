package com.sgaf.universidadedoservidor.domain.model

data class Aula(
    val id: Int,
    val titulo: String,
    val conteudo: String,
    val isCompleted: Boolean = false,
    val isFavorite: Boolean = false,
    val quiz: List<QuizPergunta> = emptyList(),
    // Estado persistido do quiz (Item 1.2): respostas escolhidas e se já foi submetido.
    val quizRespostas: Map<Int, Int> = emptyMap(),
    val quizSubmitted: Boolean = false,
    // Último acesso (Item 2.2). 0 = nunca aberta.
    val ultimoAcessoEm: Long = 0
)
