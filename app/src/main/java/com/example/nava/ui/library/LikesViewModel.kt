package com.example.nava.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nava.data.catalog.toPublicCoverUrl
import com.example.nava.domain.catalog.HomeTrack
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject

data class LikesUiState(val likedIds: Set<String> = emptySet(), val songs: List<HomeTrack> = emptyList())

@HiltViewModel class LikesViewModel @Inject constructor(private val supabase: SupabaseClient) : ViewModel() {
    private val _state = MutableStateFlow(LikesUiState())
    val state = _state.asStateFlow()
    init { reload() }

    fun toggle(track: HomeTrack) = viewModelScope.launch {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return@launch
        if (track.id in _state.value.likedIds) supabase.from("user_track_likes").delete { filter { eq("track_id", track.id) } }
        else supabase.from("user_track_likes").insert(LikeRow(userId, track.id))
        reload()
    }

    fun remove(id: String) = viewModelScope.launch {
        supabase.from("user_track_likes").delete { filter { eq("track_id", id) } }
        reload()
    }

    fun reload() = viewModelScope.launch {
        runCatching {
            val liked = supabase.from("user_track_likes").select().decodeList<LikeRow>()
            val tracks = supabase.from("home_track_cards").select().decodeList<TrackRow>()
            val titles = tracks.associateBy(TrackRow::id)
            LikesUiState(
                likedIds = liked.mapTo(mutableSetOf(), LikeRow::trackId),
                songs = liked.mapNotNull { row -> titles[row.trackId]?.toHomeTrack() },
            )
        }.onSuccess { _state.value = it }
    }

    private fun TrackRow.toHomeTrack() = HomeTrack(
        id = id,
        title = title,
        artistName = artistName,
        coverImageUrl = coverImageUrl.toPublicCoverUrl(supabase),
        audioUrl = audioUrl,
        languageCode = languageCode,
    )

    @Serializable private data class LikeRow(@SerialName("user_id") val userId: String, @SerialName("track_id") val trackId: String)
    @Serializable private data class TrackRow(
        val id: String,
        val title: String,
        @SerialName("artist_name") val artistName: String,
        @SerialName("cover_image_url") val coverImageUrl: String,
        @SerialName("audio_url") val audioUrl: String,
        @SerialName("language_code") val languageCode: String,
    )
}
