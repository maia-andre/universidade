package com.sgaf.universidadedoservidor.domain.usecase

import com.sgaf.universidadedoservidor.domain.repository.CursoRepository
import javax.inject.Inject

class SalvarResultadoQuizUseCase @Inject constructor(
    private val repository: CursoRepository
) {
    suspend operator fun invoke(
        aulaId: Int,
        respostas: Map<Int, Int>,
        acertos: Int,
        aprovado: Boolean
    ) = repository.salvarResultadoQuiz(aulaId, respostas, acertos, aprovado)
}
