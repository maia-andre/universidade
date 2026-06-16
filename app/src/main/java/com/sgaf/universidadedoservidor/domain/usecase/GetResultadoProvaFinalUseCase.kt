package com.sgaf.universidadedoservidor.domain.usecase

import com.sgaf.universidadedoservidor.domain.model.ResultadoProvaFinal
import com.sgaf.universidadedoservidor.domain.repository.ProvaFinalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetResultadoProvaFinalUseCase @Inject constructor(
    private val repository: ProvaFinalRepository
) {
    operator fun invoke(cursoId: Int): Flow<ResultadoProvaFinal?> =
        repository.getResultado(cursoId)
}
