package com.sgaf.universidadedoservidor.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object Splash

@Serializable
object Home

@Serializable
object Cursos

@Serializable
data class CursoDetail(val cursoId: Int)

@Serializable
data class Aula(val aulaId: Int)
