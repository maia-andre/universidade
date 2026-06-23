package com.sgaf.universidadedoservidor.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.sgaf.universidadedoservidor.core.util.await
import com.sgaf.universidadedoservidor.domain.repository.ConteudoRemoto
import com.sgaf.universidadedoservidor.domain.repository.ConteudoRemotoRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConteudoRemotoRepositoryImpl @Inject constructor() : ConteudoRemotoRepository {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // Um único doc `config/conteudo` carrega o catálogo inteiro (campo `json`, ~42 KB << 1 MiB)
    // versionado por `versao`. As security rules já liberam leitura por autenticados.
    override suspend fun obterConteudoRemoto(): ConteudoRemoto? = runCatching {
        val doc = db.collection("config").document("conteudo").get().await()
        val versao = doc.getLong("versao")?.toInt()
        val json = doc.getString("json")
        if (doc.exists() && versao != null && json != null) ConteudoRemoto(versao, json) else null
    }.getOrNull()
}
