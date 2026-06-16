package com.sgaf.universidadedoservidor.data.local.dao

import androidx.room.Dao
import androidx.room.Query

/** Linha bruta de um resultado de busca (JOIN curso/módulo/aula). */
data class SearchResultRow(
    val aulaId: Int,
    val aulaTitulo: String,
    val moduloTitulo: String,
    val cursoTitulo: String,
    val conteudo: String
)

@Dao
interface SearchDao {

    /**
     * Busca por termo no título/conteúdo das aulas e nos títulos de módulos/cursos.
     * Restrita a cursos disponíveis. Case-insensitive (LIKE no SQLite é por padrão
     * insensível a maiúsculas para ASCII).
     */
    @Query(
        """
        SELECT a.id AS aulaId, a.titulo AS aulaTitulo, m.titulo AS moduloTitulo,
               c.titulo AS cursoTitulo, a.conteudo AS conteudo
        FROM aulas a
        JOIN modulos m ON a.moduloId = m.id
        JOIN cursos c ON m.cursoId = c.id
        WHERE c.isAvailable = 1 AND (
            a.titulo LIKE '%' || :termo || '%' OR
            a.conteudo LIKE '%' || :termo || '%' OR
            m.titulo LIKE '%' || :termo || '%' OR
            c.titulo LIKE '%' || :termo || '%'
        )
        ORDER BY a.id ASC
        """
    )
    suspend fun buscar(termo: String): List<SearchResultRow>
}
