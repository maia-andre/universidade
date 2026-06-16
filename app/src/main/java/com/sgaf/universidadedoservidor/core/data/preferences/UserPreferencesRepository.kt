package com.sgaf.universidadedoservidor.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Modo de tema escolhido pelo usuário (Item 2.1). */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

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

    /** Tema do app; SYSTEM por padrão (segue o sistema). */
    val themeMode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        when (prefs[KEY_THEME_MODE]) {
            ThemeMode.LIGHT.name -> ThemeMode.LIGHT
            ThemeMode.DARK.name -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode.name
        }
    }

    /**
     * Escala de fonte do leitor de aulas (Item 2.3). 1.0 = padrão.
     * Limitada a [FONT_SCALE_MIN, FONT_SCALE_MAX] para não quebrar o layout.
     */
    val fontScale: Flow<Float> = dataStore.data.map { prefs ->
        (prefs[KEY_FONT_SCALE] ?: 1.0f).coerceIn(FONT_SCALE_MIN, FONT_SCALE_MAX)
    }

    suspend fun setFontScale(scale: Float) {
        dataStore.edit { prefs ->
            prefs[KEY_FONT_SCALE] = scale.coerceIn(FONT_SCALE_MIN, FONT_SCALE_MAX)
        }
    }

    companion object {
        const val DATASTORE_NAME = "user_preferences"
        const val FONT_SCALE_MIN = 0.85f
        const val FONT_SCALE_MAX = 1.5f
        const val FONT_SCALE_STEP = 0.15f
        private val KEY_CURSO_ATIVO_ID = intPreferencesKey("curso_ativo_id")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_FONT_SCALE = floatPreferencesKey("font_scale")
    }
}
