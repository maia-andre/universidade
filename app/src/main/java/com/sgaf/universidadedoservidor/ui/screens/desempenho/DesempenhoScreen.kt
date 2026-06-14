package com.sgaf.universidadedoservidor.ui.screens.desempenho

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sgaf.universidadedoservidor.core.components.EmptyMessage
import com.sgaf.universidadedoservidor.core.components.LoadingBox
import com.sgaf.universidadedoservidor.domain.model.EstatisticaModulo
import com.sgaf.universidadedoservidor.domain.model.EstatisticasCurso
import com.sgaf.universidadedoservidor.ui.theme.BlueSjc
import com.sgaf.universidadedoservidor.ui.theme.GoldSjc
import com.sgaf.universidadedoservidor.ui.theme.SuccessGreen
import com.sgaf.universidadedoservidor.ui.theme.TextGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesempenhoScreen(
    viewModel: DesempenhoViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meu Desempenho", fontWeight = FontWeight.Bold) },
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
        val stats = state.estatisticas
        when {
            state.isLoading -> LoadingBox(contentPadding = innerPadding)
            stats == null -> EmptyMessage(
                icon = Icons.Default.BarChart,
                titulo = "Sem dados ainda",
                descricao = "Comece um curso e responda aos quizzes para ver seu desempenho aqui.",
                contentPadding = innerPadding
            )
            else -> Conteudo(stats = stats, innerPadding = innerPadding)
        }
    }
}

@Composable
private fun Conteudo(stats: EstatisticasCurso, innerPadding: PaddingValues) {
    val melhor = stats.modulos.filter { it.totalQuestoes > 0 }.maxByOrNull { it.percentualAcerto }
    val pior = stats.modulos.filter { it.totalQuestoes > 0 }.minByOrNull { it.percentualAcerto }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stats.cursoTitulo,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Resumo geral
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ResumoLinha("Conclusão", stats.aulasConcluidas, stats.totalAulas, stats.percentualConclusao)
                    Spacer(modifier = Modifier.height(12.dp))
                    ResumoLinha("Aproveitamento (quiz)", stats.acertos, stats.totalQuestoes, stats.percentualAcerto)
                }
            }
        }

        if (melhor != null && pior != null && stats.modulos.size > 1) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DestaqueCard(
                        titulo = "Mais forte",
                        modulo = melhor.moduloTitulo,
                        percentual = melhor.percentualAcerto,
                        cor = SuccessGreen,
                        modifier = Modifier.weight(1f)
                    )
                    DestaqueCard(
                        titulo = "A reforçar",
                        modulo = pior.moduloTitulo,
                        percentual = pior.percentualAcerto,
                        cor = GoldSjc,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            Text(
                text = "Por módulo",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        items(stats.modulos) { modulo ->
            ModuloCard(modulo)
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun ResumoLinha(rotulo: String, valor: Int, total: Int, percentual: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(rotulo, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(
            "$valor/$total  •  ${(percentual * 100).toInt()}%",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = BlueSjc
        )
    }
    Spacer(modifier = Modifier.height(6.dp))
    LinearProgressIndicator(
        progress = { percentual },
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp)),
        color = GoldSjc,
        trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    )
}

@Composable
private fun DestaqueCard(
    titulo: String,
    modulo: String,
    percentual: Float,
    cor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(titulo, fontSize = 12.sp, color = TextGray, fontWeight = FontWeight.Medium)
            Text(
                "${(percentual * 100).toInt()}%",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = cor
            )
            Text(
                modulo,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun ModuloCard(modulo: EstatisticaModulo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                modulo.moduloTitulo,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${modulo.aulasConcluidas}/${modulo.totalAulas} aulas",
                    fontSize = 12.sp,
                    color = TextGray
                )
                Text(
                    if (modulo.totalQuestoes > 0) "${(modulo.percentualAcerto * 100).toInt()}% no quiz" else "Sem quiz",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (modulo.totalQuestoes > 0) BlueSjc else TextGray
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { modulo.percentualAcerto },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = SuccessGreen,
                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
        }
    }
}
