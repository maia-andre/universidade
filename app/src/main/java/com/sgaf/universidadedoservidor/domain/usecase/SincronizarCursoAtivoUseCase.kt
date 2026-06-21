package com.sgaf.universidadedoservidor.domain.usecase

import com.sgaf.universidadedoservidor.core.data.preferences.UserPreferencesRepository
import com.sgaf.universidadedoservidor.core.domain.repository.AuthRepository
import com.sgaf.universidadedoservidor.domain.repository.PlataformaRepository
import javax.inject.Inject

/**
 * Lê a matrícula liberada pelo RH (Firestore) e define o curso ativo local (DataStore) — v6, D3.
 * Best-effort: se não há usuário/matrícula ou falta rede, não altera o estado local.
 */
class SincronizarCursoAtivoUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val plataformaRepository: PlataformaRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    suspend operator fun invoke() {
        val uid = authRepository.getUserId() ?: return
        val cursoId = plataformaRepository.getCursoAtivoMatriculado(uid) ?: return
        userPreferencesRepository.setCursoAtivo(cursoId)
    }
}
