package com.sgaf.universidadedoservidor.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sgaf.universidadedoservidor.domain.model.ResultadoBusca
import com.sgaf.universidadedoservidor.domain.usecase.BuscarConteudoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class SearchUiState(
    val resultados: List<ResultadoBusca> = emptyList(),
    val buscando: Boolean = false,
    val semResultados: Boolean = false
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val buscarConteudoUseCase: BuscarConteudoUseCase
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val uiState: StateFlow<SearchUiState> = _query
        .debounce(300)
        .distinctUntilChanged()
        .mapLatest { termo ->
            val limpo = termo.trim()
            if (limpo.length < 2) {
                SearchUiState()
            } else {
                val resultados = buscarConteudoUseCase(limpo)
                SearchUiState(
                    resultados = resultados,
                    buscando = false,
                    semResultados = resultados.isEmpty()
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SearchUiState()
        )

    fun onQueryChange(novo: String) {
        _query.value = novo
    }
}
