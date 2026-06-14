package com.sgaf.universidadedoservidor.domain.usecase

import com.sgaf.universidadedoservidor.domain.model.AvaliacaoCurso
import com.sgaf.universidadedoservidor.domain.repository.CursoRepository
import javax.inject.Inject

class SalvarAvaliacaoUseCase @Inject constructor(
    private val repository: CursoRepository
) {
    suspend operator fun invoke(avaliacao: AvaliacaoCurso) = repository.salvarAvaliacao(avaliacao)
}
