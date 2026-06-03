package com.sgaf.universidadedoservidor.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sgaf.universidadedoservidor.data.local.entities.AulaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AulaDao {
    @Query("SELECT * FROM aulas WHERE moduloId = :moduloId ORDER BY id ASC")
    fun getAulasByModulo(moduloId: Int): Flow<List<AulaEntity>>

    @Query("SELECT * FROM aulas WHERE id = :aulaId")
    suspend fun getAulaById(aulaId: Int): AulaEntity?

    @Query("SELECT * FROM aulas")
    fun getAllAulas(): Flow<List<AulaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAulas(aulas: List<AulaEntity>)
}
