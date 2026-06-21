package com.sgaf.universidadedoservidor.ui.screens.curso_detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sgaf.universidadedoservidor.core.data.preferences.UserPreferencesRepository
import com.sgaf.universidadedoservidor.domain.model.Curso
import com.sgaf.universidadedoservidor.domain.usecase.GetCursoDetailUseCase
import com.sgaf.universidadedoservidor.domain.usecase.GetResultadoProvaFinalUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class CursoDetailState(
    val curso: Curso? = null,
    val isLoading: Boolean = true,
    val provaFinalAprovada: Boolean = false,
    /** Aluno sem acesso a este curso (nem matriculado nem concluído) — v7, Item 1. */
    val bloqueado: Boolean = false
)

@HiltViewModel
class CursoDetailViewModel @Inject constructor(
    private val getCursoDetailUseCase: GetCursoDetailUseCase,
    private val getResultadoProvaFinalUseCase: GetResultadoProvaFinalUseCase,
    userPreferencesRepository: UserPreferencesRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val cursoId: Int = savedStateHandle.get<Int>("cursoId") ?: 1

    // O "curso ativo" passa a vir só da matrícula (sync na Home) — sem a heurística antiga de
    // "entrar no curso o torna ativo" (v7, Item 1.5). Aqui só gateamos o acesso (defesa em profundidade).
    val state: StateFlow<CursoDetailState> = combine(
        getCursoDetailUseCase(cursoId),
        getResultadoProvaFinalUseCase(cursoId),
        userPreferencesRepository.cursosAcessiveis
    ) { curso, prova, acessiveis ->
        CursoDetailState(
            curso = curso,
            isLoading = false,
            provaFinalAprovada = prova?.aprovado == true,
            bloqueado = cursoId !in acessiveis
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CursoDetailState()
    )
}
