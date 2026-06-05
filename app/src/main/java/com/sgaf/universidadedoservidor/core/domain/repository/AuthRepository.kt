package com.sgaf.universidadedoservidor.core.domain.repository

interface AuthRepository {
    suspend fun loginAnonymously(): Result<Unit>
    fun getUserId(): String?
    fun isLoggedIn(): Boolean
}
