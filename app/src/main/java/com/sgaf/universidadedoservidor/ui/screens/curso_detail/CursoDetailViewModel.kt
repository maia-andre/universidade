package com.sgaf.universidadedoservidor.ui.screens.curso_detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sgaf.universidadedoservidor.core.data.preferences.UserPreferencesRepository
import com.sgaf.universidadedoservidor.domain.model.Curso
import com.sgaf.universidadedoservidor.domain.usecase.GetCursoDetailUseCase
import com.sgaf.universidadedoservidor.domain.usecase.GetResultadoProvaFinalUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CursoDetailState(
    val curso: Curso? = null,
    val isLoading: Boolean = true,
    val provaFinalAprovada: Boolean = false
)

@HiltViewModel
class CursoDetailViewModel @Inject constructor(
    private val getCursoDetailUseCase: GetCursoDetailUseCase,
    private val getResultadoProvaFinalUseCase: GetResultadoProvaFinalUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val cursoId: Int = savedStateHandle.get<Int>("cursoId") ?: 1

    init {
        // Entrar em um curso o torna o curso ativo do aluno (regra de 1 curso por vez).
        // Cursos bloqueados não são navegáveis, então só chegam aqui cursos disponíveis.
        viewModelScope.launch {
            userPreferencesRepository.setCursoAtivo(cursoId)
        }
    }

    val state: StateFlow<CursoDetailState> = combine(
        getCursoDetailUseCase(cursoId),
        getResultadoProvaFinalUseCase(cursoId)
    ) { curso, prova ->
        CursoDetailState(
            curso = curso,
            isLoading = false,
            provaFinalAprovada = prova?.aprovado == true
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CursoDetailState()
    )
}
