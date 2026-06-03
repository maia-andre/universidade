package com.sgaf.universidadedoservidor.domain.model

data class Modulo(
    val id: Int,
    val titulo: String,
    val descricao: String,
    val aulas: List<Aula>
)
