package com.sgaf.universidadedoservidor.domain.model

/** Resultado consolidado da prova final de um curso (v5). aprovado = nota >= nota mínima. */
data class ResultadoProvaFinal(
    val cursoId: Int,
    val acertos: Int,
    val totalQuestoes: Int,
    val aprovado: Boolean,
    val tentativas: Int = 0,
    val respostas: Map<Int, Int> = emptyMap()
) {
    /** Aproveitamento na prova, de 0.0 a 1.0. */
    val percentual: Float get() = if (totalQuestoes > 0) acertos.toFloat() / totalQuestoes else 0f
}
