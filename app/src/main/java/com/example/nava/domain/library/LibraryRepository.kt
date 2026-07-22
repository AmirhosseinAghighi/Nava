package com.example.nava.domain.library

import com.example.nava.domain.catalog.HomeTrack

data class UserPlaylist(
    val id: String,
    val title: String,
    val description: String?,
    val coverImageUrl: String?,
    val isPublic: Boolean,
    val trackCount: Int,
)

data class PlaylistTrack(
    val id: String,
    val title: String,
    val artistName: String,
    val coverImageUrl: String,
    val audioUrl: String,
    val durationSeconds: Int,
    val languageCode: String,
) {
    fun toHomeTrack() = HomeTrack(id, title, artistName, coverImageUrl, audioUrl, languageCode)
}

data class PlaylistDetails(val playlist: UserPlaylist, val tracks: List<PlaylistTrack>)
data class LibrarySummary(val playlists: List<UserPlaylist> = emptyList(), val likedCount: Long = 0)
data class PlaylistCoverUpload(val bytes: ByteArray, val extension: String)

interface LibraryRepository {
    suspend fun load(): Result<LibrarySummary>
    suspend fun loadPlaylist(playlistId: String): Result<PlaylistDetails>
    suspend fun loadCatalog(): Result<List<PlaylistTrack>>
    suspend fun createPlaylist(title: String, description: String?, isPublic: Boolean, cover: PlaylistCoverUpload?): Result<Unit>
    suspend fun updatePlaylist(playlistId: String, title: String, description: String?, isPublic: Boolean, cover: PlaylistCoverUpload?): Result<Unit>
    suspend fun deletePlaylist(playlistId: String): Result<Unit>
    suspend fun addTrack(playlistId: String, trackId: String): Result<Unit>
    suspend fun removeTrack(playlistId: String, trackId: String): Result<Unit>
}
