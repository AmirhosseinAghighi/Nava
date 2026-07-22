package com.example.nava.data.search

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "search_history",
    primaryKeys = ["accountId", "normalizedQuery"],
)
data class SearchHistoryEntity(
    val accountId: String,
    val normalizedQuery: String,
    val query: String,
    val searchedAt: Long,
)

@Dao
interface SearchHistoryDao {
    @Query("select * from search_history where accountId = :accountId order by searchedAt desc limit :limit")
    fun observe(accountId: String, limit: Int = 12): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: SearchHistoryEntity)

    @Query("delete from search_history where accountId = :accountId and normalizedQuery = :normalizedQuery")
    suspend fun delete(accountId: String, normalizedQuery: String)

    @Query("delete from search_history where accountId = :accountId")
    suspend fun clear(accountId: String)
}
