package com.sgaf.universidadedoservidor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sgaf.universidadedoservidor.core.data.preferences.ThemeMode
import com.sgaf.universidadedoservidor.core.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Estado de nível de app: tema e ajustes de acessibilidade (Itens 2.1 e v4/1). */
data class AppUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val highContrast: Boolean = false,
    val reducedMotion: Boolean = false,
    val fontScale: Float = 1f
)

@HiltViewModel
class MainViewModel @Inject constructor(
    userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val uiState: StateFlow<AppUiState> = combine(
        userPreferencesRepository.themeMode,
        userPreferencesRepository.highContrast,
        userPreferencesRepository.reducedMotion,
        userPreferencesRepository.fontScale
    ) { theme, contraste, movimento, fonte ->
        AppUiState(
            themeMode = theme,
            highContrast = contraste,
            reducedMotion = movimento,
            fontScale = fonte
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppUiState()
    )
}
