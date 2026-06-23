package com.sgaf.universidadedoservidor.domain.repository

/** Catálogo publicado pelo RH, lido do doc `config/conteudo` do Firestore (V8 Item 1). */
data class ConteudoRemoto(
    /** Versão monotônica; o app só sincroniza quando ela é maior que a versão local. */
    val versao: Int,
    /** Catálogo completo como string JSON (mesmo formato do `curso_data.json`). */
    val json: String
)

/**
 * Acesso ao conteúdo dinâmico no backend (Firestore), do lado do app cliente (V8 Item 1).
 * Permite ao RH atualizar cursos/módulos/aulas sem republicar o APK.
 */
interface ConteudoRemotoRepository {

    /** Lê `config/conteudo`. `null` se o doc não existe ou se estiver offline (best-effort). */
    suspend fun obterConteudoRemoto(): ConteudoRemoto?
}
