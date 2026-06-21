package com.sgaf.universidadedoservidor.ui.screens.cursos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sgaf.universidadedoservidor.domain.model.CursoComAcesso
import com.sgaf.universidadedoservidor.domain.usecase.GetCursosComAcessoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class CursosState(
    val cursos: List<CursoComAcesso> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class CursosViewModel @Inject constructor(
    getCursosComAcessoUseCase: GetCursosComAcessoUseCase
) : ViewModel() {

    val state: StateFlow<CursosState> = getCursosComAcessoUseCase().map { list ->
        CursosState(cursos = list, isLoading = false)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CursosState()
    )
}
