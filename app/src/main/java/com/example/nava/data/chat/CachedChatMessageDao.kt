package com.example.nava.data.chat

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "cached_chat_messages")
data class CachedChatMessageEntity(
    @PrimaryKey val cacheId: String,
    val accountId: String,
    val conversationId: String,
    val messageId: String,
    val senderId: String,
    val senderName: String,
    val body: String?,
    val sharedTrackId: String?,
    val sharedTrackTitle: String?,
    val sharedTrackArtist: String?,
    val sharedTrackCoverUrl: String?,
    val createdAt: String,
    val status: String,
    val isMine: Boolean,
)

@Dao
interface CachedChatMessageDao {
    @Query(
        "select * from cached_chat_messages " +
            "where accountId = :accountId and conversationId = :conversationId " +
            "order by createdAt asc",
    )
    suspend fun getConversation(accountId: String, conversationId: String): List<CachedChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(messages: List<CachedChatMessageEntity>)
}
