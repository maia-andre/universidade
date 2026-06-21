package com.sgaf.universidadedoservidor.ui.screens.splash

import androidx.lifecycle.ViewModel
import com.sgaf.universidadedoservidor.core.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Decide, ao fim do splash, se o app vai para o Login ou para a Home (v6). */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    fun isLoggedIn(): Boolean = authRepository.isLoggedIn()
}
