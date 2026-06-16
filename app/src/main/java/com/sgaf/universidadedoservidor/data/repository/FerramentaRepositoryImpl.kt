package com.sgaf.universidadedoservidor.data.repository

import com.sgaf.universidadedoservidor.data.local.dao.FerramentaDao
import com.sgaf.universidadedoservidor.data.local.entities.FerramentaEntity
import com.sgaf.universidadedoservidor.domain.model.FerramentaPreenchida
import com.sgaf.universidadedoservidor.domain.model.TipoFerramenta
import com.sgaf.universidadedoservidor.domain.repository.FerramentaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FerramentaRepositoryImpl @Inject constructor(
    private val ferramentaDao: FerramentaDao
) : FerramentaRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val camposSerializer = MapSerializer(String.serializer(), String.serializer())

    override fun listar(tipo: TipoFerramenta): Flow<List<FerramentaPreenchida>> =
        ferramentaDao.getByTipo(tipo.name).map { lista -> lista.map { it.toDomain() } }

    override suspend fun obter(id: Long): FerramentaPreenchida? =
        ferramentaDao.getById(id)?.toDomain()

    override suspend fun salvar(
        id: Long,
        tipo: TipoFerramenta,
        titulo: String,
        campos: Map<String, String>
    ): Long {
        val entity = FerramentaEntity(
            id = id,
            tipo = tipo.name,
            titulo = titulo,
            camposJson = json.encodeToString(camposSerializer, campos),
            criadoEm = System.currentTimeMillis()
        )
        // insert com REPLACE também atualiza quando o id já existe.
        return if (id == 0L) ferramentaDao.insert(entity) else {
            ferramentaDao.update(entity); id
        }
    }

    override suspend fun excluir(id: Long) {
        val existente = ferramentaDao.getById(id) ?: return
        ferramentaDao.delete(existente)
    }

    private fun FerramentaEntity.toDomain(): FerramentaPreenchida {
        val campos = try {
            json.decodeFromString(camposSerializer, camposJson)
        } catch (e: Exception) {
            emptyMap()
        }
        return FerramentaPreenchida(
            id = id,
            tipo = TipoFerramenta.fromName(tipo),
            titulo = titulo,
            campos = campos,
            criadoEm = criadoEm
        )
    }
}
