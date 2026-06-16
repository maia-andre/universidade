package com.sgaf.universidadedoservidor.ui.screens.avaliacao

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sgaf.universidadedoservidor.domain.model.AvaliacaoCurso
import com.sgaf.universidadedoservidor.domain.usecase.GetAvaliacaoUseCase
import com.sgaf.universidadedoservidor.domain.usecase.SalvarAvaliacaoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AvaliacaoUiState(
    val isLoading: Boolean = true,
    val jaAvaliada: Boolean = false,
    val respostasIniciais: List<Int> = listOf(0, 0, 0, 0, 0),
    val gostouInicial: String = "",
    val sugestoesIniciais: String = "",
    val concluido: Boolean = false
)

@HiltViewModel
class AvaliacaoViewModel @Inject constructor(
    private val getAvaliacaoUseCase: GetAvaliacaoUseCase,
    private val salvarAvaliacaoUseCase: SalvarAvaliacaoUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val cursoId: Int = savedStateHandle.get<Int>("cursoId") ?: 1

    private val _state = MutableStateFlow(AvaliacaoUiState())
    val state: StateFlow<AvaliacaoUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val existente = getAvaliacaoUseCase(cursoId)
            _state.update {
                if (existente != null) {
                    it.copy(
                        isLoading = false,
                        jaAvaliada = true,
                        respostasIniciais = existente.respostas,
                        gostouInicial = existente.oQueMaisGostou,
                        sugestoesIniciais = existente.sugestoes
                    )
                } else {
                    it.copy(isLoading = false)
                }
            }
        }
    }

    fun enviar(respostas: List<Int>, gostou: String, sugestoes: String) {
        viewModelScope.launch {
            salvarAvaliacaoUseCase(
                AvaliacaoCurso(
                    cursoId = cursoId,
                    respostas = respostas,
                    oQueMaisGostou = gostou.trim(),
                    sugestoes = sugestoes.trim()
                )
            )
            _state.update { it.copy(concluido = true) }
        }
    }
}
