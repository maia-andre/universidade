package com.sgaf.universidadedoservidor.domain.model

/** Desempenho do aluno em um módulo (v4 Item 3). */
data class EstatisticaModulo(
    val moduloTitulo: String,
    val totalAulas: Int,
    val aulasConcluidas: Int,
    val totalQuestoes: Int,
    val acertos: Int,
    val percentualAcerto: Float
)

/** Estatísticas consolidadas de um curso, por módulo e no total (v4 Item 3). */
data class EstatisticasCurso(
    val cursoTitulo: String,
    val totalAulas: Int,
    val aulasConcluidas: Int,
    val percentualConclusao: Float,
    val totalQuestoes: Int,
    val acertos: Int,
    val percentualAcerto: Float,
    val modulos: List<EstatisticaModulo>
)
