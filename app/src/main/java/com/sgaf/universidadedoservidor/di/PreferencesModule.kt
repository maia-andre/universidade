package com.sgaf.universidadedoservidor.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.sgaf.universidadedoservidor.core.data.preferences.UserPreferencesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Delegate único do DataStore por processo (recomendação da API).
private val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = UserPreferencesRepository.DATASTORE_NAME
)

@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {

    @Provides
    @Singleton
    fun provideUserPreferencesDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = context.userPreferencesDataStore
}
