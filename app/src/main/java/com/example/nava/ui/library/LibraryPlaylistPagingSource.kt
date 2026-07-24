package com.example.nava.ui.library

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.nava.domain.library.LibraryRepository
import com.example.nava.domain.library.UserPlaylist

class LibraryPlaylistPagingSource(
    private val repository: LibraryRepository,
) : PagingSource<Int, UserPlaylist>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, UserPlaylist> {
        val offset = params.key ?: 0
        return repository.loadPlaylistsPage(offset, params.loadSize).fold(
            onSuccess = { playlists ->
                LoadResult.Page(
                    data = playlists,
                    prevKey = (offset - params.loadSize).takeIf { it >= 0 },
                    nextKey = (offset + playlists.size).takeIf { playlists.size >= params.loadSize },
                )
            },
            onFailure = LoadResult<Int, UserPlaylist>::Error,
        )
    }

    override fun getRefreshKey(state: PagingState<Int, UserPlaylist>): Int? =
        state.anchorPosition?.let { position ->
            state.closestPageToPosition(position)?.prevKey?.plus(state.config.pageSize)
                ?: state.closestPageToPosition(position)?.nextKey?.minus(state.config.pageSize)
        }
}
