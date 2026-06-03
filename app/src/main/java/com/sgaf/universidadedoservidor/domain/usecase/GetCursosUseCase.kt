package com.sgaf.universidadedoservidor.domain.usecase

import com.sgaf.universidadedoservidor.domain.model.Curso
import com.sgaf.universidadedoservidor.domain.repository.CursoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCursosUseCase @Inject constructor(
    private val repository: CursoRepository
) {
    operator fun invoke(): Flow<List<Curso>> = repository.getCursos()
}
