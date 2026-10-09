package com.prslc.zhiflow.ui.page.feed

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prslc.zhiflow.core.exception.ApiException
import com.prslc.zhiflow.core.exception.onApiFailure
import com.prslc.zhiflow.data.dto.FeedDto
import com.prslc.zhiflow.data.dto.FeedbackAction
import com.prslc.zhiflow.data.repository.FeedRepository
import com.prslc.zhiflow.data.repository.FeedbackRepository
import kotlinx.coroutines.launch

class FeedViewModel(
    private val repository: FeedRepository,
    private val feedbackRepository: FeedbackRepository,
) : ViewModel() {

    @Immutable
    data class FeedUiState(
        val items: List<FeedDto> = emptyList(),
        val isRefreshing: Boolean = false,
        val isNextLoading: Boolean = false,
        val isEnd: Boolean = false,
        val globalError: ApiException? = null,
        val loadMoreError: ApiException? = null,
    )

    @Immutable
    data class FeedbackUiState(
        val isVisible: Boolean = false,
        val actions: List<FeedbackAction> = emptyList(),
        val isLoading: Boolean = false,
        val error: ApiException? = null,
        /** The author of the card the panel was opened on, where the feed still holds it. */
        val authorName: String? = null,
    )

    var uiState by mutableStateOf(FeedUiState())
        private set

    var feedbackState by mutableStateOf(FeedbackUiState())
        private set

    /** The row whose request has just gone through, until the screen has said so. */
    var feedbackToast by mutableStateOf<FeedbackAction.Request?>(null)
        private set

    var feedbackError by mutableStateOf<ApiException?>(null)
        private set

    private var feedbackTarget: FeedbackTarget? = null

    val listState = LazyListState()
    private var nextPageUrl: String? = null
    private var previousPageUrl: String? = null

    /** Load the first page if the list is currently empty. */
    fun loadIfEmpty() {
        if (uiState.items.isEmpty()) {
            load(cold = true)
        }
    }

    /**
     * Pull to refresh: asks for the page above the one on screen and replaces the list with it.
     *
     * Sets [FeedUiState.globalError] on failure.
     */
    fun refresh() {
        load(cold = false)
    }

    /**
     * Requests one page and replaces the list with it.
     *
     * A cold load asks for a new session's first page; a refresh asks through the cursor the last
     * response carried, which the server reads as "the content above what you hold".
     *
     * Sets [FeedUiState.globalError] on failure.
     *
     * @param cold True for the load the screen makes on its own, false for a gesture.
     */
    private fun load(cold: Boolean) {
        if (uiState.isRefreshing) return
        viewModelScope.launch {
            uiState = uiState.copy(isRefreshing = true, globalError = null)

            repository.getFeeds(
                isColdStart = cold,
                nextUrl = if (cold) null else previousPageUrl,
            )
                .onSuccess { result ->
                    nextPageUrl = result.nextPageUrl
                    previousPageUrl = result.previousPageUrl
                    uiState = uiState.copy(
                        // content_id keys the list; Compose throws when a key repeats.
                        items = result.items.distinctBy { it.id },
                        isEnd = result.isEnd,
                        isRefreshing = false,
                        loadMoreError = null,
                    )
                }
                .onApiFailure { error ->
                    uiState = uiState.copy(globalError = error, isRefreshing = false)
                }
        }
    }

    /**
     * Load the next page of feeds.
     *
     * Appends results to the existing list. Sets [FeedUiState.loadMoreError] on failure.
     * No-op when [nextPageUrl] is null or the server has said the feed ends here.
     */
    fun loadMore() {
        if (uiState.isNextLoading || uiState.isRefreshing) return
        if (nextPageUrl == null || uiState.isEnd) return

        viewModelScope.launch {
            uiState = uiState.copy(isNextLoading = true, loadMoreError = null)

            repository.getFeeds(isColdStart = false, nextUrl = nextPageUrl)
                .onSuccess { result ->
                    nextPageUrl = result.nextPageUrl
                    previousPageUrl = result.previousPageUrl
                    // Same key, and here the repeat crosses the page boundary.
                    val current = uiState.items
                    val known = current.mapTo(HashSet()) { it.id }
                    uiState = uiState.copy(
                        items = current + result.items.filter { known.add(it.id) },
                        isEnd = result.isEnd,
                        isNextLoading = false,
                    )
                }
                .onApiFailure { error ->
                    uiState = uiState.copy(loadMoreError = error, isNextLoading = false)
                }
        }
    }

    /**
     * Fetches the negative feedback panel for a feed card and opens it.
     *
     * @param id The content token carried by the card.
     * @param type The card's content type as the feed spells it (`answer`, `article`, …).
     */
    fun openFeedback(id: String, type: String) {
        feedbackTarget = FeedbackTarget(id, type)
        feedbackState = FeedbackUiState(
            isVisible = true,
            isLoading = true,
            authorName = uiState.items.find { it.id == id }?.authorName,
        )
        loadPanel(id, type)
    }

    /** Hides the panel; its rows stay until the next [openFeedback] resets them. */
    fun dismissFeedback() {
        feedbackTarget = null
        feedbackState = feedbackState.copy(isVisible = false)
    }

    /** Re-fetches the panel for the card it was opened on. */
    fun retryFeedback() {
        val target = feedbackTarget ?: return
        feedbackState = feedbackState.copy(isLoading = true, error = null)
        loadPanel(target.id, target.type)
    }

    private fun loadPanel(id: String, type: String) {
        viewModelScope.launch {
            feedbackRepository.getPanel(id, type)
                .onSuccess { actions ->
                    // A response that outlived its panel must not reopen it.
                    if (feedbackTarget?.id != id) return@onSuccess
                    feedbackState = feedbackState.copy(actions = actions, isLoading = false)
                }
                .onApiFailure { error ->
                    if (feedbackTarget?.id != id) return@onApiFailure
                    feedbackState = feedbackState.copy(error = error, isLoading = false)
                }
        }
    }

    /**
     * Runs the row's request, then drops the card the panel was opened on.
     *
     * The panel closes straight away. Sets [feedbackToast] from the row's `toast_text` on
     * success and [feedbackError] on failure.
     *
     * @param action A panel row whose action is a request rather than a link.
     */
    fun submitFeedback(action: FeedbackAction.Request) {
        val target = feedbackTarget ?: return
        dismissFeedback()

        viewModelScope.launch {
            feedbackRepository.submit(action.url, action.method)
                .onSuccess {
                    feedbackToast = action
                    removeContent(target.id)
                }
                .onApiFailure { error -> feedbackError = error }
        }
    }

    /**
     * Drops a card whose content the backend has just been told to stop showing.
     *
     * @param id The content token carried by the card.
     */
    fun removeContent(id: String) {
        uiState = uiState.copy(items = uiState.items.filterNot { it.id == id })
    }

    /** Clears [feedbackToast] once it has been shown. */
    fun consumeFeedbackToast() {
        feedbackToast = null
    }

    /** Clears [feedbackError] once it has been shown. */
    fun consumeFeedbackError() {
        feedbackError = null
    }

    private data class FeedbackTarget(val id: String, val type: String)
}
