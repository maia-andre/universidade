package com.sgaf.universidadedoservidor.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sgaf.universidadedoservidor.core.data.preferences.ThemeMode
import com.sgaf.universidadedoservidor.core.data.preferences.UserPreferencesRepository
import com.sgaf.universidadedoservidor.core.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Estado do fluxo de troca de senha em Configurações (v7, Item 2). */
data class TrocaSenhaUiState(
    val carregando: Boolean = false,
    val erro: String? = null,
    val sucesso: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = userPreferencesRepository.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ThemeMode.SYSTEM
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            userPreferencesRepository.setThemeMode(mode)
        }
    }

    private val _trocaSenha = MutableStateFlow(TrocaSenhaUiState())
    val trocaSenha: StateFlow<TrocaSenhaUiState> = _trocaSenha.asStateFlow()

    /** Troca a senha do aluno logado, validando localmente antes (v7, Item 2). */
    fun trocarSenha(senhaAtual: String, novaSenha: String, confirmacao: String) {
        when {
            senhaAtual.isBlank() -> {
                _trocaSenha.value = TrocaSenhaUiState(erro = "Informe a senha atual.")
                return
            }
            novaSenha.length < 6 -> {
                _trocaSenha.value = TrocaSenhaUiState(erro = "A nova senha deve ter ao menos 6 caracteres.")
                return
            }
            novaSenha != confirmacao -> {
                _trocaSenha.value = TrocaSenhaUiState(erro = "A confirmação não confere.")
                return
            }
        }
        viewModelScope.launch {
            _trocaSenha.value = TrocaSenhaUiState(carregando = true)
            authRepository.trocarSenha(senhaAtual, novaSenha).fold(
                onSuccess = { _trocaSenha.value = TrocaSenhaUiState(sucesso = true) },
                onFailure = { _trocaSenha.value = TrocaSenhaUiState(erro = mensagemErro(it)) }
            )
        }
    }

    /** Limpa o estado do diálogo (ao fechar ou após sucesso). */
    fun limparTrocaSenha() {
        _trocaSenha.value = TrocaSenhaUiState()
    }

    private fun mensagemErro(e: Throwable): String {
        val msg = e.message?.lowercase().orEmpty()
        return when {
            "wrong" in msg || "invalid-credential" in msg || "password is invalid" in msg ||
                "credential is incorrect" in msg -> "Senha atual incorreta."
            "weak" in msg -> "A nova senha é muito fraca."
            "network" in msg -> "Sem conexão. Tente novamente."
            else -> "Não foi possível trocar a senha. Tente novamente."
        }
    }

    /** Encerra a sessão (Firebase). A navegação para o Login é tratada na UI. */
    fun logout() = authRepository.logout()
}
