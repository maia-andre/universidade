package com.sgaf.universidadedoservidor.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sgaf.universidadedoservidor.data.local.dao.AulaDao
import com.sgaf.universidadedoservidor.data.local.dao.ModuloDao
import com.sgaf.universidadedoservidor.data.local.dao.ProgressoDao
import com.sgaf.universidadedoservidor.data.local.entities.AulaEntity
import com.sgaf.universidadedoservidor.data.local.entities.ModuloEntity
import com.sgaf.universidadedoservidor.data.local.entities.ProgressoEntity
import com.sgaf.universidadedoservidor.data.local.entities.AvaliacaoEntity
import com.sgaf.universidadedoservidor.data.local.dao.AvaliacaoDao
import com.sgaf.universidadedoservidor.data.local.dao.SearchDao
import com.sgaf.universidadedoservidor.data.local.entities.FerramentaEntity
import com.sgaf.universidadedoservidor.data.local.dao.FerramentaDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

import com.sgaf.universidadedoservidor.data.local.entities.CursoEntity
import com.sgaf.universidadedoservidor.data.local.dao.CursoDao

@Database(
    entities = [CursoEntity::class, ModuloEntity::class, AulaEntity::class, ProgressoEntity::class, AvaliacaoEntity::class, FerramentaEntity::class],
    version = 6,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun cursoDao(): CursoDao
    abstract fun moduloDao(): ModuloDao
    abstract fun aulaDao(): AulaDao
    abstract fun progressoDao(): ProgressoDao
    abstract fun avaliacaoDao(): AvaliacaoDao
    abstract fun searchDao(): SearchDao
    abstract fun ferramentaDao(): FerramentaDao

    companion object {
        private const val DATABASE_NAME = "universidade_database_v3"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context, coroutineScope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                // Migrações versionadas preservam o progresso dos usuários (beta ativo).
                // Destrutivo APENAS em downgrade (não ocorre em uso normal de loja).
                .addMigrations(*ALL_MIGRATIONS)
                .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                .addCallback(DatabaseCallback(context, coroutineScope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val context: Context,
        private val coroutineScope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val jsonString = context.assets.open("curso_data.json").bufferedReader().use { it.readText() }
                    val json = Json { ignoreUnknownKeys = true }
                    val coursesJson = json.decodeFromString<List<CourseJson>>(jsonString)
                    
                    val dbInstance = INSTANCE ?: return@launch
                    
                    val cursoEntities = coursesJson.map { course ->
                        CursoEntity(id = course.id, titulo = course.titulo, descricao = course.descricao)
                    }

                    val moduloEntities = coursesJson.flatMap { course -> 
                        course.modulos.map {
                            ModuloEntity(id = it.id, cursoId = course.id, titulo = it.titulo, descricao = it.descricao)
                        }
                    }
                    
                    val aulaEntities = coursesJson.flatMap { course -> 
                        course.modulos.flatMap { modulo ->
                        modulo.aulas.map { aula ->
                            val quizJsonString = json.encodeToString(
                                kotlinx.serialization.builtins.ListSerializer(QuizPerguntaJson.serializer()),
                                aula.quiz
                            )
                            val conteudoLido = aula.conteudo ?: aula.contentPath?.let { path ->
                                try {
                                    context.assets.open(path).bufferedReader().use { it.readText() }
                                } catch (e: Exception) {
                                    "Conteúdo não encontrado."
                                }
                            } ?: ""
                            
                            AulaEntity(
                                id = aula.id,
                                moduloId = modulo.id,
                                titulo = aula.titulo,
                                conteudo = conteudoLido,
                                quizJson = quizJsonString
                            )
                        }
                    }
                }
                    
                dbInstance.cursoDao().insertCursos(cursoEntities)
                dbInstance.moduloDao().insertModulos(moduloEntities)
                dbInstance.aulaDao().insertAulas(aulaEntities)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}

@Serializable
private data class CourseJson(
    val id: Int,
    val titulo: String,
    val descricao: String,
    val modulos: List<ModuloJson>
)

@Serializable
private data class ModuloJson(
    val id: Int,
    val titulo: String,
    val descricao: String,
    val aulas: List<AulaJson>
)

@Serializable
private data class AulaJson(
    val id: Int,
    val titulo: String,
    val conteudo: String? = null,
    val contentPath: String? = null,
    val quiz: List<QuizPerguntaJson> = emptyList()
)

@Serializable
data class QuizPerguntaJson(
    val pergunta: String,
    val opcoes: List<String>,
    val respostaCorretaIndex: Int
)
