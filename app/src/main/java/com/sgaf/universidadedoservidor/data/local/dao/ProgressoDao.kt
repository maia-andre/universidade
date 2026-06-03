package com.sgaf.universidadedoservidor.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sgaf.universidadedoservidor.data.local.entities.ProgressoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressoDao {
    @Query("SELECT * FROM progresso")
    fun getAllProgresso(): Flow<List<ProgressoEntity>>

    @Query("SELECT * FROM progresso WHERE aulaId = :aulaId")
    suspend fun getProgressoForAula(aulaId: Int): ProgressoEntity?

    @Query("SELECT * FROM progresso WHERE aulaId = :aulaId")
    fun getProgressoForAulaFlow(aulaId: Int): Flow<ProgressoEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgresso(progresso: ProgressoEntity)
}
