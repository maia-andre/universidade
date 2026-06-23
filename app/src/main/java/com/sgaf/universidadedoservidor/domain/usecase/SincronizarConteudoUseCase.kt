package com.sgaf.universidadedoservidor.domain.usecase

import com.sgaf.universidadedoservidor.core.data.preferences.UserPreferencesRepository
import com.sgaf.universidadedoservidor.domain.repository.ConteudoRemotoRepository
import com.sgaf.universidadedoservidor.domain.repository.CursoRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Sincroniza o conteúdo dos cursos com o backend (V8 Item 1).
 *
 * Lê `config/conteudo` e, se a versão publicada for maior que a versão local, aplica o catálogo
 * (persiste local + reconstrói o Room, preservando o progresso) e grava a nova versão. Offline-safe:
 * sem rede ou sem doc publicado, é no-op (o app segue com o conteúdo já presente).
 */
class SincronizarConteudoUseCase @Inject constructor(
    private val conteudoRemotoRepository: ConteudoRemotoRepository,
    private val cursoRepository: CursoRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    suspend operator fun invoke() {
        val remoto = conteudoRemotoRepository.obterConteudoRemoto() ?: return
        val versaoLocal = userPreferencesRepository.versaoConteudo.first()
        if (remoto.versao <= versaoLocal) return

        cursoRepository.aplicarConteudoRemoto(remoto.json)
        userPreferencesRepository.setVersaoConteudo(remoto.versao)
    }
}
