package com.example.nava.ui.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Duration.Companion.minutes
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = true,
    val displayName: String = "",
    val avatarPath: String? = null,
    val avatarUrl: String? = null,
    val isPremium: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val supabase: SupabaseClient,
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileUiState())
    val state = _state.asStateFlow()

    init { reload() }

    fun changeDisplayName(value: String) {
        _state.value = _state.value.copy(displayName = value, error = null)
    }

    fun reload() = viewModelScope.launch {
        _state.value = _state.value.copy(isLoading = true, error = null)
        runCatching { loadProfile() }
            .onSuccess { profile -> _state.value = profile }
            .onFailure { error -> _state.value = _state.value.copy(isLoading = false, error = error.message ?: "Profile could not be loaded.") }
    }

    fun saveProfile() = viewModelScope.launch {
        val current = _state.value
        _state.value = current.copy(isSaving = true, error = null)
        runCatching {
            updateProfile(current.displayName, current.avatarPath)
        }.onSuccess { profile ->
            _state.value = profile
        }.onFailure { error ->
            _state.value = _state.value.copy(isSaving = false, error = error.message ?: "Profile could not be saved.")
        }
    }

    fun uploadAvatar(uri: Uri) = viewModelScope.launch {
        val current = _state.value
        _state.value = current.copy(isSaving = true, error = null)
        runCatching {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: error("Unable to read the selected image.")
            require(bytes.size <= MAX_AVATAR_BYTES) { "Avatar images must be 2 MB or smaller." }
            val extension = when (context.contentResolver.getType(uri)) {
                "image/png" -> "png"
                "image/webp" -> "webp"
                else -> "jpg"
            }
            val userId = supabase.auth.currentUserOrNull()?.id ?: error("You must be signed in to update an avatar.")
            val objectPath = "$userId/avatar.$extension"
            supabase.storage.from(AVATAR_BUCKET).upload(objectPath, bytes) { upsert = true }
            updateProfile(current.displayName, "storage://$AVATAR_BUCKET/$objectPath")
        }.onSuccess { profile ->
            _state.value = profile
        }.onFailure { error ->
            _state.value = _state.value.copy(isSaving = false, error = error.message ?: "Avatar upload failed.")
        }
    }

    fun upgrade() = viewModelScope.launch {
        _state.value = _state.value.copy(isSaving = true, error = null)
        runCatching {
            supabase.postgrest.rpc("enable_demo_premium")
            loadProfile()
        }.onSuccess { profile -> _state.value = profile }
            .onFailure { error -> _state.value = _state.value.copy(isSaving = false, error = error.message ?: "Premium upgrade failed.") }
    }

    fun dismissError() { _state.value = _state.value.copy(error = null) }

    private suspend fun loadProfile(): ProfileUiState {
        val profile = supabase.postgrest.rpc("get_my_profile").decodeList<ProfileDto>().single()
        return profile.toUiState()
    }

    private suspend fun updateProfile(displayName: String, avatarPath: String?): ProfileUiState {
        val profile = supabase.postgrest.rpc("update_my_profile", buildJsonObject {
            put("p_display_name", displayName)
            put("p_avatar_path", avatarPath.orEmpty())
        }).decodeList<ProfileDto>().single()
        return profile.toUiState()
    }

    private suspend fun ProfileDto.toUiState(): ProfileUiState = ProfileUiState(
        isLoading = false,
        displayName = displayName,
        avatarPath = avatarPath,
        avatarUrl = avatarPath?.let { supabase.storage.from(AVATAR_BUCKET).createSignedUrl(it.removePrefix("storage://$AVATAR_BUCKET/"), 10.minutes) },
        isPremium = isPremium,
    )

    @Serializable
    private data class ProfileDto(
        @SerialName("display_name") val displayName: String,
        @SerialName("avatar_path") val avatarPath: String? = null,
        @SerialName("is_premium") val isPremium: Boolean,
    )

    private companion object {
        const val AVATAR_BUCKET = "avatars"
        const val MAX_AVATAR_BYTES = 2 * 1024 * 1024
    }
}
