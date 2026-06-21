package com.sgaf.universidadedoservidor.core.data.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.sgaf.universidadedoservidor.core.domain.repository.AuthRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Implementação real de [AuthRepository] sobre o Firebase Authentication (e-mail/senha).
 * As contas são criadas pelo Painel RH (Admin SDK); aqui o app apenas autentica.
 */
@Singleton
class FirebaseAuthRepositoryImpl @Inject constructor() : AuthRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    override val estaLogado: Flow<Boolean> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser != null) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override fun isLoggedIn(): Boolean = auth.currentUser != null

    override fun getUserId(): String? = auth.currentUser?.uid

    override suspend fun login(email: String, senha: String): Result<Unit> = runCatching {
        auth.signInWithEmailAndPassword(email.trim(), senha).await()
        Unit
    }

    override suspend fun enviarResetSenha(email: String): Result<Unit> = runCatching {
        auth.sendPasswordResetEmail(email.trim()).await()
        Unit
    }

    override fun logout() = auth.signOut()
}

/** Aguarda um [Task] do Play Services como coroutine (evita depender de play-services-coroutines). */
private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
}
