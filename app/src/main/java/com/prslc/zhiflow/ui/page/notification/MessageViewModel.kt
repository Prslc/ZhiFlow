package com.prslc.zhiflow.ui.page.notification

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prslc.zhiflow.core.exception.ApiException
import com.prslc.zhiflow.core.exception.ignoreOutcome
import com.prslc.zhiflow.core.exception.onApiFailure
import com.prslc.zhiflow.core.utils.compose.withPage
import com.prslc.zhiflow.data.dto.MessageThreadDto
import com.prslc.zhiflow.data.dto.NotificationCategoryDto
import com.prslc.zhiflow.data.dto.UnreadSummaryDto
import com.prslc.zhiflow.data.repository.NotificationRepository
import kotlinx.coroutines.launch

class MessageViewModel(
    private val repository: NotificationRepository,
) : ViewModel() {

    @Immutable
    data class MessageUiState(
        val categories: List<NotificationCategoryDto> = emptyList(),
        val threads: List<MessageThreadDto> = emptyList(),
        val unread: UnreadSummaryDto? = null,
        val isRefreshing: Boolean = false,
        val isNextLoading: Boolean = false,
        val isMarkingRead: Boolean = false,
        val globalError: ApiException? = null,
        val loadMoreError: ApiException? = null,
    )

    var uiState by mutableStateOf(MessageUiState())
        private set

    val listState = LazyListState()

    /** Set once a mark-all-read has been accepted, for the page to say so and clear it. */
    var markedAllRead by mutableStateOf(false)
        private set

    /** A mark-all-read the server refused, for the page to say out loud. */
    var actionError by mutableStateOf<ApiException?>(null)
        private set

    private var nextPageUrl: String? = null

    /**
     * Loads the page when nothing has been loaded yet.
     *
     * App start calls this, because the count the tab's badge draws arrives with the page and the
     * badge is on screen before the tab ever is.
     */
    fun loadIfEmpty() {
        if (uiState.unread != null) return
        fetch(reportVisit = false)
    }

    /**
     * Loads the page again, replacing what is on screen.
     *
     * Called when the tab is opened and when the reader pulls the page down. It reports the visit
     * with it, which is how the server counts the tab being opened.
     */
    fun refresh() = fetch(reportVisit = true)

    /** Loads the next page of conversations. No-op at the end, mid-load, or with none. */
    fun loadMore() {
        val url = nextPageUrl ?: return
        if (uiState.isNextLoading || uiState.isRefreshing) return

        viewModelScope.launch {
            uiState = uiState.copy(isNextLoading = true, loadMoreError = null)

            repository.messagePage(url)
                .onSuccess { result ->
                    nextPageUrl = result.nextPageUrl
                    uiState = uiState.copy(
                        threads = uiState.threads.withPage(result.threads, firstPage = false) {
                            it.threadKey
                        },
                        isNextLoading = false,
                    )
                }
                .onApiFailure { error ->
                    uiState = uiState.copy(loadMoreError = error, isNextLoading = false)
                }
        }
    }

    /**
     * Marks everything unread as read, which is what the page's own action asks of the server.
     *
     * The page is fetched again once it has been taken: the counts move, the rows stop reading as
     * unread, and the tab's badge is drawn from the same counts.
     */
    fun markAllRead() {
        if (uiState.isMarkingRead) return

        viewModelScope.launch {
            uiState = uiState.copy(isMarkingRead = true)

            repository.markAllMessagesRead()
                .onSuccess {
                    uiState = uiState.copy(isMarkingRead = false)
                    markedAllRead = true
                    fetch(reportVisit = false)
                }
                .onApiFailure { error ->
                    uiState = uiState.copy(isMarkingRead = false)
                    actionError = error
                }
        }
    }

    /** Clears the acceptance the page has said out loud. */
    fun consumeMarkedAllRead() {
        markedAllRead = false
    }

    /** Clears the failure the page has said out loud. */
    fun consumeActionError() {
        actionError = null
    }

    private fun fetch(reportVisit: Boolean) {
        if (uiState.isRefreshing) return

        viewModelScope.launch {
            uiState = uiState.copy(isRefreshing = true, globalError = null)

            if (reportVisit) repository.reportTabAccess().ignoreOutcome()

            repository.messagePage()
                .onSuccess { result ->
                    nextPageUrl = result.nextPageUrl
                    uiState = uiState.copy(
                        categories = result.categories,
                        threads = result.threads.distinctBy { it.threadKey },
                        unread = result.unread,
                        isRefreshing = false,
                        loadMoreError = null,
                    )
                }
                .onApiFailure { error ->
                    uiState = uiState.copy(globalError = error, isRefreshing = false)
                }
        }
    }
}

/**
 * The identity a conversation row is drawn under.
 *
 * A card the server left without an id is named by when it arrived and where it points; nothing
 * here assumes the server always sends one.
 */
internal val MessageThreadDto.threadKey: String
    get() = id.ifBlank { "${createdTime}_$targetLink" }
