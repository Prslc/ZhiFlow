package com.prslc.zhiflow.ui.page.notification

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prslc.zhiflow.core.exception.ApiException
import com.prslc.zhiflow.core.exception.onApiFailure
import com.prslc.zhiflow.data.dto.PersonDto
import com.prslc.zhiflow.data.repository.SearchRepository
import com.prslc.zhiflow.data.repository.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/** How long the box waits after the last keystroke before asking the server who was named. */
private val SEARCH_DEBOUNCE_MS = 300.milliseconds

class ChatInviteViewModel(
    private val searchRepository: SearchRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    @Immutable
    data class ChatInviteUiState(
        val followees: List<PersonDto> = emptyList(),
        val found: List<PersonDto> = emptyList(),
        val isLoading: Boolean = false,
        val isSearching: Boolean = false,
        val isNextLoading: Boolean = false,
        val error: ApiException? = null,
        val loadMoreError: ApiException? = null,
    )

    var uiState by mutableStateOf(ChatInviteUiState())
        private set

    /** What is in the box right now; blank shows the people the reader already follows. */
    var query by mutableStateOf("")
        private set

    val listState = LazyListState()

    private var searchJob: Job? = null
    private var nextPageUrl: String? = null

    /**
     * Loads the people the reader follows, which is what the page opens on.
     *
     * Called once when the page appears; the page size is the server's own.
     */
    fun loadFollowees() {
        if (uiState.isLoading || uiState.followees.isNotEmpty()) return

        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)

            userRepository.followees()
                .onSuccess { result ->
                    nextPageUrl = result.nextPageUrl
                    uiState = uiState.copy(followees = result.people, isLoading = false)
                }
                .onApiFailure { error ->
                    uiState = uiState.copy(error = error, isLoading = false)
                }
        }
    }

    /** Loads the next page of the people the reader follows. No-op at the end, mid-load, or none. */
    fun loadMore() {
        val url = nextPageUrl ?: return
        if (uiState.isNextLoading || uiState.isLoading) return

        viewModelScope.launch {
            uiState = uiState.copy(isNextLoading = true, loadMoreError = null)

            userRepository.followees(url)
                .onSuccess { result ->
                    nextPageUrl = result.nextPageUrl
                    uiState = uiState.copy(
                        followees = uiState.followees + result.people,
                        isNextLoading = false,
                    )
                }
                .onApiFailure { error ->
                    uiState = uiState.copy(loadMoreError = error, isNextLoading = false)
                }
        }
    }

    /**
     * Takes what the reader has typed, and asks who it names.
     *
     * Each keystroke replaces the one before it, so the request goes out only once they pause; a
     * blank box drops what was found and shows the reader's own list again rather than asking who
     * nobody is.
     *
     * @param value The box's new contents.
     */
    fun onQueryChange(value: String) {
        query = value
        searchJob?.cancel()

        if (value.isBlank()) {
            uiState = uiState.copy(
                found = emptyList(),
                isSearching = false,
                loadMoreError = null,
            )
            return
        }

        search(value)
    }

    /** Asks again for what is in the box, which is what a failed search needs. */
    fun retrySearch() {
        searchJob?.cancel()
        search(query)
    }

    private fun search(value: String) {
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            uiState = uiState.copy(isSearching = true, error = null)

            searchRepository.people(value)
                .onSuccess { people ->
                    uiState = uiState.copy(found = people, isSearching = false)
                }
                .onApiFailure { error ->
                    uiState = uiState.copy(error = error, isSearching = false)
                }
        }
    }
}
