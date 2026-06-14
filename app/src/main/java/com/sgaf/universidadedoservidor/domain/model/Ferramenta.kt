package com.sgaf.universidadedoservidor.domain.model

/** Um campo editável de uma ferramenta prática. */
data class CampoFerramenta(val chave: String, val rotulo: String)

/** Tipos de ferramentas práticas e seus campos (v4 Item 2). */
enum class TipoFerramenta(val rotulo: String, val descricao: String, val campos: List<CampoFerramenta>) {
    SWOT(
        rotulo = "Matriz SWOT",
        descricao = "Forças, Fraquezas, Oportunidades e Ameaças",
        campos = listOf(
            CampoFerramenta("forcas", "Forças"),
            CampoFerramenta("fraquezas", "Fraquezas"),
            CampoFerramenta("oportunidades", "Oportunidades"),
            CampoFerramenta("ameacas", "Ameaças")
        )
    ),
    CINCO_W_DOIS_H(
        rotulo = "5W2H",
        descricao = "Plano de ação: o quê, por quê, onde, quando, quem, como e quanto",
        campos = listOf(
            CampoFerramenta("what", "O quê (What)"),
            CampoFerramenta("why", "Por quê (Why)"),
            CampoFerramenta("where", "Onde (Where)"),
            CampoFerramenta("when", "Quando (When)"),
            CampoFerramenta("who", "Quem (Who)"),
            CampoFerramenta("how", "Como (How)"),
            CampoFerramenta("howMuch", "Quanto custa (How much)")
        )
    );

    companion object {
        fun fromName(nome: String): TipoFerramenta =
            entries.firstOrNull { it.name == nome } ?: SWOT
    }
}

/** Instância preenchida e persistida de uma ferramenta. */
data class FerramentaPreenchida(
    val id: Long,
    val tipo: TipoFerramenta,
    val titulo: String,
    val campos: Map<String, String>,
    val criadoEm: Long
)
