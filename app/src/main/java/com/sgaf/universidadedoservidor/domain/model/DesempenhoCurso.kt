package com.sgaf.universidadedoservidor.domain.model

/** Desempenho consolidado do aluno em um curso, usado para a prova final/certificado (Item 4). */
data class DesempenhoCurso(
    val cursoTitulo: String,
    val totalAulas: Int,
    val aulasConcluidas: Int,
    val totalQuestoes: Int,
    val acertos: Int,
    /** Aproveitamento no quiz, de 0.0 a 1.0. */
    val percentualAcerto: Float,
    /** Aprovado quando 100% das aulas concluídas e aproveitamento >= nota mínima. */
    val aprovado: Boolean
)
