package com.sgaf.universidadedoservidor.data.repository

import com.sgaf.universidadedoservidor.data.local.dao.AulaDao
import com.sgaf.universidadedoservidor.data.local.dao.CursoDao
import com.sgaf.universidadedoservidor.data.local.dao.ModuloDao
import com.sgaf.universidadedoservidor.data.local.dao.ProgressoDao
import com.sgaf.universidadedoservidor.data.local.dao.SearchDao
import com.sgaf.universidadedoservidor.data.local.dao.SearchResultRow
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CursoRepositoryImplBuscaTest {

    private val cursoDao = mockk<CursoDao>(relaxed = true)
    private val moduloDao = mockk<ModuloDao>(relaxed = true)
    private val aulaDao = mockk<AulaDao>(relaxed = true)
    private val progressoDao = mockk<ProgressoDao>(relaxed = true)
    private val searchDao = mockk<SearchDao>()

    private val repository = CursoRepositoryImpl(
        cursoDao, moduloDao, aulaDao, progressoDao, searchDao
    )

    @Test
    fun `termo com menos de 2 letras retorna vazio sem consultar o banco`() = runTest {
        val resultado = repository.buscar("a")

        assertTrue(resultado.isEmpty())
        coVerify(exactly = 0) { searchDao.buscar(any()) }
    }

    @Test
    fun `mapeia linhas e gera trecho quando o termo esta no conteudo`() = runTest {
        coEvery { searchDao.buscar("inventário") } returns listOf(
            SearchResultRow(
                aulaId = 10,
                aulaTitulo = "Gestão de estoque",
                moduloTitulo = "Módulo 1",
                cursoTitulo = "Almoxarifado",
                conteudo = "O controle de inventário garante a exatidão dos registros."
            )
        )

        val resultado = repository.buscar("inventário")

        assertEquals(1, resultado.size)
        val r = resultado.first()
        assertEquals(10, r.aulaId)
        assertEquals("Almoxarifado", r.cursoTitulo)
        assertNotNull(r.trecho)
        assertTrue(r.trecho!!.contains("inventário", ignoreCase = true))
    }

    @Test
    fun `trecho fica nulo quando o termo casa so no titulo`() = runTest {
        coEvery { searchDao.buscar("licitação") } returns listOf(
            SearchResultRow(
                aulaId = 20,
                aulaTitulo = "Introdução à Licitação",
                moduloTitulo = "Módulo 1",
                cursoTitulo = "Licitações",
                conteudo = "Conteúdo sem o termo no corpo do texto."
            )
        )

        val resultado = repository.buscar("licitação")

        assertEquals(1, resultado.size)
        assertNull(resultado.first().trecho)
    }
}
