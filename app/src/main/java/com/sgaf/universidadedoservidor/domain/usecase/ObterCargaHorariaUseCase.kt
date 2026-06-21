package com.sgaf.universidadedoservidor.domain.usecase

import com.sgaf.universidadedoservidor.domain.repository.CursoRepository
import javax.inject.Inject

/** Carga horária (em horas) do curso, para exibir no certificado (v6). Null se não definida. */
class ObterCargaHorariaUseCase @Inject constructor(
    private val repository: CursoRepository
) {
    suspend operator fun invoke(cursoId: Int): Int? = repository.getCargaHoraria(cursoId)
}
