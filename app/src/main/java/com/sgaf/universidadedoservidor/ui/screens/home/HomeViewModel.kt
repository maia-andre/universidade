package com.sgaf.universidadedoservidor.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sgaf.universidadedoservidor.core.data.preferences.UserPreferencesRepository
import com.sgaf.universidadedoservidor.domain.model.Curso
import com.sgaf.universidadedoservidor.domain.usecase.GetCursosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HomeState(
    val cursoAtivoTitulo: String? = null,
    val totalAulas: Int = 0,
    val concluídasAulas: Int = 0,
    val percentualConclusao: Float = 0f,
    val aulasFavoritas: List<Pair<Int, String>> = emptyList(),
    // "Continuar de onde parou" (Item 2.2): última aula acessada e ainda não concluída.
    val continuarAula: Pair<Int, String>? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    getCursosUseCase: GetCursosUseCase,
    userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val state: StateFlow<HomeState> = combine(
        getCursosUseCase(),
        userPreferencesRepository.cursoAtivoId
    ) { cursos, cursoAtivoId ->
        val cursoAtivo = resolverCursoAtivo(cursos, cursoAtivoId)
            ?: return@combine HomeState(isLoading = false)

        val allAulas = cursoAtivo.modulos.flatMap { it.aulas }
        val total = allAulas.size
        val completed = allAulas.count { it.isCompleted }
        val percent = if (total > 0) completed.toFloat() / total.toFloat() else 0f
        val favorites = allAulas.filter { it.isFavorite }.map { it.id to it.titulo }

        // Última aula acessada que ainda não foi concluída.
        val continuar = allAulas
            .filter { it.ultimoAcessoEm > 0 && !it.isCompleted }
            .maxByOrNull { it.ultimoAcessoEm }
            ?.let { it.id to it.titulo }

        HomeState(
            cursoAtivoTitulo = cursoAtivo.titulo,
            totalAulas = total,
            concluídasAulas = completed,
            percentualConclusao = percent,
            aulasFavoritas = favorites,
            continuarAula = continuar,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeState()
    )

    /**
     * Curso ativo do aluno: o explicitamente escolhido (se ainda disponível) ou,
     * como fallback, o primeiro curso disponível. Null quando não há curso disponível.
     */
    private fun resolverCursoAtivo(cursos: List<Curso>, cursoAtivoId: Int?): Curso? {
        val escolhido = cursoAtivoId?.let { id -> cursos.firstOrNull { it.id == id && it.isAvailable } }
        return escolhido ?: cursos.firstOrNull { it.isAvailable }
    }
}
