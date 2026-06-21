package com.sgaf.universidadedoservidor.core.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Contrato de autenticação. A implementação real (Firebase, v6) substitui o mock anterior
 * sem mexer em UI/ViewModels — propósito da preparação desde a V2.
 */
interface AuthRepository {

    /** Estado de autenticação, reativo (true = há usuário logado). */
    val estaLogado: Flow<Boolean>

    fun isLoggedIn(): Boolean

    /** UID do usuário autenticado (uid do Firebase), ou null. */
    fun getUserId(): String?

    /** Login com e-mail/senha (conta provisionada pelo Painel RH). */
    suspend fun login(email: String, senha: String): Result<Unit>

    /** Dispara o e-mail de redefinição de senha. */
    suspend fun enviarResetSenha(email: String): Result<Unit>

    /**
     * Troca a senha do usuário logado (v7, Item 2). Reautentica com a senha atual antes
     * (o Firebase exige login recente para [updatePassword]).
     */
    suspend fun trocarSenha(senhaAtual: String, novaSenha: String): Result<Unit>

    fun logout()
}
