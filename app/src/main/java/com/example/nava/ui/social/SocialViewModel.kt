package com.example.nava.ui.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Duration.Companion.minutes
import javax.inject.Inject

enum class SocialSection { PEOPLE, FOLLOWING, FOLLOWERS, PLAYLISTS }
data class SocialPerson(
    val id: String,
    val displayName: String,
    val avatarPath: String?,
    val avatarUrl: String?,
    val isFollowing: Boolean,
)
data class PublicPlaylist(val id: String, val title: String, val description: String?, val ownerName: String, val trackCount: Long)
data class SocialProfileDetails(
    val person: SocialPerson,
    val followersCount: Int,
    val followingCount: Int,
    val playlists: List<PublicPlaylist>,
)
data class SocialUiState(
    val section: SocialSection = SocialSection.PEOPLE,
    val query: String = "",
    val people: List<SocialPerson> = emptyList(),
    val playlists: List<PublicPlaylist> = emptyList(),
    val loading: Boolean = false,
    val error: Boolean = false,
    val selectedPerson: SocialPerson? = null,
    val profileDetails: SocialProfileDetails? = null,
    val profileLoading: Boolean = false,
)

@HiltViewModel
class SocialViewModel @Inject constructor(private val supabase: SupabaseClient) : ViewModel() {
    private val _state = MutableStateFlow(SocialUiState())
    val state = _state.asStateFlow()
    private var searchJob: Job? = null
    private var loadJob: Job? = null
    private var profileJob: Job? = null

    init { load(SocialSection.PEOPLE) }

    fun select(section: SocialSection) = load(section)
    fun search(query: String) {
        _state.value = _state.value.copy(query = query, error = false)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            load(SocialSection.PEOPLE, query)
        }
    }

    private fun load(section: SocialSection, query: String = _state.value.query) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
        _state.value = _state.value.copy(section = section, query = query, loading = true, error = false)
        runCatching {
            when (section) {
                SocialSection.PEOPLE -> SocialPayload(people = supabase.postgrest.rpc("search_people", buildJsonObject { put("p_query", query); put("p_limit", 30) }).decodeList<PersonDto>().map { it.toPerson(signedAvatarUrl(it.avatarPath)) })
                SocialSection.FOLLOWING, SocialSection.FOLLOWERS -> {
                    val userId = supabase.auth.currentUserOrNull()?.id ?: error("Not signed in")
                    SocialPayload(people = supabase.postgrest.rpc("get_social_connections", buildJsonObject { put("p_user_id", userId); put("p_kind", if (section == SocialSection.FOLLOWERS) "followers" else "following"); put("p_limit", 30) }).decodeList<ConnectionDto>().map { it.toPerson(signedAvatarUrl(it.avatarPath)) })
                }
                SocialSection.PLAYLISTS -> {
                    val userId = supabase.auth.currentUserOrNull()?.id ?: error("Not signed in")
                    SocialPayload(playlists = supabase.postgrest.rpc("get_public_playlists", buildJsonObject { put("p_owner_id", userId); put("p_limit", 30) }).decodeList<PlaylistDto>().map { PublicPlaylist(it.id, it.title, it.description, it.ownerName, it.trackCount) })
                }
            }
        }.onSuccess { payload -> _state.value = _state.value.copy(people = payload.people, playlists = payload.playlists, loading = false) }
            .onFailure { _state.value = _state.value.copy(loading = false, error = true) }
        }
    }

    fun openProfile(person: SocialPerson) {
        profileJob?.cancel()
        _state.value = _state.value.copy(
            selectedPerson = person,
            profileDetails = null,
            profileLoading = true,
            error = false,
        )
        profileJob = viewModelScope.launch {
            runCatching {
                val followers = loadConnections(person.id, "followers")
                val following = loadConnections(person.id, "following")
                val playlists = loadPlaylists(person.id)
                SocialProfileDetails(person, followers.size, following.size, playlists)
            }.onSuccess { details ->
                _state.value = _state.value.copy(profileDetails = details, profileLoading = false)
            }.onFailure {
                _state.value = _state.value.copy(profileLoading = false, error = true)
            }
        }
    }

    fun closeProfile() {
        profileJob?.cancel()
        _state.value = _state.value.copy(selectedPerson = null, profileDetails = null, profileLoading = false, error = false)
    }

    fun toggleFollow(person: SocialPerson) = viewModelScope.launch {
        runCatching { supabase.postgrest.rpc("set_follow", buildJsonObject { put("p_target_id", person.id); put("p_follow", !person.isFollowing) }).decodeAs<Boolean>() }
            .onSuccess { following ->
                val updatedPerson = person.copy(isFollowing = following)
                _state.value = _state.value.copy(
                    people = _state.value.people.map { if (it.id == person.id) updatedPerson else it },
                    selectedPerson = _state.value.selectedPerson?.let { if (it.id == person.id) updatedPerson else it },
                    profileDetails = _state.value.profileDetails?.let { details ->
                        if (details.person.id == person.id) details.copy(person = updatedPerson) else details
                    },
                )
            }
            .onFailure { _state.value = _state.value.copy(error = true) }
    }

    private suspend fun loadConnections(userId: String, kind: String): List<ConnectionDto> =
        supabase.postgrest.rpc("get_social_connections", buildJsonObject {
            put("p_user_id", userId)
            put("p_kind", kind)
            put("p_limit", 50)
        }).decodeList()

    private suspend fun loadPlaylists(userId: String): List<PublicPlaylist> =
        supabase.postgrest.rpc("get_public_playlists", buildJsonObject {
            put("p_owner_id", userId)
            put("p_limit", 50)
        }).decodeList<PlaylistDto>().map { PublicPlaylist(it.id, it.title, it.description, it.ownerName, it.trackCount) }

    private suspend fun signedAvatarUrl(path: String?): String? = path?.let {
        supabase.storage.from(AVATAR_BUCKET).createSignedUrl(
            it.removePrefix("storage://$AVATAR_BUCKET/"),
            AVATAR_URL_DURATION,
        )
    }

    private data class SocialPayload(val people: List<SocialPerson> = emptyList(), val playlists: List<PublicPlaylist> = emptyList())
    @Serializable private data class PersonDto(val id: String, @SerialName("display_name") val displayName: String, @SerialName("avatar_path") val avatarPath: String? = null, @SerialName("is_following") val isFollowing: Boolean) { fun toPerson(avatarUrl: String?) = SocialPerson(id, displayName, avatarPath, avatarUrl, isFollowing) }
    @Serializable private data class ConnectionDto(val id: String, @SerialName("display_name") val displayName: String, @SerialName("avatar_path") val avatarPath: String? = null, @SerialName("is_following") val isFollowing: Boolean) { fun toPerson(avatarUrl: String?) = SocialPerson(id, displayName, avatarPath, avatarUrl, isFollowing) }
    @Serializable private data class PlaylistDto(val id: String, val title: String, val description: String? = null, @SerialName("owner_name") val ownerName: String, @SerialName("track_count") val trackCount: Long)

    private companion object {
        const val AVATAR_BUCKET = "avatars"
        val AVATAR_URL_DURATION = 10.minutes
    }
}
