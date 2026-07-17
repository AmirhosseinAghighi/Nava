package com.example.nava.data.catalog

import com.example.nava.domain.catalog.SearchRepository
import com.example.nava.domain.catalog.SearchTrack
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseSearchRepository @Inject constructor(private val supabase: SupabaseClient) : SearchRepository {
    override suspend fun search(query: String, language: String?): Result<List<SearchTrack>> = runCatching {
        supabase.postgrest.rpc("search_catalog", buildJsonObject {
            put("p_query", query)
            put("p_language_code", language?.let(::JsonPrimitive) ?: kotlinx.serialization.json.JsonNull)
            put("p_limit", 30)
            put("p_offset", 0)
        }).decodeList<SearchTrackDto>().map { SearchTrack(it.id, it.title, it.artistName, it.genre, it.languageCode) }
    }
}

@Serializable private data class SearchTrackDto(
    val id: String, val title: String, @SerialName("artist_name") val artistName: String,
    val genre: String, @SerialName("language_code") val languageCode: String,
)
