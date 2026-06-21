package com.sgaf.universidadedoservidor.di

import android.content.Context
import com.sgaf.universidadedoservidor.data.local.dao.AulaDao
import com.sgaf.universidadedoservidor.data.local.dao.CursoDao
import com.sgaf.universidadedoservidor.data.local.dao.ModuloDao
import com.sgaf.universidadedoservidor.data.local.dao.ProgressoDao
import com.sgaf.universidadedoservidor.data.local.dao.SearchDao
import com.sgaf.universidadedoservidor.data.local.dao.AvaliacaoDao
import com.sgaf.universidadedoservidor.data.local.dao.FerramentaDao
import com.sgaf.universidadedoservidor.data.local.dao.ProvaFinalDao
import com.sgaf.universidadedoservidor.data.local.database.AppDatabase
import com.sgaf.universidadedoservidor.data.repository.CursoRepositoryImpl
import com.sgaf.universidadedoservidor.data.repository.FerramentaRepositoryImpl
import com.sgaf.universidadedoservidor.data.repository.ProvaFinalRepositoryImpl
import com.sgaf.universidadedoservidor.domain.repository.CursoRepository
import com.sgaf.universidadedoservidor.domain.repository.FerramentaRepository
import com.sgaf.universidadedoservidor.domain.repository.ProvaFinalRepository
import com.sgaf.universidadedoservidor.core.data.repository.FirebaseAuthRepositoryImpl
import com.sgaf.universidadedoservidor.core.domain.repository.AuthRepository
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

    @Binds
    @Singleton
    abstract fun bindFerramentaRepository(
        impl: FerramentaRepositoryImpl
    ): FerramentaRepository

    @Binds
    @Singleton
    abstract fun bindProvaFinalRepository(
        impl: ProvaFinalRepositoryImpl
    ): ProvaFinalRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: FirebaseAuthRepositoryImpl
    ): AuthRepository

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
        fun provideCursoDao(database: AppDatabase): CursoDao {
            return database.cursoDao()
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

        @Provides
        @Singleton
        fun provideSearchDao(database: AppDatabase): SearchDao {
            return database.searchDao()
        }

        @Provides
        @Singleton
        fun provideAvaliacaoDao(database: AppDatabase): AvaliacaoDao {
            return database.avaliacaoDao()
        }

        @Provides
        @Singleton
        fun provideFerramentaDao(database: AppDatabase): FerramentaDao {
            return database.ferramentaDao()
        }

        @Provides
        @Singleton
        fun provideProvaFinalDao(database: AppDatabase): ProvaFinalDao {
            return database.provaFinalDao()
        }
    }
}
