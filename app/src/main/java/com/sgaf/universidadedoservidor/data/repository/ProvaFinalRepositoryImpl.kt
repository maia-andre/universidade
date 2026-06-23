package com.sgaf.universidadedoservidor.data.repository

import com.sgaf.universidadedoservidor.data.local.ConteudoLocalSource
import com.sgaf.universidadedoservidor.data.local.dao.ProvaFinalDao
import com.sgaf.universidadedoservidor.data.local.entities.ProvaFinalResultadoEntity
import com.sgaf.universidadedoservidor.domain.model.QuizPergunta
import com.sgaf.universidadedoservidor.domain.model.ResultadoProvaFinal
import com.sgaf.universidadedoservidor.domain.repository.ProvaFinalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProvaFinalRepositoryImpl @Inject constructor(
    private val conteudoLocalSource: ConteudoLocalSource,
    private val provaFinalDao: ProvaFinalDao
) : ProvaFinalRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val respostasSerializer = MapSerializer(Int.serializer(), Int.serializer())

    // As perguntas vêm do conteúdo em runtime (arquivo remoto sincronizado ou baseline do APK),
    // via ConteudoLocalSource — chegam às instalações já publicadas sem re-seed (V8 Item 1).
    override suspend fun getPerguntas(cursoId: Int): List<QuizPergunta> =
        conteudoLocalSource.catalogo().firstOrNull { it.id == cursoId }
            ?.provaFinal
            ?.map { QuizPergunta(it.pergunta, it.opcoes, it.respostaCorretaIndex) }
            .orEmpty()

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
