package com.sgaf.universidadedoservidor.ui.screens.search

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Busca Global") })
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            var query by remember { mutableStateOf("") }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Pesquisar cursos e aulas...") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Resultados:")
            // Mock results
        }
    }
}
