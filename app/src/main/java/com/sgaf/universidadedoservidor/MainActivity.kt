package com.sgaf.universidadedoservidor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.sgaf.universidadedoservidor.core.data.preferences.ThemeMode
import com.sgaf.universidadedoservidor.ui.navigation.Splash
import com.sgaf.universidadedoservidor.ui.navigation.Home
import com.sgaf.universidadedoservidor.ui.navigation.Cursos
import com.sgaf.universidadedoservidor.ui.navigation.Configuracoes
import com.sgaf.universidadedoservidor.ui.navigation.Acessibilidade
import com.sgaf.universidadedoservidor.ui.navigation.Desempenho
import com.sgaf.universidadedoservidor.ui.navigation.Busca
import com.sgaf.universidadedoservidor.ui.navigation.CursoDetail
import com.sgaf.universidadedoservidor.ui.navigation.Certificado
import com.sgaf.universidadedoservidor.ui.navigation.Avaliacao
import com.sgaf.universidadedoservidor.ui.navigation.Aula
import com.sgaf.universidadedoservidor.ui.screens.certificado.CertificadoScreen
import com.sgaf.universidadedoservidor.ui.screens.certificado.CertificadoViewModel
import com.sgaf.universidadedoservidor.ui.screens.avaliacao.AvaliacaoScreen
import com.sgaf.universidadedoservidor.ui.screens.avaliacao.AvaliacaoViewModel
import com.sgaf.universidadedoservidor.ui.screens.settings.SettingsScreen
import com.sgaf.universidadedoservidor.ui.screens.settings.SettingsViewModel
import com.sgaf.universidadedoservidor.ui.screens.acessibilidade.AcessibilidadeScreen
import com.sgaf.universidadedoservidor.ui.screens.acessibilidade.AcessibilidadeViewModel
import com.sgaf.universidadedoservidor.ui.screens.desempenho.DesempenhoScreen
import com.sgaf.universidadedoservidor.ui.screens.desempenho.DesempenhoViewModel
import com.sgaf.universidadedoservidor.ui.screens.search.SearchScreen
import com.sgaf.universidadedoservidor.ui.screens.search.SearchViewModel
import com.sgaf.universidadedoservidor.ui.screens.splash.SplashScreen
import com.sgaf.universidadedoservidor.ui.screens.home.HomeScreen
import com.sgaf.universidadedoservidor.ui.screens.home.HomeViewModel
import com.sgaf.universidadedoservidor.ui.screens.cursos.CursosScreen
import com.sgaf.universidadedoservidor.ui.screens.cursos.CursosViewModel
import com.sgaf.universidadedoservidor.ui.screens.curso_detail.CursoDetailScreen
import com.sgaf.universidadedoservidor.ui.screens.curso_detail.CursoDetailViewModel
import com.sgaf.universidadedoservidor.ui.screens.aula.AulaScreen
import com.sgaf.universidadedoservidor.ui.screens.aula.AulaViewModel
import com.sgaf.universidadedoservidor.ui.theme.UniversidadeDoServidorTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val ui by mainViewModel.uiState.collectAsState()
            val darkTheme = when (ui.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            UniversidadeDoServidorTheme(darkTheme = darkTheme, highContrast = ui.highContrast) {
                // Escala global da tipografia (acessibilidade v4): só sp, mantém os dp.
                val baseDensity = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(
                        density = baseDensity.density,
                        fontScale = baseDensity.fontScale * ui.fontScale
                    )
                ) {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        AppNavigation(
                            reducedMotion = ui.reducedMotion,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    reducedMotion: Boolean = false,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    val anim = tween<Float>(300)
    NavHost(
        navController = navController,
        startDestination = Splash,
        modifier = modifier,
        // Transições slide + fade (Item 2.5); desligadas quando "redução de movimento" (v4 acessibilidade)
        enterTransition = {
            if (reducedMotion) EnterTransition.None
            else slideIntoContainer(SlideDirection.Start, tween(300)) + fadeIn(anim)
        },
        exitTransition = {
            if (reducedMotion) ExitTransition.None
            else slideOutOfContainer(SlideDirection.Start, tween(300)) + fadeOut(anim)
        },
        popEnterTransition = {
            if (reducedMotion) EnterTransition.None
            else slideIntoContainer(SlideDirection.End, tween(300)) + fadeIn(anim)
        },
        popExitTransition = {
            if (reducedMotion) ExitTransition.None
            else slideOutOfContainer(SlideDirection.End, tween(300)) + fadeOut(anim)
        }
    ) {
        composable<Splash> {
            SplashScreen(
                reducedMotion = reducedMotion,
                onSplashFinished = {
                    navController.navigate(Home) {
                        popUpTo(Splash) { inclusive = true }
                    }
                }
            )
        }
        
        composable<Home> {
            val viewModel: HomeViewModel = hiltViewModel()
            HomeScreen(
                viewModel = viewModel,
                onNavigateToCursos = {
                    navController.navigate(Cursos)
                },
                onNavigateToAula = { aulaId ->
                    navController.navigate(Aula(aulaId))
                },
                onNavigateToConfig = {
                    navController.navigate(Configuracoes)
                },
                onNavigateToBusca = {
                    navController.navigate(Busca)
                },
                onNavigateToDesempenho = {
                    navController.navigate(Desempenho)
                }
            )
        }

        composable<Desempenho> {
            val viewModel: DesempenhoViewModel = hiltViewModel()
            DesempenhoScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<Busca> {
            val viewModel: SearchViewModel = hiltViewModel()
            SearchScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAula = { aulaId ->
                    navController.navigate(Aula(aulaId))
                }
            )
        }

        composable<Configuracoes> {
            val viewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToAcessibilidade = {
                    navController.navigate(Acessibilidade)
                }
            )
        }

        composable<Acessibilidade> {
            val viewModel: AcessibilidadeViewModel = hiltViewModel()
            AcessibilidadeScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable<Cursos> {
            val viewModel: CursosViewModel = hiltViewModel()
            CursosScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToCursoDetail = { cursoId ->
                    navController.navigate(CursoDetail(cursoId))
                }
            )
        }
        
        composable<CursoDetail> {
            val viewModel: CursoDetailViewModel = hiltViewModel()
            CursoDetailScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToAula = { aulaId ->
                    navController.navigate(Aula(aulaId))
                },
                onNavigateToCertificado = { cursoId ->
                    navController.navigate(Certificado(cursoId))
                },
                onNavigateToAvaliacao = { cursoId ->
                    navController.navigate(Avaliacao(cursoId))
                }
            )
        }

        composable<Certificado> {
            val viewModel: CertificadoViewModel = hiltViewModel()
            CertificadoScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<Avaliacao> {
            val viewModel: AvaliacaoViewModel = hiltViewModel()
            AvaliacaoScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable<Aula> {
            val viewModel: AulaViewModel = hiltViewModel()
            AulaScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}