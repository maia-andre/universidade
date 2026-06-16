package com.sgaf.universidadedoservidor.domain.usecase

import com.sgaf.universidadedoservidor.core.utils.Constants
import com.sgaf.universidadedoservidor.domain.model.DesempenhoCurso
import com.sgaf.universidadedoservidor.domain.repository.CursoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Calcula o desempenho do aluno no curso e determina a aprovação (Item 4).
 *
 * Regra de negócio (V2): aprovado quando 100% das aulas estão concluídas E o
 * aproveitamento no quiz é >= [Constants.MINIMUM_PASSING_SCORE]%.
 */
class VerificarAprovacaoCursoUseCase @Inject constructor(
    private val repository: CursoRepository
) {
    operator fun invoke(cursoId: Int): Flow<DesempenhoCurso?> =
        repository.getCursoById(cursoId).map { curso ->
            if (curso == null) return@map null

            val aulas = curso.modulos.flatMap { it.aulas }
            val totalAulas = aulas.size
            val concluidas = aulas.count { it.isCompleted }

            var totalQuestoes = 0
            var acertos = 0
            aulas.forEach { aula ->
                totalQuestoes += aula.quiz.size
                aula.quiz.forEachIndexed { index, questao ->
                    if (aula.quizRespostas[index] == questao.respostaCorretaIndex) acertos++
                }
            }

            val percentual = if (totalQuestoes > 0) acertos.toFloat() / totalQuestoes else 0f
            val aprovado = totalAulas > 0 &&
                concluidas == totalAulas &&
                percentual >= Constants.MINIMUM_PASSING_SCORE / 100f

            DesempenhoCurso(
                cursoTitulo = curso.titulo,
                totalAulas = totalAulas,
                aulasConcluidas = concluidas,
                totalQuestoes = totalQuestoes,
                acertos = acertos,
                percentualAcerto = percentual,
                aprovado = aprovado
            )
        }
}
