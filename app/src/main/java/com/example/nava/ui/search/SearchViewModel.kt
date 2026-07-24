package com.example.nava.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.nava.data.search.SearchHistoryRepository
import com.example.nava.domain.catalog.SearchRepository
import com.example.nava.domain.catalog.SearchTrack
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SearchResultFilter { All, Tracks, Artists, Genres }

data class SearchArtistResult(
    val name: String,
    val coverImageUrl: String,
    val trackCount: Int,
)

data class SearchGenreResult(
    val name: String,
    val coverImageUrl: String,
    val trackCount: Int,
)

data class SearchUiState(
    val query: String = "",
    val language: String? = null,
    val resultFilter: SearchResultFilter = SearchResultFilter.All,
    val loading: Boolean = false,
    val results: List<SearchTrack> = emptyList(),
    val history: List<String> = emptyList(),
    val failed: Boolean = false,
) {
    val artists: List<SearchArtistResult>
        get() = results
            .groupBy(SearchTrack::artistName)
            .map { (name, tracks) -> SearchArtistResult(name, tracks.first().coverImageUrl, tracks.size) }
            .sortedBy(SearchArtistResult::name)

    val genres: List<SearchGenreResult>
        get() = results
            .filter { it.genre.isNotBlank() }
            .groupBy(SearchTrack::genre)
            .map { (name, tracks) -> SearchGenreResult(name, tracks.first().coverImageUrl, tracks.size) }
            .sortedBy(SearchGenreResult::name)

    val visibleResultCount: Int
        get() = when (resultFilter) {
            SearchResultFilter.All, SearchResultFilter.Tracks -> results.size
            SearchResultFilter.Artists -> artists.size
            SearchResultFilter.Genres -> genres.size
        }
}

private data class SearchRequest(val query: String = "", val language: String? = null)

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: SearchRepository,
    private val historyRepository: SearchHistoryRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()
    private val requests = MutableStateFlow(SearchRequest())
    val pagedResults = requests
        .debounce(SEARCH_DEBOUNCE_MS)
        .distinctUntilChanged()
        .flatMapLatest { request ->
            val query = request.query.trim()
            if (query.isBlank()) {
                flowOf(PagingData.empty())
            } else {
                Pager(
                    config = PagingConfig(
                        pageSize = SEARCH_PAGE_SIZE,
                        initialLoadSize = SEARCH_PAGE_SIZE,
                        prefetchDistance = SEARCH_PREFETCH_DISTANCE,
                        enablePlaceholders = false,
                    ),
                    pagingSourceFactory = {
                        SearchPagingSource(repository, query, request.language)
                    },
                ).flow
            }
        }
        .cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            historyRepository.observe().collect { history ->
                _state.update { it.copy(history = history) }
            }
        }
        viewModelScope.launch {
            requests
                .debounce(SEARCH_DEBOUNCE_MS)
                .distinctUntilChanged()
                .collectLatest(::performSearch)
        }
    }

    fun updateQuery(value: String) {
        _state.update { it.copy(query = value, failed = false) }
        requests.value = SearchRequest(value, _state.value.language)
    }

    fun setLanguage(value: String?) {
        _state.update { it.copy(language = value, failed = false) }
        requests.value = SearchRequest(_state.value.query, value)
    }

    fun setResultFilter(value: SearchResultFilter) {
        _state.update { it.copy(resultFilter = value) }
    }

    fun submitSearch() {
        val query = _state.value.query
        if (query.isBlank()) return
        viewModelScope.launch { historyRepository.record(query) }
    }

    fun selectHistory(query: String) {
        _state.update { it.copy(query = query, failed = false) }
        requests.value = SearchRequest(query, _state.value.language)
        viewModelScope.launch { historyRepository.record(query) }
    }

    fun selectSuggestion(query: String) {
        _state.update {
            it.copy(query = query, resultFilter = SearchResultFilter.Tracks, failed = false)
        }
        requests.value = SearchRequest(query, _state.value.language)
        viewModelScope.launch { historyRepository.record(query) }
    }

    fun removeHistory(query: String) = viewModelScope.launch { historyRepository.delete(query) }

    fun clearHistory() = viewModelScope.launch { historyRepository.clear() }

    fun retry() = viewModelScope.launch {
        performSearch(SearchRequest(_state.value.query, _state.value.language))
    }

    private suspend fun performSearch(request: SearchRequest) {
        val query = request.query.trim()
        if (query.isBlank()) {
            _state.update { it.copy(loading = false, results = emptyList(), failed = false) }
            return
        }
        _state.update { it.copy(loading = true, failed = false, results = emptyList()) }
        repository.search(query, request.language, 0).fold(
            onSuccess = { page ->
                if (_state.value.query.trim() == query && _state.value.language == request.language) {
                    _state.update {
                        it.copy(
                            loading = false,
                            results = page.tracks,
                        )
                    }
                }
            },
            onFailure = {
                if (_state.value.query.trim() == query && _state.value.language == request.language) {
                    _state.update { it.copy(loading = false, failed = true) }
                }
            },
        )
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 350L
        const val SEARCH_PAGE_SIZE = 30
        const val SEARCH_PREFETCH_DISTANCE = 8
    }
}
