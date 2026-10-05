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
        val globalError: ApiException? = null,
        val loadMoreError: ApiException? = null,
    )

    @Immutable
    data class FeedbackUiState(
        val isVisible: Boolean = false,
        val actions: List<FeedbackAction> = emptyList(),
        val isLoading: Boolean = false,
        val error: ApiException? = null,
    )

    var uiState by mutableStateOf(FeedUiState())
        private set

    var feedbackState by mutableStateOf(FeedbackUiState())
        private set

    var feedbackToast by mutableStateOf<String?>(null)
        private set

    var feedbackError by mutableStateOf<ApiException?>(null)
        private set

    private var feedbackTarget: FeedbackTarget? = null

    val listState = LazyListState()
    private var nextPageUrl: String? = null

    /** Load feeds if the list is currently empty. */
    fun loadIfEmpty() {
        if (uiState.items.isEmpty()) {
            refresh()
        }
    }

    /**
     * Force refresh: clear existing items and reload from page 1.
     *
     * Sets [FeedUiState.globalError] on failure.
     */
    fun refresh() {
        if (uiState.isRefreshing) return
        viewModelScope.launch {
            uiState = uiState.copy(isRefreshing = true, globalError = null)

            repository.getFeeds(isRefresh = true, nextUrl = null)
                .onSuccess { result ->
                    nextPageUrl = result.nextPageUrl
                    uiState = uiState.copy(
                        items = result.items,
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
     * No-op when [nextPageUrl] is null (all pages consumed).
     */
    fun loadMore() {
        if (uiState.isNextLoading || uiState.isRefreshing || nextPageUrl == null) return

        viewModelScope.launch {
            uiState = uiState.copy(isNextLoading = true, loadMoreError = null)

            repository.getFeeds(isRefresh = false, nextUrl = nextPageUrl)
                .onSuccess { result ->
                    nextPageUrl = result.nextPageUrl
                    uiState = uiState.copy(
                        items = uiState.items + result.items,
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
        feedbackState = FeedbackUiState(isVisible = true, isLoading = true)
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
                    feedbackToast = action.toastText
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
