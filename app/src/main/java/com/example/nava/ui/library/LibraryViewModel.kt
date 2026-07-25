package com.example.nava.ui.library

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
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
import kotlinx.coroutines.flow.Flow
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
    /**
     * A playlist opened from public browsing. Kept apart from [selectedPlaylist] so opening
     * someone's playlist from Top playlists does not also open it in the Library tab.
     */
    val viewedPlaylist: PlaylistDetails? = null,
    val viewedLoading: Boolean = false,
    val viewedFailed: Boolean = false,
)

@HiltViewModel class LibraryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: LibraryRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(LibraryUiState())
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()
    private var playlistPagingSource: LibraryPlaylistPagingSource? = null
    val pagedPlaylists: Flow<PagingData<com.example.nava.domain.library.UserPlaylist>> = Pager(
        config = PagingConfig(
            pageSize = PLAYLIST_PAGE_SIZE,
            initialLoadSize = PLAYLIST_PAGE_SIZE,
            prefetchDistance = PLAYLIST_PREFETCH_DISTANCE,
            enablePlaceholders = false,
        ),
        pagingSourceFactory = {
            LibraryPlaylistPagingSource(repository).also { playlistPagingSource = it }
        },
    ).flow.cachedIn(viewModelScope)

    init { reload() }

    fun reload() = viewModelScope.launch {
        playlistPagingSource?.invalidate()
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

    /** Opens any playlist the signed-in user is allowed to read, including other people's public ones. */
    fun openViewedPlaylist(playlistId: String) = viewModelScope.launch {
        _state.update { it.copy(viewedLoading = true, viewedFailed = false, viewedPlaylist = null) }
        repository.loadPlaylist(playlistId).fold(
            onSuccess = { details -> _state.update { it.copy(viewedPlaylist = details, viewedLoading = false) } },
            onFailure = { _state.update { it.copy(viewedLoading = false, viewedFailed = true) } },
        )
    }

    fun closeViewedPlaylist() = _state.update {
        it.copy(viewedPlaylist = null, viewedLoading = false, viewedFailed = false, catalog = emptyList())
    }

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
        _state.update { it.copy(selectedPlaylist = null, viewedPlaylist = null, catalog = emptyList()) }
        refreshSummary()
    }

    fun addTrackToPlaylist(playlistId: String, trackId: String, onResult: (Boolean) -> Unit = {}) {
        mutate(onResult = onResult) {
            repository.addTrack(playlistId, trackId).getOrThrow()
            refreshSelected(playlistId)
            refreshSummary()
        }
    }

    fun removeTrack(playlistId: String, trackId: String) = mutate {
        repository.removeTrack(playlistId, trackId).getOrThrow()
        refreshSelected(playlistId)
        refreshSummary()
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

    /** Refreshes whichever open view is showing [playlistId] — the Library tab, public browsing, or both. */
    private suspend fun refreshSelected(playlistId: String) {
        val showsSelected = _state.value.selectedPlaylist?.playlist?.id == playlistId
        val showsViewed = _state.value.viewedPlaylist?.playlist?.id == playlistId
        if (!showsSelected && !showsViewed) return
        repository.loadPlaylist(playlistId).getOrThrow().let { details ->
            _state.update {
                it.copy(
                    selectedPlaylist = if (showsSelected) details else it.selectedPlaylist,
                    viewedPlaylist = if (showsViewed) details else it.viewedPlaylist,
                )
            }
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
        const val PLAYLIST_PAGE_SIZE = 20
        const val PLAYLIST_PREFETCH_DISTANCE = 5
    }
}
