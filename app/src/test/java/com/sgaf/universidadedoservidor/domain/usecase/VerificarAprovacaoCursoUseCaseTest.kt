package com.sgaf.universidadedoservidor.domain.usecase

import com.sgaf.universidadedoservidor.domain.model.Aula
import com.sgaf.universidadedoservidor.domain.model.Curso
import com.sgaf.universidadedoservidor.domain.model.Modulo
import com.sgaf.universidadedoservidor.domain.model.ResultadoProvaFinal
import com.sgaf.universidadedoservidor.domain.repository.CursoRepository
import com.sgaf.universidadedoservidor.domain.repository.ProvaFinalRepository
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
    private val provaFinalRepository = mockk<ProvaFinalRepository>()
    private val useCase = VerificarAprovacaoCursoUseCase(repository, provaFinalRepository)

    private fun aula(id: Int, completa: Boolean) = Aula(
        id = id,
        titulo = "Aula $id",
        conteudo = "",
        isCompleted = completa
    )

    private fun curso(aulas: List<Aula>) = Curso(
        id = 1,
        titulo = "Curso",
        descricao = "",
        modulos = listOf(Modulo(id = 1, titulo = "M", descricao = "", aulas = aulas))
    )

    private fun prova(aprovado: Boolean, acertos: Int = 0, total: Int = 0) =
        ResultadoProvaFinal(cursoId = 1, acertos = acertos, totalQuestoes = total, aprovado = aprovado)

    @Test
    fun `curso inexistente emite null`() = runTest {
        every { repository.getCursoById(1) } returns flowOf(null)
        every { provaFinalRepository.getResultado(1) } returns flowOf(null)

        assertNull(useCase(1).first())
    }

    @Test
    fun `100 por cento concluido e prova final aprovada aprova`() = runTest {
        val aulas = listOf(aula(1, completa = true), aula(2, completa = true))
        every { repository.getCursoById(1) } returns flowOf(curso(aulas))
        every { provaFinalRepository.getResultado(1) } returns
            flowOf(prova(aprovado = true, acertos = 6, total = 6))

        val d = useCase(1).first()!!

        assertTrue(d.aprovado)
        assertEquals(2, d.aulasConcluidas)
        assertEquals(6, d.acertos)
        assertEquals(1.0f, d.percentualAcerto, 0.001f)
    }

    @Test
    fun `concluido mas prova final reprovada nao aprova`() = runTest {
        val aulas = listOf(aula(1, completa = true), aula(2, completa = true))
        every { repository.getCursoById(1) } returns flowOf(curso(aulas))
        every { provaFinalRepository.getResultado(1) } returns
            flowOf(prova(aprovado = false, acertos = 2, total = 6))

        val d = useCase(1).first()!!

        assertFalse(d.aprovado)
        assertEquals(2, d.acertos)
    }

    @Test
    fun `prova final nao realizada nao aprova`() = runTest {
        val aulas = listOf(aula(1, completa = true), aula(2, completa = true))
        every { repository.getCursoById(1) } returns flowOf(curso(aulas))
        every { provaFinalRepository.getResultado(1) } returns flowOf(null)

        val d = useCase(1).first()!!

        assertFalse(d.aprovado)
        assertEquals(0, d.totalQuestoes)
    }

    @Test
    fun `aulas pendentes nao aprovam mesmo com prova final aprovada`() = runTest {
        val aulas = listOf(aula(1, completa = true), aula(2, completa = false))
        every { repository.getCursoById(1) } returns flowOf(curso(aulas))
        every { provaFinalRepository.getResultado(1) } returns
            flowOf(prova(aprovado = true, acertos = 6, total = 6))

        val d = useCase(1).first()!!

        assertFalse(d.aprovado)
        assertEquals(1, d.aulasConcluidas)
        assertEquals(2, d.totalAulas)
    }
}
