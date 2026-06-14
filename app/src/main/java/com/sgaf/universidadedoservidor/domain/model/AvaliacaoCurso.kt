package com.sgaf.universidadedoservidor.domain.model

/**
 * Avaliação do curso pelo aluno (v4 Item 3.3). [respostas] são 5 notas Likert (1–5),
 * na ordem das perguntas exibidas.
 */
data class AvaliacaoCurso(
    val cursoId: Int,
    val respostas: List<Int>,
    val oQueMaisGostou: String,
    val sugestoes: String
)
