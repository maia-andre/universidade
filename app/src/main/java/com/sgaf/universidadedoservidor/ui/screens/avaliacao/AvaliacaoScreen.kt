package com.sgaf.universidadedoservidor.ui.screens.avaliacao

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sgaf.universidadedoservidor.core.components.LoadingBox
import com.sgaf.universidadedoservidor.ui.theme.BlueSjc
import com.sgaf.universidadedoservidor.ui.theme.TextGray

private val PERGUNTAS = listOf(
    "O conteúdo foi claro e útil para o meu trabalho.",
    "A profundidade e a duração do curso foram adequadas.",
    "Os quizzes ajudaram a fixar o aprendizado.",
    "Pretendo aplicar o que aprendi no dia a dia.",
    "Recomendaria este curso a outros servidores."
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvaliacaoScreen(
    viewModel: AvaliacaoViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    // Encerra a tela ao concluir o envio.
    LaunchedEffect(state.concluido) {
        if (state.concluido) onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Avaliar curso", fontWeight = FontWeight.Bold) },
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
        if (state.isLoading) {
            LoadingBox(contentPadding = innerPadding)
            return@Scaffold
        }

        val respostas = remember(state.respostasIniciais) {
            mutableStateListOf(*state.respostasIniciais.toTypedArray())
        }
        var gostou by rememberSaveable(state.gostouInicial) { mutableStateOf(state.gostouInicial) }
        var sugestoes by rememberSaveable(state.sugestoesIniciais) { mutableStateOf(state.sugestoesIniciais) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.jaAvaliada) {
                Surface(
                    color = BlueSjc.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Você já avaliou este curso. Pode atualizar suas respostas abaixo.",
                        fontSize = 13.sp,
                        color = BlueSjc,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Text(
                text = "Sua opinião ajuda a melhorar a plataforma. Dê uma nota de 1 (discordo) a 5 (concordo):",
                fontSize = 14.sp,
                color = TextGray
            )

            PERGUNTAS.forEachIndexed { index, pergunta ->
                PerguntaLikert(
                    enunciado = "${index + 1}. $pergunta",
                    nota = respostas[index],
                    onNota = { respostas[index] = it }
                )
            }

            OutlinedTextField(
                value = gostou,
                onValueChange = { gostou = it },
                label = { Text("O que você mais gostou? (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            OutlinedTextField(
                value = sugestoes,
                onValueChange = { sugestoes = it },
                label = { Text("Sugestões de melhoria (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            val todasRespondidas = respostas.all { it in 1..5 }
            Button(
                onClick = { viewModel.enviar(respostas.toList(), gostou, sugestoes) },
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
                Text(
                    text = if (state.jaAvaliada) "Atualizar avaliação" else "Enviar avaliação",
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PerguntaLikert(
    enunciado: String,
    nota: Int,
    onNota: (Int) -> Unit
) {
    Column {
        Text(
            text = enunciado,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            (1..5).forEach { valor ->
                val selecionado = nota == valor
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(CircleShape)
                        .clickable { onNota(valor) },
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (selecionado) BlueSjc else Color.Transparent,
                        border = BorderStroke(1.dp, if (selecionado) BlueSjc else TextGray.copy(alpha = 0.5f)),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "$valor",
                                color = if (selecionado) Color.White else MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Discordo", fontSize = 10.sp, color = TextGray)
            Text("Concordo", fontSize = 10.sp, color = TextGray, textAlign = TextAlign.End)
        }
    }
}
