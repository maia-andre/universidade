package com.sgaf.universidadedoservidor.data.repository

import com.sgaf.universidadedoservidor.data.local.dao.AulaDao
import com.sgaf.universidadedoservidor.data.local.dao.ModuloDao
import com.sgaf.universidadedoservidor.data.local.dao.ProgressoDao
import com.sgaf.universidadedoservidor.data.local.entities.AulaEntity
import com.sgaf.universidadedoservidor.data.local.entities.ProgressoEntity
import com.sgaf.universidadedoservidor.data.local.database.QuizPerguntaJson
import com.sgaf.universidadedoservidor.domain.model.Aula
import com.sgaf.universidadedoservidor.domain.model.Curso
import com.sgaf.universidadedoservidor.domain.model.Modulo
import com.sgaf.universidadedoservidor.domain.model.QuizPergunta
import com.sgaf.universidadedoservidor.domain.repository.CursoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CursoRepositoryImpl @Inject constructor(
    private val moduloDao: ModuloDao,
    private val aulaDao: AulaDao,
    private val progressoDao: ProgressoDao
) : CursoRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override fun getCursos(): Flow<List<Curso>> {
        return getCursoById(1).map { supervisoresCurso ->
            val list = mutableListOf<Curso>()
            supervisoresCurso?.let { list.add(it) }
            list.addAll(getMockCourses())
            list
        }
    }

    override fun getCursoById(id: Int): Flow<Curso?> {
        if (id != 1) return kotlinx.coroutines.flow.flowOf(null)
        
        return combine(
            moduloDao.getModulos(),
            aulaDao.getAllAulas(),
            progressoDao.getAllProgresso()
        ) { modulos, aulas, progressos ->
            val progressMap = progressos.associateBy { it.aulaId }
            val domainModulos = modulos.map { moduloEntity ->
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
                id = 1,
                titulo = "Curso de Supervisores",
                descricao = "Curso essencial de capacitação para supervisores da Prefeitura Municipal de São José dos Campos.",
                modulos = domainModulos,
                isAvailable = true
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

        return Aula(
            id = entity.id,
            titulo = entity.titulo,
            conteudo = entity.conteudo,
            isCompleted = progresso?.isCompleted ?: false,
            isFavorite = progresso?.isFavorite ?: false,
            quiz = quizList
        )
    }

    private fun getMockCourses(): List<Curso> {
        return listOf(
            Curso(2, "Curso de Excel", "Domine planilhas de nível intermediário a avançado.", isAvailable = false),
            Curso(3, "Planejamento Estratégico", "Planejamento e metas para o setor público municipal.", isAvailable = false),
            Curso(4, "Curso de Formação de Diretores", "Liderança e gestão para novos diretores da prefeitura.", isAvailable = false),
            Curso(5, "Curso de Formação de Chefes de Divisão", "Coordenação prática de equipes de divisão.", isAvailable = false),
            Curso(6, "Curso de Sustentabilidade", "Práticas ecológicas e sustentabilidade no ambiente público.", isAvailable = false),
            Curso(7, "Conhecendo o SUS", "Introdução ao funcionamento e diretrizes do Sistema Único de Saúde.", isAvailable = false),
            Curso(8, "Educação 5.0", "Inovação tecnológica e novas diretrizes de ensino municipal.", isAvailable = false)
        )
    }
}
