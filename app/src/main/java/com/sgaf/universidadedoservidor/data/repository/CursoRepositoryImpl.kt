package com.sgaf.universidadedoservidor.data.repository

import com.sgaf.universidadedoservidor.data.local.dao.AulaDao
import com.sgaf.universidadedoservidor.data.local.dao.CursoDao
import com.sgaf.universidadedoservidor.data.local.dao.ModuloDao
import com.sgaf.universidadedoservidor.data.local.dao.ProgressoDao
import com.sgaf.universidadedoservidor.data.local.dao.SearchDao
import com.sgaf.universidadedoservidor.data.local.entities.AulaEntity
import com.sgaf.universidadedoservidor.data.local.entities.ProgressoEntity
import com.sgaf.universidadedoservidor.data.local.database.QuizPerguntaJson
import com.sgaf.universidadedoservidor.domain.model.Aula
import com.sgaf.universidadedoservidor.domain.model.Curso
import com.sgaf.universidadedoservidor.domain.model.Modulo
import com.sgaf.universidadedoservidor.domain.model.QuizPergunta
import com.sgaf.universidadedoservidor.domain.model.ResultadoBusca
import com.sgaf.universidadedoservidor.domain.repository.CursoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CursoRepositoryImpl @Inject constructor(
    private val cursoDao: CursoDao,
    private val moduloDao: ModuloDao,
    private val aulaDao: AulaDao,
    private val progressoDao: ProgressoDao,
    private val searchDao: SearchDao
) : CursoRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val respostasSerializer = MapSerializer(Int.serializer(), Int.serializer())

    override fun getCursos(): Flow<List<Curso>> {
        return combine(
            cursoDao.getCursos(),
            moduloDao.getModulos(),
            aulaDao.getAllAulas(),
            progressoDao.getAllProgresso()
        ) { cursos, modulos, aulas, progressos ->
            val progressMap = progressos.associateBy { it.aulaId }
            cursos.map { cursoEntity ->
                val cursoModulos = modulos.filter { it.cursoId == cursoEntity.id }.map { moduloEntity ->
                    val moduloAulas = aulas.filter { it.moduloId == moduloEntity.id }.map { aulaEntity ->
                        mapToDomainAula(aulaEntity, progressMap[aulaEntity.id])
                    }
                    Modulo(
                        id = moduloEntity.id,
                        titulo = moduloEntity.titulo,
                        descricao = moduloEntity.descricao,
                        aulas = moduloAulas
                    )
                }
                Curso(
                    id = cursoEntity.id,
                    titulo = cursoEntity.titulo,
                    descricao = cursoEntity.descricao,
                    modulos = cursoModulos,
                    isAvailable = cursoEntity.isAvailable
                )
            }
        }
    }

    override fun getCursoById(id: Int): Flow<Curso?> {
        return combine(
            cursoDao.getCursoById(id),
            moduloDao.getModulos(),
            aulaDao.getAllAulas(),
            progressoDao.getAllProgresso()
        ) { cursoEntity, modulos, aulas, progressos ->
            if (cursoEntity == null) return@combine null
            val progressMap = progressos.associateBy { it.aulaId }
            val cursoModulos = modulos.filter { it.cursoId == cursoEntity.id }.map { moduloEntity ->
                val moduloAulas = aulas.filter { it.moduloId == moduloEntity.id }.map { aulaEntity ->
                    mapToDomainAula(aulaEntity, progressMap[aulaEntity.id])
                }
                Modulo(
                    id = moduloEntity.id,
                    titulo = moduloEntity.titulo,
                    descricao = moduloEntity.descricao,
                    aulas = moduloAulas
                )
            }
            Curso(
                id = cursoEntity.id,
                titulo = cursoEntity.titulo,
                descricao = cursoEntity.descricao,
                modulos = cursoModulos,
                isAvailable = cursoEntity.isAvailable
            )
        }
    }

    override fun getModulos(): Flow<List<Modulo>> {
        return combine(
            moduloDao.getModulos(),
            aulaDao.getAllAulas(),
            progressoDao.getAllProgresso()
        ) { modulos, aulas, progressos ->
            val progressMap = progressos.associateBy { it.aulaId }
            modulos.map { moduloEntity ->
                val moduloAulas = aulas.filter { it.moduloId == moduloEntity.id }.map { aulaEntity ->
                    mapToDomainAula(aulaEntity, progressMap[aulaEntity.id])
                }
                Modulo(
                    id = moduloEntity.id,
                    titulo = moduloEntity.titulo,
                    descricao = moduloEntity.descricao,
                    aulas = moduloAulas
                )
            }
        }
    }

    override fun getAulasByModulo(moduloId: Int): Flow<List<Aula>> {
        return combine(
            aulaDao.getAulasByModulo(moduloId),
            progressoDao.getAllProgresso()
        ) { aulas, progressos ->
            val progressMap = progressos.associateBy { it.aulaId }
            aulas.map { mapToDomainAula(it, progressMap[it.id]) }
        }
    }

    override fun getAulaById(aulaId: Int): Flow<Aula?> {
        return combine(
            progressoDao.getProgressoForAulaFlow(aulaId),
            aulaDao.getAllAulas().map { aulas -> aulas.firstOrNull { it.id == aulaId } }
        ) { progresso, aulaEntity ->
            aulaEntity?.let { mapToDomainAula(it, progresso) }
        }
    }

    override suspend fun toggleFavorito(aulaId: Int) {
        val existing = progressoDao.getProgressoForAula(aulaId)
        if (existing != null) {
            progressoDao.saveProgresso(existing.copy(isFavorite = !existing.isFavorite))
        } else {
            progressoDao.saveProgresso(ProgressoEntity(aulaId = aulaId, isFavorite = true))
        }
    }

    override suspend fun marcarConcluida(aulaId: Int) {
        val existing = progressoDao.getProgressoForAula(aulaId)
        if (existing != null) {
            progressoDao.saveProgresso(existing.copy(isCompleted = true))
        } else {
            progressoDao.saveProgresso(ProgressoEntity(aulaId = aulaId, isCompleted = true))
        }
    }

    override suspend fun salvarResultadoQuiz(
        aulaId: Int,
        respostas: Map<Int, Int>,
        acertos: Int,
        aprovado: Boolean
    ) {
        val respostasJson = json.encodeToString(respostasSerializer, respostas)
        val existing = progressoDao.getProgressoForAula(aulaId)
        val base = existing ?: ProgressoEntity(aulaId = aulaId)
        progressoDao.saveProgresso(
            base.copy(
                quizSubmitted = true,
                quizAcertos = acertos,
                quizRespostasJson = respostasJson,
                // Conclusão só avança para true; nunca regride por uma submissão.
                isCompleted = base.isCompleted || aprovado
            )
        )
    }

    override suspend fun resetarQuiz(aulaId: Int) {
        val existing = progressoDao.getProgressoForAula(aulaId) ?: return
        progressoDao.saveProgresso(
            existing.copy(
                quizSubmitted = false,
                quizAcertos = 0,
                quizRespostasJson = ""
            )
        )
    }

    override suspend fun registrarAcesso(aulaId: Int) {
        val agora = System.currentTimeMillis()
        val existing = progressoDao.getProgressoForAula(aulaId)
        val base = existing ?: ProgressoEntity(aulaId = aulaId)
        progressoDao.saveProgresso(base.copy(ultimoAcessoEm = agora))
    }

    override suspend fun buscar(termo: String): List<ResultadoBusca> {
        val limpo = termo.trim()
        if (limpo.length < 2) return emptyList()
        return searchDao.buscar(limpo).map { row ->
            ResultadoBusca(
                aulaId = row.aulaId,
                aulaTitulo = row.aulaTitulo,
                moduloTitulo = row.moduloTitulo,
                cursoTitulo = row.cursoTitulo,
                trecho = extrairTrecho(row.conteudo, limpo)
            )
        }
    }

    /** Extrai um trecho do conteúdo ao redor da primeira ocorrência do termo (sem Markdown). */
    private fun extrairTrecho(conteudo: String, termo: String): String? {
        val texto = conteudo.replace(Regex("[#*`>\\-\\[\\]()]"), " ").replace(Regex("\\s+"), " ").trim()
        val idx = texto.indexOf(termo, ignoreCase = true)
        if (idx < 0) return null
        val inicio = (idx - 40).coerceAtLeast(0)
        val fim = (idx + termo.length + 60).coerceAtMost(texto.length)
        val prefixo = if (inicio > 0) "…" else ""
        val sufixo = if (fim < texto.length) "…" else ""
        return "$prefixo${texto.substring(inicio, fim)}$sufixo"
    }

    private fun mapToDomainAula(entity: AulaEntity, progresso: ProgressoEntity?): Aula {
        val quizList = try {
            json.decodeFromString<List<QuizPerguntaJson>>(entity.quizJson).map {
                QuizPergunta(
                    pergunta = it.pergunta,
                    opcoes = it.opcoes,
                    respostaCorretaIndex = it.respostaCorretaIndex
                )
            }
        } catch (e: Exception) {
            emptyList()
        }

        val respostas = progresso?.quizRespostasJson
            ?.takeIf { it.isNotEmpty() }
            ?.let {
                try {
                    json.decodeFromString(respostasSerializer, it)
                } catch (e: Exception) {
                    emptyMap()
                }
            } ?: emptyMap()

        return Aula(
            id = entity.id,
            titulo = entity.titulo,
            conteudo = entity.conteudo,
            isCompleted = progresso?.isCompleted ?: false,
            isFavorite = progresso?.isFavorite ?: false,
            quiz = quizList,
            quizRespostas = respostas,
            quizSubmitted = progresso?.quizSubmitted ?: false,
            ultimoAcessoEm = progresso?.ultimoAcessoEm ?: 0
        )
    }

}
