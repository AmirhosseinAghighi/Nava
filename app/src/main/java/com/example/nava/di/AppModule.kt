package com.example.nava.di

import android.content.Context
import com.example.nava.data.auth.SupabaseAuthRepository
import com.example.nava.data.catalog.SupabaseHomeRepository
import com.example.nava.data.preferences.DataStorePreferencesRepository
import com.example.nava.domain.auth.AuthRepository
import com.example.nava.domain.catalog.HomeRepository
import com.example.nava.domain.preferences.PreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.Provides
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton abstract fun bindAuthRepository(repository: SupabaseAuthRepository): AuthRepository
    @Binds @Singleton abstract fun bindHomeRepository(repository: SupabaseHomeRepository): HomeRepository
    @Binds @Singleton abstract fun bindPreferencesRepository(repository: DataStorePreferencesRepository): PreferencesRepository
}

@Module
@InstallIn(SingletonComponent::class)
object ContextModule {
    @Provides @Singleton fun provideContext(@ApplicationContext context: Context): Context = context
}
