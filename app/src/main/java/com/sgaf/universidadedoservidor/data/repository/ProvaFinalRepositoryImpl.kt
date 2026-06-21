package com.sgaf.universidadedoservidor.data.repository

import android.content.Context
import com.sgaf.universidadedoservidor.data.local.dao.ProvaFinalDao
import com.sgaf.universidadedoservidor.data.local.database.QuizPerguntaJson
import com.sgaf.universidadedoservidor.data.local.entities.ProvaFinalResultadoEntity
import com.sgaf.universidadedoservidor.domain.model.QuizPergunta
import com.sgaf.universidadedoservidor.domain.model.ResultadoProvaFinal
import com.sgaf.universidadedoservidor.domain.repository.ProvaFinalRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProvaFinalRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val provaFinalDao: ProvaFinalDao
) : ProvaFinalRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val respostasSerializer = MapSerializer(Int.serializer(), Int.serializer())

    // As perguntas são imutáveis (assets) — carrega uma vez e mantém em cache.
    @Volatile
    private var perguntasCache: Map<Int, List<QuizPergunta>>? = null
    private val mutex = Mutex()

    override suspend fun getPerguntas(cursoId: Int): List<QuizPergunta> =
        carregarPerguntas()[cursoId].orEmpty()

    private suspend fun carregarPerguntas(): Map<Int, List<QuizPergunta>> {
        perguntasCache?.let { return it }
        return mutex.withLock {
            perguntasCache ?: run {
                val texto = withContext(Dispatchers.IO) {
                    context.assets.open("curso_data.json").bufferedReader().use { it.readText() }
                }
                val cursos = json.decodeFromString(
                    ListSerializer(ProvaCursoJson.serializer()), texto
                )
                cursos.associate { curso ->
                    curso.id to curso.provaFinal.map {
                        QuizPergunta(it.pergunta, it.opcoes, it.respostaCorretaIndex)
                    }
                }.also { perguntasCache = it }
            }
        }
    }

    override fun getResultado(cursoId: Int): Flow<ResultadoProvaFinal?> =
        provaFinalDao.getResultadoFlow(cursoId).map { it?.toDomain() }

    override suspend fun salvarResultado(
        cursoId: Int,
        respostas: Map<Int, Int>,
        acertos: Int,
        totalQuestoes: Int,
        aprovado: Boolean
    ) {
        val anterior = provaFinalDao.getResultado(cursoId)
        provaFinalDao.upsert(
            ProvaFinalResultadoEntity(
                cursoId = cursoId,
                respostasJson = json.encodeToString(respostasSerializer, respostas),
                acertos = acertos,
                totalQuestoes = totalQuestoes,
                aprovado = aprovado,
                tentativas = (anterior?.tentativas ?: 0) + 1,
                atualizadoEm = System.currentTimeMillis()
            )
        )
    }

    private fun ProvaFinalResultadoEntity.toDomain(): ResultadoProvaFinal {
        val respostas = respostasJson.takeIf { it.isNotEmpty() }?.let {
            try {
                json.decodeFromString(respostasSerializer, it)
            } catch (e: Exception) {
                emptyMap()
            }
        } ?: emptyMap()
        return ResultadoProvaFinal(
            cursoId = cursoId,
            acertos = acertos,
            totalQuestoes = totalQuestoes,
            aprovado = aprovado,
            tentativas = tentativas,
            respostas = respostas
        )
    }
}

/** DTO para ler apenas a prova final de cada curso do curso_data.json (ignora o resto). */
@Serializable
private data class ProvaCursoJson(
    val id: Int,
    val provaFinal: List<QuizPerguntaJson> = emptyList()
)
