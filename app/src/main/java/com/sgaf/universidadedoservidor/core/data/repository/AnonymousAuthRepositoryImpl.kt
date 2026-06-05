package com.sgaf.universidadedoservidor.core.data.repository

import com.sgaf.universidadedoservidor.core.domain.repository.AuthRepository
import java.util.UUID

class AnonymousAuthRepositoryImpl : AuthRepository {
    private var mockUserId: String? = null

    override suspend fun loginAnonymously(): Result<Unit> {
        // Mocking anonymous login
        mockUserId = UUID.randomUUID().toString()
        return Result.success(Unit)
    }

    override fun getUserId(): String? {
        return mockUserId
    }

    override fun isLoggedIn(): Boolean {
        return mockUserId != null
    }
}
