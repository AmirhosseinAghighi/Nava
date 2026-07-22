package com.example.nava.data.downloads

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.nava.data.chat.CachedChatMessageDao
import com.example.nava.data.chat.CachedChatMessageEntity
import com.example.nava.data.search.SearchHistoryDao
import com.example.nava.data.search.SearchHistoryEntity

@Database(
    entities = [OfflineTrackEntity::class, CachedChatMessageEntity::class, SearchHistoryEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class NavaDatabase : RoomDatabase() {
    abstract fun offlineTrackDao(): OfflineTrackDao
    abstract fun cachedChatMessageDao(): CachedChatMessageDao
    abstract fun searchHistoryDao(): SearchHistoryDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    create table if not exists search_history (
                        accountId text not null,
                        normalizedQuery text not null,
                        query text not null,
                        searchedAt integer not null,
                        primary key(accountId, normalizedQuery)
                    )
                    """.trimIndent(),
                )
            }
        }
    }
}
