package com.sgaf.universidadedoservidor.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sgaf.universidadedoservidor.core.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val carregando: Boolean = false,
    val erro: String? = null,
    val mensagem: String? = null,
    val sucesso: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(email: String, senha: String) {
        if (email.isBlank() || senha.isBlank()) {
            _uiState.update { it.copy(erro = "Informe e-mail e senha.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(carregando = true, erro = null, mensagem = null) }
            val resultado = authRepository.login(email, senha)
            _uiState.update {
                if (resultado.isSuccess) it.copy(carregando = false, sucesso = true)
                else it.copy(carregando = false, erro = traduzirErro(resultado.exceptionOrNull()))
            }
        }
    }

    fun enviarReset(email: String) {
        if (email.isBlank()) {
            _uiState.update { it.copy(erro = "Informe o e-mail para redefinir a senha.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(carregando = true, erro = null, mensagem = null) }
            val resultado = authRepository.enviarResetSenha(email)
            _uiState.update {
                if (resultado.isSuccess) it.copy(carregando = false, mensagem = "E-mail de redefinição enviado.")
                else it.copy(carregando = false, erro = traduzirErro(resultado.exceptionOrNull()))
            }
        }
    }

    fun limparMensagens() {
        _uiState.update { it.copy(erro = null, mensagem = null) }
    }

    private fun traduzirErro(e: Throwable?): String {
        val msg = e?.message?.lowercase() ?: return "Erro desconhecido."
        return when {
            "password" in msg || "credential" in msg || "no user record" in msg ||
                "invalid" in msg -> "E-mail ou senha inválidos."
            "network" in msg -> "Sem conexão. Verifique a internet e tente novamente."
            "blocked" in msg || "too many" in msg -> "Muitas tentativas. Aguarde e tente novamente."
            else -> e?.message ?: "Falha ao entrar."
        }
    }
}
