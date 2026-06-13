package com.sgaf.universidadedoservidor.domain.repository

import com.sgaf.universidadedoservidor.domain.model.Curso
import com.sgaf.universidadedoservidor.domain.model.Modulo
import com.sgaf.universidadedoservidor.domain.model.Aula
import kotlinx.coroutines.flow.Flow

interface CursoRepository {
    fun getCursos(): Flow<List<Curso>>
    fun getCursoById(id: Int): Flow<Curso?>
    fun getModulos(): Flow<List<Modulo>>
    fun getAulasByModulo(moduloId: Int): Flow<List<Aula>>
    fun getAulaById(aulaId: Int): Flow<Aula?>
    suspend fun toggleFavorito(aulaId: Int)
    suspend fun marcarConcluida(aulaId: Int)

    /** Persiste o resultado de uma submissão de quiz. Se [aprovado], marca a aula como concluída. */
    suspend fun salvarResultadoQuiz(
        aulaId: Int,
        respostas: Map<Int, Int>,
        acertos: Int,
        aprovado: Boolean
    )

    /** Limpa o estado submetido do quiz (botão "Tentar Novamente"). Não altera a conclusão. */
    suspend fun resetarQuiz(aulaId: Int)
}
