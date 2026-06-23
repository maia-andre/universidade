package com.sgaf.universidadedoservidor.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sgaf.universidadedoservidor.data.local.entities.ModuloEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ModuloDao {
    @Query("SELECT * FROM modulos ORDER BY id ASC")
    fun getModulos(): Flow<List<ModuloEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModulos(modulos: List<ModuloEntity>)

    /** Limpa a tabela de módulos (sync de conteúdo: full-replace; não toca no progresso). */
    @Query("DELETE FROM modulos")
    suspend fun deleteAll()
}
