package com.sgaf.universidadedoservidor.ui.screens.ferramentas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sgaf.universidadedoservidor.domain.model.FerramentaPreenchida
import com.sgaf.universidadedoservidor.domain.model.TipoFerramenta
import com.sgaf.universidadedoservidor.domain.repository.FerramentaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class FerramentasUiState(
    val swot: List<FerramentaPreenchida> = emptyList(),
    val cincoWDoisH: List<FerramentaPreenchida> = emptyList()
)

@HiltViewModel
class FerramentasViewModel @Inject constructor(
    repository: FerramentaRepository
) : ViewModel() {

    val uiState: StateFlow<FerramentasUiState> = combine(
        repository.listar(TipoFerramenta.SWOT),
        repository.listar(TipoFerramenta.CINCO_W_DOIS_H)
    ) { swot, cinco ->
        FerramentasUiState(swot = swot, cincoWDoisH = cinco)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FerramentasUiState()
    )
}
