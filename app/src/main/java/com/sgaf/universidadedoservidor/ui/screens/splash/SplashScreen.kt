package com.sgaf.universidadedoservidor.ui.screens.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        UniversidadeLogo(
            modifier = Modifier.padding(16.dp),
            gearSize = 42.dp
        )
    }
}
