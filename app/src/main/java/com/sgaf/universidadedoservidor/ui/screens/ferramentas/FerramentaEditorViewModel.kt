package com.sgaf.universidadedoservidor.ui.screens.ferramentas

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sgaf.universidadedoservidor.domain.model.TipoFerramenta
import com.sgaf.universidadedoservidor.domain.repository.FerramentaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditorUiState(
    val tipo: TipoFerramenta = TipoFerramenta.SWOT,
    val isLoading: Boolean = true,
    val tituloInicial: String = "",
    val camposIniciais: Map<String, String> = emptyMap(),
    val podeExcluir: Boolean = false,
    val concluido: Boolean = false
)

@HiltViewModel
class FerramentaEditorViewModel @Inject constructor(
    private val repository: FerramentaRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val tipo: TipoFerramenta =
        TipoFerramenta.fromName(savedStateHandle.get<String>("tipo") ?: TipoFerramenta.SWOT.name)
    private val ferramentaId: Long = savedStateHandle.get<Long>("ferramentaId") ?: 0L

    private val _state = MutableStateFlow(EditorUiState(tipo = tipo))
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val existente = if (ferramentaId != 0L) repository.obter(ferramentaId) else null
            _state.update {
                it.copy(
                    isLoading = false,
                    tituloInicial = existente?.titulo ?: "",
                    camposIniciais = existente?.campos ?: emptyMap(),
                    podeExcluir = existente != null
                )
            }
        }
    }

    fun salvar(titulo: String, campos: Map<String, String>) {
        viewModelScope.launch {
            repository.salvar(
                id = ferramentaId,
                tipo = tipo,
                titulo = titulo.trim().ifBlank { tipo.rotulo },
                campos = campos
            )
            _state.update { it.copy(concluido = true) }
        }
    }

    fun excluir() {
        viewModelScope.launch {
            repository.excluir(ferramentaId)
            _state.update { it.copy(concluido = true) }
        }
    }
}
