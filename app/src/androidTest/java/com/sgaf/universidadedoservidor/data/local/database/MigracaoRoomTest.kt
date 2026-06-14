package com.sgaf.universidadedoservidor.data.local.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Testes de migração do Room (v4 Item 4.3). Garantem que as migrações v3→v4→v5
 * preservam o progresso do usuário — requisito crítico com beta ativo na Play Store.
 *
 * Instrumentado: executar com `./gradlew connectedAndroidTest` (precisa de device/emulador).
 * Usa os schemas exportados em app/schemas/ (expostos como assets de androidTest no build.gradle).
 */
@RunWith(AndroidJUnit4::class)
class MigracaoRoomTest {

    private val dbName = "migracao-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun migra_v3_para_v6_preservando_progresso() {
        // Cria o banco na v3 e insere um progresso (esquema antigo: sem colunas de quiz/acesso).
        helper.createDatabase(dbName, 3).apply {
            execSQL(
                "INSERT INTO progresso (aulaId, isCompleted, isFavorite) VALUES (1, 1, 1)"
            )
            close()
        }

        // Aplica v3→v4→v5→v6 e valida o esquema final contra o schema exportado.
        val db = helper.runMigrationsAndValidate(
            dbName, 6, true, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6
        )

        db.query(
            "SELECT isCompleted, isFavorite, quizSubmitted, quizAcertos, " +
                "quizRespostasJson, ultimoAcessoEm FROM progresso WHERE aulaId = 1"
        ).use { cursor ->
            assertTrue("linha de progresso deve sobreviver à migração", cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0)) // isCompleted preservado
            assertEquals(1, cursor.getInt(1)) // isFavorite preservado
            // Colunas do quiz (v4) existem com os defaults da migração.
            assertEquals(0, cursor.getInt(2)) // quizSubmitted
            assertEquals(0, cursor.getInt(3)) // quizAcertos
            assertEquals("", cursor.getString(4)) // quizRespostasJson
            assertEquals(0L, cursor.getLong(5)) // ultimoAcessoEm
        }

        // A tabela de ferramentas (v6) deve existir e estar vazia.
        db.query("SELECT COUNT(*) FROM ferramentas").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
    }
}
