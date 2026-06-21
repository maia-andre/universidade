package com.sgaf.universidadedoservidor.domain.repository

import com.sgaf.universidadedoservidor.domain.model.QuizPergunta
import com.sgaf.universidadedoservidor.domain.model.ResultadoProvaFinal
import kotlinx.coroutines.flow.Flow

/**
 * Repositório dedicado à prova final (v5), separado do [CursoRepository] para não inchá-lo.
 * Perguntas vêm dos assets (chegam a qualquer instalação sem re-seed); o resultado fica no Room.
 */
interface ProvaFinalRepository {
    /** Perguntas da prova final do curso. Lista vazia quando o curso não tem prova cadastrada. */
    suspend fun getPerguntas(cursoId: Int): List<QuizPergunta>

    /** Último resultado salvo da prova final do curso, reativo (null se nunca foi feita). */
    fun getResultado(cursoId: Int): Flow<ResultadoProvaFinal?>

    /** Persiste o resultado de uma tentativa, incrementando o contador de tentativas. */
    suspend fun salvarResultado(
        cursoId: Int,
        respostas: Map<Int, Int>,
        acertos: Int,
        totalQuestoes: Int,
        aprovado: Boolean
    )
}
