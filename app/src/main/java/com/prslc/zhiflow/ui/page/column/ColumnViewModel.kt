package com.prslc.zhiflow.ui.page.column

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prslc.zhiflow.core.exception.ApiException
import com.prslc.zhiflow.core.exception.onApiFailure
import com.prslc.zhiflow.data.dto.ColumnDto
import com.prslc.zhiflow.data.dto.ColumnItemDto
import com.prslc.zhiflow.data.repository.ColumnRepository
import com.prslc.zhiflow.data.repository.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** One column: what it is, and the articles it holds, from the newest. */
class ColumnViewModel(
    private val repository: ColumnRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    @Immutable
    data class ColumnUiState(
        val isLoading: Boolean = false,
        val column: ColumnDto? = null,
        val items: List<ColumnItemDto> = emptyList(),
        val isNextLoading: Boolean = false,
        val isEnd: Boolean = false,
        /** Set when the column itself could not be read, which is the whole page failing. */
        val error: ApiException? = null,
        val loadMoreError: ApiException? = null,
    )

    var uiState by mutableStateOf(ColumnUiState(isLoading = true))
        private set

    val listState = LazyListState()

    /** Set when the follow toggle fails, so the screen can explain the rollback. */
    var actionError by mutableStateOf<ApiException?>(null)
        private set

    fun consumeActionError() {
        actionError = null
    }

    private var currentToken: String? = null

    /** How many articles the loaded list holds, which is the offset the next page asks from. */
    private var nextOffset = 0

    private var followJob: Job? = null

    private var authorFollowJob: Job? = null

    /**
     * Reads the column and its first page of articles.
     *
     * Both go out together: the list is what the page is for, and the header arriving a moment
     * after it is a header filling in, not a page that was slow to open. Called as the screen
     * appears, which happens again after a pop back to it; a column already loaded is left alone.
     *
     * @param urlToken The column to read.
     */
    fun loadColumn(urlToken: String) {
        if (currentToken == urlToken && !uiState.isLoading && uiState.column != null) return
        read(urlToken)
    }

    /** Reads the column again after a failure — the retry the error state offers. */
    fun retry() {
        currentToken?.let(::read)
    }

    /** Reads the page after the last, or does nothing at the end or while one is in flight. */
    fun loadMore() {
        val urlToken = currentToken ?: return
        if (uiState.isEnd || uiState.isNextLoading) return
        readItems(urlToken, firstPage = false)
    }

    /**
     * Optimistically toggles the column's follow state, rolling back on API failure.
     */
    fun toggleFollow() {
        val column = uiState.column ?: return
        if (column.urlToken.isBlank()) return
        if (followJob?.isActive == true) return

        val target = !column.isFollowing
        uiState = uiState.copy(column = column.copy(isFollowing = target))

        followJob = viewModelScope.launch {
            val result = if (target) {
                repository.follow(column.urlToken)
            } else {
                repository.unfollow(column.urlToken)
            }

            result.onApiFailure { error ->
                uiState = uiState.copy(column = uiState.column?.copy(isFollowing = !target))
                actionError = error
            }
        }
    }

    /**
     * Optimistically toggles the author's follow state, rolling back on API failure.
     */
    fun toggleAuthorFollow() {
        val column = uiState.column ?: return
        val author = column.author
        if (author.id.isEmpty()) return
        if (authorFollowJob?.isActive == true) return

        val target = !author.isFollowing
        uiState = uiState.copy(column = column.copy(author = author.copy(isFollowing = target)))

        authorFollowJob = viewModelScope.launch {
            val result = if (target) {
                userRepository.followUser(author.id)
            } else {
                userRepository.unfollowUser(author.id)
            }

            result.onApiFailure { error ->
                val current = uiState.column ?: return@onApiFailure
                uiState = uiState.copy(
                    column = current.copy(author = current.author.copy(isFollowing = !target)),
                )
                actionError = error
            }
        }
    }

    private fun read(urlToken: String) {
        currentToken = urlToken
        nextOffset = 0
        uiState = ColumnUiState(isLoading = true)

        viewModelScope.launch {
            repository.detail(urlToken)
                .onSuccess { column ->
                    uiState = uiState.copy(column = column, isLoading = false)
                }
                .onApiFailure { error ->
                    uiState = uiState.copy(error = error, isLoading = false)
                }
        }

        readItems(urlToken, firstPage = true)
    }

    /**
     * Reads one page of articles, counting what comes back into the offset.
     *
     * The offset advances by what was read rather than by what was asked for, so a page the server
     * shortened does not make the next one skip the articles it left out.
     */
    private fun readItems(urlToken: String, firstPage: Boolean) {
        if (uiState.isNextLoading) return

        viewModelScope.launch {
            uiState = uiState.copy(isNextLoading = true, loadMoreError = null)

            repository.items(urlToken, nextOffset)
                .onSuccess { result ->
                    nextOffset += result.items.size
                    uiState = uiState.copy(
                        items = if (firstPage) result.items else uiState.items + result.items,
                        isNextLoading = false,
                        isEnd = result.isEnd,
                    )
                }
                .onApiFailure { error ->
                    uiState = uiState.copy(loadMoreError = error, isNextLoading = false)
                }
        }
    }
}
