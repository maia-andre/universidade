package com.sgaf.universidadedoservidor.domain.usecase

import com.sgaf.universidadedoservidor.core.utils.Constants
import com.sgaf.universidadedoservidor.domain.model.QuizPergunta
import com.sgaf.universidadedoservidor.domain.model.ResultadoProvaFinal
import com.sgaf.universidadedoservidor.domain.repository.ProvaFinalRepository
import javax.inject.Inject

/**
 * Calcula a nota da prova final, persiste e devolve o resultado.
 * Aprovado quando o aproveitamento é >= [Constants.MINIMUM_PASSING_SCORE]%.
 */
class SubmeterProvaFinalUseCase @Inject constructor(
    private val repository: ProvaFinalRepository
) {
    suspend operator fun invoke(
        cursoId: Int,
        perguntas: List<QuizPergunta>,
        respostas: Map<Int, Int>
    ): ResultadoProvaFinal {
        val total = perguntas.size
        val acertos = perguntas.indices.count { respostas[it] == perguntas[it].respostaCorretaIndex }
        val aprovado = total > 0 && acertos.toFloat() / total >= Constants.MINIMUM_PASSING_SCORE / 100f
        repository.salvarResultado(cursoId, respostas, acertos, total, aprovado)
        return ResultadoProvaFinal(
            cursoId = cursoId,
            acertos = acertos,
            totalQuestoes = total,
            aprovado = aprovado,
            respostas = respostas
        )
    }
}
