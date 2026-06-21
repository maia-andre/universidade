package com.sgaf.universidadedoservidor.domain.usecase

import com.sgaf.universidadedoservidor.core.domain.repository.AuthRepository
import com.sgaf.universidadedoservidor.domain.repository.PlataformaRepository
import javax.inject.Inject

/**
 * Nome do servidor logado, do cadastro do RH (Firestore), para preencher o certificado
 * automaticamente (v7, Item 4). Null se não houver usuário, cadastro ou rede — a UI mantém
 * o campo digitável como fallback offline.
 */
class ObterNomeServidorUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val plataformaRepository: PlataformaRepository
) {
    suspend operator fun invoke(): String? {
        val uid = authRepository.getUserId() ?: return null
        return plataformaRepository.getNomeServidor(uid)
    }
}
