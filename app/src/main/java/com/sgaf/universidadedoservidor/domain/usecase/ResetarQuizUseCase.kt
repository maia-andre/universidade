package com.sgaf.universidadedoservidor.domain.usecase

import com.sgaf.universidadedoservidor.domain.repository.CursoRepository
import javax.inject.Inject

class ResetarQuizUseCase @Inject constructor(
    private val repository: CursoRepository
) {
    suspend operator fun invoke(aulaId: Int) = repository.resetarQuiz(aulaId)
}
