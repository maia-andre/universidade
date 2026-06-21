package com.sgaf.universidadedoservidor.ui.screens.certificado

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sgaf.universidadedoservidor.domain.model.DesempenhoCurso
import com.sgaf.universidadedoservidor.domain.usecase.ObterCargaHorariaUseCase
import com.sgaf.universidadedoservidor.domain.usecase.RegistrarConclusaoUseCase
import com.sgaf.universidadedoservidor.domain.usecase.VerificarAprovacaoCursoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CertificadoViewModel @Inject constructor(
    verificarAprovacaoCursoUseCase: VerificarAprovacaoCursoUseCase,
    obterCargaHorariaUseCase: ObterCargaHorariaUseCase,
    private val registrarConclusaoUseCase: RegistrarConclusaoUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val cursoId: Int = savedStateHandle.get<Int>("cursoId") ?: 1

    val desempenho: StateFlow<DesempenhoCurso?> =
        verificarAprovacaoCursoUseCase(cursoId).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _cargaHoraria = MutableStateFlow<Int?>(null)
    /** Carga horária do curso (horas) para o certificado; null enquanto carrega ou se não definida. */
    val cargaHoraria: StateFlow<Int?> = _cargaHoraria.asStateFlow()

    init {
        viewModelScope.launch {
            _cargaHoraria.value = obterCargaHorariaUseCase(cursoId)
        }
    }

    /** Registra a conclusão no backend (upstream RH) ao emitir o certificado. Best-effort/offline-safe. */
    fun registrarConclusao(aproveitamento: Int) {
        viewModelScope.launch { registrarConclusaoUseCase(cursoId, aproveitamento) }
    }
}
