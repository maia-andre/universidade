package com.sgaf.universidadedoservidor.ui.screens.acessibilidade

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sgaf.universidadedoservidor.core.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AcessibilidadeUiState(
    val fontScale: Float = 1f,
    val highContrast: Boolean = false,
    val reducedMotion: Boolean = false
)

@HiltViewModel
class AcessibilidadeViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val uiState: StateFlow<AcessibilidadeUiState> = combine(
        userPreferencesRepository.fontScale,
        userPreferencesRepository.highContrast,
        userPreferencesRepository.reducedMotion
    ) { fonte, contraste, movimento ->
        AcessibilidadeUiState(fonte, contraste, movimento)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AcessibilidadeUiState()
    )

    fun aumentarFonte() = ajustarFonte(UserPreferencesRepository.FONT_SCALE_STEP)

    fun diminuirFonte() = ajustarFonte(-UserPreferencesRepository.FONT_SCALE_STEP)

    private fun ajustarFonte(delta: Float) {
        viewModelScope.launch {
            val atual = userPreferencesRepository.fontScale.first()
            userPreferencesRepository.setFontScale(atual + delta)
        }
    }

    fun setHighContrast(ativo: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setHighContrast(ativo) }
    }

    fun setReducedMotion(ativo: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setReducedMotion(ativo) }
    }
}
