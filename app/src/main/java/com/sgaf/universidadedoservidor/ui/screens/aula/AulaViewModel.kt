package com.sgaf.universidadedoservidor.ui.screens.aula

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sgaf.universidadedoservidor.core.data.preferences.UserPreferencesRepository
import com.sgaf.universidadedoservidor.domain.model.Aula
import com.sgaf.universidadedoservidor.domain.usecase.GetAulaContentUseCase
import com.sgaf.universidadedoservidor.domain.usecase.RegistrarAcessoAulaUseCase
import com.sgaf.universidadedoservidor.domain.usecase.ResetarQuizUseCase
import com.sgaf.universidadedoservidor.domain.usecase.SalvarResultadoQuizUseCase
import com.sgaf.universidadedoservidor.domain.usecase.ToggleFavoritoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AulaUiState(
    val aula: Aula? = null,
    val isLoading: Boolean = true,
    val selectedAnswers: Map<Int, Int> = emptyMap(),
    val quizSubmitted: Boolean = false,
    val quizCorrect: Boolean = false,
    val showFeedback: Boolean = false,
    val fontScale: Float = 1f
)

@HiltViewModel
class AulaViewModel @Inject constructor(
    private val getAulaContentUseCase: GetAulaContentUseCase,
    private val toggleFavoritoUseCase: ToggleFavoritoUseCase,
    private val salvarResultadoQuizUseCase: SalvarResultadoQuizUseCase,
    private val resetarQuizUseCase: ResetarQuizUseCase,
    private val registrarAcessoAulaUseCase: RegistrarAcessoAulaUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val aulaId: Int = savedStateHandle.get<Int>("aulaId") ?: 101

    init {
        // Registra o acesso para alimentar o "continuar de onde parou" (Item 2.2).
        viewModelScope.launch {
            registrarAcessoAulaUseCase(aulaId)
        }
    }

    private data class QuizState(
        val selectedAnswers: Map<Int, Int>,
        val submitted: Boolean,
        val correct: Boolean,
        val showFeedback: Boolean
    )

    // null = ainda não houve interação nesta sessão; o estado exibido vem do que está persistido.
    private val _quizState = MutableStateFlow<QuizState?>(null)

    val state: StateFlow<AulaUiState> = combine(
        getAulaContentUseCase(aulaId),
        _quizState,
        userPreferencesRepository.fontScale
    ) { aula, quiz, fontScale ->
        // Sem interação local: reflete o que foi salvo no banco (quiz preenchido e travado, se já submetido).
        val effective = quiz ?: aula?.let { restoredStateFrom(it) } ?: EMPTY_QUIZ
        AulaUiState(
            aula = aula,
            isLoading = aula == null,
            selectedAnswers = effective.selectedAnswers,
            quizSubmitted = effective.submitted,
            quizCorrect = effective.correct,
            showFeedback = effective.showFeedback,
            fontScale = fontScale
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AulaUiState()
    )

    private fun restoredStateFrom(aula: Aula): QuizState {
        if (!aula.quizSubmitted) return EMPTY_QUIZ
        val correct = isAllCorrect(aula, aula.quizRespostas)
        return QuizState(
            selectedAnswers = aula.quizRespostas,
            submitted = true,
            correct = correct,
            showFeedback = true
        )
    }

    private fun isAllCorrect(aula: Aula, answers: Map<Int, Int>): Boolean {
        if (aula.quiz.isEmpty()) return false
        return aula.quiz.indices.all { answers[it] == aula.quiz[it].respostaCorretaIndex }
    }

    fun toggleFavorito() {
        viewModelScope.launch {
            toggleFavoritoUseCase(aulaId)
        }
    }

    fun selectAnswer(questionIndex: Int, optionIndex: Int) {
        val current = state.value
        if (current.quizSubmitted) return

        val updated = current.selectedAnswers.toMutableMap().apply {
            put(questionIndex, optionIndex)
        }
        _quizState.value = QuizState(
            selectedAnswers = updated,
            submitted = false,
            correct = false,
            showFeedback = false
        )
    }

    fun submitQuiz() {
        val current = state.value
        val aula = current.aula ?: return
        if (current.quizSubmitted) return
        if (current.selectedAnswers.size < aula.quiz.size) return

        val acertos = aula.quiz.indices.count {
            current.selectedAnswers[it] == aula.quiz[it].respostaCorretaIndex
        }
        val allCorrect = acertos == aula.quiz.size

        _quizState.value = QuizState(
            selectedAnswers = current.selectedAnswers,
            submitted = true,
            correct = allCorrect,
            showFeedback = true
        )

        viewModelScope.launch {
            salvarResultadoQuizUseCase(
                aulaId = aulaId,
                respostas = current.selectedAnswers,
                acertos = acertos,
                aprovado = allCorrect
            )
        }
    }

    fun resetQuiz() {
        _quizState.value = EMPTY_QUIZ
        viewModelScope.launch {
            resetarQuizUseCase(aulaId)
        }
    }

    fun aumentarFonte() = ajustarFonte(UserPreferencesRepository.FONT_SCALE_STEP)

    fun diminuirFonte() = ajustarFonte(-UserPreferencesRepository.FONT_SCALE_STEP)

    private fun ajustarFonte(delta: Float) {
        viewModelScope.launch {
            val atual = userPreferencesRepository.fontScale.first()
            userPreferencesRepository.setFontScale(atual + delta)
        }
    }

    companion object {
        private val EMPTY_QUIZ = QuizState(
            selectedAnswers = emptyMap(),
            submitted = false,
            correct = false,
            showFeedback = false
        )
    }
}
