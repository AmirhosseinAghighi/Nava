package com.example.nava.domain.library

data class UserPlaylist(val id: String, val title: String, val description: String?)
data class LibrarySummary(val playlists: List<UserPlaylist>, val likedCount: Long)

interface LibraryRepository { suspend fun load(): Result<LibrarySummary> }
