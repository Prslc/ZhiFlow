package com.prslc.zhiflow.ui.page.feed

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prslc.zhiflow.core.exception.ApiException
import com.prslc.zhiflow.data.mapper.FeedDisplay
import com.prslc.zhiflow.data.repository.FeedRepository
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class FeedViewModel(private val repository: FeedRepository) : ViewModel() {

    @Immutable
    data class FeedUiState(
        val items: List<FeedDisplay> = emptyList(),
        val isRefreshing: Boolean = false,
        val isNextLoading: Boolean = false,
        val globalError: ApiException? = null,
        val loadMoreError: ApiException? = null,
    )

    var uiState by mutableStateOf(FeedUiState())
        private set

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
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    uiState = uiState.copy(globalError = e as? ApiException, isRefreshing = false)
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
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    uiState = uiState.copy(loadMoreError = e as? ApiException, isNextLoading = false)
                }
        }
    }
}
