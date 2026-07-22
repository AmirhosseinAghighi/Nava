package com.example.nava.data.library

import com.example.nava.data.catalog.toPublicCoverUrl
import com.example.nava.domain.library.LibraryRepository
import com.example.nava.domain.library.LibrarySummary
import com.example.nava.domain.library.PlaylistDetails
import com.example.nava.domain.library.PlaylistTrack
import com.example.nava.domain.library.UserPlaylist
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import javax.inject.Inject
import javax.inject.Singleton

@Singleton class SupabaseLibraryRepository @Inject constructor(private val supabase: SupabaseClient) : LibraryRepository {
    override suspend fun load(): Result<LibrarySummary> = runCatching {
        val userId = requireUserId()
        val playlists = supabase.from("playlists").select {
            filter { eq("owner_id", userId) }
        }.decodeList<PlaylistDto>()
        val memberships = supabase.from("playlist_tracks").select().decodeList<PlaylistTrackRow>()
        val catalog = loadCatalogRows()
        val tracksById = catalog.associateBy(TrackCardDto::id)
        val membershipsByPlaylist = memberships.groupBy(PlaylistTrackRow::playlistId)
        val likes = supabase.from("user_track_likes").select().decodeList<LikeDto>().size.toLong()
        LibrarySummary(
            playlists = playlists.map { playlist ->
                val playlistTracks = membershipsByPlaylist[playlist.id].orEmpty().sortedBy(PlaylistTrackRow::position)
                playlist.toDomain(
                    trackCount = playlistTracks.size,
                    fallbackCover = playlistTracks.firstNotNullOfOrNull { tracksById[it.trackId]?.coverImageUrl },
                )
            },
            likedCount = likes,
        )
    }

    override suspend fun loadPlaylist(playlistId: String): Result<PlaylistDetails> = runCatching {
        val playlist = supabase.from("playlists").select {
            filter { eq("id", playlistId) }
        }.decodeSingle<PlaylistDto>()
        val memberships = membershipsFor(playlistId)
        val catalog = loadCatalogRows().associateBy(TrackCardDto::id)
        val tracks = memberships.mapNotNull { row -> catalog[row.trackId]?.toDomain() }
        PlaylistDetails(
            playlist = playlist.toDomain(tracks.size, tracks.firstOrNull()?.coverImageUrl),
            tracks = tracks,
        )
    }

    override suspend fun loadCatalog(): Result<List<PlaylistTrack>> = runCatching {
        loadCatalogRows().map { it.toDomain() }
    }

    override suspend fun createPlaylist(title: String, description: String?, isPublic: Boolean): Result<Unit> = runCatching {
        supabase.from("playlists").insert(
            CreatePlaylistDto(requireUserId(), title.trim(), description.clean(), isPublic),
        )
    }

    override suspend fun updatePlaylist(
        playlistId: String,
        title: String,
        description: String?,
        isPublic: Boolean,
    ): Result<Unit> = runCatching {
        supabase.from("playlists").update(EditPlaylistDto(title.trim(), description.clean(), isPublic)) {
            filter { eq("id", playlistId) }
        }
    }

    override suspend fun deletePlaylist(playlistId: String): Result<Unit> = runCatching {
        supabase.from("playlists").delete { filter { eq("id", playlistId) } }
    }

    override suspend fun addTrack(playlistId: String, trackId: String): Result<Unit> = runCatching {
        val memberships = membershipsFor(playlistId)
        if (memberships.none { it.trackId == trackId }) {
            val nextPosition = (memberships.maxOfOrNull(PlaylistTrackRow::position) ?: -1) + 1
            supabase.from("playlist_tracks").insert(AddPlaylistTrackDto(playlistId, trackId, nextPosition))
        }
    }

    override suspend fun removeTrack(playlistId: String, trackId: String): Result<Unit> = runCatching {
        supabase.from("playlist_tracks").delete {
            filter {
                eq("playlist_id", playlistId)
                eq("track_id", trackId)
            }
        }
    }

    private suspend fun membershipsFor(playlistId: String): List<PlaylistTrackRow> =
        supabase.from("playlist_tracks").select {
            filter { eq("playlist_id", playlistId) }
        }.decodeList<PlaylistTrackRow>().sortedBy(PlaylistTrackRow::position)

    private suspend fun loadCatalogRows(): List<TrackCardDto> =
        supabase.from("home_track_cards").select().decodeList()

    private fun requireUserId(): String = supabase.auth.currentUserOrNull()?.id ?: error("Not signed in")

    private fun PlaylistDto.toDomain(trackCount: Int, fallbackCover: String?) = UserPlaylist(
        id = id,
        title = title,
        description = description,
        coverImageUrl = (coverImageUrl ?: fallbackCover)?.toPublicCoverUrl(supabase),
        isPublic = isPublic,
        trackCount = trackCount,
    )

    private fun TrackCardDto.toDomain() = PlaylistTrack(
        id = id,
        title = title,
        artistName = artistName,
        coverImageUrl = coverImageUrl.toPublicCoverUrl(supabase),
        audioUrl = audioUrl,
        durationSeconds = durationSeconds,
        languageCode = languageCode,
    )

    private fun String?.clean(): String? = this?.trim()?.takeIf(String::isNotEmpty)
}

@Serializable private data class PlaylistDto(
    val id: String,
    val title: String,
    val description: String? = null,
    @SerialName("cover_image_url") val coverImageUrl: String? = null,
    @SerialName("is_public") val isPublic: Boolean = false,
)

@Serializable private data class PlaylistTrackRow(
    @SerialName("playlist_id") val playlistId: String,
    @SerialName("track_id") val trackId: String,
    val position: Int,
)

@Serializable private data class TrackCardDto(
    val id: String,
    val title: String,
    @SerialName("artist_name") val artistName: String,
    @SerialName("cover_image_url") val coverImageUrl: String,
    @SerialName("audio_url") val audioUrl: String,
    @SerialName("duration_seconds") val durationSeconds: Int,
    @SerialName("language_code") val languageCode: String,
)

@Serializable private data class CreatePlaylistDto(
    @SerialName("owner_id") val ownerId: String,
    val title: String,
    val description: String?,
    @SerialName("is_public") val isPublic: Boolean,
)

@Serializable private data class EditPlaylistDto(
    val title: String,
    val description: String?,
    @SerialName("is_public") val isPublic: Boolean,
)

@Serializable private data class AddPlaylistTrackDto(
    @SerialName("playlist_id") val playlistId: String,
    @SerialName("track_id") val trackId: String,
    val position: Int,
)

@Serializable private data class LikeDto(@SerialName("track_id") val trackId: String)
