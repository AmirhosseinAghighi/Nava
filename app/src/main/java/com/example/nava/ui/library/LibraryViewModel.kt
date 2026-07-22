package com.example.nava.ui.library

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nava.domain.library.LibraryRepository
import com.example.nava.domain.library.LibrarySummary
import com.example.nava.domain.library.PlaylistCoverUpload
import com.example.nava.domain.library.PlaylistDetails
import com.example.nava.domain.library.PlaylistTrack
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class LibraryUiState(
    val loading: Boolean = true,
    val summary: LibrarySummary = LibrarySummary(),
    val selectedPlaylist: PlaylistDetails? = null,
    val catalog: List<PlaylistTrack> = emptyList(),
    val loadingDetails: Boolean = false,
    val busy: Boolean = false,
    val failed: Boolean = false,
    val operationFailed: Boolean = false,
)

@HiltViewModel class LibraryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: LibraryRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(LibraryUiState())
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()

    init { reload() }

    fun reload() = viewModelScope.launch {
        _state.update { it.copy(loading = true, failed = false) }
        repository.load().fold(
            onSuccess = { summary -> _state.update { it.copy(loading = false, summary = summary) } },
            onFailure = { _state.update { it.copy(loading = false, failed = true) } },
        )
    }

    fun openPlaylist(playlistId: String) = viewModelScope.launch {
        _state.update { it.copy(loadingDetails = true, operationFailed = false) }
        repository.loadPlaylist(playlistId).fold(
            onSuccess = { details -> _state.update { it.copy(selectedPlaylist = details, loadingDetails = false) } },
            onFailure = { _state.update { it.copy(loadingDetails = false, operationFailed = true) } },
        )
    }

    fun closePlaylist() = _state.update { it.copy(selectedPlaylist = null, catalog = emptyList()) }

    fun loadCatalog() = viewModelScope.launch {
        if (_state.value.catalog.isNotEmpty()) return@launch
        _state.update { it.copy(busy = true, operationFailed = false) }
        repository.loadCatalog().fold(
            onSuccess = { catalog -> _state.update { it.copy(catalog = catalog, busy = false) } },
            onFailure = { _state.update { it.copy(busy = false, operationFailed = true) } },
        )
    }

    fun createPlaylist(title: String, description: String?, isPublic: Boolean, coverUri: Uri?) = mutate {
        repository.createPlaylist(title, description, isPublic, coverUri?.let { readCover(it) }).getOrThrow()
        refreshSummary()
    }

    fun updatePlaylist(playlistId: String, title: String, description: String?, isPublic: Boolean, coverUri: Uri?) = mutate {
        repository.updatePlaylist(playlistId, title, description, isPublic, coverUri?.let { readCover(it) }).getOrThrow()
        refreshSummary()
        refreshSelected(playlistId)
    }

    fun deletePlaylist(playlistId: String) = mutate {
        repository.deletePlaylist(playlistId).getOrThrow()
        _state.update { it.copy(selectedPlaylist = null, catalog = emptyList()) }
        refreshSummary()
    }

    fun addTrack(trackId: String) {
        val playlistId = _state.value.selectedPlaylist?.playlist?.id ?: return
        addTrackToPlaylist(playlistId, trackId)
    }

    fun addTrackToPlaylist(playlistId: String, trackId: String, onResult: (Boolean) -> Unit = {}) {
        mutate(onResult = onResult) {
            repository.addTrack(playlistId, trackId).getOrThrow()
            if (_state.value.selectedPlaylist?.playlist?.id == playlistId) {
                refreshSelected(playlistId)
            }
            refreshSummary()
        }
    }

    fun removeTrack(trackId: String) {
        val playlistId = _state.value.selectedPlaylist?.playlist?.id ?: return
        mutate {
            repository.removeTrack(playlistId, trackId).getOrThrow()
            refreshSelected(playlistId)
            refreshSummary()
        }
    }

    fun clearOperationError() = _state.update { it.copy(operationFailed = false) }

    private fun mutate(onResult: (Boolean) -> Unit = {}, block: suspend () -> Unit) = viewModelScope.launch {
        _state.update { it.copy(busy = true, operationFailed = false) }
        runCatching { block() }.fold(
            onSuccess = {
                _state.update { it.copy(busy = false) }
                onResult(true)
            },
            onFailure = {
                _state.update { it.copy(busy = false, operationFailed = true) }
                onResult(false)
            },
        )
    }

    private suspend fun refreshSummary() {
        repository.load().getOrThrow().let { summary -> _state.update { it.copy(summary = summary) } }
    }

    private suspend fun refreshSelected(playlistId: String) {
        repository.loadPlaylist(playlistId).getOrThrow().let { details ->
            _state.update { it.copy(selectedPlaylist = details) }
        }
    }

    private suspend fun readCover(uri: Uri): PlaylistCoverUpload = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Unable to read playlist cover")
        require(bytes.size <= MAX_PLAYLIST_COVER_BYTES) { "Playlist cover is too large" }
        val extension = when (context.contentResolver.getType(uri)) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        PlaylistCoverUpload(bytes, extension)
    }

    private companion object {
        const val MAX_PLAYLIST_COVER_BYTES = 2 * 1024 * 1024
    }
}
