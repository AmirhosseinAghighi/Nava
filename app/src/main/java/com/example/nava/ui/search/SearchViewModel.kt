package com.example.nava.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nava.domain.catalog.SearchRepository
import com.example.nava.domain.catalog.SearchTrack
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(val query: String = "", val language: String? = null, val loading: Boolean = false, val results: List<SearchTrack> = emptyList(), val failed: Boolean = false, val canLoadMore: Boolean = false)

@HiltViewModel class SearchViewModel @Inject constructor(private val repository: SearchRepository) : ViewModel() {
    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()
    private var searchJob: Job? = null
    fun updateQuery(value: String) { _state.value = _state.value.copy(query = value, failed = false); request() }
    fun setLanguage(value: String?) { _state.value = _state.value.copy(language = value); request() }
    fun retry() = request(skipDelay = true)
    fun loadMore() { val current = _state.value; if (current.loading || !current.canLoadMore) return; viewModelScope.launch { _state.value = current.copy(loading = true); repository.search(current.query, current.language, current.results.size).fold({ page -> _state.value = current.copy(loading = false, results = current.results + page.tracks, canLoadMore = current.results.size + page.tracks.size < page.totalCount) }, { _state.value = current.copy(loading = false, failed = true) }) } }
    private fun request(skipDelay: Boolean = false) { searchJob?.cancel(); val request = _state.value; if (request.query.isBlank()) { _state.value = request.copy(loading = false, results = emptyList(), canLoadMore = false); return }; searchJob = viewModelScope.launch { if (!skipDelay) delay(350); _state.value = _state.value.copy(loading = true, failed = false); repository.search(request.query, request.language, 0).fold({ page -> _state.value = _state.value.copy(loading = false, results = page.tracks, canLoadMore = page.tracks.size < page.totalCount) }, { _state.value = _state.value.copy(loading = false, failed = true) }) } }
}
