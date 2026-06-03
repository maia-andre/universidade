package com.sgaf.universidadedoservidor.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sgaf.universidadedoservidor.domain.model.Aula
import com.sgaf.universidadedoservidor.domain.usecase.GetCursoDetailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HomeState(
    val totalAulas: Int = 0,
    val concluídasAulas: Int = 0,
    val percentualConclusao: Float = 0f,
    val aulasFavoritas: List<Pair<Int, String>> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    getCursoDetailUseCase: GetCursoDetailUseCase
) : ViewModel() {

    val state: StateFlow<HomeState> = getCursoDetailUseCase(1).map { curso ->
        if (curso == null) {
            HomeState(isLoading = true)
        } else {
            val allAulas = curso.modulos.flatMap { it.aulas }
            val total = allAulas.size
            val completed = allAulas.count { it.isCompleted }
            val percent = if (total > 0) completed.toFloat() / total.toFloat() else 0f
            
            val favorites = allAulas.filter { it.isFavorite }.map { it.id to it.titulo }
            
            HomeState(
                totalAulas = total,
                concluídasAulas = completed,
                percentualConclusao = percent,
                aulasFavoritas = favorites,
                isLoading = false
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeState()
    )
}
