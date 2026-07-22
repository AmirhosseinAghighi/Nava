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
    val pendingAvatarUri: Uri? = null,
    val isPremium: Boolean = false,
    val followersCount: Long = 0,
    val followingCount: Long = 0,
    val publicPlaylistsCount: Long = 0,
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val hasChanges: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val supabase: SupabaseClient,
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileUiState())
    val state = _state.asStateFlow()
    private var savedDisplayName = ""

    init { reload() }

    fun changeDisplayName(value: String) {
        _state.value = _state.value.copy(
            displayName = value,
            hasChanges = value.trim() != savedDisplayName || _state.value.pendingAvatarUri != null,
            error = null,
        )
    }

    fun startEditing() {
        _state.value = _state.value.copy(isEditing = true, error = null)
    }

    fun cancelEditing() {
        _state.value = _state.value.copy(
            displayName = savedDisplayName,
            pendingAvatarUri = null,
            hasChanges = false,
            isEditing = false,
            error = null,
        )
    }

    fun selectAvatar(uri: Uri) {
        _state.value = _state.value.copy(
            pendingAvatarUri = uri,
            hasChanges = true,
            error = null,
        )
    }

    fun reload() = viewModelScope.launch {
        _state.value = _state.value.copy(isLoading = true, error = null)
        runCatching { loadProfile() }
            .onSuccess(::acceptSavedProfile)
            .onFailure { _state.value = _state.value.copy(isLoading = false, error = PROFILE_LOAD_ERROR) }
    }

    fun saveProfile() = viewModelScope.launch {
        val current = _state.value
        if (!current.hasChanges || current.displayName.trim().length !in 2..60) return@launch
        _state.value = current.copy(isSaving = true, error = null)
        runCatching {
            val avatarPath = current.pendingAvatarUri?.let { uploadAvatarFile(it) } ?: current.avatarPath
            updateProfile(current.displayName.trim(), avatarPath)
            loadProfile()
        }.onSuccess { profile ->
            acceptSavedProfile(profile)
        }.onFailure {
            _state.value = _state.value.copy(isSaving = false, error = PROFILE_SAVE_ERROR)
        }
    }

    fun upgrade() = viewModelScope.launch {
        _state.value = _state.value.copy(isSaving = true, error = null)
        runCatching {
            supabase.postgrest.rpc("enable_demo_premium")
            loadProfile()
        }.onSuccess(::acceptSavedProfile)
            .onFailure { _state.value = _state.value.copy(isSaving = false, error = PREMIUM_UPGRADE_ERROR) }
    }

    fun dismissError() { _state.value = _state.value.copy(error = null) }

    private suspend fun loadProfile(): ProfileUiState {
        return runCatching {
            supabase.postgrest.rpc("get_my_profile_overview")
                .decodeList<ProfileDto>()
                .single()
                .toUiState()
        }.getOrElse {
            loadLegacyProfile()
        }
    }

    private suspend fun loadLegacyProfile(): ProfileUiState {
        val profile = supabase.postgrest.rpc("get_my_profile").decodeList<LegacyProfileDto>().single()
        val userId = supabase.auth.currentUserOrNull()?.id ?: error("Not signed in")
        val followersCount = loadConnectionCount(userId, "followers")
        val followingCount = loadConnectionCount(userId, "following")
        val publicPlaylistsCount = runCatching {
            supabase.postgrest.rpc("get_public_playlists", buildJsonObject {
                put("p_owner_id", userId)
                put("p_limit", SOCIAL_COUNT_LIMIT)
            }).decodeList<IdDto>().size.toLong()
        }.getOrDefault(0)
        return profile.toUiState(followersCount, followingCount, publicPlaylistsCount)
    }

    private suspend fun loadConnectionCount(userId: String, kind: String): Long = runCatching {
        supabase.postgrest.rpc("get_social_connections", buildJsonObject {
            put("p_user_id", userId)
            put("p_kind", kind)
            put("p_limit", SOCIAL_COUNT_LIMIT)
        }).decodeList<IdDto>().size.toLong()
    }.getOrDefault(0)

    private suspend fun updateProfile(displayName: String, avatarPath: String?) {
        supabase.postgrest.rpc("update_my_profile", buildJsonObject {
            put("p_display_name", displayName)
            put("p_avatar_path", avatarPath.orEmpty())
        })
    }

    private suspend fun uploadAvatarFile(uri: Uri): String {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Unable to read the selected image.")
        require(bytes.size <= MAX_AVATAR_BYTES) { "Avatar images must be 2 MB or smaller." }
        val extension = when (context.contentResolver.getType(uri)) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: error("You must be signed in to update an avatar.")
        val objectPath = "$userId/avatar.$extension"
        supabase.storage.from(AVATAR_BUCKET).upload(objectPath, bytes) { upsert = true }
        return "storage://$AVATAR_BUCKET/$objectPath"
    }

    private fun acceptSavedProfile(profile: ProfileUiState) {
        savedDisplayName = profile.displayName
        _state.value = profile.copy(
            pendingAvatarUri = null,
            hasChanges = false,
            isSaving = false,
            isEditing = false,
        )
    }

    private suspend fun ProfileDto.toUiState(): ProfileUiState = ProfileUiState(
        isLoading = false,
        displayName = displayName,
        avatarPath = avatarPath,
        avatarUrl = avatarPath?.let { supabase.storage.from(AVATAR_BUCKET).createSignedUrl(it.removePrefix("storage://$AVATAR_BUCKET/"), 10.minutes) },
        isPremium = isPremium,
        followersCount = followersCount,
        followingCount = followingCount,
        publicPlaylistsCount = publicPlaylistsCount,
    )

    private suspend fun LegacyProfileDto.toUiState(
        followersCount: Long,
        followingCount: Long,
        publicPlaylistsCount: Long,
    ): ProfileUiState = ProfileUiState(
        isLoading = false,
        displayName = displayName,
        avatarPath = avatarPath,
        avatarUrl = avatarPath?.let {
            supabase.storage.from(AVATAR_BUCKET)
                .createSignedUrl(it.removePrefix("storage://$AVATAR_BUCKET/"), 10.minutes)
        },
        isPremium = isPremium,
        followersCount = followersCount,
        followingCount = followingCount,
        publicPlaylistsCount = publicPlaylistsCount,
    )

    @Serializable
    private data class ProfileDto(
        @SerialName("display_name") val displayName: String,
        @SerialName("avatar_path") val avatarPath: String? = null,
        @SerialName("is_premium") val isPremium: Boolean,
        @SerialName("followers_count") val followersCount: Long,
        @SerialName("following_count") val followingCount: Long,
        @SerialName("public_playlists_count") val publicPlaylistsCount: Long,
    )

    @Serializable
    private data class LegacyProfileDto(
        @SerialName("display_name") val displayName: String,
        @SerialName("avatar_path") val avatarPath: String? = null,
        @SerialName("is_premium") val isPremium: Boolean,
    )

    @Serializable
    private data class IdDto(val id: String)

    private companion object {
        const val AVATAR_BUCKET = "avatars"
        const val MAX_AVATAR_BYTES = 2 * 1024 * 1024
        const val SOCIAL_COUNT_LIMIT = 50
        const val PROFILE_LOAD_ERROR = "Your profile could not be loaded. Please try again."
        const val PROFILE_SAVE_ERROR = "Your profile changes could not be saved. Please try again."
        const val PREMIUM_UPGRADE_ERROR = "Premium could not be enabled right now. Please try again."
    }
}
