package com.sgaf.universidadedoservidor.domain.usecase

import com.sgaf.universidadedoservidor.domain.model.ResultadoBusca
import com.sgaf.universidadedoservidor.domain.repository.CursoRepository
import javax.inject.Inject

class BuscarConteudoUseCase @Inject constructor(
    private val repository: CursoRepository
) {
    suspend operator fun invoke(termo: String): List<ResultadoBusca> = repository.buscar(termo)
}
