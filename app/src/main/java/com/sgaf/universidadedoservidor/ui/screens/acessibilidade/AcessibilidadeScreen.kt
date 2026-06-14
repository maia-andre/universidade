package com.sgaf.universidadedoservidor.ui.screens.acessibilidade

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sgaf.universidadedoservidor.core.data.preferences.UserPreferencesRepository
import com.sgaf.universidadedoservidor.ui.theme.BlueSjc
import com.sgaf.universidadedoservidor.ui.theme.TextGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcessibilidadeScreen(
    viewModel: AcessibilidadeViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Acessibilidade", fontWeight = FontWeight.Bold) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Tamanho da fonte
            SecaoCard(icon = Icons.Default.TextFields, titulo = "Tamanho da fonte") {
                Text(
                    text = "Ajusta o texto de todo o aplicativo.",
                    fontSize = 13.sp,
                    color = TextGray
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = { viewModel.diminuirFonte() },
                        enabled = state.fontScale > UserPreferencesRepository.FONT_SCALE_MIN
                    ) { Text("A-", fontWeight = FontWeight.Bold) }

                    Text(
                        text = "${(state.fontScale * 100).toInt()}%",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlueSjc
                    )

                    OutlinedButton(
                        onClick = { viewModel.aumentarFonte() },
                        enabled = state.fontScale < UserPreferencesRepository.FONT_SCALE_MAX
                    ) { Text("A+", fontWeight = FontWeight.Bold) }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Texto de exemplo para conferir o tamanho.",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Alto contraste
            ToggleCard(
                icon = Icons.Default.Contrast,
                titulo = "Alto contraste",
                descricao = "Cores mais fortes entre texto e fundo.",
                checked = state.highContrast,
                onCheckedChange = viewModel::setHighContrast
            )

            // Redução de movimento
            ToggleCard(
                icon = Icons.Default.Animation,
                titulo = "Redução de movimento",
                descricao = "Desativa animações e transições de tela.",
                checked = state.reducedMotion,
                onCheckedChange = viewModel::setReducedMotion
            )
        }
    }
}

@Composable
private fun SecaoCard(
    icon: ImageVector,
    titulo: String,
    conteudo: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = BlueSjc)
                Spacer(modifier = Modifier.width(8.dp))
                Text(titulo, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            conteudo()
        }
    }
}

@Composable
private fun ToggleCard(
    icon: ImageVector,
    titulo: String,
    descricao: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = BlueSjc)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(titulo, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(descricao, fontSize = 13.sp, color = TextGray)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
