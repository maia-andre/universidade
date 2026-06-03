package com.sgaf.universidadedoservidor.ui.screens.aula

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sgaf.universidadedoservidor.domain.model.Aula
import com.sgaf.universidadedoservidor.domain.usecase.GetAulaContentUseCase
import com.sgaf.universidadedoservidor.domain.usecase.MarcarConclusaoUseCase
import com.sgaf.universidadedoservidor.domain.usecase.ToggleFavoritoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AulaUiState(
    val aula: Aula? = null,
    val isLoading: Boolean = true,
    val selectedAnswers: Map<Int, Int> = emptyMap(),
    val quizSubmitted: Boolean = false,
    val quizCorrect: Boolean = false,
    val showFeedback: Boolean = false
)

@HiltViewModel
class AulaViewModel @Inject constructor(
    private val getAulaContentUseCase: GetAulaContentUseCase,
    private val toggleFavoritoUseCase: ToggleFavoritoUseCase,
    private val marcarConclusaoUseCase: MarcarConclusaoUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val aulaId: Int = savedStateHandle.get<Int>("aulaId") ?: 101

    private val _quizState = MutableStateFlow(
        QuizState(
            selectedAnswers = emptyMap(),
            submitted = false,
            correct = false,
            showFeedback = false
        )
    )

    private data class QuizState(
        val selectedAnswers: Map<Int, Int>,
        val submitted: Boolean,
        val correct: Boolean,
        val showFeedback: Boolean
    )

    val state: StateFlow<AulaUiState> = combine(
        getAulaContentUseCase(aulaId),
        _quizState
    ) { aula, quiz ->
        AulaUiState(
            aula = aula,
            isLoading = aula == null,
            selectedAnswers = quiz.selectedAnswers,
            quizSubmitted = quiz.submitted,
            quizCorrect = quiz.correct,
            showFeedback = quiz.showFeedback
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AulaUiState()
    )

    fun toggleFavorito() {
        viewModelScope.launch {
            toggleFavoritoUseCase(aulaId)
        }
    }

    fun selectAnswer(questionIndex: Int, optionIndex: Int) {
        val current = _quizState.value
        if (current.submitted) return
        
        val updated = current.selectedAnswers.toMutableMap().apply {
            put(questionIndex, optionIndex)
        }
        _quizState.value = current.copy(selectedAnswers = updated)
    }

    fun submitQuiz() {
        val current = _quizState.value
        val aula = state.value.aula ?: return
        
        if (current.selectedAnswers.size < aula.quiz.size) return
        
        var allCorrect = true
        aula.quiz.forEachIndexed { index, question ->
            val selected = current.selectedAnswers[index]
            if (selected != question.respostaCorretaIndex) {
                allCorrect = false
            }
        }
        
        _quizState.value = current.copy(
            submitted = true,
            correct = allCorrect,
            showFeedback = true
        )
        
        if (allCorrect) {
            viewModelScope.launch {
                marcarConclusaoUseCase(aulaId)
            }
        }
    }

    fun resetQuiz() {
        _quizState.value = QuizState(
            selectedAnswers = emptyMap(),
            submitted = false,
            correct = false,
            showFeedback = false
        )
    }
}
