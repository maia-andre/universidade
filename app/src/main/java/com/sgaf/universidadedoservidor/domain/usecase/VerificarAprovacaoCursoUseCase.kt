package com.sgaf.universidadedoservidor.domain.usecase

import com.sgaf.universidadedoservidor.domain.model.DesempenhoCurso
import com.sgaf.universidadedoservidor.domain.repository.CursoRepository
import com.sgaf.universidadedoservidor.domain.repository.ProvaFinalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Calcula o desempenho do aluno no curso e determina a aprovação (v5).
 *
 * Regra de negócio: aprovado quando 100% das aulas estão concluídas E o aluno foi
 * aprovado na PROVA FINAL do curso. A nota exibida (acertos/aproveitamento) passa a
 * refletir a prova final — que é o instrumento de avaliação do curso.
 */
class VerificarAprovacaoCursoUseCase @Inject constructor(
    private val repository: CursoRepository,
    private val provaFinalRepository: ProvaFinalRepository
) {
    operator fun invoke(cursoId: Int): Flow<DesempenhoCurso?> =
        combine(
            repository.getCursoById(cursoId),
            provaFinalRepository.getResultado(cursoId)
        ) { curso, prova ->
            if (curso == null) return@combine null

            val aulas = curso.modulos.flatMap { it.aulas }
            val totalAulas = aulas.size
            val concluidas = aulas.count { it.isCompleted }
            val todasConcluidas = totalAulas > 0 && concluidas == totalAulas

            DesempenhoCurso(
                cursoTitulo = curso.titulo,
                totalAulas = totalAulas,
                aulasConcluidas = concluidas,
                totalQuestoes = prova?.totalQuestoes ?: 0,
                acertos = prova?.acertos ?: 0,
                percentualAcerto = prova?.percentual ?: 0f,
                aprovado = todasConcluidas && (prova?.aprovado == true)
            )
        }
}
