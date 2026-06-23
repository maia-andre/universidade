package com.sgaf.universidadedoservidor.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sgaf.universidadedoservidor.core.data.preferences.UserPreferencesRepository
import com.sgaf.universidadedoservidor.domain.model.Curso
import com.sgaf.universidadedoservidor.domain.usecase.GetCursosUseCase
import com.sgaf.universidadedoservidor.domain.usecase.SincronizarConteudoUseCase
import com.sgaf.universidadedoservidor.domain.usecase.SincronizarCursoAtivoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
    userPreferencesRepository: UserPreferencesRepository,
    private val sincronizarCursoAtivoUseCase: SincronizarCursoAtivoUseCase,
    private val sincronizarConteudoUseCase: SincronizarConteudoUseCase
) : ViewModel() {

    init {
        // D3: ao abrir a Home (após login ou no arranque já-logado), puxa a matrícula liberada
        // pelo RH e define o curso ativo. O state reage via cursoAtivoId. Best-effort/offline-safe.
        viewModelScope.launch { runCatching { sincronizarCursoAtivoUseCase() } }
        // V8 Item 1: sincroniza o conteúdo dos cursos publicado pelo RH (sem novo APK).
        // No-op quando offline ou sem versão nova. O grafo reage via getCursosUseCase.
        viewModelScope.launch { runCatching { sincronizarConteudoUseCase() } }
    }

    val state: StateFlow<HomeState> = combine(
        getCursosUseCase(),
        userPreferencesRepository.cursoAtivoId,
        userPreferencesRepository.cursosAcessiveis
    ) { cursos, cursoAtivoId, acessiveis ->
        val cursoAtivo = resolverCursoAtivo(cursos, cursoAtivoId, acessiveis)
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
     * Curso ativo do aluno: o explicitamente escolhido (se acessível) ou, como fallback, o
     * primeiro curso acessível. Só considera cursos a que o aluno tem acesso — matriculado ou
     * concluído (v7, Item 1). Null quando não há nenhum acessível.
     */
    private fun resolverCursoAtivo(cursos: List<Curso>, cursoAtivoId: Int?, acessiveis: Set<Int>): Curso? {
        fun acessivel(c: Curso) = c.isAvailable && c.id in acessiveis
        val escolhido = cursoAtivoId?.let { id -> cursos.firstOrNull { it.id == id && acessivel(it) } }
        return escolhido ?: cursos.firstOrNull { acessivel(it) }
    }
}
