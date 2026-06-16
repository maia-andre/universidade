package com.sgaf.universidadedoservidor.ui.screens.ferramentas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sgaf.universidadedoservidor.domain.model.FerramentaPreenchida
import com.sgaf.universidadedoservidor.domain.model.TipoFerramenta
import com.sgaf.universidadedoservidor.ui.theme.BlueSjc
import com.sgaf.universidadedoservidor.ui.theme.TextGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FerramentasScreen(
    viewModel: FerramentasViewModel,
    onNavigateBack: () -> Unit,
    onAbrirEditor: (TipoFerramenta, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ferramentas Práticas", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Aplique os conceitos na prática. Crie e salve suas análises:",
                    fontSize = 14.sp,
                    color = TextGray
                )
            }

            secaoFerramenta(
                tipo = TipoFerramenta.SWOT,
                itens = state.swot,
                onAbrirEditor = onAbrirEditor
            )
            secaoFerramenta(
                tipo = TipoFerramenta.CINCO_W_DOIS_H,
                itens = state.cincoWDoisH,
                onAbrirEditor = onAbrirEditor
            )

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.secaoFerramenta(
    tipo: TipoFerramenta,
    itens: List<FerramentaPreenchida>,
    onAbrirEditor: (TipoFerramenta, Long) -> Unit
) {
    item(key = "header_${tipo.name}") {
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(tipo.rotulo, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(tipo.descricao, fontSize = 12.sp, color = TextGray)
            }
            FilledTonalButton(onClick = { onAbrirEditor(tipo, 0L) }) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Nova")
            }
        }
    }

    if (itens.isEmpty()) {
        item(key = "empty_${tipo.name}") {
            Text(
                text = "Nenhuma ${tipo.rotulo} salva ainda.",
                fontSize = 13.sp,
                color = TextGray.copy(alpha = 0.8f),
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    } else {
        items(itens, key = { "f_${it.id}" }) { ferramenta ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAbrirEditor(tipo, ferramenta.id) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Description, contentDescription = null, tint = BlueSjc)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = ferramenta.titulo,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
