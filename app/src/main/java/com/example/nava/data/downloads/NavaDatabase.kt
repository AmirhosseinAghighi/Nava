package com.example.nava.data.downloads

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.nava.data.chat.CachedChatMessageDao
import com.example.nava.data.chat.CachedChatMessageEntity

@Database(entities = [OfflineTrackEntity::class, CachedChatMessageEntity::class], version = 2, exportSchema = false)
abstract class NavaDatabase : RoomDatabase() {
    abstract fun offlineTrackDao(): OfflineTrackDao
    abstract fun cachedChatMessageDao(): CachedChatMessageDao
}
