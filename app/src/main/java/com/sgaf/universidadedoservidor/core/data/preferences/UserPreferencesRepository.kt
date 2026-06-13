package com.sgaf.universidadedoservidor.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Preferências locais do usuário (offline) via DataStore.
 *
 * Ponto único para escolhas persistidas que não pertencem ao banco de conteúdo,
 * como o "curso ativo" (regra de 1 curso por vez) e, futuramente, o tema (Item 2.1).
 */
@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    /** Id do curso que o aluno está cursando atualmente; null se ainda não definido. */
    val cursoAtivoId: Flow<Int?> = dataStore.data.map { prefs ->
        prefs[KEY_CURSO_ATIVO_ID]
    }

    suspend fun setCursoAtivo(cursoId: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_CURSO_ATIVO_ID] = cursoId
        }
    }

    companion object {
        const val DATASTORE_NAME = "user_preferences"
        private val KEY_CURSO_ATIVO_ID = intPreferencesKey("curso_ativo_id")
    }
}
