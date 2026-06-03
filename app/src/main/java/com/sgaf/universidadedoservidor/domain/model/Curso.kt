package com.sgaf.universidadedoservidor.domain.model

data class Curso(
    val id: Int,
    val titulo: String,
    val descricao: String,
    val modulos: List<Modulo> = emptyList(),
    val isAvailable: Boolean = true
)
