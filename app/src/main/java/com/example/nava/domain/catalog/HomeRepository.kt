package com.example.nava.domain.catalog

data class HomeTrack(
    val id: String,
    val title: String,
    val artistName: String,
    val coverImageUrl: String,
    val audioUrl: String,
    val languageCode: String,
)

data class HomeFeed(
    val featured: List<HomeTrack>,
    val trending: List<HomeTrack>,
    val newest: List<HomeTrack>,
    val global: List<HomeTrack>,
    val local: List<HomeTrack>,
)

interface HomeRepository {
    suspend fun loadHome(): Result<HomeFeed>
}
