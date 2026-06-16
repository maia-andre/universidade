package com.sgaf.universidadedoservidor.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object Splash

@Serializable
object Home

@Serializable
object Cursos

@Serializable
object Configuracoes

@Serializable
object Acessibilidade

@Serializable
object Desempenho

@Serializable
object Ferramentas

@Serializable
data class FerramentaEditor(val tipo: String, val ferramentaId: Long)

@Serializable
object Busca

@Serializable
data class CursoDetail(val cursoId: Int)

@Serializable
data class Certificado(val cursoId: Int)

@Serializable
data class Avaliacao(val cursoId: Int)

@Serializable
data class Aula(val aulaId: Int)
