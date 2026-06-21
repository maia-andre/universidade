package com.sgaf.universidadedoservidor.domain.usecase

import com.sgaf.universidadedoservidor.core.data.preferences.UserPreferencesRepository
import com.sgaf.universidadedoservidor.domain.model.AcessoCurso
import com.sgaf.universidadedoservidor.domain.model.CursoComAcesso
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Catálogo de cursos com o estado de acesso de cada um resolvido (v7, Item 1):
 * combina os cursos com os conjuntos locais de matriculados e concluídos
 * (`acessível = matriculado OU concluído`). Reage a mudanças nos três.
 */
class GetCursosComAcessoUseCase @Inject constructor(
    private val getCursosUseCase: GetCursosUseCase,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    operator fun invoke(): Flow<List<CursoComAcesso>> = combine(
        getCursosUseCase(),
        userPreferencesRepository.cursosMatriculados,
        userPreferencesRepository.cursosConcluidos
    ) { cursos, matriculados, concluidos ->
        cursos.map { curso ->
            val acesso = when {
                curso.id in concluidos -> AcessoCurso.CONCLUIDO
                curso.id in matriculados -> AcessoCurso.ATIVO
                else -> AcessoCurso.BLOQUEADO
            }
            CursoComAcesso(curso, acesso)
        }
    }
}
