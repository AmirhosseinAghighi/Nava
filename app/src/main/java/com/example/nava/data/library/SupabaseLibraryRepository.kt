package com.example.nava.data.library

import com.example.nava.domain.library.LibraryRepository
import com.example.nava.domain.library.LibrarySummary
import com.example.nava.domain.library.UserPlaylist
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton class SupabaseLibraryRepository @Inject constructor(private val supabase: SupabaseClient) : LibraryRepository {
    override suspend fun load(): Result<LibrarySummary> = runCatching {
        val playlists = supabase.from("playlists").select().decodeList<PlaylistDto>().map { UserPlaylist(it.id, it.title, it.description) }
        val likes = supabase.from("user_track_likes").select().decodeList<LikeDto>().size.toLong()
        LibrarySummary(playlists, likes)
    }
}
@Serializable private data class PlaylistDto(val id: String, val title: String, val description: String? = null)
@Serializable private data class LikeDto(val track_id: String)
