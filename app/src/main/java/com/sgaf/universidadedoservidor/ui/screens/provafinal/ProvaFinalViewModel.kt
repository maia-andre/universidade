package com.sgaf.universidadedoservidor.ui.screens.provafinal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sgaf.universidadedoservidor.domain.model.QuizPergunta
import com.sgaf.universidadedoservidor.domain.usecase.GetResultadoProvaFinalUseCase
import com.sgaf.universidadedoservidor.domain.usecase.ObterProvaFinalUseCase
import com.sgaf.universidadedoservidor.domain.usecase.SubmeterProvaFinalUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProvaFinalUiState(
    val isLoading: Boolean = true,
    val cursoId: Int = 0,
    val perguntas: List<QuizPergunta> = emptyList(),
    val selectedAnswers: Map<Int, Int> = emptyMap(),
    val submitted: Boolean = false,
    val aprovado: Boolean = false,
    val acertos: Int = 0,
    val tentativas: Int = 0
)

@HiltViewModel
class ProvaFinalViewModel @Inject constructor(
    private val obterProvaFinalUseCase: ObterProvaFinalUseCase,
    private val submeterProvaFinalUseCase: SubmeterProvaFinalUseCase,
    private val getResultadoProvaFinalUseCase: GetResultadoProvaFinalUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val cursoId: Int = savedStateHandle.get<Int>("cursoId") ?: 1

    private val _state = MutableStateFlow(ProvaFinalUiState())
    val state: StateFlow<ProvaFinalUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val perguntas = obterProvaFinalUseCase(cursoId)
            val anterior = getResultadoProvaFinalUseCase(cursoId).first()
            _state.update {
                it.copy(
                    isLoading = false,
                    cursoId = cursoId,
                    perguntas = perguntas,
                    tentativas = anterior?.tentativas ?: 0
                )
            }
        }
    }

    fun selecionar(questaoIndex: Int, opcaoIndex: Int) {
        if (_state.value.submitted) return
        _state.update {
            it.copy(
                selectedAnswers = it.selectedAnswers.toMutableMap().apply { put(questaoIndex, opcaoIndex) }
            )
        }
    }

    fun enviar() {
        val atual = _state.value
        if (atual.submitted || atual.selectedAnswers.size < atual.perguntas.size) return
        viewModelScope.launch {
            val resultado = submeterProvaFinalUseCase(cursoId, atual.perguntas, atual.selectedAnswers)
            _state.update {
                it.copy(
                    submitted = true,
                    aprovado = resultado.aprovado,
                    acertos = resultado.acertos,
                    tentativas = it.tentativas + 1
                )
            }
        }
    }

    fun tentarNovamente() {
        _state.update {
            it.copy(submitted = false, selectedAnswers = emptyMap(), aprovado = false, acertos = 0)
        }
    }
}
