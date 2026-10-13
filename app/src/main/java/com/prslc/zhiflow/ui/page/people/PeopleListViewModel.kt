package com.prslc.zhiflow.ui.page.people

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prslc.zhiflow.core.exception.ApiException
import com.prslc.zhiflow.core.exception.onApiFailure
import com.prslc.zhiflow.core.utils.compose.withPage
import com.prslc.zhiflow.data.dto.PersonDto
import com.prslc.zhiflow.data.model.user.PeopleListKind
import com.prslc.zhiflow.data.repository.UserRepository
import kotlinx.coroutines.launch

/** One of an account's two lists of people, as a page of its own. */
class PeopleListViewModel(
    private val repository: UserRepository,
) : ViewModel() {

    @Immutable
    data class PeopleListUiState(
        val people: List<PersonDto> = emptyList(),
        val isRefreshing: Boolean = false,
        val isNextLoading: Boolean = false,
        val isEnd: Boolean = false,
        val globalError: ApiException? = null,
        val loadMoreError: ApiException? = null,
    )

    var uiState by mutableStateOf(PeopleListUiState(isRefreshing = true))
        private set

    val listState = LazyListState()

    private var nextPageUrl: String? = null

    /** Which list, of whose account, the state above holds — a page reads one and only one. */
    private var loadedUserId: String? = null
    private var loadedKind: PeopleListKind? = null

    /**
     * Reads the first page, unless the page already holds the list that was asked for.
     *
     * Called as the screen appears, which happens again after a pop back to it; the list is only
     * read the once, so what the reader scrolled to survives.
     *
     * @param userId Whose lists to read: the account's hash id.
     * @param kind Which of the two lists.
     */
    fun loadIfEmpty(userId: String, kind: PeopleListKind) {
        if (loadedUserId == userId && loadedKind == kind && uiState.people.isNotEmpty()) return
        loadedUserId = userId
        loadedKind = kind
        read(userId = userId, kind = kind, nextUrl = null)
    }

    /** Reads the page after the last, or does nothing at the end or while one is in flight. */
    fun loadMore() {
        val url = nextPageUrl ?: return
        if (uiState.isNextLoading || uiState.isRefreshing || uiState.isEnd) return
        read(userId = loadedUserId.orEmpty(), kind = loadedKind ?: return, nextUrl = url)
    }

    /** Reads the first page again, replacing what the page holds. */
    fun refresh() {
        val userId = loadedUserId ?: return
        val kind = loadedKind ?: return
        if (uiState.isRefreshing) return
        read(userId = userId, kind = kind, nextUrl = null)
    }

    private fun read(userId: String, kind: PeopleListKind, nextUrl: String?) {
        val isFirstPage = nextUrl == null

        viewModelScope.launch {
            uiState = if (isFirstPage) {
                uiState.copy(isRefreshing = true, globalError = null, loadMoreError = null)
            } else {
                uiState.copy(isNextLoading = true, loadMoreError = null)
            }

            repository.people(kind, userId, nextUrl)
                .onSuccess { result ->
                    nextPageUrl = result.nextPageUrl
                    uiState = uiState.copy(
                        people = uiState.people.withPage(result.people, firstPage = isFirstPage) {
                            it.id
                        },
                        isRefreshing = false,
                        isNextLoading = false,
                        isEnd = result.isEnd,
                    )
                }
                .onApiFailure { error ->
                    uiState = if (isFirstPage) {
                        uiState.copy(globalError = error, isRefreshing = false)
                    } else {
                        uiState.copy(loadMoreError = error, isNextLoading = false)
                    }
                }
        }
    }
}
