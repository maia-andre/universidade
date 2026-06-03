package com.sgaf.universidadedoservidor.ui.screens.curso_detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sgaf.universidadedoservidor.domain.model.Curso
import com.sgaf.universidadedoservidor.domain.usecase.GetCursoDetailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class CursoDetailState(
    val curso: Curso? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class CursoDetailViewModel @Inject constructor(
    private val getCursoDetailUseCase: GetCursoDetailUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val cursoId: Int = savedStateHandle.get<Int>("cursoId") ?: 1

    val state: StateFlow<CursoDetailState> = getCursoDetailUseCase(cursoId).map { curso ->
        CursoDetailState(curso = curso, isLoading = false)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CursoDetailState()
    )
}
