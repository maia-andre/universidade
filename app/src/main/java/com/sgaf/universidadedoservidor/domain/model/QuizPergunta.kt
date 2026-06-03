package com.sgaf.universidadedoservidor.domain.model

data class QuizPergunta(
    val pergunta: String,
    val opcoes: List<String>,
    val respostaCorretaIndex: Int
)
