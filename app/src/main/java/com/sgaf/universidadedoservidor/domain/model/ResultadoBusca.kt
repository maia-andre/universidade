package com.sgaf.universidadedoservidor.domain.model

data class ResultadoBusca(
    val aulaId: Int,
    val aulaTitulo: String,
    val moduloTitulo: String,
    val cursoTitulo: String,
    /** Pequeno trecho do conteúdo ao redor do termo, quando o casamento foi no corpo da aula. */
    val trecho: String?
)
