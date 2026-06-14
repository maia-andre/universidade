package com.sgaf.universidadedoservidor.domain.usecase

import com.sgaf.universidadedoservidor.domain.model.Curso
import com.sgaf.universidadedoservidor.domain.model.EstatisticaModulo
import com.sgaf.universidadedoservidor.domain.model.EstatisticasCurso
import javax.inject.Inject

/**
 * Calcula o desempenho do aluno em um curso, por módulo e no total (v4 Item 3).
 * Função pura sobre o grafo de domínio (curso → módulos → aulas + respostas do quiz).
 */
class CalcularEstatisticasCursoUseCase @Inject constructor() {

    operator fun invoke(curso: Curso): EstatisticasCurso {
        val modulos = curso.modulos.map { modulo ->
            var totalQuestoes = 0
            var acertos = 0
            modulo.aulas.forEach { aula ->
                totalQuestoes += aula.quiz.size
                aula.quiz.forEachIndexed { index, questao ->
                    if (aula.quizRespostas[index] == questao.respostaCorretaIndex) acertos++
                }
            }
            EstatisticaModulo(
                moduloTitulo = modulo.titulo,
                totalAulas = modulo.aulas.size,
                aulasConcluidas = modulo.aulas.count { it.isCompleted },
                totalQuestoes = totalQuestoes,
                acertos = acertos,
                percentualAcerto = if (totalQuestoes > 0) acertos.toFloat() / totalQuestoes else 0f
            )
        }

        val totalAulas = modulos.sumOf { it.totalAulas }
        val concluidas = modulos.sumOf { it.aulasConcluidas }
        val totalQuestoes = modulos.sumOf { it.totalQuestoes }
        val acertos = modulos.sumOf { it.acertos }

        return EstatisticasCurso(
            cursoTitulo = curso.titulo,
            totalAulas = totalAulas,
            aulasConcluidas = concluidas,
            percentualConclusao = if (totalAulas > 0) concluidas.toFloat() / totalAulas else 0f,
            totalQuestoes = totalQuestoes,
            acertos = acertos,
            percentualAcerto = if (totalQuestoes > 0) acertos.toFloat() / totalQuestoes else 0f,
            modulos = modulos
        )
    }
}
