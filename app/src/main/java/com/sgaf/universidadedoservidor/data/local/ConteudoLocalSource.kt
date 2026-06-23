package com.sgaf.universidadedoservidor.data.local

import android.content.Context
import com.sgaf.universidadedoservidor.data.local.database.QuizPerguntaJson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fonte única do catálogo de conteúdo (cursos/módulos/aulas/quiz/provaFinal).
 *
 * Lê do arquivo [ARQUIVO_REMOTO] em `filesDir` quando ele existe — conteúdo dinâmico publicado
 * pelo RH e sincronizado (V8 Item 1) — e, na ausência dele, do asset [ASSET_BASELINE] embarcado
 * no APK. Assim o app:
 *  - nunca abre vazio (há sempre o baseline offline no APK);
 *  - reflete edições do RH **sem novo APK** (o arquivo sobrepõe o baseline após o sync).
 *
 * O parse é cacheado e invalidado por [salvarRemoto]. É a base dos leitores em runtime
 * (carga horária e prova final) e do full-replace do Room feito no sync.
 */
@Singleton
class ConteudoLocalSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()

    @Volatile
    private var cache: List<ConteudoCursoJson>? = null

    private val arquivoRemoto: File
        get() = File(context.filesDir, ARQUIVO_REMOTO)

    /** Catálogo atual: do arquivo remoto se existir, senão do asset baseline. Cacheado. */
    suspend fun catalogo(): List<ConteudoCursoJson> {
        cache?.let { return it }
        return mutex.withLock {
            cache ?: carregar().also { cache = it }
        }
    }

    /**
     * Persiste o JSON publicado (campo `json` do doc `config/conteudo`) e invalida o cache,
     * para que a próxima [catalogo] reflita o conteúdo novo.
     */
    suspend fun salvarRemoto(jsonCatalogo: String) {
        withContext(Dispatchers.IO) { arquivoRemoto.writeText(jsonCatalogo) }
        mutex.withLock { cache = null }
    }

    /** Faz o parse (puro) de um JSON de catálogo; lista vazia se inválido. Usado para validar. */
    fun parse(jsonCatalogo: String): List<ConteudoCursoJson> = runCatching {
        json.decodeFromString(ListSerializer(ConteudoCursoJson.serializer()), jsonCatalogo)
    }.getOrDefault(emptyList())

    /**
     * Markdown de uma aula: o [ConteudoAulaJson.conteudo] inline quando presente (o conteúdo
     * remoto já vem embutido), senão resolve o [ConteudoAulaJson.contentPath] do asset (baseline).
     * Vazio se nada resolver.
     */
    suspend fun resolverConteudo(aula: ConteudoAulaJson): String {
        aula.conteudo?.let { return it }
        val path = aula.contentPath ?: return ""
        return withContext(Dispatchers.IO) {
            runCatching {
                context.assets.open(path).bufferedReader().use { it.readText() }
            }.getOrDefault("")
        }
    }

    private suspend fun carregar(): List<ConteudoCursoJson> = withContext(Dispatchers.IO) {
        val texto = runCatching {
            arquivoRemoto.takeIf { it.exists() }?.readText()
                ?: context.assets.open(ASSET_BASELINE).bufferedReader().use { it.readText() }
        }.getOrElse {
            // Arquivo remoto ilegível: cai para o baseline embarcado.
            context.assets.open(ASSET_BASELINE).bufferedReader().use { it.readText() }
        }
        parse(texto)
    }

    companion object {
        const val ASSET_BASELINE = "curso_data.json"
        const val ARQUIVO_REMOTO = "conteudo_remoto.json"
    }
}

@Serializable
data class ConteudoCursoJson(
    val id: Int,
    val titulo: String,
    val descricao: String,
    val cargaHoraria: Int? = null,
    val provaFinal: List<QuizPerguntaJson> = emptyList(),
    val modulos: List<ConteudoModuloJson> = emptyList()
)

@Serializable
data class ConteudoModuloJson(
    val id: Int,
    val titulo: String,
    val descricao: String,
    val aulas: List<ConteudoAulaJson> = emptyList()
)

@Serializable
data class ConteudoAulaJson(
    val id: Int,
    val titulo: String,
    val conteudo: String? = null,
    val contentPath: String? = null,
    val quiz: List<QuizPerguntaJson> = emptyList()
)
