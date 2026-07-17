package com.example.nava.domain.catalog

data class SearchTrack(val id: String, val title: String, val artistName: String, val genre: String, val languageCode: String)
data class SearchPage(val tracks: List<SearchTrack>, val totalCount: Long)

interface SearchRepository {
    suspend fun search(query: String, language: String?, offset: Int): Result<SearchPage>
}
