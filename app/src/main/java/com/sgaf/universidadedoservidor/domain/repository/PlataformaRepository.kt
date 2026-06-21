package com.sgaf.universidadedoservidor.domain.repository

/**
 * Acesso ao backend da plataforma (Firestore) do lado do app cliente (v6).
 * Lê a matrícula liberada pelo RH (downstream) e grava as conclusões (upstream).
 */
interface PlataformaRepository {

    /** Curso liberado para o servidor (matrícula "ativa"), ou null se não houver. */
    suspend fun getCursoAtivoMatriculado(uid: String): Int?

    /** Todos os cursos com matrícula "ativa" liberada pelo RH (v7, Item 1). */
    suspend fun getCursosMatriculados(uid: String): List<Int>

    /** Todos os cursos que o servidor já concluiu (registro durável no Firestore) (v7, Item 1). */
    suspend fun getCursosConcluidos(uid: String): List<Int>

    /** Nome do servidor no cadastro (para o certificado), ou null. */
    suspend fun getNomeServidor(uid: String): String?

    /** Registra a conclusão de um curso (upstream para o RH). */
    suspend fun registrarConclusao(
        uid: String,
        cursoId: Int,
        nota: Int,
        certificadoId: String
    ): Result<Unit>
}
