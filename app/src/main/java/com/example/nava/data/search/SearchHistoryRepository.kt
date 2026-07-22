package com.example.nava.data.search

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchHistoryRepository @Inject constructor(
    private val dao: SearchHistoryDao,
    private val supabase: SupabaseClient,
) {
    fun observe(): Flow<List<String>> = accountId()?.let { accountId ->
        dao.observe(accountId).map { entries -> entries.map(SearchHistoryEntity::query) }
    } ?: flowOf(emptyList())

    suspend fun record(query: String) {
        val accountId = accountId() ?: return
        val trimmed = query.trim().take(MAX_QUERY_LENGTH)
        if (trimmed.isBlank()) return
        dao.upsert(
            SearchHistoryEntity(
                accountId = accountId,
                normalizedQuery = trimmed.lowercase(Locale.ROOT),
                query = trimmed,
                searchedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun delete(query: String) {
        val accountId = accountId() ?: return
        dao.delete(accountId, query.trim().lowercase(Locale.ROOT))
    }

    suspend fun clear() {
        accountId()?.let { dao.clear(it) }
    }

    private fun accountId(): String? = supabase.auth.currentUserOrNull()?.id

    private companion object {
        const val MAX_QUERY_LENGTH = 80
    }
}
