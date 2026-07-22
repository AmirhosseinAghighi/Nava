package com.example.nava.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nava.data.catalog.toPublicCoverUrl
import com.example.nava.domain.catalog.HomeTrack
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject

data class HomeQuickUiState(
    val recentTracks: List<HomeTrack> = emptyList(),
    val loadingRecent: Boolean = false,
    val recentFailed: Boolean = false,
)

@HiltViewModel
class HomeQuickViewModel @Inject constructor(
    private val supabase: SupabaseClient,
) : ViewModel() {
    private val _state = MutableStateFlow(HomeQuickUiState())
    val state = _state.asStateFlow()

    init { reloadRecent() }

    fun reloadRecent() = viewModelScope.launch {
        _state.value = _state.value.copy(loadingRecent = true, recentFailed = false)
        runCatching {
            val recentIds = supabase.from("playback_events").select().decodeList<PlaybackEventRow>()
                .asSequence()
                .filter { it.eventType == "started" }
                .sortedByDescending(PlaybackEventRow::createdAt)
                .map(PlaybackEventRow::trackId)
                .distinct()
                .take(RECENT_LIMIT)
                .toList()
            val tracksById = supabase.from("home_track_cards").select().decodeList<TrackCardRow>()
                .associateBy(TrackCardRow::id)
            recentIds.mapNotNull { tracksById[it]?.toHomeTrack() }
        }.onSuccess { tracks ->
            _state.value = _state.value.copy(recentTracks = tracks, loadingRecent = false)
        }.onFailure {
            _state.value = _state.value.copy(loadingRecent = false, recentFailed = true)
        }
    }

    private fun TrackCardRow.toHomeTrack() = HomeTrack(
        id = id,
        title = title,
        artistName = artistName,
        coverImageUrl = coverImageUrl.toPublicCoverUrl(supabase),
        audioUrl = audioUrl,
        languageCode = languageCode,
    )

    @Serializable
    private data class PlaybackEventRow(
        @SerialName("track_id") val trackId: String,
        @SerialName("event_type") val eventType: String,
        @SerialName("created_at") val createdAt: String,
    )

    @Serializable
    private data class TrackCardRow(
        val id: String,
        val title: String,
        @SerialName("artist_name") val artistName: String,
        @SerialName("cover_image_url") val coverImageUrl: String,
        @SerialName("audio_url") val audioUrl: String,
        @SerialName("language_code") val languageCode: String,
    )

    private companion object {
        const val RECENT_LIMIT = 30
    }
}
