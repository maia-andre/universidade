package com.sgaf.universidadedoservidor.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sgaf.universidadedoservidor.data.local.entities.CursoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CursoDao {
    @Query("SELECT * FROM cursos ORDER BY id ASC")
    fun getCursos(): Flow<List<CursoEntity>>

    @Query("SELECT * FROM cursos WHERE id = :id LIMIT 1")
    fun getCursoById(id: Int): Flow<CursoEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCursos(cursos: List<CursoEntity>)

    /** Limpa a tabela de cursos (sync de conteúdo: full-replace; não toca no progresso). */
    @Query("DELETE FROM cursos")
    suspend fun deleteAll()
}
