package com.example.nava.data.catalog

import com.example.nava.domain.catalog.HomeFeed
import com.example.nava.domain.catalog.HomeRepository
import com.example.nava.domain.catalog.HomeTrack
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseHomeRepository @Inject constructor(
    private val supabase: SupabaseClient,
) : HomeRepository {
    override suspend fun loadHome(): Result<HomeFeed> = runCatching {
        val tracks = supabase.from("home_track_cards").select().decodeList<HomeTrackDto>()
            .map { it.toDomain(supabase) }

        require(tracks.isNotEmpty()) { "The Nava catalog is empty." }
        HomeFeed(
            featured = tracks.take(5),
            trending = tracks.take(10),
            newest = tracks.take(10),
            global = tracks.filter { it.languageCode == "en" }.take(10),
            local = tracks.filter { it.languageCode == "fa" }.take(10),
        )
    }
}

@Serializable
private data class HomeTrackDto(
    val id: String,
    val title: String,
    @SerialName("artist_name") val artistName: String,
    @SerialName("cover_image_url") val coverImageUrl: String,
    @SerialName("audio_url") val audioUrl: String,
    @SerialName("language_code") val languageCode: String,
) {
    fun toDomain(supabase: SupabaseClient) = HomeTrack(
        id = id,
        title = title,
        artistName = artistName,
        coverImageUrl = coverImageUrl.toPublicCoverUrl(supabase),
        audioUrl = audioUrl,
        languageCode = languageCode,
    )
}

internal fun String.toPublicCoverUrl(supabase: SupabaseClient): String =
    takeIf { it.startsWith(COVER_STORAGE_PREFIX) }
        ?.removePrefix(COVER_STORAGE_PREFIX)
        ?.let { path -> supabase.storage.from(COVERS_BUCKET).publicUrl(path) }
        ?: this

private const val COVERS_BUCKET = "covers"
private const val COVER_STORAGE_PREFIX = "storage://covers/"
