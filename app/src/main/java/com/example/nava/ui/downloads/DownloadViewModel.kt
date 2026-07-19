package com.example.nava.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nava.data.downloads.OfflineDownloadRepository
import com.example.nava.data.downloads.OfflineTrackEntity
import com.example.nava.domain.catalog.HomeTrack
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel class DownloadViewModel @Inject constructor(private val repository: OfflineDownloadRepository) : ViewModel() {
    val downloads: StateFlow<List<OfflineTrackEntity>> = repository.observeDownloads().stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    private val _premiumGateVisible = MutableStateFlow(false)
    val premiumGateVisible = _premiumGateVisible.asStateFlow()
    fun request(track: HomeTrack) = viewModelScope.launch { if (repository.requestDownload(track).isFailure) _premiumGateVisible.value = true }
    fun remove(track: OfflineTrackEntity) = viewModelScope.launch { repository.remove(track) }
    fun dismissPremiumGate() { _premiumGateVisible.value = false }
}
