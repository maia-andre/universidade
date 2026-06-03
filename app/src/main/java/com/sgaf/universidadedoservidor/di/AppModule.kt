package com.sgaf.universidadedoservidor.di

import android.content.Context
import com.sgaf.universidadedoservidor.data.local.dao.AulaDao
import com.sgaf.universidadedoservidor.data.local.dao.ModuloDao
import com.sgaf.universidadedoservidor.data.local.dao.ProgressoDao
import com.sgaf.universidadedoservidor.data.local.database.AppDatabase
import com.sgaf.universidadedoservidor.data.repository.CursoRepositoryImpl
import com.sgaf.universidadedoservidor.domain.repository.CursoRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindCursoRepository(
        impl: CursoRepositoryImpl
    ): CursoRepository

    companion object {

        @Provides
        @Singleton
        fun provideCoroutineScope(): CoroutineScope {
            return CoroutineScope(SupervisorJob())
        }

        @Provides
        @Singleton
        fun provideAppDatabase(
            @ApplicationContext context: Context,
            coroutineScope: CoroutineScope
        ): AppDatabase {
            return AppDatabase.getInstance(context, coroutineScope)
        }

        @Provides
        @Singleton
        fun provideModuloDao(database: AppDatabase): ModuloDao {
            return database.moduloDao()
        }

        @Provides
        @Singleton
        fun provideAulaDao(database: AppDatabase): AulaDao {
            return database.aulaDao()
        }

        @Provides
        @Singleton
        fun provideProgressoDao(database: AppDatabase): ProgressoDao {
            return database.progressoDao()
        }
    }
}
