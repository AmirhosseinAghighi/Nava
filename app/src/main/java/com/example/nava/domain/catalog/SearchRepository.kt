package com.example.nava.domain.catalog

data class SearchTrack(val id: String, val title: String, val artistName: String, val genre: String, val languageCode: String)

interface SearchRepository {
    suspend fun search(query: String, language: String?): Result<List<SearchTrack>>
}
