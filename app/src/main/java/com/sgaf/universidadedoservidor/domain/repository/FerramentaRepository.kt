package com.sgaf.universidadedoservidor.domain.repository

import com.sgaf.universidadedoservidor.domain.model.FerramentaPreenchida
import com.sgaf.universidadedoservidor.domain.model.TipoFerramenta
import kotlinx.coroutines.flow.Flow

/** Persistência das ferramentas práticas preenchidas pelo aluno (v4 Item 2). */
interface FerramentaRepository {
    fun listar(tipo: TipoFerramenta): Flow<List<FerramentaPreenchida>>
    suspend fun obter(id: Long): FerramentaPreenchida?
    /** Cria (id = 0) ou atualiza uma ferramenta. Retorna o id resultante. */
    suspend fun salvar(id: Long, tipo: TipoFerramenta, titulo: String, campos: Map<String, String>): Long
    suspend fun excluir(id: Long)
}
