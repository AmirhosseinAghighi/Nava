package com.example.nava.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.nava.data.chat.CachedChatMessageDao
import com.example.nava.data.downloads.NavaDatabase
import com.example.nava.data.downloads.OfflineTrackDao
import com.example.nava.data.auth.SupabaseAuthRepository
import com.example.nava.data.catalog.SupabaseHomeRepository
import com.example.nava.data.catalog.SupabaseSearchRepository
import com.example.nava.data.preferences.DataStorePreferencesRepository
import com.example.nava.data.library.SupabaseLibraryRepository
import com.example.nava.data.downloads.OfflineDownloadRepository
import com.example.nava.domain.auth.AuthRepository
import com.example.nava.domain.catalog.HomeRepository
import com.example.nava.domain.catalog.SearchRepository
import com.example.nava.domain.preferences.PreferencesRepository
import com.example.nava.domain.library.LibraryRepository
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
    @Binds @Singleton abstract fun bindSearchRepository(repository: SupabaseSearchRepository): SearchRepository
    @Binds @Singleton abstract fun bindLibraryRepository(repository: SupabaseLibraryRepository): LibraryRepository
    @Binds @Singleton abstract fun bindPreferencesRepository(repository: DataStorePreferencesRepository): PreferencesRepository
}

@Module
@InstallIn(SingletonComponent::class)
object ContextModule {
    @Provides @Singleton fun provideContext(@ApplicationContext context: Context): Context = context
    @Provides @Singleton fun provideDatabase(context: Context): NavaDatabase =
        Room.databaseBuilder(context, NavaDatabase::class.java, "nava.db")
            .addMigrations(MIGRATION_1_2)
            .build()
    @Provides fun provideOfflineTrackDao(database: NavaDatabase): OfflineTrackDao = database.offlineTrackDao()
    @Provides fun provideCachedChatMessageDao(database: NavaDatabase): CachedChatMessageDao = database.cachedChatMessageDao()

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                create table if not exists cached_chat_messages (
                    cacheId text not null primary key,
                    accountId text not null,
                    conversationId text not null,
                    messageId text not null,
                    senderId text not null,
                    senderName text not null,
                    body text,
                    sharedTrackId text,
                    sharedTrackTitle text,
                    sharedTrackArtist text,
                    sharedTrackCoverUrl text,
                    createdAt text not null,
                    status text not null,
                    isMine integer not null
                )
                """.trimIndent(),
            )
        }
    }
}
