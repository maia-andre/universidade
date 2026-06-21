package com.sgaf.universidadedoservidor.domain.usecase

import com.sgaf.universidadedoservidor.domain.model.QuizPergunta
import com.sgaf.universidadedoservidor.domain.repository.ProvaFinalRepository
import javax.inject.Inject

class ObterProvaFinalUseCase @Inject constructor(
    private val repository: ProvaFinalRepository
) {
    suspend operator fun invoke(cursoId: Int): List<QuizPergunta> =
        repository.getPerguntas(cursoId)
}
