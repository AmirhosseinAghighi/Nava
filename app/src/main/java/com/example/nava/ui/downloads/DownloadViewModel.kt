package com.example.nava.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nava.data.downloads.OfflineDownloadRepository
import com.example.nava.data.downloads.OfflineTrackEntity
import com.example.nava.data.downloads.DownloadTransfer
import com.example.nava.data.downloads.PremiumDownloadRequired
import com.example.nava.domain.catalog.HomeTrack
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DownloadsUiState(
    val downloads: List<OfflineTrackEntity> = emptyList(),
    val activeDownloads: List<DownloadTransfer> = emptyList(),
) {
    val downloadedTrackIds: Set<String> get() = downloads.mapTo(mutableSetOf(), OfflineTrackEntity::trackId)
    val downloadingTrackIds: Set<String> get() = activeDownloads.mapTo(mutableSetOf(), DownloadTransfer::trackId)
}

enum class DownloadUiError { PremiumRequired, Unavailable }

@HiltViewModel class DownloadViewModel @Inject constructor(private val repository: OfflineDownloadRepository) : ViewModel() {
    private val preparingDownloads = MutableStateFlow<List<DownloadTransfer>>(emptyList())
    val state: StateFlow<DownloadsUiState> = combine(
        repository.observeDownloads(),
        repository.observeActiveDownloads(),
        preparingDownloads,
    ) { downloads, active, preparing ->
        val activeByTrack = (preparing + active).associateBy(DownloadTransfer::trackId)
        DownloadsUiState(downloads = downloads, activeDownloads = activeByTrack.values.filter { it.trackId !in downloads.map(OfflineTrackEntity::trackId) })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DownloadsUiState())
    private val _downloadError = MutableStateFlow<DownloadUiError?>(null)
    val downloadError = _downloadError.asStateFlow()
    fun request(track: HomeTrack) = viewModelScope.launch {
        preparingDownloads.update { current ->
            (current.filterNot { it.trackId == track.id } + DownloadTransfer(track.id, track.title, track.artistName, track.coverImageUrl, 0))
        }
        repository.requestDownload(track).onFailure { error ->
            _downloadError.value = if (error is PremiumDownloadRequired) {
                DownloadUiError.PremiumRequired
            } else {
                DownloadUiError.Unavailable
            }
        }
        preparingDownloads.update { current -> current.filterNot { it.trackId == track.id } }
    }
    fun remove(track: OfflineTrackEntity) = viewModelScope.launch { repository.remove(track) }
    fun dismissDownloadError() { _downloadError.value = null }
}
