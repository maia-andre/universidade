package com.sgaf.universidadedoservidor.domain.usecase

import com.sgaf.universidadedoservidor.domain.model.Aula
import com.sgaf.universidadedoservidor.domain.model.Curso
import com.sgaf.universidadedoservidor.domain.model.Modulo
import com.sgaf.universidadedoservidor.domain.model.QuizPergunta
import com.sgaf.universidadedoservidor.domain.repository.CursoRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VerificarAprovacaoCursoUseCaseTest {

    private val repository = mockk<CursoRepository>()
    private val useCase = VerificarAprovacaoCursoUseCase(repository)

    private fun quiz(correta: Int) = QuizPergunta(
        pergunta = "P",
        opcoes = listOf("a", "b"),
        respostaCorretaIndex = correta
    )

    private fun aula(
        id: Int,
        completa: Boolean,
        respostas: Map<Int, Int>
    ) = Aula(
        id = id,
        titulo = "Aula $id",
        conteudo = "",
        isCompleted = completa,
        quiz = listOf(quiz(0), quiz(1)),
        quizRespostas = respostas
    )

    private fun curso(aulas: List<Aula>) = Curso(
        id = 1,
        titulo = "Curso",
        descricao = "",
        modulos = listOf(Modulo(id = 1, titulo = "M", descricao = "", aulas = aulas))
    )

    @Test
    fun `curso inexistente emite null`() = runTest {
        every { repository.getCursoById(1) } returns flowOf(null)

        val resultado = useCase(1).first()

        assertNull(resultado)
    }

    @Test
    fun `100 por cento concluido e gabarito perfeito aprova`() = runTest {
        val aulas = listOf(
            aula(1, completa = true, respostas = mapOf(0 to 0, 1 to 1)),
            aula(2, completa = true, respostas = mapOf(0 to 0, 1 to 1))
        )
        every { repository.getCursoById(1) } returns flowOf(curso(aulas))

        val d = useCase(1).first()!!

        assertTrue(d.aprovado)
        assertEquals(4, d.totalQuestoes)
        assertEquals(4, d.acertos)
        assertEquals(1.0f, d.percentualAcerto, 0.001f)
        assertEquals(2, d.aulasConcluidas)
    }

    @Test
    fun `concluido mas aproveitamento abaixo de 70 nao aprova`() = runTest {
        // Todas concluídas, mas só 1 de 4 respostas corretas (25%).
        val aulas = listOf(
            aula(1, completa = true, respostas = mapOf(0 to 0, 1 to 0)), // 1 acerto
            aula(2, completa = true, respostas = mapOf(0 to 1, 1 to 0))  // 0 acertos
        )
        every { repository.getCursoById(1) } returns flowOf(curso(aulas))

        val d = useCase(1).first()!!

        assertFalse(d.aprovado)
        assertEquals(1, d.acertos)
        assertEquals(0.25f, d.percentualAcerto, 0.001f)
    }

    @Test
    fun `aulas pendentes nao aprovam mesmo com gabarito perfeito`() = runTest {
        val aulas = listOf(
            aula(1, completa = true, respostas = mapOf(0 to 0, 1 to 1)),
            aula(2, completa = false, respostas = mapOf(0 to 0, 1 to 1))
        )
        every { repository.getCursoById(1) } returns flowOf(curso(aulas))

        val d = useCase(1).first()!!

        assertFalse(d.aprovado)
        assertEquals(1, d.aulasConcluidas)
        assertEquals(2, d.totalAulas)
    }
}
