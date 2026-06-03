package com.sgaf.universidadedoservidor.domain.usecase

import com.sgaf.universidadedoservidor.domain.model.Aula
import com.sgaf.universidadedoservidor.domain.repository.CursoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAulaContentUseCase @Inject constructor(
    private val repository: CursoRepository
) {
    operator fun invoke(aulaId: Int): Flow<Aula?> = repository.getAulaById(aulaId)
}
