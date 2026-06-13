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
val ALL_MIGRATIONS: Array<Migration> = arrayOf(
    // Nenhuma migração ainda — baseline é a versão 3.
)
