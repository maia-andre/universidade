package com.sgaf.universidadedoservidor.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.sgaf.universidadedoservidor.core.util.await
import com.sgaf.universidadedoservidor.domain.repository.PlataformaRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlataformaRepositoryImpl @Inject constructor() : PlataformaRepository {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    override suspend fun getCursoAtivoMatriculado(uid: String): Int? = runCatching {
        // Filtra por uid e escolhe a matrícula "ativa" em código (evita índice composto no Firestore).
        val snap = db.collection("matriculas").whereEqualTo("uid", uid).get().await()
        val matricula = snap.documents.firstOrNull { it.getString("status") == "ativa" }
            ?: snap.documents.firstOrNull()
        matricula?.getLong("cursoId")?.toInt()
    }.getOrNull()

    override suspend fun getCursosMatriculados(uid: String): List<Int> = runCatching {
        db.collection("matriculas").whereEqualTo("uid", uid).get().await()
            .documents
            .filter { it.getString("status") == "ativa" }
            .mapNotNull { it.getLong("cursoId")?.toInt() }
    }.getOrDefault(emptyList())

    override suspend fun getCursosConcluidos(uid: String): List<Int> = runCatching {
        db.collection("conclusoes").whereEqualTo("uid", uid).get().await()
            .documents
            .mapNotNull { it.getLong("cursoId")?.toInt() }
    }.getOrDefault(emptyList())

    override suspend fun getNomeServidor(uid: String): String? = runCatching {
        db.collection("servidores").document(uid).get().await().getString("nome")
    }.getOrNull()

    override suspend fun registrarConclusao(
        uid: String,
        cursoId: Int,
        nota: Int,
        certificadoId: String
    ): Result<Unit> = runCatching {
        val dados = mapOf(
            "uid" to uid,
            "cursoId" to cursoId,
            "nota" to nota,
            "certificadoId" to certificadoId,
            "concluidoEm" to FieldValue.serverTimestamp()
        )
        db.collection("conclusoes").document("${uid}_$cursoId")
            .set(dados, SetOptions.merge()).await()
        Unit
    }
}
