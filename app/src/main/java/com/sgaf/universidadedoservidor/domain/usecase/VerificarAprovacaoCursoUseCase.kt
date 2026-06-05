package com.sgaf.universidadedoservidor.domain.usecase

import com.sgaf.universidadedoservidor.data.local.dao.AulaDao
import com.sgaf.universidadedoservidor.data.local.dao.ProgressoDao
import com.sgaf.universidadedoservidor.core.utils.Constants
import kotlinx.coroutines.flow.first

class VerificarAprovacaoCursoUseCase(
    private val aulaDao: AulaDao,
    private val progressoDao: ProgressoDao
) {
    suspend operator fun invoke(cursoId: Int): Boolean {
        // Obter todas as aulas do curso (via modulos)
        // Simplificado: Assumiremos que a regra é Nota >= 70%
        // O progresso salva a pontuacao de cada aula.
        
        // Aqui deve buscar do banco o progresso. Como é mock:
        return true
    }
}
