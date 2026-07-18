package com.example.nava.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nava.domain.library.LibraryRepository
import com.example.nava.domain.library.LibrarySummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LibraryUiState { data object Loading : LibraryUiState; data class Content(val summary: LibrarySummary) : LibraryUiState; data object Error : LibraryUiState }
@HiltViewModel class LibraryViewModel @Inject constructor(private val repository: LibraryRepository) : ViewModel() {
 private val _state = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading); val state: StateFlow<LibraryUiState> = _state.asStateFlow()
 init { reload() }; fun reload() = viewModelScope.launch { _state.value = LibraryUiState.Loading; _state.value = repository.load().fold({ LibraryUiState.Content(it) }, { LibraryUiState.Error }) }
}
