package com.sgaf.universidadedoservidor.domain.usecase

import com.sgaf.universidadedoservidor.core.data.preferences.UserPreferencesRepository
import com.sgaf.universidadedoservidor.domain.repository.ConteudoRemoto
import com.sgaf.universidadedoservidor.domain.repository.ConteudoRemotoRepository
import com.sgaf.universidadedoservidor.domain.repository.CursoRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SincronizarConteudoUseCaseTest {

    private val conteudoRemotoRepository = mockk<ConteudoRemotoRepository>()
    private val cursoRepository = mockk<CursoRepository>()
    private val preferences = mockk<UserPreferencesRepository>(relaxed = true)
    private val useCase =
        SincronizarConteudoUseCase(conteudoRemotoRepository, cursoRepository, preferences)

    @Test
    fun `sem doc remoto e no-op`() = runTest {
        coEvery { conteudoRemotoRepository.obterConteudoRemoto() } returns null

        useCase()

        coVerify(exactly = 0) { cursoRepository.aplicarConteudoRemoto(any()) }
        coVerify(exactly = 0) { preferences.setVersaoConteudo(any()) }
    }

    @Test
    fun `versao remota menor ou igual a local e no-op`() = runTest {
        coEvery { conteudoRemotoRepository.obterConteudoRemoto() } returns ConteudoRemoto(2, "[]")
        every { preferences.versaoConteudo } returns flowOf(2)

        useCase()

        coVerify(exactly = 0) { cursoRepository.aplicarConteudoRemoto(any()) }
        coVerify(exactly = 0) { preferences.setVersaoConteudo(any()) }
    }

    @Test
    fun `catalogo aplicado grava a nova versao`() = runTest {
        coEvery { conteudoRemotoRepository.obterConteudoRemoto() } returns ConteudoRemoto(3, "[…]")
        every { preferences.versaoConteudo } returns flowOf(2)
        coEvery { cursoRepository.aplicarConteudoRemoto("[…]") } returns true

        useCase()

        coVerify(exactly = 1) { preferences.setVersaoConteudo(3) }
    }

    @Test
    fun `catalogo invalido nao grava a versao (nao queima a versao)`() = runTest {
        coEvery { conteudoRemotoRepository.obterConteudoRemoto() } returns
            ConteudoRemoto(3, "nao-e-json")
        every { preferences.versaoConteudo } returns flowOf(2)
        coEvery { cursoRepository.aplicarConteudoRemoto("nao-e-json") } returns false

        useCase()

        coVerify(exactly = 0) { preferences.setVersaoConteudo(any()) }
    }
}
