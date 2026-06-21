package com.sgaf.universidadedoservidor.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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

    /**
     * Cursos em que o aluno está matriculado (liberados pelo RH), espelho local da
     * coleção `matriculas` do Firestore — sincronizado na Home (v7, Item 1). Junto com
     * [cursosConcluidos] define o ACESSO: `acessível = matriculado OU concluído`.
     */
    val cursosMatriculados: Flow<Set<Int>> = dataStore.data.map { prefs ->
        prefs[KEY_CURSOS_MATRICULADOS]?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
    }

    /** Substitui o conjunto de matrículas (resultado do sync com o Firestore). */
    suspend fun setCursosMatriculados(ids: Set<Int>) {
        dataStore.edit { prefs ->
            prefs[KEY_CURSOS_MATRICULADOS] = ids.map { it.toString() }.toSet()
        }
    }

    /**
     * Cursos já concluídos pelo aluno. Estado **durável** (sobrevive à reinstalação):
     * alimentado tanto na emissão do certificado ([adicionarCursoConcluido]) quanto pelo
     * sync das `conclusoes` do Firestore ([mesclarCursosConcluidos]). Curso concluído fica
     * acessível para sempre (v7, Item 1).
     */
    val cursosConcluidos: Flow<Set<Int>> = dataStore.data.map { prefs ->
        prefs[KEY_CURSOS_CONCLUIDOS]?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
    }

    /** Marca um curso como concluído (chamado na emissão do certificado). Aditivo. */
    suspend fun adicionarCursoConcluido(cursoId: Int) {
        dataStore.edit { prefs ->
            val atual = prefs[KEY_CURSOS_CONCLUIDOS] ?: emptySet()
            prefs[KEY_CURSOS_CONCLUIDOS] = atual + cursoId.toString()
        }
    }

    /** Mescla os concluídos vindos do Firestore com os locais (sync downstream; nunca apaga). */
    suspend fun mesclarCursosConcluidos(ids: Set<Int>) {
        dataStore.edit { prefs ->
            val atual = prefs[KEY_CURSOS_CONCLUIDOS] ?: emptySet()
            prefs[KEY_CURSOS_CONCLUIDOS] = atual + ids.map { it.toString() }
        }
    }

    /** Ids dos cursos a que o aluno tem acesso: `matriculado OU concluído` (v7, Item 1). */
    val cursosAcessiveis: Flow<Set<Int>> =
        combine(cursosMatriculados, cursosConcluidos) { matriculados, concluidos ->
            matriculados + concluidos
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

    /** Alto contraste (acessibilidade, v4). Off por padrão. */
    val highContrast: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_HIGH_CONTRAST] ?: false
    }

    suspend fun setHighContrast(ativo: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_HIGH_CONTRAST] = ativo }
    }

    /** Redução de movimento: desabilita animações/transições (acessibilidade, v4). Off por padrão. */
    val reducedMotion: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_REDUCED_MOTION] ?: false
    }

    suspend fun setReducedMotion(ativo: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_REDUCED_MOTION] = ativo }
    }

    companion object {
        const val DATASTORE_NAME = "user_preferences"
        const val FONT_SCALE_MIN = 0.85f
        const val FONT_SCALE_MAX = 1.5f
        const val FONT_SCALE_STEP = 0.15f
        private val KEY_CURSO_ATIVO_ID = intPreferencesKey("curso_ativo_id")
        private val KEY_CURSOS_MATRICULADOS = stringSetPreferencesKey("cursos_matriculados")
        private val KEY_CURSOS_CONCLUIDOS = stringSetPreferencesKey("cursos_concluidos")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_FONT_SCALE = floatPreferencesKey("font_scale")
        private val KEY_HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
        private val KEY_REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
    }
}
