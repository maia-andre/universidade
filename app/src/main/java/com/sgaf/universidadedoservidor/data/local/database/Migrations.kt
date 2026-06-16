package com.sgaf.universidadedoservidor.data.local.database

import androidx.room.migration.Migration

/**
 * Migrações versionadas do banco.
 *
 * REGRA DO PROJETO (a partir da branch new_features): com o beta ativo na Play Store,
 * NUNCA use migração destrutiva em upgrade. Todo incremento de [AppDatabase.version]
 * deve vir acompanhado de um objeto [Migration] aqui e ser registrado em [ALL_MIGRATIONS],
 * preservando o progresso dos usuários (aulas concluídas, favoritos, notas de quiz).
 *
 * Como adicionar uma migração:
 *   1. Suba a `version` em [AppDatabase] (ex.: 3 -> 4).
 *   2. Crie `val MIGRATION_3_4 = Migration(3, 4) { db -> db.execSQL("ALTER TABLE ...") }`.
 *   3. Adicione-a a [ALL_MIGRATIONS].
 *   4. Compile: o Room valida o schema final contra o JSON exportado em app/schemas.
 */
/**
 * v3 -> v4 (Item 1.2): persistência do estado do quiz na tabela `progresso`.
 * Adiciona colunas preservando todo o progresso existente (concluídas/favoritos).
 */
val MIGRATION_3_4 = Migration(3, 4) { db ->
    db.execSQL("ALTER TABLE progresso ADD COLUMN quizSubmitted INTEGER NOT NULL DEFAULT 0")
    db.execSQL("ALTER TABLE progresso ADD COLUMN quizAcertos INTEGER NOT NULL DEFAULT 0")
    db.execSQL("ALTER TABLE progresso ADD COLUMN quizRespostasJson TEXT NOT NULL DEFAULT ''")
}

/**
 * v4 -> v5 (Item 2.2): registra o último acesso de cada aula para "continuar de onde parou".
 */
val MIGRATION_4_5 = Migration(4, 5) { db ->
    db.execSQL("ALTER TABLE progresso ADD COLUMN ultimoAcessoEm INTEGER NOT NULL DEFAULT 0")
}

val ALL_MIGRATIONS: Array<Migration> = arrayOf(
    MIGRATION_3_4,
    MIGRATION_4_5,
)
