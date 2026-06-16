package com.sgaf.universidadedoservidor.ui.screens.desempenho

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sgaf.universidadedoservidor.core.data.preferences.UserPreferencesRepository
import com.sgaf.universidadedoservidor.domain.model.Curso
import com.sgaf.universidadedoservidor.domain.model.EstatisticasCurso
import com.sgaf.universidadedoservidor.domain.usecase.CalcularEstatisticasCursoUseCase
import com.sgaf.universidadedoservidor.domain.usecase.GetCursosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class DesempenhoUiState(
    val estatisticas: EstatisticasCurso? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class DesempenhoViewModel @Inject constructor(
    getCursosUseCase: GetCursosUseCase,
    userPreferencesRepository: UserPreferencesRepository,
    private val calcularEstatisticasCursoUseCase: CalcularEstatisticasCursoUseCase
) : ViewModel() {

    val uiState: StateFlow<DesempenhoUiState> = combine(
        getCursosUseCase(),
        userPreferencesRepository.cursoAtivoId
    ) { cursos, cursoAtivoId ->
        val ativo = resolverCursoAtivo(cursos, cursoAtivoId)
            ?: return@combine DesempenhoUiState(estatisticas = null, isLoading = false)
        DesempenhoUiState(
            estatisticas = calcularEstatisticasCursoUseCase(ativo),
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DesempenhoUiState()
    )

    private fun resolverCursoAtivo(cursos: List<Curso>, cursoAtivoId: Int?): Curso? {
        val escolhido = cursoAtivoId?.let { id -> cursos.firstOrNull { it.id == id && it.isAvailable } }
        return escolhido ?: cursos.firstOrNull { it.isAvailable }
    }
}
