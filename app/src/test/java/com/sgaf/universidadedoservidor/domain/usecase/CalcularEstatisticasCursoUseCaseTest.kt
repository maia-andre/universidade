package com.sgaf.universidadedoservidor.domain.usecase

import com.sgaf.universidadedoservidor.domain.model.Aula
import com.sgaf.universidadedoservidor.domain.model.Curso
import com.sgaf.universidadedoservidor.domain.model.Modulo
import com.sgaf.universidadedoservidor.domain.model.QuizPergunta
import org.junit.Assert.assertEquals
import org.junit.Test

class CalcularEstatisticasCursoUseCaseTest {

    private val useCase = CalcularEstatisticasCursoUseCase()

    private val quiz = listOf(
        QuizPergunta("P1", listOf("a", "b"), respostaCorretaIndex = 0),
        QuizPergunta("P2", listOf("a", "b"), respostaCorretaIndex = 1)
    )

    private fun aula(id: Int, completa: Boolean, respostas: Map<Int, Int>) = Aula(
        id = id, titulo = "A$id", conteudo = "",
        isCompleted = completa, quiz = quiz, quizRespostas = respostas
    )

    @Test
    fun `agrega conclusao e acertos por modulo e no total`() {
        val curso = Curso(
            id = 1, titulo = "Curso", descricao = "",
            modulos = listOf(
                Modulo(1, "M1", "", aulas = listOf(
                    aula(1, completa = true, respostas = mapOf(0 to 0, 1 to 1)) // 2/2
                )),
                Modulo(2, "M2", "", aulas = listOf(
                    aula(2, completa = false, respostas = mapOf(0 to 1, 1 to 1)) // 1/2
                ))
            )
        )

        val s = useCase(curso)

        assertEquals(2, s.totalAulas)
        assertEquals(1, s.aulasConcluidas)
        assertEquals(0.5f, s.percentualConclusao, 0.001f)
        assertEquals(4, s.totalQuestoes)
        assertEquals(3, s.acertos)
        assertEquals(0.75f, s.percentualAcerto, 0.001f)
        assertEquals(2, s.modulos.size)
        assertEquals(1.0f, s.modulos[0].percentualAcerto, 0.001f)
        assertEquals(0.5f, s.modulos[1].percentualAcerto, 0.001f)
    }
}
