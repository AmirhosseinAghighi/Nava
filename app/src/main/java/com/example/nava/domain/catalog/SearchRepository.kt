package com.example.nava.domain.catalog

data class SearchTrack(
    val id: String,
    val title: String,
    val artistName: String,
    val coverImageUrl: String,
    val audioUrl: String,
    val durationSeconds: Int,
    val genre: String,
    val languageCode: String,
) {
    fun toHomeTrack() = HomeTrack(
        id = id,
        title = title,
        artistName = artistName,
        coverImageUrl = coverImageUrl,
        audioUrl = audioUrl,
        languageCode = languageCode,
    )
}

data class SearchPage(val tracks: List<SearchTrack>, val totalCount: Long)

interface SearchRepository {
    suspend fun search(query: String, language: String?, offset: Int): Result<SearchPage>
}
