package com.sgaf.universidadedoservidor.domain.usecase

import com.sgaf.universidadedoservidor.core.data.preferences.UserPreferencesRepository
import com.sgaf.universidadedoservidor.core.domain.repository.AuthRepository
import com.sgaf.universidadedoservidor.domain.repository.PlataformaRepository
import javax.inject.Inject

/**
 * Sincroniza o ACESSO do aluno com o backend (Firestore) — v6 (D3) + v7 (Item 1).
 *
 * Espelha localmente (DataStore) os cursos **matriculados** e **concluídos** — que juntos
 * definem o acesso (`acessível = matriculado OU concluído`) — e define o **curso ativo**
 * (destaque da Home) como um curso matriculado em andamento. Best-effort: sem usuário ou
 * sem rede, não altera o estado local (a persistência local segue valendo offline).
 */
class SincronizarCursoAtivoUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val plataformaRepository: PlataformaRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    suspend operator fun invoke() {
        val uid = authRepository.getUserId() ?: return

        val matriculados = plataformaRepository.getCursosMatriculados(uid).toSet()
        val concluidos = plataformaRepository.getCursosConcluidos(uid).toSet()
        userPreferencesRepository.setCursosMatriculados(matriculados)
        // Mescla (nunca apaga): preserva concluídos marcados localmente nesta instalação.
        userPreferencesRepository.mesclarCursosConcluidos(concluidos)

        // Curso ativo = matrícula em andamento (não concluída); senão, qualquer matriculado.
        val ativo = matriculados.firstOrNull { it !in concluidos } ?: matriculados.firstOrNull()
        if (ativo != null) userPreferencesRepository.setCursoAtivo(ativo)
    }
}
