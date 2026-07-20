package com.example.nava.ui.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

data class SocialPerson(val id: String, val displayName: String, val isFollowing: Boolean)
data class SocialUiState(
    val query: String = "",
    val people: List<SocialPerson> = emptyList(),
    val loading: Boolean = false,
    val error: Boolean = false,
)

@HiltViewModel
class SocialViewModel @Inject constructor(private val supabase: SupabaseClient) : ViewModel() {
    private val _state = MutableStateFlow(SocialUiState())
    val state = _state.asStateFlow()

    init { search("") }

    fun search(query: String) = viewModelScope.launch {
        _state.value = _state.value.copy(query = query, loading = true, error = false)
        runCatching {
            supabase.postgrest.rpc("search_people", buildJsonObject {
                put("p_query", query)
                put("p_limit", 30)
            }).decodeList<PersonDto>().map { SocialPerson(it.id, it.displayName, it.isFollowing) }
        }.onSuccess { people ->
            _state.value = _state.value.copy(people = people, loading = false)
        }.onFailure {
            _state.value = _state.value.copy(loading = false, error = true)
        }
    }

    fun toggleFollow(person: SocialPerson) = viewModelScope.launch {
        runCatching {
            supabase.postgrest.rpc("set_follow", buildJsonObject {
                put("p_target_id", person.id)
                put("p_follow", !person.isFollowing)
            }).decodeAs<Boolean>()
        }.onSuccess { following ->
            _state.value = _state.value.copy(
                people = _state.value.people.map { if (it.id == person.id) it.copy(isFollowing = following) else it },
            )
        }.onFailure { _state.value = _state.value.copy(error = true) }
    }

    @Serializable
    private data class PersonDto(
        val id: String,
        @SerialName("display_name") val displayName: String,
        @SerialName("is_following") val isFollowing: Boolean,
    )
}
