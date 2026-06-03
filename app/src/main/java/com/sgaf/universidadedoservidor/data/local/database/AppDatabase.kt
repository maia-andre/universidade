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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Database(
    entities = [ModuloEntity::class, AulaEntity::class, ProgressoEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun moduloDao(): ModuloDao
    abstract fun aulaDao(): AulaDao
    abstract fun progressoDao(): ProgressoDao

    companion object {
        private const val DATABASE_NAME = "universidade_database"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context, coroutineScope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
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
                    val courseJson = json.decodeFromString<CourseJson>(jsonString)
                    
                    val dbInstance = INSTANCE ?: return@launch
                    
                    val moduloEntities = courseJson.modulos.map {
                        ModuloEntity(id = it.id, titulo = it.titulo, descricao = it.descricao)
                    }
                    
                    val aulaEntities = courseJson.modulos.flatMap { modulo ->
                        modulo.aulas.map { aula ->
                            val quizJsonString = json.encodeToString(
                                kotlinx.serialization.builtins.ListSerializer(QuizPerguntaJson.serializer()),
                                aula.quiz
                            )
                            AulaEntity(
                                id = aula.id,
                                moduloId = modulo.id,
                                titulo = aula.titulo,
                                conteudo = aula.conteudo,
                                quizJson = quizJsonString
                            )
                        }
                    }
                    
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
    val conteudo: String,
    val quiz: List<QuizPerguntaJson> = emptyList()
)

@Serializable
data class QuizPerguntaJson(
    val pergunta: String,
    val opcoes: List<String>,
    val respostaCorretaIndex: Int
)
