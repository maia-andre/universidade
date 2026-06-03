package com.sgaf.universidadedoservidor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.sgaf.universidadedoservidor.ui.navigation.Splash
import com.sgaf.universidadedoservidor.ui.navigation.Home
import com.sgaf.universidadedoservidor.ui.navigation.Cursos
import com.sgaf.universidadedoservidor.ui.navigation.CursoDetail
import com.sgaf.universidadedoservidor.ui.navigation.Aula
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UniversidadeDoServidorTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNavigation(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = Splash,
        modifier = modifier
    ) {
        composable<Splash> {
            SplashScreen(
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
                }
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
                }
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