package com.sgaf.universidadedoservidor.ui.screens.certificado

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sgaf.universidadedoservidor.domain.model.DesempenhoCurso
import com.sgaf.universidadedoservidor.domain.usecase.VerificarAprovacaoCursoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class CertificadoViewModel @Inject constructor(
    verificarAprovacaoCursoUseCase: VerificarAprovacaoCursoUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val cursoId: Int = savedStateHandle.get<Int>("cursoId") ?: 1

    val desempenho: StateFlow<DesempenhoCurso?> =
        verificarAprovacaoCursoUseCase(cursoId).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
}
