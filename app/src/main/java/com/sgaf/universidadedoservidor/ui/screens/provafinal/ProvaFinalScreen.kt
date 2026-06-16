package com.sgaf.universidadedoservidor.ui.screens.provafinal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sgaf.universidadedoservidor.core.components.EmptyMessage
import com.sgaf.universidadedoservidor.core.components.LoadingBox
import com.sgaf.universidadedoservidor.core.utils.Constants
import com.sgaf.universidadedoservidor.domain.model.QuizPergunta
import com.sgaf.universidadedoservidor.ui.theme.BlueSjc
import com.sgaf.universidadedoservidor.ui.theme.GoldSjc
import com.sgaf.universidadedoservidor.ui.theme.SuccessGreen
import com.sgaf.universidadedoservidor.ui.theme.TextGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProvaFinalScreen(
    viewModel: ProvaFinalViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCertificado: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val notaMinima = Constants.MINIMUM_PASSING_SCORE

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Prova Final", fontWeight = FontWeight.Bold) },
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
        when {
            state.isLoading -> LoadingBox(contentPadding = innerPadding)

            state.perguntas.isEmpty() -> EmptyMessage(
                icon = Icons.Default.WorkspacePremium,
                titulo = "Prova final indisponível",
                descricao = "Este curso ainda não possui uma prova final cadastrada.",
                contentPadding = innerPadding
            )

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Avaliação final do curso",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Responda às ${state.perguntas.size} questões. É preciso ${notaMinima}% de acerto " +
                                "para ser aprovado e emitir o certificado.",
                            fontSize = 13.sp,
                            color = TextGray,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    if (state.submitted) {
                        item {
                            ResultadoBanner(
                                aprovado = state.aprovado,
                                acertos = state.acertos,
                                total = state.perguntas.size
                            )
                        }
                    }

                    state.perguntas.forEachIndexed { index, pergunta ->
                        item {
                            QuestaoCard(
                                numero = index + 1,
                                pergunta = pergunta,
                                selecionada = state.selectedAnswers[index],
                                submitted = state.submitted,
                                onSelecionar = { opcao -> viewModel.selecionar(index, opcao) }
                            )
                        }
                    }

                    item {
                        when {
                            !state.submitted -> {
                                val todasRespondidas = state.selectedAnswers.size == state.perguntas.size
                                Button(
                                    onClick = { viewModel.enviar() },
                                    enabled = todasRespondidas,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BlueSjc,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Enviar prova", fontWeight = FontWeight.Bold)
                                }
                            }

                            state.aprovado -> {
                                Button(
                                    onClick = { onNavigateToCertificado(state.cursoId) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = GoldSjc,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.WorkspacePremium, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Emitir certificado", fontWeight = FontWeight.Bold)
                                }
                            }

                            else -> {
                                Button(
                                    onClick = { viewModel.tentarNovamente() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BlueSjc,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Tentar novamente", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultadoBanner(aprovado: Boolean, acertos: Int, total: Int) {
    val pct = if (total > 0) acertos * 100 / total else 0
    Surface(
        color = if (aprovado) SuccessGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (aprovado) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = if (aprovado) SuccessGreen else MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (aprovado) "Aprovado! Parabéns." else "Você não atingiu a nota mínima.",
                    color = if (aprovado) SuccessGreen else MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
            Text(
                text = "Você acertou $acertos de $total ($pct%).",
                color = if (aprovado) SuccessGreen.copy(alpha = 0.9f)
                else MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun QuestaoCard(
    numero: Int,
    pergunta: QuizPergunta,
    selecionada: Int?,
    submitted: Boolean,
    onSelecionar: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Questão $numero: ${pergunta.pergunta}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            pergunta.opcoes.forEachIndexed { opcaoIndex, opcaoTexto ->
                val isSelected = selecionada == opcaoIndex
                val isCorreta = opcaoIndex == pergunta.respostaCorretaIndex

                val bgColor = when {
                    submitted && isCorreta -> SuccessGreen.copy(alpha = 0.15f)
                    submitted && isSelected -> MaterialTheme.colorScheme.errorContainer
                    isSelected -> BlueSjc.copy(alpha = 0.1f)
                    else -> Color.Transparent
                }
                val borderStroke = when {
                    submitted && isCorreta -> BorderStroke(2.dp, SuccessGreen)
                    submitted && isSelected -> BorderStroke(2.dp, MaterialTheme.colorScheme.error)
                    isSelected -> BorderStroke(2.dp, BlueSjc)
                    else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                }
                val textColor = when {
                    submitted && isCorreta -> SuccessGreen
                    submitted && isSelected -> MaterialTheme.colorScheme.onErrorContainer
                    isSelected -> BlueSjc
                    else -> MaterialTheme.colorScheme.onSurface
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(bgColor)
                        .clickable(enabled = !submitted) { onSelecionar(opcaoIndex) }
                        .border(borderStroke, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { if (!submitted) onSelecionar(opcaoIndex) },
                            enabled = !submitted,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = BlueSjc,
                                unselectedColor = TextGray.copy(alpha = 0.6f)
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = opcaoTexto,
                            fontSize = 14.sp,
                            color = textColor,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        // Acessibilidade (daltonismo): acerto/erro também por ícone, não só cor.
                        if (submitted && isCorreta) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Resposta correta",
                                tint = SuccessGreen
                            )
                        } else if (submitted && isSelected) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = "Sua resposta (incorreta)",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}
