package com.example.nava.ui.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

enum class SocialSection { PEOPLE, FOLLOWING, FOLLOWERS, PLAYLISTS }
data class SocialPerson(val id: String, val displayName: String, val isFollowing: Boolean)
data class PublicPlaylist(val id: String, val title: String, val description: String?, val ownerName: String, val trackCount: Long)
data class SocialUiState(
    val section: SocialSection = SocialSection.PEOPLE,
    val query: String = "",
    val people: List<SocialPerson> = emptyList(),
    val playlists: List<PublicPlaylist> = emptyList(),
    val loading: Boolean = false,
    val error: Boolean = false,
)

@HiltViewModel
class SocialViewModel @Inject constructor(private val supabase: SupabaseClient) : ViewModel() {
    private val _state = MutableStateFlow(SocialUiState())
    val state = _state.asStateFlow()

    init { load(SocialSection.PEOPLE) }

    fun select(section: SocialSection) = load(section)
    fun search(query: String) = load(SocialSection.PEOPLE, query)

    private fun load(section: SocialSection, query: String = _state.value.query) = viewModelScope.launch {
        _state.value = _state.value.copy(section = section, query = query, loading = true, error = false)
        runCatching {
            when (section) {
                SocialSection.PEOPLE -> SocialPayload(people = supabase.postgrest.rpc("search_people", buildJsonObject { put("p_query", query); put("p_limit", 30) }).decodeList<PersonDto>().map { it.toPerson() })
                SocialSection.FOLLOWING, SocialSection.FOLLOWERS -> {
                    val userId = supabase.auth.currentUserOrNull()?.id ?: error("Not signed in")
                    SocialPayload(people = supabase.postgrest.rpc("get_social_connections", buildJsonObject { put("p_user_id", userId); put("p_kind", if (section == SocialSection.FOLLOWERS) "followers" else "following"); put("p_limit", 30) }).decodeList<ConnectionDto>().map { it.toPerson() })
                }
                SocialSection.PLAYLISTS -> {
                    val userId = supabase.auth.currentUserOrNull()?.id ?: error("Not signed in")
                    SocialPayload(playlists = supabase.postgrest.rpc("get_public_playlists", buildJsonObject { put("p_owner_id", userId); put("p_limit", 30) }).decodeList<PlaylistDto>().map { PublicPlaylist(it.id, it.title, it.description, it.ownerName, it.trackCount) })
                }
            }
        }.onSuccess { payload -> _state.value = _state.value.copy(people = payload.people, playlists = payload.playlists, loading = false) }
            .onFailure { _state.value = _state.value.copy(loading = false, error = true) }
    }

    fun toggleFollow(person: SocialPerson) = viewModelScope.launch {
        runCatching { supabase.postgrest.rpc("set_follow", buildJsonObject { put("p_target_id", person.id); put("p_follow", !person.isFollowing) }).decodeAs<Boolean>() }
            .onSuccess { following -> _state.value = _state.value.copy(people = _state.value.people.map { if (it.id == person.id) it.copy(isFollowing = following) else it }) }
            .onFailure { _state.value = _state.value.copy(error = true) }
    }

    private data class SocialPayload(val people: List<SocialPerson> = emptyList(), val playlists: List<PublicPlaylist> = emptyList())
    @Serializable private data class PersonDto(val id: String, @SerialName("display_name") val displayName: String, @SerialName("is_following") val isFollowing: Boolean) { fun toPerson() = SocialPerson(id, displayName, isFollowing) }
    @Serializable private data class ConnectionDto(val id: String, @SerialName("display_name") val displayName: String, @SerialName("is_following") val isFollowing: Boolean) { fun toPerson() = SocialPerson(id, displayName, isFollowing) }
    @Serializable private data class PlaylistDto(val id: String, val title: String, val description: String? = null, @SerialName("owner_name") val ownerName: String, @SerialName("track_count") val trackCount: Long)
}
