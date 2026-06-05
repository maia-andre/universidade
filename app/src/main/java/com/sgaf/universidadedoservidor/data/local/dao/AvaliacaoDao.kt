package com.sgaf.universidadedoservidor.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sgaf.universidadedoservidor.data.local.entities.AvaliacaoEntity

@Dao
interface AvaliacaoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAvaliacao(avaliacao: AvaliacaoEntity)

    @Query("SELECT * FROM avaliacoes WHERE cursoId = :cursoId")
    suspend fun getAvaliacaoForCurso(cursoId: Int): AvaliacaoEntity?
}
