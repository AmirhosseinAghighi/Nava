package com.example.nava.ui.search

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.nava.domain.catalog.SearchRepository
import com.example.nava.domain.catalog.SearchTrack

class SearchPagingSource(
    private val repository: SearchRepository,
    private val query: String,
    private val language: String?,
) : PagingSource<Int, SearchTrack>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SearchTrack> {
        val offset = params.key ?: 0
        return repository.search(query, language, offset).fold(
            onSuccess = { page ->
                val loaded = offset + page.tracks.size
                LoadResult.Page(
                    data = page.tracks,
                    prevKey = null,
                    nextKey = loaded.takeIf { page.tracks.isNotEmpty() && it < page.totalCount },
                )
            },
            onFailure = { error -> LoadResult.Error(error) },
        )
    }

    override fun getRefreshKey(state: PagingState<Int, SearchTrack>): Int? {
        val anchor = state.anchorPosition ?: return null
        val page = state.closestPageToPosition(anchor) ?: return null
        return page.prevKey?.plus(state.config.pageSize)
            ?: page.nextKey?.minus(state.config.pageSize)?.coerceAtLeast(0)
    }
}
