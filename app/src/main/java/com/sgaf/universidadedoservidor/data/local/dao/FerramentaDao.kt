package com.sgaf.universidadedoservidor.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sgaf.universidadedoservidor.data.local.entities.FerramentaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FerramentaDao {

    @Query("SELECT * FROM ferramentas WHERE tipo = :tipo ORDER BY criadoEm DESC")
    fun getByTipo(tipo: String): Flow<List<FerramentaEntity>>

    @Query("SELECT * FROM ferramentas WHERE id = :id")
    suspend fun getById(id: Long): FerramentaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ferramenta: FerramentaEntity): Long

    @Update
    suspend fun update(ferramenta: FerramentaEntity)

    @Delete
    suspend fun delete(ferramenta: FerramentaEntity)
}
