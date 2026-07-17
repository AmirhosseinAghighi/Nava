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

data class SearchUiState(val query: String = "", val language: String? = null, val loading: Boolean = false, val results: List<SearchTrack> = emptyList(), val failed: Boolean = false)

@HiltViewModel class SearchViewModel @Inject constructor(private val repository: SearchRepository) : ViewModel() {
    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()
    private var searchJob: Job? = null
    fun updateQuery(value: String) { _state.value = _state.value.copy(query = value, failed = false); request() }
    fun setLanguage(value: String?) { _state.value = _state.value.copy(language = value); request() }
    private fun request() { searchJob?.cancel(); val request = _state.value; if (request.query.isBlank()) { _state.value = request.copy(loading = false, results = emptyList()); return }; searchJob = viewModelScope.launch { delay(350); _state.value = _state.value.copy(loading = true); repository.search(request.query, request.language).fold({ _state.value = _state.value.copy(loading = false, results = it) }, { _state.value = _state.value.copy(loading = false, failed = true) }) } }
}
