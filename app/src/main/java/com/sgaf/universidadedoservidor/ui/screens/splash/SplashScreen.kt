package com.sgaf.universidadedoservidor.ui.screens.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.sgaf.universidadedoservidor.ui.components.UniversidadeLogo
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(key1 = true) {
        delay(2500)
        onSplashFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF007BFF), // Azul vibrante, moderno e claro no topo
                        Color(0xFF00275E)  // Azul profundo elegante na base
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        UniversidadeLogo(
            modifier = Modifier.padding(16.dp),
            gearSize = 56.dp, // Gear ajustada para a nova escala
            scale = 1.35f, // Aumenta todo o conjunto (textos e capelo)
            textColor = Color.White,
            backgroundColor = Color(0xFF003882) // Match intermediário do gradiente
        )
    }
}
