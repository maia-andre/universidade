package com.sgaf.universidadedoservidor.domain.usecase

import com.sgaf.universidadedoservidor.domain.model.AvaliacaoCurso
import com.sgaf.universidadedoservidor.domain.repository.CursoRepository
import javax.inject.Inject

class GetAvaliacaoUseCase @Inject constructor(
    private val repository: CursoRepository
) {
    suspend operator fun invoke(cursoId: Int): AvaliacaoCurso? = repository.getAvaliacao(cursoId)
}
