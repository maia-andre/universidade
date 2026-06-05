package com.sgaf.universidadedoservidor.ui.screens.avaliacao

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvaliacaoScreen(
    cursoId: Int,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Avaliação do Curso") })
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            Text("O que você achou do curso?", style = MaterialTheme.typography.titleMedium)
            // Mocking Likert Scale and Free Text
            var nota by remember { mutableStateOf(3f) }
            Slider(value = nota, onValueChange = { nota = it }, valueRange = 1f..5f, steps = 3)
            
            Spacer(modifier = Modifier.height(16.dp))
            var sugestao by remember { mutableStateOf("") }
            OutlinedTextField(
                value = sugestao,
                onValueChange = { sugestao = it },
                label = { Text("Sugestões") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth()) {
                Text("Enviar Avaliação")
            }
        }
    }
}
