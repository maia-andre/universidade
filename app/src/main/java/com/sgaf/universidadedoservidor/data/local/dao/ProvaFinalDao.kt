package com.sgaf.universidadedoservidor.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sgaf.universidadedoservidor.data.local.entities.ProvaFinalResultadoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProvaFinalDao {
    @Query("SELECT * FROM prova_final_resultado WHERE cursoId = :cursoId LIMIT 1")
    fun getResultadoFlow(cursoId: Int): Flow<ProvaFinalResultadoEntity?>

    @Query("SELECT * FROM prova_final_resultado WHERE cursoId = :cursoId LIMIT 1")
    suspend fun getResultado(cursoId: Int): ProvaFinalResultadoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(resultado: ProvaFinalResultadoEntity)
}
