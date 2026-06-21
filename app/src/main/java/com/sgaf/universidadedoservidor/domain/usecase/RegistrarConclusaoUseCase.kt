package com.sgaf.universidadedoservidor.domain.usecase

import com.sgaf.universidadedoservidor.core.data.preferences.UserPreferencesRepository
import com.sgaf.universidadedoservidor.core.domain.repository.AuthRepository
import com.sgaf.universidadedoservidor.domain.repository.PlataformaRepository
import javax.inject.Inject

/**
 * Registra a conclusão do curso no backend (upstream para o RH) na emissão do certificado — v6, D4.
 * Gera um código de validação determinístico do certificado. Também marca o curso como concluído
 * localmente (v7, Item 1) para o acesso "concluído fica acessível para sempre" valer de imediato.
 */
class RegistrarConclusaoUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val plataformaRepository: PlataformaRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    suspend operator fun invoke(cursoId: Int, nota: Int): Result<Unit> {
        val uid = authRepository.getUserId()
            ?: return Result.failure(IllegalStateException("Sem usuário autenticado."))
        val certificadoId = "UNISJC-$cursoId-${uid.takeLast(5).uppercase()}"
        return plataformaRepository.registrarConclusao(uid, cursoId, nota, certificadoId)
            .onSuccess { userPreferencesRepository.adicionarCursoConcluido(cursoId) }
    }
}
