package com.sgaf.universidadedoservidor.ui.screens.aula

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sgaf.universidadedoservidor.ui.theme.*
import com.mikepenz.markdown.m3.Markdown

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AulaScreen(
    viewModel: AulaViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.aula?.titulo ?: "Visualização de Aula",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                },
                actions = {
                    state.aula?.let { aula ->
                        IconButton(onClick = { viewModel.toggleFavorito() }) {
                            Icon(
                                imageVector = if (aula.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favoritar",
                                tint = if (aula.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurface
                            )
                        }
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = GoldSjc)
            }
        } else {
            val aula = state.aula
            if (aula == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Aula não encontrada.", color = TextGray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Completion Status
                    item {
                        if (aula.isCompleted) {
                            Surface(
                                color = SuccessGreen.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = SuccessGreen
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Você concluiu esta aula!",
                                        color = SuccessGreen,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Content Section
                    item {
                        Markdown(
                            content = aula.conteudo,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }

                    // Quiz Section Header
                    item {
                        Text(
                            text = "Desafio de Fixação (Quiz)",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Acerte as duas perguntas para concluir a aula e computar o seu progresso.",
                            fontSize = 13.sp,
                            color = TextGray,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    // Display Feedback Banner
                    if (state.quizSubmitted) {
                        item {
                            if (state.quizCorrect) {
                                Surface(
                                    color = SuccessGreen.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = SuccessGreen
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Parabéns! Respostas Corretas!",
                                                color = SuccessGreen,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                        }
                                        Text(
                                            text = "Seu progresso nesta aula foi salvo e computado com sucesso.",
                                            color = SuccessGreen.copy(alpha = 0.9f),
                                            fontSize = 13.sp,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            } else {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Error,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Ops! Alguma resposta está errada.",
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                        }
                                        Text(
                                            text = "Reveja o conteúdo da aula e tente novamente para concluir.",
                                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f),
                                            fontSize = 13.sp,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(
                                            onClick = { viewModel.resetQuiz() },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.error
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(text = "Tentar Novamente", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Render Questions
                    aula.quiz.forEachIndexed { questionIndex, question ->
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSystemInDarkTheme()) CardDarkBg else Color.White
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Questão ${questionIndex + 1}: ${question.pergunta}",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    question.opcoes.forEachIndexed { optionIndex, optionText ->
                                        val isSelected = state.selectedAnswers[questionIndex] == optionIndex
                                        val isCorrectAnswer = optionIndex == question.respostaCorretaIndex
                                        
                                        // Colors based on validation state
                                        val cardBgColor = when {
                                            state.quizSubmitted && isCorrectAnswer -> SuccessGreen.copy(alpha = 0.15f)
                                            state.quizSubmitted && isSelected -> MaterialTheme.colorScheme.errorContainer
                                            isSelected -> BlueSjc.copy(alpha = 0.1f)
                                            else -> Color.Transparent
                                        }
                                        
                                        val borderStroke = when {
                                            state.quizSubmitted && isCorrectAnswer -> BorderStroke(2.dp, SuccessGreen)
                                            state.quizSubmitted && isSelected -> BorderStroke(2.dp, MaterialTheme.colorScheme.error)
                                            isSelected -> BorderStroke(2.dp, BlueSjc)
                                            else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                                        }
                                        
                                        val textColor = when {
                                            state.quizSubmitted && isCorrectAnswer -> SuccessGreen
                                            state.quizSubmitted && isSelected -> MaterialTheme.colorScheme.onErrorContainer
                                            isSelected -> BlueSjc
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(cardBgColor)
                                                .clickable(enabled = !state.quizSubmitted) {
                                                    viewModel.selectAnswer(questionIndex, optionIndex)
                                                }
                                                .border(borderStroke, RoundedCornerShape(8.dp))
                                                .padding(12.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                RadioButton(
                                                    selected = isSelected,
                                                    onClick = {
                                                        if (!state.quizSubmitted) {
                                                            viewModel.selectAnswer(questionIndex, optionIndex)
                                                        }
                                                    },
                                                    enabled = !state.quizSubmitted,
                                                    colors = RadioButtonDefaults.colors(
                                                        selectedColor = BlueSjc,
                                                        unselectedColor = TextGray.copy(alpha = 0.6f)
                                                    )
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = optionText,
                                                    fontSize = 14.sp,
                                                    color = textColor,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Submit Button
                    if (!state.quizSubmitted) {
                        item {
                            val allAnswered = state.selectedAnswers.size == aula.quiz.size
                            Button(
                                onClick = { viewModel.submitQuiz() },
                                enabled = allAnswered,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .padding(vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BlueSjc,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Validar Respostas",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}


