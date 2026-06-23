package com.sgaf.universidadedoservidor.domain.repository

import com.sgaf.universidadedoservidor.domain.model.Curso
import com.sgaf.universidadedoservidor.domain.model.Modulo
import com.sgaf.universidadedoservidor.domain.model.Aula
import com.sgaf.universidadedoservidor.domain.model.AvaliacaoCurso
import com.sgaf.universidadedoservidor.domain.model.ResultadoBusca
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

    /** Registra o instante do último acesso à aula (Item 2.2). */
    suspend fun registrarAcesso(aulaId: Int)

    /** Busca global por termo em cursos/módulos/aulas disponíveis (Item 3). */
    suspend fun buscar(termo: String): List<ResultadoBusca>

    /** Salva (ou substitui) a avaliação Likert do curso (v4 Item 3.3). */
    suspend fun salvarAvaliacao(avaliacao: AvaliacaoCurso)

    /** Avaliação previamente enviada para o curso, se houver. */
    suspend fun getAvaliacao(cursoId: Int): AvaliacaoCurso?

    /**
     * Carga horária do curso (em horas), lida do conteúdo em runtime (arquivo remoto sincronizado
     * ou baseline do APK) — assim chega a instalações já publicadas sem re-seed (v6, certificado).
     */
    suspend fun getCargaHoraria(cursoId: Int): Int?

    /**
     * Aplica um catálogo publicado pelo RH (V8 Item 1): persiste o JSON localmente e reconstrói o
     * Room (full-replace) preservando o progresso. No-op se o JSON for inválido/vazio.
     */
    suspend fun aplicarConteudoRemoto(jsonCatalogo: String)
}
