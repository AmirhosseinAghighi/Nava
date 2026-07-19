package com.example.nava.data.downloads

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "offline_tracks")
data class OfflineTrackEntity(
    @PrimaryKey val trackId: String,
    val title: String,
    val artistName: String,
    val coverImageUrl: String,
    val audioPath: String,
    val downloadedAt: Long,
    val byteCount: Long,
)

@Dao
interface OfflineTrackDao {
    @Query("select * from offline_tracks order by downloadedAt desc")
    fun observeAll(): Flow<List<OfflineTrackEntity>>

    @Query("select * from offline_tracks order by downloadedAt desc")
    suspend fun getAll(): List<OfflineTrackEntity>

    @Query("select * from offline_tracks where trackId = :trackId limit 1")
    suspend fun find(trackId: String): OfflineTrackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(track: OfflineTrackEntity)

    @Query("delete from offline_tracks where trackId = :trackId")
    suspend fun delete(trackId: String)
}
